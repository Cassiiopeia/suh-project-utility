# 소만사 버스 자동예약 스케줄러 — Quartz 제거 및 DB 기반 폴러 전환 설계

## 배경

기존 Quartz 기반 `SomansaBusAutoReservationJob` 은 in-memory JobStore 사용으로 재배포 시 Trigger 휘발. 또한 `SomansaBusQuartzConfig` 의 첫 Trigger 가 항상 "내일 22~23시 random" 으로 설정되어 앱 기동 첫날 발화하지 못함. 이로 인해 자동 예약이 사실상 동작하지 않거나 누락되는 문제 발생.

## 목표

- Quartz 의존성·코드 완전 제거
- 다음 발화 시각을 DB 에 영속화하여 재배포 무관 동작 보장
- 폴링 주기는 느슨하게 (10분), 긴급도 낮은 작업 특성 반영
- 누락 발화는 당일 자정까지 복구 (그 이후는 의미 없으므로 skip)
- 다음 발화 시각은 ServerOption 설정의 허용 요일 중 가장 가까운 날로 계산

## 비목표

- `daysAhead` 복원 (별도 이슈)
- 멀티 인스턴스 분산 락 (단일 서버 가정)
- Quartz JDBC JobStore 도입

## 결정 사항

| 항목 | 결정 |
|------|------|
| Q1 폴링 주기 | 10분 (`fixedDelay = 600_000`) |
| Q2 누락 발화 컷오프 | 당일 자정 — `nextFireAt` 의 같은 날짜 안에 폴링하면 복구, 다음날 넘어가면 skip |
| Q3 nextFireAt 초기화 | 다음 허용 요일의 윈도우 — 요일 설정도 반영하여 가장 가까운 허용 요일 22~23시 random 시각 |
| Q4 daysAhead | 현 상태 (`int daysAhead = 1` 하드코딩) 유지, 별도 이슈로 분리 |
| Q5 Quartz 의존성 | `Suh-Common/build.gradle` 에서 `spring-boot-starter-quartz` 완전 제거 |
| Q6 @EnableScheduling 위치 | `Suh-Web/src/main/java/me/suhsaechan/web/config/SchedulingConfig.java` 신규 |
| state row 식별 | 고정 UUID 상수로 단일 row 보장 |
| 테스트 방식 | `@SpringBootTest` 통합 테스트, mock 없음, `mainTest` 단일 메서드 + `timeLog` 순차 실행 |

## 아키텍처

```
[@Scheduled 폴러]
   ↓ 10분마다
[SomansaBusSchedulerPoller (Web 모듈)]
   ↓ try-catch 로 예외 격리
[SomansaBusSchedulerService.tick() (Bus 도메인)]
   ├─ state 조회/없으면 자동 생성
   ├─ enabled 체크
   ├─ now < nextFireAt → 무동작 return
   ├─ 컷오프 초과 → 발화 skip + nextFireAt 만 갱신
   ├─ 내일 비허용 요일 → 발화 skip + nextFireAt 갱신
   └─ 정상 발화 → reservationService.scheduledAutoReservation() + lastFiredAt + nextFireAt 갱신
   ↓
[SomansaBusSchedulerStateRepository]
   ↓
[somansa_bus_scheduler_state 테이블 (단일 row)]
```

**책임 분리:**
- Poller (Web 모듈) — `@Scheduled` 트리거 + 예외 로깅. 비즈니스 로직 없음
- Service (Bus 도메인) — 상태 관리, 시각 계산, 발화 결정, reservation 호출
- State (DB 단일 row) — `nextFireAt`, `lastFiredAt` 영속화

## 데이터 모델

### 테이블: `somansa_bus_scheduler_state`

```sql
CREATE TABLE IF NOT EXISTS somansa_bus_scheduler_state (
  somansa_bus_scheduler_state_id UUID PRIMARY KEY,
  next_fire_at TIMESTAMP NOT NULL,
  last_fired_at TIMESTAMP,
  created_date TIMESTAMP NOT NULL,
  updated_date TIMESTAMP NOT NULL
);
```

- `CREATE TABLE IF NOT EXISTS` — 초기 상태 안전
- 초기 row INSERT 안 함 — Service 의 `tick()` 첫 호출 시 자동 생성

