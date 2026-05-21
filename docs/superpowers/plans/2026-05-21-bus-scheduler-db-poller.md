# 소만사 버스 스케줄러 DB 폴러 전환 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Quartz 기반 자동 예약 스케줄러를 제거하고 DB 영속 `nextFireAt` + Spring `@Scheduled(10분)` 폴러로 전환하여 재배포 무관 동작 및 누락 발화 복구를 보장한다.

**Architecture:** 단일 row DB 테이블 `somansa_bus_scheduler_state` 에 `nextFireAt`/`lastFiredAt` 영속. `SomansaBusSchedulerPoller` 가 10분마다 `SomansaBusSchedulerService.tick()` 호출. tick 은 비트랜잭션, 내부 DB 갱신 메서드만 `@Transactional`. 발화 실패해도 `nextFireAt` 갱신 보장.

**Tech Stack:** Spring Boot 3.4.2, Spring Data JPA, Spring `@Scheduled`, PostgreSQL, Flyway, suh-logger.

---

## File Structure

**신규:**
- `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/entity/SomansaBusSchedulerState.java`
- `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/repository/SomansaBusSchedulerStateRepository.java`
- `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusSchedulerService.java`
- `Suh-Domain-Somansa-Bus/src/test/java/me/suhsaechan/somansabus/service/SomansaBusSchedulerServiceTest.java`
- `Suh-Web/src/main/java/me/suhsaechan/web/scheduler/SomansaBusSchedulerPoller.java`
- `Suh-Web/src/main/java/me/suhsaechan/web/config/SchedulingConfig.java`
- `Suh-Web/src/main/resources/db/migration/V2_5_35__create_somansa_bus_scheduler_state.sql`

**삭제:**
- `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/job/SomansaBusAutoReservationJob.java`
- `Suh-Web/src/main/java/me/suhsaechan/web/config/SomansaBusQuartzConfig.java`

**수정:**
- `Suh-Common/build.gradle` — `spring-boot-starter-quartz` 의존성 제거

---

## Task 1: DB 마이그레이션 작성

**Files:**
- Create: `Suh-Web/src/main/resources/db/migration/V2_5_35__create_somansa_bus_scheduler_state.sql`

- [ ] **Step 1: 마이그레이션 SQL 파일 작성**

파일 내용:

```sql
CREATE TABLE IF NOT EXISTS somansa_bus_scheduler_state (
  somansa_bus_scheduler_state_id UUID PRIMARY KEY,
  next_fire_at TIMESTAMP NOT NULL,
  last_fired_at TIMESTAMP,
  created_date TIMESTAMP NOT NULL,
  updated_date TIMESTAMP NOT NULL
);
```

- [ ] **Step 2: 파일명·버전 확인**

확인:
- `version.yml` 의 version = `2.5.35` 와 파일명 prefix `V2_5_35` 일치
- 같은 prefix 의 기존 마이그레이션 파일 없음
- 명령: `ls Suh-Web/src/main/resources/db/migration/` → `V2_5_27`, `V2_5_30`, `V2_5_35` 만 존재해야 함

- [ ] **Step 3: 커밋 보류 (사용자 명시 요청 시에만 커밋)**

커밋은 모든 Task 완료 후 사용자 요청 시 일괄 수행.

---

## Task 2: Entity 작성

**Files:**
- Create: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/entity/SomansaBusSchedulerState.java`

- [ ] **Step 1: Entity 파일 작성**

파일 내용:

```java
package me.suhsaechan.somansabus.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.entity.BasePostgresEntity;

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

**주의:** `@GeneratedValue` 미적용 (싱글톤 UUID 수동 지정 위해).

---

## Task 3: Repository 작성

**Files:**
- Create: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/repository/SomansaBusSchedulerStateRepository.java`

- [ ] **Step 1: Repository 인터페이스 작성**

파일 내용:

```java
package me.suhsaechan.somansabus.repository;

