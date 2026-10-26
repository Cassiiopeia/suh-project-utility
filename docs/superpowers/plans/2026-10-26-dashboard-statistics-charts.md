# 대시보드 서버 통계 차트 개편 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 대시보드 서버 통계 섹션을 기간별(7/30/90일) Chart.js 분석 화면으로 바꾸고 Task D-Day 섹션을 없앤다.

**Architecture:** 기존 로그 테이블(`page_visit_log`, `feature_usage_log`, `chat_message`)을 기간 단위로 집계하는 조회를 Repository 에 추가하고, `StatisticsService`(도메인)가 방문·기능 집계를, `DashboardService`(응용)가 기간 계산·챗봇 토큰 합치기·Redis 캐시를 맡는다. 화면은 Thymeleaf fragment + 전용 JS 파일이 새 API 를 호출해 Chart.js 로 그린다.

**Tech Stack:** Java 17, Spring Boot 3.4.2, Spring Data JPA (PostgreSQL), Redis 캐시, Thymeleaf, Tailwind v4 + DaisyUI 5, jQuery, Chart.js 4.4.7 (CDN)

**Spec:** `docs/superpowers/specs/2026-10-26-dashboard-statistics-charts-design.md` (시안: https://claude.ai/artifact/Dwq8Wq44nq48tkZaX7S5Tr A안)

## Global Constraints

- 모든 명령은 `source ~/.zshrc && ` 를 앞에 붙여 실행
- **커밋 금지** (구현자·리뷰어 모두). `git add` 도 하지 않는다. 커밋은 마지막에 메인 세션이 `/pro-commit` 으로 한 번만 한다
- 작업 브랜치는 `main` 그대로 (브랜치·worktree 생성 금지)
- 단일 Request/Response, POST + `MediaType.MULTIPART_FORM_DATA_VALUE`, `@ModelAttribute`, `@LogMonitor`
- 실패는 `CustomException(ErrorCode.XXX)`, `RuntimeException` 직접 throw 금지
- 새 코드에 `== null` 비교 금지 (Optional / `Objects` 유틸 / `StringUtils.hasText` 사용), Boolean 은 `Boolean.TRUE.equals`
- Boolean 필드는 `is` 접두사, 약어 금지, 세부 DTO 는 독립 클래스(static inner 금지), `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- 숫자 필드는 `Long`/`Integer` 래퍼로 선언 (Redis `GenericJackson2JsonRedisSerializer` 역직렬화 시 타입 유지)
- `LocalDate` 필드에는 `@JsonFormat(pattern = "yyyy-MM-dd")`
- 화면: 하드코딩 색상 클래스(`bg-white`, `text-gray-*` 등) 금지 → 시맨틱 토큰, 인라인 `style=""` 금지, Tailwind 대괄호 값 금지, `tailwind.generated.css` 직접 편집 금지 (`cd Suh-Web/frontend && npm run build:css`)
- 주석: 한국어, 한 줄 요약·블록 머리글 수준, LLM 스타일 설명 주석 금지
- 봇 제외(`is_bot = false`), 고유 방문자 = `client_hash IS NOT NULL` 의 COUNT DISTINCT
- 기간 경계: 현재 기간 = `[오늘 - (N-1)일 00:00, 내일 00:00)`, 직전 기간 = `[현재 시작 - N일, 현재 시작)`, 끝 경계는 항상 미포함
- 허용 기간: 7, 30, 90 (그 외·누락은 `ErrorCode.INVALID_PARAMETER`)
- 기능 통계 제외 3종: `LOGIN`, `PROFILE_VIEW`, `NOTICE_VIEW`
- 아키텍처 규칙 테스트(`Suh-Web/src/test/java/architecture/ArchitectureRulesTest.java`) 통과 유지

## Review Focus

1. **데이터가 전혀 없는 기간** (신규 DB, 90일 전 구간 공백): 모든 차트가 0 으로 그려지고 JS 오류가 없어야 함 → Task 2 `기록이_없으면_모든_날짜를_0으로_채운다`, Task 4 수동 확인
2. **referrer 가 이상한 값** (`null`, 빈 문자열, `android-app://...`, 스킴 없는 문자열, 대문자 호스트): 예외 없이 DIRECT / OTHER 로 분류 → Task 1 테스트
3. **자기 사이트 서브도메인·포트 포함 referrer** (`https://lab.suhsaechan.kr:443/notice`, `http://localhost:8080/`): INTERNAL 로 빠져야 함 → Task 1 테스트
4. **직전 기간 0** : 증감률 배지가 `Infinity%`/`NaN%` 로 보이면 안 되고 숨김 → Task 4 JS `formatChangePercent`
5. **테마 토글 직후** : 차트 글자·격자색이 새 테마 색으로 다시 그려져야 함 → Task 4 수동 확인(라이트·다크 캡처)

---

### Task 1: 유입 경로 분류기와 내부 호스트 설정

**Files:**
- Create: `Suh-Domain-Statistics/src/main/java/me/suhsaechan/statistics/service/ReferrerSourceClassifier.java`
- Create: `Suh-Domain-Statistics/src/main/java/me/suhsaechan/statistics/dto/ReferrerSource.java`
- Modify: `Suh-Common/src/main/java/me/suhsaechan/common/constant/ServerOptionKey.java`
- Test: `Suh-Domain-Statistics/src/test/java/me/suhsaechan/statistics/service/ReferrerSourceClassifierTest.java`

**Interfaces:**
- Produces:
  - `enum ReferrerSource { GOOGLE, NAVER, GITHUB, DIRECT, OTHER, INTERNAL }`
  - `public static ReferrerSource ReferrerSourceClassifier.classify(String referrer, List<String> internalHosts)` (상태 없는 유틸, private 생성자)
  - `public static List<String> ReferrerSourceClassifier.parseInternalHosts(String commaSeparatedHosts)` (trim, 소문자, 빈 항목 제거)
  - `ServerOptionKey.STATISTICS_INTERNAL_REFERRER_HOSTS("통계 유입 경로에서 제외할 자기 사이트 호스트 (쉼표 구분, 서브도메인 포함)", "lab.suhsaechan.kr,suh-project.synology.me,localhost")`

분류 규칙(위에서부터 첫 일치):
1. 빈 값/공백 → `DIRECT`
2. `URI` 파싱 실패 또는 호스트 없음 → `OTHER`
3. 호스트(소문자)가 내부 호스트 `h` 와 같거나 `.h` 로 끝남 → `INTERNAL`
4. 호스트가 정규식 `^(www\.)?google\.[a-z.]+$` 에 맞음 → `GOOGLE`
5. 호스트가 `naver.com` 이거나 `.naver.com` 으로 끝남 → `NAVER`
6. 호스트가 `github.com`/`.github.com` 또는 `.github.io` 로 끝남 → `GITHUB`
7. 그 외 → `OTHER`

- [ ] **Step 1: 실패하는 테스트 작성** (`ReferrerSourceClassifierTest`, JUnit5 + AssertJ, 내부 호스트 `List.of("lab.suhsaechan.kr", "localhost")`)
  - `null`, `""`, `"   "` → `DIRECT`
  - `"https://www.google.com/"`, `"https://www.google.co.kr/search?q=x"` → `GOOGLE`
  - `"https://search.naver.com/search.naver?query=x"`, `"https://m.naver.com"` → `NAVER`
  - `"https://github.com/Cassiiopeia"`, `"https://cassiiopeia.github.io/x"` → `GITHUB`
  - `"https://LAB.SUHSAECHAN.KR:443/notice"`, `"http://localhost:8080/"` → `INTERNAL`
  - `"https://evil-lab.suhsaechan.kr.attacker.com/"` → `OTHER` (접미사 위장)
  - `"android-app://com.google.android.gm/"` → `OTHER`
  - `"not a url"` → `OTHER`
  - `parseInternalHosts(" A.com, ,b.com ")` → `["a.com", "b.com"]`
- [ ] **Step 2: 실패 확인** — `source ~/.zshrc && ./gradlew :Suh-Domain-Statistics:test --tests '*ReferrerSourceClassifierTest'` → 컴파일 실패
- [ ] **Step 3: `ReferrerSource`, `ReferrerSourceClassifier`, `ServerOptionKey` 항목 구현**
- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS

---

### Task 2: 기간 통계 집계 (Repository + StatisticsService + DTO + 인덱스)

**Files:**
- Create (`Suh-Domain-Statistics/src/main/java/me/suhsaechan/statistics/dto/`): `DashboardStatisticsDto.java`, `DailyStatisticsDto.java`, `StatisticsPeriodTotalDto.java`, `FeatureUsageStatisticsDto.java`, `ReferrerSourceStatisticsDto.java`, `PageViewStatisticsDto.java`, `VisitHeatmapCellDto.java`
- Modify: `Suh-Domain-Statistics/src/main/java/me/suhsaechan/statistics/repository/PageVisitLogRepository.java`
- Modify: `Suh-Domain-Statistics/src/main/java/me/suhsaechan/statistics/repository/FeatureUsageLogRepository.java`
- Modify: `Suh-Domain-Statistics/src/main/java/me/suhsaechan/statistics/service/StatisticsService.java`
- Create: `Suh-Web/src/main/resources/db/migration/V2_5_111__add_statistics_time_indexes.sql` (작성 직전 `version.yml` 의 `version` 재확인, 같은 prefix 파일 없을 것)
- Test: `Suh-Domain-Statistics/src/test/java/me/suhsaechan/statistics/service/StatisticsServicePeriodTest.java`

**Interfaces:**
- Consumes: Task 1 `ReferrerSourceClassifier.classify/parseInternalHosts`, `ServerOptionKey.STATISTICS_INTERNAL_REFERRER_HOSTS`, `ServerOptionService.getOption(key).getOptionValue()`
- Produces (DTO 필드, 전부 래퍼 타입):
  - `DashboardStatisticsDto { Integer periodDays; StatisticsPeriodTotalDto currentTotal; StatisticsPeriodTotalDto previousTotal; List<DailyStatisticsDto> dailyStatistics; List<FeatureUsageStatisticsDto> featureUsages; List<ReferrerSourceStatisticsDto> referrerSources; List<PageViewStatisticsDto> topPages; List<VisitHeatmapCellDto> heatmapCells; }`
  - `DailyStatisticsDto { LocalDate date; Long uniqueVisitors; Long pageViews; Long featureUsages; Long chatTokens; }`
  - `StatisticsPeriodTotalDto { Long uniqueVisitors; Long pageViews; Long featureUsages; Long chatTokens; }`
  - `FeatureUsageStatisticsDto { FeatureType featureName; Long usageCount; }`
  - `ReferrerSourceStatisticsDto { ReferrerSource source; Long viewCount; }`
  - `PageViewStatisticsDto { String pagePath; Long viewCount; }`
  - `VisitHeatmapCellDto { Integer dayOfWeek; Integer hourBucket; Long pageViews; }` (dayOfWeek 1 월 ~ 7 일, hourBucket 0~7 = 0시·3시·…·21시)
  - `StatisticsService.buildPeriodStatistics(LocalDateTime startInclusive, LocalDateTime endExclusive): DashboardStatisticsDto` — `periodDays`, `previousTotal` 은 비워 둠, `chatTokens` 는 0
  - `StatisticsService.getPeriodTotal(LocalDateTime startInclusive, LocalDateTime endExclusive): StatisticsPeriodTotalDto` — `chatTokens` 는 0

Repository 추가 (모두 `start`/`end` 파라미터, `visited_at >= :start AND visited_at < :end`, 봇 제외):
- `PageVisitLogRepository`
  - `@Query(nativeQuery = true)` `List<Object[]> countDailyVisitsBetween(start, end)` → `[java.sql.Date day, Number uniqueVisitors, Number pageViews]` (`CAST(visited_at AS date)` 그룹)
  - `long countDistinctClientHashBetween(start, end)` (JPQL)
  - `long countPageViewsBetween(start, end)` (JPQL)
  - `List<Object[]> countGroupByPagePathBetween(start, end)` → `[String, Long]`
  - `List<Object[]> countGroupByReferrerBetween(start, end)` → `[String(nullable), Long]`
  - native `List<Object[]> countHeatmapBetween(start, end)` → `[Number isoDayOfWeek, Number hourBucket, Number pageViews]` (`EXTRACT(ISODOW FROM visited_at)`, `FLOOR(EXTRACT(HOUR FROM visited_at) / 3)`)
- `FeatureUsageLogRepository` (`featureName NOT IN :excludedFeatures` 조건 포함)
  - native `List<Object[]> countDailyUsageBetween(start, end, List<String> excludedFeatureNames)` → `[Date, Number]`
  - `long countUsageBetween(start, end, Collection<FeatureType> excludedFeatures)`
  - `List<Object[]> countGroupByFeatureNameBetween(start, end, Collection<FeatureType> excludedFeatures)`

집계 규칙:
- 일별: `startInclusive` 날짜부터 `endExclusive` 전날까지 빠짐없이 0 채움, 날짜 오름차순
- 기능: 사용 횟수 내림차순 상위 6개, 0 건 제외
- 인기 페이지: 경로를 첫 세그먼트로 정규화(`/dashboard` → `/`, `/notice/12` → `/notice`, 쿼리 문자열 제거) 후 합산, 내림차순 상위 5개
- 유입 경로: referrer 별 건수를 분류해 합산, `INTERNAL` 제외, 0 건 제외, 내림차순
- 히트맵: 7 × 8 = 56 칸 모두 반환(없는 칸 0), dayOfWeek → hourBucket 오름차순

Flyway (`CREATE INDEX IF NOT EXISTS` 만):
- `idx_page_visit_log_visited_at` ON `page_visit_log (visited_at)`
- `idx_feature_usage_log_used_at` ON `feature_usage_log (used_at)`
- 테이블이 아직 없는 완전 초기 DB 에서도 실패하지 않도록 엔티티 기준 `CREATE TABLE IF NOT EXISTS` 를 앞에 둔다 (CLAUDE.md Flyway 초기화 안전 전략)

- [ ] **Step 1: 실패하는 테스트 작성** (`StatisticsServicePeriodTest`, Mockito `mock()` 으로 두 Repository + `ServerOptionService`, 기간 `2026-10-24T00:00` ~ `2026-10-27T00:00`)
  - `기록이_없으면_모든_날짜를_0으로_채운다`: 모든 쿼리 빈 리스트 → `dailyStatistics` 날짜 `[10-24, 10-25, 10-26]`, 값 전부 0, `heatmapCells` 크기 56, `topPages`/`featureUsages`/`referrerSources` 빈 리스트
  - `일별_방문을_날짜에_맞춰_채운다`: 10-25 행 `[Date.valueOf("2026-10-25"), 3L, 9L]` → 10-25 만 `uniqueVisitors=3, pageViews=9`
  - `인기_페이지는_첫_경로로_합산해_상위_5개만`: `/`=5, `/dashboard`=4, `/notice/1`=2, `/notice/2`=2, `/login`=1, `/profile`=1, `/chatbot`=1, `/docker`=1 → 첫 항목 `("/", 9)`, 둘째 `("/notice", 4)`, 크기 5
  - `유입_경로는_내부_이동을_빼고_분류별로_합산`: 옵션값 `"lab.suhsaechan.kr"`, 행 `(null,5)`, `("https://www.google.com/",3)`, `("https://www.google.co.kr/",2)`, `("https://lab.suhsaechan.kr/x",7)` → `[(GOOGLE,5),(DIRECT,5)]` 두 개 (동률은 enum 선언 순서), INTERNAL 없음
  - `기능은_상위_6개_내림차순`: 7종 행 → 크기 6, 내림차순
  - `히트맵_칸에_값을_넣는다`: 행 `[4, 5, 11]` → `dayOfWeek=4, hourBucket=5` 칸만 11
- [ ] **Step 2: 실패 확인** — `source ~/.zshrc && ./gradlew :Suh-Domain-Statistics:test --tests '*StatisticsServicePeriodTest'` → 컴파일 실패
- [ ] **Step 3: DTO, Repository 메서드, `buildPeriodStatistics`, `getPeriodTotal`, Flyway 파일 구현** (StatisticsService 는 단계별 private 메서드로 나눠 공개 메서드는 호출 흐름만, 제외 기능 목록은 클래스 상수 `EXCLUDED_FEATURE_TYPES = EnumSet.of(LOGIN, PROFILE_VIEW, NOTICE_VIEW)`)
- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS. 이어서 `source ~/.zshrc && ./gradlew :Suh-Domain-Statistics:compileJava` 경고 외 오류 없음

---

### Task 3: 기간 통계 API (DashboardService + Controller + 캐시)

**Files:**
- Create: `Suh-Application/src/main/java/me/suhsaechan/application/dto/DashboardRequest.java` (`Integer periodDays`)
- Modify: `Suh-Application/src/main/java/me/suhsaechan/application/service/DashboardService.java`
- Modify: `Suh-Domain-Chatbot/.../repository/ChatMessageRepository.java` (실제 경로는 `find . -name ChatMessageRepository.java -path '*main*'`)
- Modify: `Suh-Web/src/main/java/me/suhsaechan/web/controller/api/DashboardController.java`
- Modify: `Suh-Web/src/main/java/me/suhsaechan/web/config/PublicEndpointConfig.java` (`"/api/dashboard/statistics"` 공개 추가, refresh 는 추가하지 않음. `/api/task/goal/list` 줄은 다른 화면이 쓰는지 확인 후 쓰는 곳이 없어도 이번엔 그대로 둠)
- Modify: `Suh-Web/src/main/java/me/suhsaechan/web/config/RedisConfig.java` (`"dashboardStatistics"` 5분 TTL)
- Test: `Suh-Application/src/test/java/me/suhsaechan/application/service/DashboardServiceStatisticsTest.java` (Application 모듈 `build.gradle` 에 테스트 의존성이 없으면 추가: 기존 모듈과 같은 방식)

**Interfaces:**
- Consumes: Task 2 `StatisticsService.buildPeriodStatistics`, `getPeriodTotal`, DTO 들
- Produces:
  - `DashboardService.validatePeriodDays(Integer periodDays): void`
  - `DashboardService.getDashboardStatistics(Integer periodDays): DashboardStatisticsDto` — `@Cacheable(value = "dashboardStatistics", key = "#periodDays")`
  - `DashboardService.refreshDashboardStatistics(Integer periodDays): DashboardStatisticsDto` — `@CachePut` 같은 키
  - `POST /api/dashboard/statistics` (공개, 200) / `POST /api/dashboard/statistics/refresh` (로그인 필요, 200) — 둘 다 `@ModelAttribute DashboardRequest`, 응답 `DashboardStatisticsDto`
  - `ChatMessageRepository`: native `List<Object[]> sumDailyTokensBetween(start, end)` → `[Date, Number]` (`COALESCE(input_tokens,0)+COALESCE(output_tokens,0)` 합), JPQL `Long sumTokensBetween(start, end)` (`COALESCE(SUM(...),0)`)

동작:
- 검증: 공개 메서드 `DashboardService.validatePeriodDays(Integer periodDays): void` (7/30/90 아니면·누락이면 `CustomException(ErrorCode.INVALID_PARAMETER)`), 컨트롤러가 캐시 메서드 호출 전에 먼저 호출
- 기간 계산은 `LocalDate.now()` 기준 Global Constraints 의 경계 그대로, `Clock` 주입 없이 테스트는 결과 날짜 개수와 Repository 호출 인자로 검증
- 결과: `buildPeriodStatistics(현재)` + `periodDays` + `previousTotal = getPeriodTotal(직전)` + 일별 `chatTokens` 합치기 + `currentTotal.chatTokens`/`previousTotal.chatTokens` = `sumTokensBetween`

- [ ] **Step 1: 실패하는 테스트 작성** (`DashboardServiceStatisticsTest`, Mockito)
  - `허용되지_않는_기간이면_INVALID_PARAMETER`: `validatePeriodDays(14)`, `validatePeriodDays(null)` → `CustomException` 의 `getErrorCode()` == `INVALID_PARAMETER`, `validatePeriodDays(30)` 은 예외 없음
  - `직전_기간은_같은_길이로_바로_앞`: `periodDays=7` → `getPeriodTotal` 호출 인자 end == `buildPeriodStatistics` 호출 인자 start, 두 구간 길이 7일
  - `챗봇_토큰을_날짜별로_합친다`: `sumDailyTokensBetween` 행 `[오늘 Date, 120]` → 오늘 `DailyStatisticsDto.chatTokens == 120`, 나머지 0
- [ ] **Step 2: 실패 확인** — `source ~/.zshrc && ./gradlew :Suh-Application:test --tests '*DashboardServiceStatisticsTest'` → 실패
- [ ] **Step 3: 구현** (Repository·Service·Controller·PublicEndpointConfig·RedisConfig)
- [ ] **Step 4: 통과 확인** — 같은 명령 PASS, 이어서 `source ~/.zshrc && ./gradlew test` 전체 PASS (아키텍처 규칙 포함)

---

### Task 4: 대시보드 화면 개편 (Chart.js) + Task D-Day 제거

**Files:**
- Create: `Suh-Web/src/main/resources/templates/fragments/dashboard-statistics.html` (`th:fragment="section"`)
- Create: `Suh-Web/src/main/resources/static/js/dashboard-statistics.js`
- Modify: `Suh-Web/src/main/resources/templates/pages/dashboard.html`
  - 서버 통계 `<section id="summary-section">`(현재 803~990행 부근) → `<section th:replace="~{fragments/dashboard-statistics :: section}">`
  - `<section id="task-dday-section">`(993~1031행 부근) 삭제
  - 페이지 스크립트에서 `loadDashboardSummary`, `refreshDashboardSummary`, `loadTaskDDay` 와 이 함수들만 쓰는 보조 함수·`$(document).ready` 호출 삭제 (`formatNumber`, `formatTokens` 는 다른 곳에서 안 쓰면 삭제)
  - 페이지 스크립트 앞에 `<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js"></script>` 와 `<script th:src="@{/js/dashboard-statistics.js}"></script>`
- Modify: `Suh-Web/src/main/resources/templates/fragments/header.html` (`window.MSG` 에 JS 용 문구 키 추가)
- Modify: `Suh-Web/src/main/resources/messages.properties`, `messages_ko.properties`, `messages_en.properties` (새 키 추가, 이번 변경으로 안 쓰게 된 `dashboard.stat.*`, `dashboard.task.*` 키는 다른 템플릿·JS 에서 안 쓰면 삭제)
- Regenerate: `Suh-Web/src/main/resources/static/css/tailwind.generated.css` (`cd Suh-Web/frontend && npm run build:css`)

**Interfaces:**
- Consumes: Task 3 API 와 `DashboardStatisticsDto` JSON (필드명 Task 2 그대로, `date` 는 `"yyyy-MM-dd"`), 기존 `/api/dashboard/summary` 의 `totalUniqueVisitors`, `totalPageViews`, `featureUsageCounts`, `totalInputTokens + totalOutputTokens`, `common.js` 의 `sendFormRequest(url, params, onSuccess, onError)`
- Produces: 전역 함수 `initDashboardStatistics()` (페이지 스크립트 ready 에서 호출), `refreshDashboardStatistics()` (새로고침 버튼 onclick)

화면 구성 (시안 A안, 위에서 아래):
1. 헤더: 제목(기존 `dashboard.summary.title`) + "봇 제외" 보조 문구 / 우측 기간 선택 `join` 버튼 3개(7일·30일·90일, 기본 30일, 선택 `btn-primary`) + 새로고침 아이콘 버튼(`sec:authorize="isAuthenticated()"`, `aria-label`)
2. KPI 카드 4개 (`grid-cols-1 sm:grid-cols-2 lg:grid-cols-4`): 라벨 / 기간 합계 / 증감 배지(`badge-success` 증가, `badge-error` 감소, 직전 0 이면 숨김) / Chart.js 스파크라인(축·범례·툴팁 없음) / "전체 누적 N" (summary 값; 기능 사용 누적 = `featureUsageCounts` 에서 제외 3종 뺀 합)
3. 일별 방문 추이 카드: Chart.js line, 방문자(선, primary) + 페이지뷰(채움 면, primary 저투명), `interaction.mode: 'index'` 툴팁, x 축 날짜 `M/D`
4. 2열(`lg:grid-cols-2`): 많이 쓴 기능(Chart.js 가로 bar, 라벨 = `window.MSG['dashboard.feature.' + featureName]`, 툴팁에 횟수·비중%) | 유입 경로(Chart.js doughnut + 옆 목록: 색 점·이름·건수·비중%)
5. 2열: 인기 페이지(Chart.js 가로 bar, 라벨 = 경로) | 요일×시간대 히트맵(CSS grid 8열 + 요일 라벨 열, 4단계 클래스 `bg-base-200`, `bg-primary/25`, `bg-primary/55`, `bg-primary` — 클래스 문자열을 JS 에 리터럴로 써서 Tailwind 가 스캔하게, 단계 = 최댓값 대비 0 / ≤1/3 / ≤2/3 / 그 이상, 칸마다 `title` 로 "목 15시~18시 · 11회")
6. 각 카드: 로딩 중 DaisyUI `skeleton`, 기간 데이터가 모두 0 이면 차트 대신 "아직 기록이 없어요" 빈 상태

JS 규칙:
- 차트 인스턴스는 모듈 내 객체에 보관, 다시 그릴 때 `destroy()` 후 생성
- 색: `--color-primary`, `--color-secondary`, `--color-accent`, `--color-info`, `--color-warning`, `--color-base-content`, `--color-base-300` 을 숨긴 probe 요소의 `getComputedStyle(...).color` 로 해석해 사용 (투명도 변형은 probe 에 `color-mix(in oklab, var(--color-primary) 30%, transparent)` 를 넣어 해석). 도넛 5색은 primary, secondary, accent, info, warning 순
- `document.documentElement` 의 `data-theme` 변경을 `MutationObserver` 로 감지해 마지막 응답으로 전부 다시 그림
- `formatChangePercent(current, previous)`: previous 가 0 이하면 `null`(배지 숨김), 아니면 반올림 정수 `+12%` / `-5%`
- 기간 버튼 클릭 → 해당 기간 재요청, 진행 중 요청 결과가 늦게 와도 마지막 선택 기간만 반영
- 사용자 표시 문구는 전부 `window.MSG` / `th:text` (하드코딩 한글 금지), 숫자는 `toLocaleString()`
- `alert()` 금지(`showToast`), HTML 이스케이프는 `common.js` 함수 재사용(규칙 `no-local-escape-html`)

필요한 문구 키 (ko / en):
- `dashboard.stats.subtitle` 봇 제외 · 기간 기준 / Bots excluded
- `dashboard.stats.period.7` 7일 / 7 days, `.30` 30일 / 30 days, `.90` 90일 / 90 days
- `dashboard.stats.kpi.visitors` 방문자 / Visitors, `.pageViews` 페이지뷰 / Page views, `.featureUsages` 기능 사용 / Feature uses, `.chatTokens` 챗봇 토큰 / Chatbot tokens
- `dashboard.stats.cumulative` 전체 누적 / All time
- `dashboard.stats.trend.title` 일별 방문 추이 / Daily visits
- `dashboard.stats.features.title` 많이 쓴 기능 / Top features
- `dashboard.stats.sources.title` 어디서 들어왔나 / Traffic sources
- `dashboard.stats.pages.title` 인기 페이지 / Top pages
- `dashboard.stats.heatmap.title` 언제 많이 오나 / When people visit, `dashboard.stats.heatmap.caption` 요일 × 3시간 단위 / Weekday × 3-hour blocks, `.less` 적음 / Less, `.more` 많음 / More
- `dashboard.stats.empty` 아직 기록이 없어요 / No data yet
- `dashboard.stats.unit.times` 회 / times
- `dashboard.stats.day.1`~`.7` 월~일 / Mon~Sun
- `dashboard.stats.source.GOOGLE` 구글 검색 / Google, `.NAVER` 네이버 검색 / Naver, `.GITHUB` GitHub / GitHub, `.DIRECT` 직접 접속 / Direct, `.OTHER` 기타 / Other
- `dashboard.feature.{FeatureType}` 각 enum 값 (CHATBOT 챗봇/Chatbot, GITHUB_ISSUE_HELPER 이슈 헬퍼/Issue helper, AI_TRANSLATOR AI 번역/AI translator, CONTAINER_LOG 컨테이너 로그/Container logs, GRASS_PLANTER 잔디 심기/Grass planter, SWAGGER_GENERATOR Swagger 생성/Swagger generator, TS_API_CONVERTER TS API 변환/TS API converter, SEJONG_AUTH 세종대 인증/Sejong auth, SUH_RANDOM 랜덤 엔진/Random engine)

- [ ] **Step 1: fragment, JS, dashboard.html 정리, 문구 키, header MSG 작성**
- [ ] **Step 2: CSS 재생성** — `source ~/.zshrc && cd Suh-Web/frontend && npm run build:css` 후 `ls -l ../src/main/resources/static/css/tailwind.generated.css` 크기 100KB 이상, `grep -c 'bg-primary\\/25' ../src/main/resources/static/css/tailwind.generated.css` ≥ 1
- [ ] **Step 3: 남은 참조 확인** — `grep -rn "task-dday\|loadTaskDDay\|stat-total-\|loadDashboardSummary" Suh-Web/src/main/resources` 결과 0줄
- [ ] **Step 4: 빌드·테스트** — `source ~/.zshrc && ./gradlew clean build` 성공 (테스트 포함)
- [ ] **Step 5: 실제 화면 확인은 메인 세션이 배포 후 수행** (라이트·다크, 데스크톱·모바일 폭, 기간 3종 전환, 콘솔 오류 0)