### 엔티티: `SomansaBusSchedulerState`

```java
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class SomansaBusSchedulerState extends BasePostgresEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID somansaBusSchedulerStateId;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Column(nullable = false)
  private LocalDateTime nextFireAt;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Column
  private LocalDateTime lastFiredAt;
}
```

**싱글톤 처리:**
- Service 에 `static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001")` 상수 박아넣음
- `repository.findById(SINGLETON_ID)` 로 항상 같은 row 조회/저장
- ID 자동 생성 안 함 (수동 지정)

## 폴러 로직

### `SomansaBusSchedulerPoller` (Web 모듈)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class SomansaBusSchedulerPoller {

  private final SomansaBusSchedulerService schedulerService;

  @Scheduled(fixedDelay = 600_000)  // 10분
  public void poll() {
    try {
      schedulerService.tick();
    } catch (Exception e) {
      log.error("스케줄러 폴링 중 예외 발생 — 다음 폴링까지 대기", e);
    }
  }
}
```

### `SomansaBusSchedulerService.tick()` (의사코드)

```
tick():  // 비트랜잭션
  state ← loadOrInitState()  // 내부 @Transactional, 없으면 생성

  if (!isEnabled())  // SOMANSA_BUS_SCHEDULER_ENABLED != "true"
    log.info("스케줄러 비활성화 — skip")
    return

  now ← LocalDateTime.now(Asia/Seoul)
  if (now < state.nextFireAt)
    return  // 아직 발화 시각 안 됨

  // 컷오프 체크: 당일 자정 기준
  cutoff ← state.nextFireAt.toLocalDate().atTime(23:59:59)
  if (now > cutoff):
    log.warn("발화 시각 컷오프 초과 — skip, nextFireAt 만 갱신")
    updateNextFireAtOnly(state, now)
    return

  // 내일 요일 체크
  tomorrow ← LocalDate.now() + 1
  allowedDays ← parseDays(ServerOption.SOMANSA_BUS_SCHEDULER_DAYS)
  if (!allowedDays.contains(tomorrow.dayOfWeek)):
    log.info("내일({}) 비허용 요일 — 예약 skip, nextFireAt 만 갱신", tomorrow.dayOfWeek)
    updateNextFireAtOnly(state, now)
    return

  // 정상 발화
  invokeReservationSafely()  // 자체 @Transactional + 예외 삼킴
  markFired(state, now)  // 내부 @Transactional, lastFiredAt + nextFireAt 갱신
```

### `computeNextFireAt(referenceTime)` 로직 — Q3 반영

```
fromHour ← ServerOption.SOMANSA_BUS_SCHEDULER_TIME_FROM (기본 22)
toHour ← ServerOption.SOMANSA_BUS_SCHEDULER_TIME_TO (기본 23)
allowedDays ← parseDays(ServerOption.SOMANSA_BUS_SCHEDULER_DAYS) (기본 MON~FRI)

if (fromHour > toHour) toHour = fromHour
rangeMinutes = (toHour - fromHour) * 60 + 59

candidateDate = referenceTime.toLocalDate()
windowEnd = candidateDate.atTime(toHour, 59)

// 오늘 윈도우 이미 지났으면 다음날부터 탐색
if (referenceTime > windowEnd):
  candidateDate = candidateDate.plusDays(1)

// 허용 요일까지 전진
maxIter = 8  // 안전 가드
while (!allowedDays.contains(candidateDate.dayOfWeek) && maxIter-- > 0):
  candidateDate = candidateDate.plusDays(1)

if (maxIter <= 0):
  log.error("허용 요일 설정이 비어있음 — fallback: 다음날 사용")
  candidateDate = referenceTime.toLocalDate().plusDays(1)

random = ThreadLocalRandom.nextInt(rangeMinutes + 1)
nextTime = LocalTime.of(fromHour, 0).plusMinutes(random)