import java.util.UUID;
import me.suhsaechan.somansabus.entity.SomansaBusSchedulerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SomansaBusSchedulerStateRepository
    extends JpaRepository<SomansaBusSchedulerState, UUID> {
}
```

---

## Task 4: Service 작성 — 스켈레톤 + 상수

**Files:**
- Create: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusSchedulerService.java`

- [ ] **Step 1: Service 스켈레톤 작성 (loadOrInitState 만 우선 구현)**

파일 내용:

```java
package me.suhsaechan.somansabus.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.somansabus.entity.SomansaBusSchedulerState;
import me.suhsaechan.somansabus.repository.SomansaBusSchedulerStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusSchedulerService {

  static final UUID SINGLETON_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  static final int DEFAULT_FROM_HOUR = 22;
  static final int DEFAULT_TO_HOUR = 23;

  private final SomansaBusSchedulerStateRepository repository;
  private final ServerOptionService serverOptionService;
  private final SomansaBusReservationService reservationService;

  public void tick() {
    log.info("스케줄러 tick 시작");
  }

  @Transactional
  SomansaBusSchedulerState loadOrInitState() {
    return repository.findById(SINGLETON_ID).orElseGet(() -> {
      LocalDateTime nextFireAt = computeNextFireAt(LocalDateTime.now(SEOUL));
      SomansaBusSchedulerState fresh = SomansaBusSchedulerState.builder()
          .somansaBusSchedulerStateId(SINGLETON_ID)
          .nextFireAt(nextFireAt)
          .lastFiredAt(null)
          .build();
      SomansaBusSchedulerState saved = repository.save(fresh);
      log.info("스케줄러 state row 신규 생성 - nextFireAt: {}", nextFireAt);
      return saved;
    });
  }

  LocalDateTime computeNextFireAt(LocalDateTime referenceTime) {
    int fromHour = readHour(ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_FROM, DEFAULT_FROM_HOUR);
    int toHour = readHour(ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_TO, DEFAULT_TO_HOUR);
    if (fromHour > toHour) toHour = fromHour;
    int rangeMinutes = (toHour - fromHour) * 60 + 59;

    Set<DayOfWeek> allowedDays = parseDays(
        serverOptionService.getOptionValue(ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS));

    LocalDate candidateDate = referenceTime.toLocalDate();
    LocalDateTime windowEnd = candidateDate.atTime(toHour, 59);
    if (referenceTime.isAfter(windowEnd)) {
      candidateDate = candidateDate.plusDays(1);
    }

    int guard = 8;
    while (!allowedDays.contains(candidateDate.getDayOfWeek()) && guard-- > 0) {
      candidateDate = candidateDate.plusDays(1);
    }
    if (guard <= 0) {
      log.error("허용 요일 설정이 비어있거나 잘못됨 — fallback: 다음날 사용");
      candidateDate = referenceTime.toLocalDate().plusDays(1);
    }

    int randomMinutes = ThreadLocalRandom.current().nextInt(rangeMinutes + 1);
    LocalTime nextTime = LocalTime.of(fromHour, 0).plusMinutes(randomMinutes);
    return candidateDate.atTime(nextTime);
  }

  private int readHour(ServerOptionKey key, int fallback) {
    try {
      return serverOptionService.getOptionValueAsInt(key);
    } catch (Exception e) {
      log.warn("ServerOption {} 읽기 실패, 기본값 {} 사용", key, fallback, e);
      return fallback;
    }
  }

  Set<DayOfWeek> parseDays(String daysStr) {
    Set<DayOfWeek> days = new HashSet<>();
    if (daysStr == null || daysStr.isBlank()) return days;
    for (String d : daysStr.split(",")) {
      try {
        days.add(DayOfWeek.valueOf(d.trim().toUpperCase()));
      } catch (IllegalArgumentException e) {
        log.warn("알 수 없는 요일 설정값 무시: {}", d.trim());
      }
    }
    return days;
  }
}
```

