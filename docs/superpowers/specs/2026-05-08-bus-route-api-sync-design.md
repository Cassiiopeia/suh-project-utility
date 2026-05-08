# 버스 노선 정적 데이터 → 외부 API 동기화 방식 변경

**관련 이슈**: [#188](https://github.com/Cassiiopeia/suh-project-utility/issues/188)
**작성일**: 2026-05-08
**상태**: 설계 승인 대기

---

## 1. 배경 및 목표

### 현재 상태
- `SomansaBusDataInitializer` (ApplicationRunner) 가 부팅 시 하드코딩 15개 노선을 DB 시드
- `SomansaBusRouteService.initializeRoutes()` 도 별도 하드코딩 14개 (호출 없는 dead code)
- 외부 buseezy(`csios.busin.co.kr`) 의 실제 노선 변경/추가 시 반영 불가
- UI 는 동적 fetch 구조이지만, 백엔드 데이터가 정적이라 실효적으로 정적 페이지로 동작

### 목표
- 노선 데이터를 외부 buseezy `drivelist.aspx` 응답으로부터 동기화
- 하드코딩 시드 완전 제거
- 운영 중 노선 변경 발생 시 관리자가 수동 트리거로 동기화 가능
- FK 무결성 보존 (`SomansaBusSchedule`, `SomansaBusReservationHistory` 가 노선 참조)

### 범위 외 (별도 이슈)
- buseezy SSO 로그인 흐름 정상화 (#189)
- 수동 예약 동작 검증
- 스케줄 기반 자동 예약 스케줄러
- 동기화 이력 영구 보존 (필요시 별도 이슈)

---

## 2. 외부 API 분석

### 도메인 변경 ⚠️ 본 이슈에서 일괄 갱신
- 옛 코드: `cs.android.busin.co.kr` (옛 안드로이드 앱 endpoint, 현재 미동작 추정)
- 신규: `csios.busin.co.kr`
- 영향 메서드: `login`, `createSession`, `makeReservation` 모두 신규 도메인으로 변경. `fetchRouteList` (신규) 도 동일.
- `@Value` 기본값 (`SomansaBusApiService`) 의 URL 도 갱신.

### 로그인 흐름 (기존 코드 활용)
1. `GET https://csios.busin.co.kr/Login.aspx?device=pc` → `ASP.NET_SessionId` 쿠키 획득
2. `POST https://csios.busin.co.kr/Login.aspx/LoginCheck` body `{ data: "{loginId},pc" }` → response `{"d": <passengerId>}`
3. `POST https://csios.busin.co.kr/Default.aspx/CreateSession` body `{ data: "{loginId},{passengerId},pc,pc" }`

### 노선 조회
- `GET https://csios.busin.co.kr/driving/drivelist.aspx` (로그인 세션 쿠키 필요)
- 응답: HTML. 3개 탭(`tab_fo`/`tab_ba`/`tab_sh`) 각각의 `<li>` 안에 `goPage(disptid, shuttletype, shuttlebus)` 함수 호출 + `<span class="text">` 라벨

### 파싱 대상
| HTML 위치 | 추출값 | entity 필드 |
|----------|--------|------------|
| `goPage(N, ...)` 첫 인자 | `disptid` (int) | `disptid` |
| `goPage(_, '출근'/'퇴근', _)` | `caralias` | `caralias` |
| `goPage(_, _, 'True'/'False')` | `isShuttle` | `isShuttle` (신규) |
| `<span class="text">` 텍스트 | `description`, `departureTime`, `station`, `busNumber` | 동명 필드 |

### 라벨 텍스트 파싱
예: `06:55 당산역 1호 - 출근`
- `departureTime` = "06:55"
- `station` = "당산역"
- `busNumber` = 1 (셔틀은 null)
- `description` = 원본 그대로

---

## 3. 아키텍처 변경

### 3-1. Entity 변경

#### `SomansaBusRoute` 신규 필드
```java
@Column(nullable = false)
@Builder.Default
private Boolean isShuttle = false;
```

기존 필드는 그대로 유지. `disptid` 가 unique key 역할 (upsert 기준).

> 참고: 기존 row 는 마이그레이션 시 `isShuttle = false` 기본값 적용. JPA `ddl-auto: update` 로 컬럼 자동 추가.

---

### 3-2. ServerOptionKey 신규 키

```java
SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID(
    "소만사 버스 노선 동기화 트리거 회원 loginId",
    "chan4760@somansa.com"
)
```

기존 admin 페이지(`/api/server-option/list` 자동 노출)에서 변경 가능. 별도 UI 추가 불필요.

---

### 3-3. Service — `SomansaBusRouteService` 메서드 추가

```java
@Transactional
public SomansaBusResponse syncRoutes() {
    // 1. ServerOption 에서 trigger loginId 조회
    String triggerLoginId = serverOptionService
        .getOption(ServerOptionKey.SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID)
        .getOptionValue();

    // 2. buseezy 로그인 → CreateSession (apiService 활용)
    int passengerId = apiService.login(triggerLoginId);
    if (passengerId <= 0) throw new CustomException(ErrorCode.SOMANSA_BUS_API_LOGIN_FAILED);

    boolean sessionCreated = apiService.createSession(triggerLoginId, passengerId);
    if (!sessionCreated) throw new CustomException(ErrorCode.SOMANSA_BUS_API_SESSION_FAILED);

    // 3. drivelist.aspx GET → HTML 파싱 (apiService 가 처리)
    List<RouteData> remoteRoutes = apiService.fetchRouteList();

    // 4. Upsert + Soft delete
    int created = 0, updated = 0, deactivated = 0;

    Set<Integer> remoteDisptids = remoteRoutes.stream()
        .map(RouteData::getDisptid).collect(Collectors.toSet());

    // 4-1. 외부 응답 → DB upsert
    for (RouteData remote : remoteRoutes) {
        Optional<SomansaBusRoute> existing = routeRepository.findByDisptid(remote.getDisptid());
        if (existing.isPresent()) {
            SomansaBusRoute route = existing.get();
            // 변경 감지 후 update
            if (isChanged(route, remote)) {
                applyChanges(route, remote);
                routeRepository.save(route);
                updated++;
            }
            // soft delete 복구
            if (Boolean.FALSE.equals(route.getIsActive())) {
                route.setIsActive(true);
                routeRepository.save(route);
            }
        } else {
            routeRepository.save(toEntity(remote));
            created++;
        }
    }

    // 4-2. 외부에 없는 active 노선 → soft delete
    List<SomansaBusRoute> activeRoutes = routeRepository.findByIsActiveTrue();
    for (SomansaBusRoute route : activeRoutes) {
        if (!remoteDisptids.contains(route.getDisptid())) {
            route.setIsActive(false);
            routeRepository.save(route);
            deactivated++;
        }
    }

    long totalCount = routeRepository.count();

    return SomansaBusResponse.builder()
        .syncCreatedCount(created)
        .syncUpdatedCount(updated)
        .syncDeactivatedCount(deactivated)
        .syncTotalCount((int) totalCount)
        .syncedAt(LocalDateTime.now())
        .syncTriggerLoginId(triggerLoginId)
        .build();
}
```

#### 제거되는 메서드/클래스
- `SomansaBusDataInitializer` (전체 제거)
- `SomansaBusRouteService.initializeRoutes()` (호출 없는 dead code)

---

### 3-3-1. 세션 관리

- sync 메서드 종료 시 **logout 호출 안 함** (세션 자연 만료 대기)
- 기존 `executeReservation` 패턴과 동일. 단순성 우선.
- 잦은 로그인 차단 발생 시 → 별도 이슈에서 세션 캐싱 도입 검토.

### 3-4. ApiService — HTML 파싱 메서드 추가

`SomansaBusApiService` 에 메서드 추가:

```java
private static final String DRIVELIST_URL = "https://csios.busin.co.kr/driving/drivelist.aspx";

public List<RouteData> fetchRouteList() {
    Request request = new Request.Builder()
        .url(DRIVELIST_URL)
        .get()
        .addHeader("User-Agent", USER_AGENT)
        .build();

    try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
            throw new CustomException(ErrorCode.SOMANSA_BUS_API_FETCH_FAILED);
        }
        String html = response.body().string();
        return parseRouteList(html);
    } catch (IOException e) {
        log.error("노선 목록 조회 실패", e);
        throw new CustomException(ErrorCode.SOMANSA_BUS_API_FETCH_FAILED);
    }
}

private List<RouteData> parseRouteList(String html) {
    Document doc = Jsoup.parse(html);
    List<RouteData> result = new ArrayList<>();

    for (Element li : doc.select(".line-list li")) {
        Element a = li.selectFirst("a.cont");
        if (a == null) continue;

        String onclick = a.attr("onclick");
        // goPage(47040, '출근', 'False') 파싱
        Matcher m = Pattern.compile("goPage\\((\\d+),\\s*'([^']+)'\\s*,\\s*'(True|False)'\\)").matcher(onclick);
        if (!m.find()) continue;

        int disptid = Integer.parseInt(m.group(1));
        String caralias = m.group(2);  // "출근" or "퇴근"
        boolean isShuttle = "True".equals(m.group(3));

        Element textSpan = a.selectFirst("span.text");
        String description = textSpan != null ? textSpan.text().trim() : "";

        // "06:55 당산역 1호 - 출근" 파싱
        RouteLabelParts parts = parseLabel(description);

        result.add(RouteData.builder()
            .disptid(disptid)
            .caralias(caralias)
            .isShuttle(isShuttle)
            .description(description)
            .departureTime(parts.getDepartureTime())
            .station(parts.getStation())
            .busNumber(parts.getBusNumber())
            .build());
    }
    return result;
}

private RouteLabelParts parseLabel(String description) {
    // "06:55 당산역 1호 - 출근" → time="06:55", station="당산역", busNumber=1
    // "07:30 판교역 - 셔틀" → time="07:30", station="판교역", busNumber=null
    // 정규식: ^\\s*(\\d{2}:\\d{2})\\s+(.+?)(?:\\s+(\\d+)호)?\\s*-.*$
    Matcher m = Pattern.compile("^\\s*(\\d{2}:\\d{2})\\s+(.+?)(?:\\s+(\\d+)호)?\\s*-.*$").matcher(description);
    if (!m.find()) return RouteLabelParts.empty();
    return RouteLabelParts.builder()
        .departureTime(m.group(1))
        .station(m.group(2))
        .busNumber(m.group(3) != null ? Integer.parseInt(m.group(3)) : null)
        .build();
}
```

> `RouteData`, `RouteLabelParts` 는 `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/` 안에 독립 DTO 로 분리 (CLAUDE.md "세부 DTO 독립 클래스" 규칙 부합).

---

### 3-5. Controller — endpoint 추가

`SomansaBusController` 에 추가:

```java
@PostMapping(value = "/route/sync", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@LogMonitor
public ResponseEntity<SomansaBusResponse> syncRoutes() {
    return ResponseEntity.ok(routeService.syncRoutes());
}
```

기존 `/route/list` 응답에 `lastSyncedAt` 추가:

```java
public SomansaBusResponse getAllRoutes() {
    List<SomansaBusRoute> routes = routeRepository.findByIsActiveTrueOrderByDepartureTimeAsc();
    LocalDateTime lastSyncedAt = routes.stream()
        .map(SomansaBusRoute::getUpdatedDate)
        .max(LocalDateTime::compareTo)
        .orElse(null);
    return SomansaBusResponse.builder()
        .routes(routes)
        .totalCount((long) routes.size())
        .lastSyncedAt(lastSyncedAt)
        .build();
}
```

---

### 3-6. Response DTO 변경

`SomansaBusResponse` 에 필드 추가 (별도 DTO 분리 안 함):

```java
// 노선 동기화 결과
private Integer syncCreatedCount;
private Integer syncUpdatedCount;
private Integer syncDeactivatedCount;
private Integer syncTotalCount;

@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
private LocalDateTime syncedAt;

private String syncTriggerLoginId;

// 노선 list 조회 시 마지막 동기화 시각
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
private LocalDateTime lastSyncedAt;
```

---

### 3-7. ErrorCode 추가

`me.suhsaechan.common.exception.ErrorCode` 에:

```java
SOMANSA_BUS_API_LOGIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "버스인 로그인에 실패했습니다."),
SOMANSA_BUS_API_SESSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "버스인 세션 생성에 실패했습니다."),
SOMANSA_BUS_API_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "버스인 노선 조회에 실패했습니다."),
```

`SOMANSA_BUS_ROUTE_NOT_FOUND` 등 기존 항목 그대로 유지.

---

### 3-8. UI — `somansaBusDashboard.html`

#### "버스 노선" 섹션 변경
```html
<!-- 기존 노선 카드 헤더에 동기화 버튼 + 마지막 동기화 시각 추가 -->
<div class="route-section-header">
    <div class="route-title">버스 노선 (<span id="routeTotalCount">0</span>건)</div>
    <button id="btnSyncRoutes" class="btn btn-primary">🔄 노선 동기화</button>
    <div class="last-synced">마지막 동기화: <span id="lastSyncedAt">-</span></div>
</div>
```

#### JavaScript 변경
```javascript
// 페이지 진입 시 노선 목록 로드 → lastSyncedAt 표시
sendFormRequest('/api/somansa-bus/route/list', {}, function(data) {
    document.getElementById('routeTotalCount').textContent = data.totalCount;
    document.getElementById('lastSyncedAt').textContent = data.lastSyncedAt || '없음';
    renderRoutes(data.routes);
});

// 동기화 버튼
document.getElementById('btnSyncRoutes').addEventListener('click', function() {
    if (!confirm('노선 동기화를 시작합니다. 진행할까요?')) return;
    sendFormRequest('/api/somansa-bus/route/sync', {}, function(data) {
        showToast(`✅ 동기화 완료: 신규 +${data.syncCreatedCount} / 변경 ${data.syncUpdatedCount} / 비활성 ${data.syncDeactivatedCount} / 총 ${data.syncTotalCount}건`);
        // 노선 목록 재조회
        loadRoutes();
    }, function(err) {
        showToast('❌ 동기화 실패: ' + err.message, 'error');
    });
});
```

---

## 4. CLAUDE.md 컨벤션 추가

본 이슈와 함께 다음 컨벤션을 CLAUDE.md "Config 설정 위치" 직후에 추가 (이미 worktree 에 적용됨):

> **시스템 동적 설정값 관리 (ServerOptionKey)** ⚠️ 필수 준수
> - 시스템 전역 동적 설정값은 반드시 `ServerOptionKey` enum + `ServerOption` entity 로 관리한다
> - `application.yml` 하드코딩 금지 (환경별 비밀값/인프라 값 제외)
> - Service 상수(`private static final`) 하드코딩 금지

전체 본문은 CLAUDE.md 변경분 참조.

---

## 5. 영향 범위

### 변경 파일
| 파일 | 변경 |
|------|------|
| `Suh-Common/src/main/java/me/suhsaechan/common/constant/ServerOptionKey.java` | 신규 enum 키 1개 추가 |
| `Suh-Common/src/main/java/me/suhsaechan/common/exception/ErrorCode.java` | error code 3개 추가 |
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/entity/SomansaBusRoute.java` | `isShuttle` 필드 추가 |
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusApiService.java` | `fetchRouteList()`, `parseRouteList()`, `parseLabel()` 추가. **모든 도메인 URL `cs.android.busin.co.kr` → `csios.busin.co.kr` 일괄 갱신** (`@Value` 기본값 4개 + 코드 내 Referer URL 등) |
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusRouteService.java` | `syncRoutes()` 추가, `initializeRoutes()` (dead code) 제거. `getAllRoutes()` 에 `lastSyncedAt` 추가 |
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/SomansaBusResponse.java` | sync 결과 필드 7개 추가 |
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/RouteData.java` | 신규 DTO |
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/dto/RouteLabelParts.java` | 신규 DTO |
| `Suh-Web/src/main/java/me/suhsaechan/web/controller/api/SomansaBusController.java` | `/route/sync` endpoint 추가 |
| `Suh-Web/src/main/resources/templates/pages/somansaBusDashboard.html` | sync 버튼 + 마지막 동기화 시각 표시 + JS 핸들러 |
| `CLAUDE.md` | "시스템 동적 설정값 관리 (ServerOptionKey)" 섹션 추가 (이미 적용) |

### 제거 파일
| 파일 | 이유 |
|------|------|
| `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/config/SomansaBusDataInitializer.java` | 외부 API 동기화로 대체. 부팅 시 자동 시드 제거 |

### 의존성
- 기존 `Suh-Common/build.gradle` 의 `org.jsoup:jsoup:1.15.4 (api)` 활용. 추가 의존성 없음.

---

## 6. 마이그레이션 / 호환성

- DB: JPA `ddl-auto: update` 로 `is_shuttle` 컬럼 자동 추가. 기본값 `false` 적용.
- 기존 노선 데이터: 외부 동기화 후 매칭되는 disptid 는 그대로 유지, 매칭 안 되는 노선은 `isActive=false` 로 변경.
- `SomansaBusDataInitializer` 제거: 부팅 시 시드 안 함. 첫 부팅 후 admin 이 dashboard 에서 한 번 sync 클릭 필요.

---

## 7. 테스트 시나리오

| # | 시나리오 | 기대 결과 |
|---|---------|----------|
| 1 | sync 호출 (정상) | 외부 노선과 DB 동기화. created/updated/deactivated 카운트 응답 |
| 2 | trigger member 로그인 실패 | `SOMANSA_BUS_API_LOGIN_FAILED` 예외 |
| 3 | drivelist 응답 비정상 (HTML 파싱 실패) | `SOMANSA_BUS_API_FETCH_FAILED` 예외 |
| 4 | DB 노선이 외부에 없음 | 해당 노선 `isActive=false` |
| 5 | 외부 노선이 DB 비활성 → 다시 등장 | 해당 노선 `isActive=true` 복구 |
| 6 | 셔틀 노선 (`isShuttle=true`) sync | DB 저장됨. 단 자동/수동 예약 로직에서는 제외되어야 함 (별도 검증) |
| 7 | sync 후 `/route/list` 호출 | `lastSyncedAt` 에 max(updatedDate) 반환 |

---

## 8. 미정 사항 / 후속 작업

- buseezy 가 잦은 로그인 차단 시 → 세션 캐싱 도입 검토 (별도 이슈)
- 동기화 이력 영구 보존 필요 시 → `SomansaBusRouteSyncHistory` entity 추가 (별도 이슈)
- 셔틀 노선의 예약 로직 제외 처리는 본 이슈 범위 외 (예약 정상화는 별도 이슈)

---

## 9. 결정 이력 (Brainstorming)

| Q | 결정 | 이유 |
|---|------|------|
| 동기화 트리거 | 수동 only | 부팅/스케줄러 시점엔 어떤 회원으로 로그인할지 모호. 노선 변경 빈도 낮음. SSO 안정화(#189) 이후 자동화 검토 |
| 트리거 회원 결정 | 호출자 명시 (ServerOption 기반) | 명시적. 향후 확장 시 회원별 권한 추적 가능 |
| 충돌 처리 | Upsert + Soft delete | FK 무결성 보존 (Schedule/History 가 Route 참조). 외부에 다시 살아나면 복구 가능 |
| 셔틀 처리 | DB 저장하되 isShuttle 플래그 | 외부 데이터 충실성. 예약 로직에선 별도 필터링 |
| HTML 파싱 | Jsoup | 정규식 대비 견고. 이미 의존성 있음 |
| API 흐름 | 매번 fresh login | 기존 `executeReservation` 패턴 일관성. 만료 감지 로직 불필요 |
| DataInitializer | 완전 제거 | 단일 진실 원천 (외부 API). YAGNI |
| 응답 | 통계만 | YAGNI. DB 이력 entity 안 만듦 |
| 트리거 회원 저장 | ServerOptionKey | 시스템 동적 설정 패턴. 신규 entity 안 만듦 |
| 응답 DTO 분리 | 안 함 (기존 Response 평면 필드) | 기존 Response 패턴 일관성 |
| UI | 미니멀 (버튼 + 마지막 동기화) | 결과 toast 로 즉시 피드백. 이력은 별도 이슈 |
| 마지막 동기화 시각 | max(Route.updatedDate) | 별도 저장 필요 X |