return candidateDate.atTime(nextTime)
```

## 예외 처리

### 원칙

1. **폴러 한 번 실패 ≠ 영구 정지** — 모든 예외 catch, 다음 폴링 보장
2. **state UPDATE 보장** — 발화 실패해도 `nextFireAt` 반드시 갱신 (무한 재시도 방지)
3. **부분 실패 격리** — reservation 실행 실패가 state 갱신 막지 못함
4. **트랜잭션 격리** — reservation 트랜잭션과 state 트랜잭션 분리. `tick()` 자체는 비트랜잭션, 내부 DB 접근 메서드만 `@Transactional`

### 트랜잭션 경계

| 메서드 | 트랜잭션 | 이유 |
|--------|----------|------|
| `tick()` | 비트랜잭션 | 외부 조율자 |
| `loadOrInitState()` | `@Transactional` | row 조회/생성, 반환은 detached |
| `updateNextFireAtOnly(UUID, LocalDateTime)` | `@Transactional` | nextFireAt 만 UPDATE |
| `markFired(UUID, LocalDateTime)` | `@Transactional` | nextFireAt + lastFiredAt UPDATE |
| `invokeReservationSafely()` | 비트랜잭션 + 내부 catch | reservationService 자체 @Transactional 사용 |
| `reservationService.scheduledAutoReservation()` | 기존 `@Transactional` 유지 | 변경 없음 |

**detached 처리:** `tick()` 비트랜잭션에서 `loadOrInitState()` 반환 객체는 detached 상태. 갱신 메서드는 객체 전달 대신 ID + 새 값 전달 → 갱신 메서드 내부에서 다시 `findById` 후 setter 적용. dirty checking 정상 동작.

### 예외 케이스 매트릭스

| 케이스 | 처리 |
|--------|------|
| ServerOption 키 미존재 | `serverOptionService.getOption` 이 enum defaultValue 반환 (기존 동작) |
| `TIME_FROM`/`TIME_TO` 비숫자 | `NumberFormatException` catch → 기본값 22/23 사용 + warn 로그 |
| `DAYS` 파싱 실패/빈값 | computeNextFireAt 의 maxIter 가드로 fallback (다음날 사용) + error 로그 |
| state row 중복 | `findById(SINGLETON_ID)` 사용하므로 항상 같은 row. 다른 row 무시 |
| DB 연결 끊김 | `tick()` 전체 예외 → poller catch → 다음 폴링 재시도 |
| reservation 호출 예외 | `invokeReservationSafely` 내부 catch, lastFiredAt 미갱신, nextFireAt 은 정상 갱신 |
| 시스템 시각 오류 | 별도 처리 안 함, 시스템 시각 신뢰 |

### 로깅 규칙

- `log.info` — 정상 흐름 (발화 완료, skip 사유, 비활성화)
- `log.warn` — 컷오프 초과, 파싱 실패 같은 회복 가능 이상
- `log.error` — 예외 + stack trace, 허용 요일 fallback

## 파일 변경 목록

### 신규

1. `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/entity/SomansaBusSchedulerState.java`
2. `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/repository/SomansaBusSchedulerStateRepository.java`
3. `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusSchedulerService.java`
4. `Suh-Domain-Somansa-Bus/src/test/java/me/suhsaechan/somansabus/service/SomansaBusSchedulerServiceTest.java`
5. `Suh-Web/src/main/java/me/suhsaechan/web/scheduler/SomansaBusSchedulerPoller.java`
6. `Suh-Web/src/main/java/me/suhsaechan/web/config/SchedulingConfig.java` (`@EnableScheduling`)
7. `Suh-Web/src/main/resources/db/migration/V2_5_35__create_somansa_bus_scheduler_state.sql`

### 삭제

1. `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/job/SomansaBusAutoReservationJob.java`
2. `Suh-Web/src/main/java/me/suhsaechan/web/config/SomansaBusQuartzConfig.java`
3. `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/job/` 폴더 (비면)

### 수정

1. `Suh-Common/build.gradle` — `spring-boot-starter-quartz` 라인 제거

### 그대로 유지

- `SomansaBusReservationService.scheduledAutoReservation()` — 진입점 그대로
- `ServerOptionKey` 의 `SOMANSA_BUS_SCHEDULER_*` 4개 키
- `SomansaBusSchedule` 엔티티
- `version.yml` — 마이그레이션 파일명에 현재 `2.5.35` 재활용

## 마이그레이션 SQL

`V2_5_35__create_somansa_bus_scheduler_state.sql`:

```sql
CREATE TABLE IF NOT EXISTS somansa_bus_scheduler_state (
  somansa_bus_scheduler_state_id UUID PRIMARY KEY,
  next_fire_at TIMESTAMP NOT NULL,
  last_fired_at TIMESTAMP,
  created_date TIMESTAMP NOT NULL,
  updated_date TIMESTAMP NOT NULL
);
```

- `CREATE TABLE IF NOT EXISTS` 로 초기 DB 안전 보장
- 초기 row INSERT 없음 — 첫 `tick()` 호출 시 service 가 자동 생성
- Quartz `qrtz_*` 테이블 DROP 불필요 (JDBC JobStore 미사용으로 존재하지 않음)

## 테스트 전략

### 파일

`Suh-Domain-Somansa-Bus/src/test/java/me/suhsaechan/somansabus/service/SomansaBusSchedulerServiceTest.java`

### 패턴

- `@SpringBootTest(classes = SuhProjectUtilityApplication.class)` + `@ActiveProfiles("dev")`
- `@MockBean` 사용 안 함 — 실제 빈 그대로 통합 테스트
- `mainTest` 단일 `@Test` 메서드, suh-logger `lineLog` / `timeLog` 로 케이스 순차 실행
- `@AfterEach` cleanup 없음 — `findById(SINGLETON_ID)` 라 row 누적 안 됨
- 외부 API 호출 격리: dev DB 에 활성 SomansaBusSchedule 없으면 `scheduledAutoReservation` 의 for-loop 빈 채로 종료 → 실제 외부 API 호출 0회

### 테스트 케이스

```java
@Test
public void mainTest() {
  lineLog("테스트시작");

  lineLog(null);
  timeLog(this::tick_초기_row_생성_테스트);
  lineLog(null);
  timeLog(this::tick_미래시각_무동작_테스트);
  lineLog(null);
  timeLog(this::tick_컷오프초과_skip_테스트);
  lineLog(null);
  timeLog(this::tick_정상발화_테스트);
  lineLog(null);
  timeLog(this::computeNextFireAt_허용요일_전진_테스트);

  lineLog("테스트종료");
}
```

| 케이스 | 시나리오 | 검증 |
|--------|----------|------|
| `tick_초기_row_생성_테스트` | repo 에 row 없는 상태에서 tick() 호출 | row 1개 생성, nextFireAt 미래 |
| `tick_미래시각_무동작_테스트` | state.nextFireAt = now+1시간 → tick() | state 변경 없음 |
| `tick_컷오프초과_skip_테스트` | state.nextFireAt = 어제 22시 → tick() | lastFiredAt null 유지, nextFireAt 미래로 갱신 |
| `tick_정상발화_테스트` | state.nextFireAt = now-1분, 허용 요일 → tick() | lastFiredAt 갱신, nextFireAt 미래로 갱신 |
| `computeNextFireAt_허용요일_전진_테스트` | DAYS 변경 후 직접 호출 | 비허용 요일 입력 시 다음 허용 요일 반환 |

### 가시성

`computeNextFireAt` 메서드는 package-private 으로 노출 (`me.suhsaechan.somansabus.service` 패키지), 테스트가 같은 패키지에서 직접 호출 가능.

## 운영 관찰 포인트

- DB 직접 조회로 다음 발화 시각 즉시 확인 가능:
  ```sql
  SELECT next_fire_at, last_fired_at FROM somansa_bus_scheduler_state;
  ```
- ServerOption 런타임 변경 시 다음 폴링 (10분 이내) 부터 자동 반영
- 재배포 후 부팅 → 첫 10분 내 폴링 시 영속 nextFireAt 읽어 정상 동작 보장

## 향후 확장 여지 (이번 scope 아님)

- 멤버·노선별 `daysAhead` 별도 컬럼 복원 (Schedule 엔티티)
- 멀티 인스턴스 대응을 위한 분산 락 (`SELECT FOR UPDATE` or Redis)
- 발화 이력을 별도 로그 테이블에 추가