**주의:**
- 메서드 가시성 `package-private` (`loadOrInitState`, `computeNextFireAt`, `parseDays`) — 같은 패키지 테스트에서 호출
- `SINGLETON_ID`, `SEOUL` 도 package-private 상수
- `tick()` 은 이번 단계에선 빈 로깅만, Task 5 에서 채움

---

## Task 5: Service `tick()` 본 로직 구현

**Files:**
- Modify: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusSchedulerService.java`

- [ ] **Step 1: `tick()` 본 로직 + 트랜잭션 분리 메서드 추가**

`tick()` 메서드 전체를 아래로 교체:

```java
public void tick() {
  SomansaBusSchedulerState state;
  try {
    state = loadOrInitState();
  } catch (Exception e) {
    log.error("스케줄러 state 로드/생성 실패", e);
    return;
  }

  String enabled = serverOptionService.getOptionValue(
      ServerOptionKey.SOMANSA_BUS_SCHEDULER_ENABLED);
  if (!"true".equalsIgnoreCase(enabled)) {
    log.info("스케줄러 비활성화 상태 — tick skip");
    return;
  }

  LocalDateTime now = LocalDateTime.now(SEOUL);
  if (now.isBefore(state.getNextFireAt())) {
    log.debug("아직 발화 시각 전 - nextFireAt: {}, now: {}", state.getNextFireAt(), now);
    return;
  }

  LocalDateTime cutoff = state.getNextFireAt().toLocalDate().atTime(23, 59, 59);
  if (now.isAfter(cutoff)) {
    log.warn("발화 시각 컷오프 초과 — skip, nextFireAt 만 갱신 (nextFireAt: {}, now: {})",
        state.getNextFireAt(), now);
    updateNextFireAtOnly(now);
    return;
  }

  LocalDate tomorrow = LocalDate.now(SEOUL).plusDays(1);
  Set<DayOfWeek> allowedDays = parseDays(serverOptionService.getOptionValue(
      ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS));
  if (!allowedDays.contains(tomorrow.getDayOfWeek())) {
    log.info("내일({}) 비허용 요일 — 예약 skip, nextFireAt 만 갱신", tomorrow.getDayOfWeek());
    updateNextFireAtOnly(now);
    return;
  }

  boolean fired = invokeReservationSafely();
  markStateAfterFire(now, fired);
}

private boolean invokeReservationSafely() {
  try {
    reservationService.scheduledAutoReservation();
    log.info("자동 예약 호출 완료");
    return true;
  } catch (Exception e) {
    log.error("자동 예약 호출 중 예외 발생 — nextFireAt 만 갱신", e);
    return false;
  }
}

@Transactional
void updateNextFireAtOnly(LocalDateTime now) {
  SomansaBusSchedulerState state = repository.findById(SINGLETON_ID).orElseThrow();
  state.setNextFireAt(computeNextFireAt(now));
  repository.save(state);
}

@Transactional
void markStateAfterFire(LocalDateTime now, boolean fired) {
  SomansaBusSchedulerState state = repository.findById(SINGLETON_ID).orElseThrow();
  if (fired) {
    state.setLastFiredAt(now);
  }
  state.setNextFireAt(computeNextFireAt(now));
  repository.save(state);
  log.info("state 갱신 완료 - fired: {}, nextFireAt: {}", fired, state.getNextFireAt());
}
```

**검증 포인트:**
- `tick()` 자체는 `@Transactional` 없음
- `loadOrInitState`, `updateNextFireAtOnly`, `markStateAfterFire` 각각 `@Transactional`
- 발화 실패해도 `markStateAfterFire(now, false)` 호출되어 `nextFireAt` 반드시 갱신

---

## Task 6: SchedulingConfig 작성

**Files:**
- Create: `Suh-Web/src/main/java/me/suhsaechan/web/config/SchedulingConfig.java`

- [ ] **Step 1: `@EnableScheduling` Config 작성**

파일 내용:

```java
package me.suhsaechan.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

---

## Task 7: SomansaBusSchedulerPoller 작성

**Files:**
- Create: `Suh-Web/src/main/java/me/suhsaechan/web/scheduler/SomansaBusSchedulerPoller.java`

- [ ] **Step 1: Poller 컴포넌트 작성**

먼저 디렉터리 존재 확인:
```bash
ls Suh-Web/src/main/java/me/suhsaechan/web/
```
`scheduler` 폴더 없으면 새로 생성.

파일 내용:

```java
package me.suhsaechan.web.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.somansabus.service.SomansaBusSchedulerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SomansaBusSchedulerPoller {

  private static final long FIXED_DELAY_MS = 600_000L;

  private final SomansaBusSchedulerService schedulerService;

  @Scheduled(fixedDelay = FIXED_DELAY_MS)
  public void poll() {
    try {
      schedulerService.tick();
    } catch (Exception e) {
      log.error("스케줄러 폴링 중 예외 발생 — 다음 폴링까지 대기", e);
    }
  }
}
```

---

## Task 8: 기존 Quartz 코드 삭제

**Files:**
- Delete: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/job/SomansaBusAutoReservationJob.java`
- Delete: `Suh-Web/src/main/java/me/suhsaechan/web/config/SomansaBusQuartzConfig.java`

- [ ] **Step 1: Job 파일 삭제**

```bash
rm Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/job/SomansaBusAutoReservationJob.java
```

- [ ] **Step 2: 빈 job 폴더 삭제 (다른 파일 없으면)**

```bash
ls Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/job/
# 비어있으면:
rmdir Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/job
```

- [ ] **Step 3: QuartzConfig 삭제**

```bash
rm Suh-Web/src/main/java/me/suhsaechan/web/config/SomansaBusQuartzConfig.java
```

- [ ] **Step 4: 남은 Quartz 참조 검색**

```bash
grep -rn "Quartz\|QuartzJobBean\|JobBuilder\|TriggerBuilder\|SchedulerException\|org.quartz" --include="*.java" Suh-Domain-Somansa-Bus Suh-Web Suh-Common
```

기대 결과: 검색 결과 0건.

---

## Task 9: build.gradle 에서 Quartz 의존성 제거

**Files:**
- Modify: `Suh-Common/build.gradle:33-34`

- [ ] **Step 1: Quartz 라인 삭제**

`Suh-Common/build.gradle` 의 다음 두 라인 제거:

```gradle
    // Quartz Scheduler
    api 'org.springframework.boot:spring-boot-starter-quartz'
```

수정 후 해당 위치는 그 위/아래 의존성 사이에 빈 줄만 남도록 정리.

- [ ] **Step 2: Gradle 재동기화 확인**

(빌드는 사용자가 수행, plan 에선 코드 변경만)

---

## Task 10: 통합 테스트 작성

**Files:**
- Create: `Suh-Domain-Somansa-Bus/src/test/java/me/suhsaechan/somansabus/service/SomansaBusSchedulerServiceTest.java`

- [ ] **Step 1: 테스트 파일 작성**

파일 내용:

```java
package me.suhsaechan.somansabus.service;

import static me.suhsaechan.suhlogger.util.SuhLogger.lineLog;
import static me.suhsaechan.suhlogger.util.SuhLogger.timeLog;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.somansabus.entity.SomansaBusSchedulerState;
import me.suhsaechan.somansabus.repository.SomansaBusSchedulerStateRepository;
import me.suhsaechan.web.SuhProjectUtilityApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SuhProjectUtilityApplication.class)
@ActiveProfiles("dev")
@Slf4j
class SomansaBusSchedulerServiceTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  @Autowired
  private SomansaBusSchedulerService schedulerService;

  @Autowired
  private SomansaBusSchedulerStateRepository stateRepository;

  @Autowired
  private ServerOptionService serverOptionService;

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
    timeLog(this::tick_비허용요일_skip_테스트);
    lineLog(null);
    timeLog(this::computeNextFireAt_허용요일_전진_테스트);

    lineLog("테스트종료");
  }

  public void tick_초기_row_생성_테스트() {
    lineLog("초기 row 생성 테스트 실행중");

    serverOptionService.setOptionValue(ServerOptionKey.SOMANSA_BUS_SCHEDULER_ENABLED, "true");

    schedulerService.tick();

    SomansaBusSchedulerState state = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    assertThat(state.getNextFireAt()).isAfter(LocalDateTime.now(SEOUL));
    log.info("생성된 state - nextFireAt: {}", state.getNextFireAt());
  }

  public void tick_미래시각_무동작_테스트() {
    lineLog("미래 시각 무동작 테스트 실행중");

    LocalDateTime futureFireAt = LocalDateTime.now(SEOUL).plusHours(1);
    SomansaBusSchedulerState state = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    state.setNextFireAt(futureFireAt);
    state.setLastFiredAt(null);
    stateRepository.saveAndFlush(state);

    schedulerService.tick();

    SomansaBusSchedulerState after = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    assertThat(after.getNextFireAt()).isEqualTo(futureFireAt);
    assertThat(after.getLastFiredAt()).isNull();
  }

  public void tick_컷오프초과_skip_테스트() {
    lineLog("컷오프 초과 skip 테스트 실행중");

    LocalDateTime yesterdayFireAt = LocalDateTime.now(SEOUL).minusDays(1).withHour(22).withMinute(0).withSecond(0).withNano(0);
    SomansaBusSchedulerState state = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    state.setNextFireAt(yesterdayFireAt);
    state.setLastFiredAt(null);
    stateRepository.saveAndFlush(state);

    schedulerService.tick();

    SomansaBusSchedulerState after = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    assertThat(after.getLastFiredAt()).isNull();
    assertThat(after.getNextFireAt()).isAfter(LocalDateTime.now(SEOUL));
  }

  public void tick_비허용요일_skip_테스트() {
    lineLog("비허용 요일 skip 테스트 실행중");

    DayOfWeek tomorrow = LocalDateTime.now(SEOUL).toLocalDate().plusDays(1).getDayOfWeek();
    DayOfWeek disallowed = oppositeDay(tomorrow);
    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS, disallowed.name());

    LocalDateTime pastFireAt = LocalDateTime.now(SEOUL).minusMinutes(1);
    SomansaBusSchedulerState state = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    state.setNextFireAt(pastFireAt);
    state.setLastFiredAt(null);
    stateRepository.saveAndFlush(state);

    schedulerService.tick();

    SomansaBusSchedulerState after = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    assertThat(after.getLastFiredAt()).isNull();
    assertThat(after.getNextFireAt()).isAfter(LocalDateTime.now(SEOUL));

    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS, "MON,TUE,WED,THU,FRI");
  }

  public void computeNextFireAt_허용요일_전진_테스트() {
    lineLog("허용 요일 전진 계산 테스트 실행중");

    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS, "MON");
    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_FROM, "22");
    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_TO, "23");

    LocalDateTime reference = LocalDateTime.now(SEOUL);
    LocalDateTime next = schedulerService.computeNextFireAt(reference);

    assertThat(next.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(next.getHour()).isBetween(22, 23);
    log.info("계산된 nextFireAt: {} ({})", next, next.getDayOfWeek());

    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS, "MON,TUE,WED,THU,FRI");
  }

  private DayOfWeek oppositeDay(DayOfWeek day) {
    return day == DayOfWeek.MONDAY ? DayOfWeek.TUESDAY : DayOfWeek.MONDAY;
  }
}
```

**주의:**
- `@MockBean` 없음 — 실제 빈 사용
- `@AfterEach` cleanup 없음 — `SINGLETON_ID` 라 row 누적 안 됨
- 외부 API 호출 격리: dev DB 에 활성 SomansaBusSchedule 없으면 자동 예약 for-loop 빈 채로 종료
- `ServerOption` 변경은 캐시 갱신 위해 `setOptionValue` 사용 (`@CacheEvict` 트리거)
- 마지막 두 테스트는 끝나기 전에 DAYS 설정을 `MON,TUE,WED,THU,FRI` 로 복원

- [ ] **Step 2: 테스트 실행 (사용자 환경에서 별도 수행)**

빌드/테스트 실행은 사용자 환경에서 별도 수행. plan 작성 단계에서는 코드 작성까지만.

---

## Task 11: 최종 검증 — Quartz 잔재 0건 확인

- [ ] **Step 1: 전 코드베이스 Quartz 잔재 검색**

명령:
```bash
grep -rn "Quartz\|QuartzJobBean\|JobBuilder\|TriggerBuilder\|SchedulerException\|org.quartz\|spring-boot-starter-quartz" --include="*.java" --include="*.gradle" Suh-Domain-Somansa-Bus Suh-Web Suh-Common
```

기대 결과: 0건.

- [ ] **Step 2: 변경 파일 목록 확인**

```bash
git status
```

기대 결과 (신규/수정/삭제):
- **신규:**
  - `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/entity/SomansaBusSchedulerState.java`
  - `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/repository/SomansaBusSchedulerStateRepository.java`
  - `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusSchedulerService.java`
  - `Suh-Domain-Somansa-Bus/src/test/java/me/suhsaechan/somansabus/service/SomansaBusSchedulerServiceTest.java`
  - `Suh-Web/src/main/java/me/suhsaechan/web/scheduler/SomansaBusSchedulerPoller.java`
  - `Suh-Web/src/main/java/me/suhsaechan/web/config/SchedulingConfig.java`
  - `Suh-Web/src/main/resources/db/migration/V2_5_35__create_somansa_bus_scheduler_state.sql`
- **삭제:**
  - `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/job/SomansaBusAutoReservationJob.java`
  - `Suh-Web/src/main/java/me/suhsaechan/web/config/SomansaBusQuartzConfig.java`
- **수정:**
  - `Suh-Common/build.gradle`

- [ ] **Step 3: 사용자 검토 요청**

모든 Task 완료 후 사용자에게 diff 검토 요청. 사용자 명시적 "커밋해줘" 요청 시에만 commit 수행 (CLAUDE.md 절대 규칙).

---

## Self-Review

**1. Spec coverage:**
- ✅ Quartz 완전 제거 (Task 8, 9)
- ✅ DB 영속 nextFireAt (Task 1, 2, 3)
- ✅ 10분 폴링 (Task 7)
- ✅ 당일 자정 컷오프 (Task 5)
- ✅ 다음 허용 요일 윈도우 (Task 4)
- ✅ Quartz 의존성 제거 (Task 9)
- ✅ @EnableScheduling (Task 6)
- ✅ 싱글톤 UUID 상수 (Task 4)
- ✅ mock 없는 통합 테스트 (Task 10)
- ✅ mainTest + timeLog 패턴 (Task 10)
- ✅ 트랜잭션 경계 (Task 5: tick 비트랜잭션, 갱신 메서드 @Transactional)
- ✅ 예외 처리 (Task 5 invokeReservationSafely, Task 7 poller try-catch)

**2. Placeholder scan:** 모든 step 실제 코드 포함, "TBD"/"TODO"/"add appropriate" 없음.

**3. Type consistency:**
- `SINGLETON_ID`, `SEOUL` — Service/Test 양쪽 일관
- `loadOrInitState`, `computeNextFireAt`, `parseDays`, `updateNextFireAtOnly`, `markStateAfterFire`, `invokeReservationSafely` — 정의/호출 시그니처 일치
- entity field 명 (`somansaBusSchedulerStateId`, `nextFireAt`, `lastFiredAt`) — entity/repo/service/test 양쪽 일관
