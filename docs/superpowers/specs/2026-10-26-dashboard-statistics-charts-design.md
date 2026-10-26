# 대시보드 서버 통계 차트 개편 설계

## 의도
대시보드(`/`, `pages/dashboard.html`)의 "서버 통계" 섹션이 "전체 N / 오늘 +N" 숫자 타일 10개뿐이라 밋밋하다.
사용자는 방문자 추이와 어떤 기능이 쓰였는지를 Chart.js 차트로 보기 좋게 보고 싶어 한다.
시안 A안(한눈에 보는 분석 화면)으로 확정: https://claude.ai/artifact/Dwq8Wq44nq48tkZaX7S5Tr

## 목표
- 기간 선택(7일 / 30일 / 90일, 기본 30일)에 따라 아래를 한 섹션에 표시
  1. 핵심 숫자 카드 4개: 고유 방문자, 페이지뷰, 기능 사용, 챗봇 토큰
     - 기간 합계 + 직전 같은 길이 기간 대비 증감률(%) + 일별 스파크라인 + 전체 누적
  2. 일별 방문 추이: 고유 방문자(선) + 페이지뷰(면) 이중 시리즈, 툴팁
  3. 많이 쓴 기능: 기간 내 사용 횟수 상위 6개 가로 막대 + 비중(%)
  4. 유입 경로: 구글 검색 / 네이버 검색 / GitHub / 직접 접속 / 기타 비중 (누적 막대 + 목록)
  5. 인기 페이지 상위 5개 (`/`와 `/dashboard`는 `/`로 합산)
  6. 요일 × 3시간 단위 페이지뷰 히트맵 (7 × 8)
- 통계는 비로그인 방문자에게도 공개 (현재 `/api/dashboard/summary`와 동일)
- 라이트·다크 테마 모두 자연스럽게 표시, 모바일(~767px)에서 1열로 쌓임
- Task D-Day 섹션 대시보드에서 제거 (사용자 요청: 잘 안 씀)

## 하지 않는 것
- 포트폴리오 영역(소개·수상·프로젝트·라이브러리), 공지, 관리 도구 섹션 디자인 변경
- 새 수집 항목 추가 (기존 `PageVisitLog`, `FeatureUsageLog`, `ChatMessage` 만 사용)
- Task 도메인 자체 삭제 (대시보드 노출만 제거)
- 실시간(웹소켓) 갱신

## 실패로 보는 것
- IP·clientHash·원본 referrer URL 같은 개별 방문 정보가 공개 응답에 노출
- 다크 모드에서 차트 글자·격자가 안 보임
- 차트 집계 쿼리로 대시보드 첫 로딩이 눈에 띄게 느려짐

## 제약
- 프로젝트 CLAUDE.md 규칙 준수: 단일 Request/Response, POST multipart, `CustomException`, null 비교 금지(Optional), 하드코딩 색상 클래스 금지(시맨틱 토큰), 인라인 `style=""` 금지, Tailwind 대괄호 금지, `tailwind.generated.css` 는 `npm run build:css` 로만 재생성, 아키텍처 규칙 테스트 통과
- 시간대: 저장된 `visitedAt`/`usedAt` 는 서버 LocalDateTime 그대로 사용
- 봇(`isBot=true`) 제외, 고유 방문자는 `clientHash IS NOT NULL` 기준 COUNT DISTINCT

## 결정 사항
- 시안 A안 레이아웃 (사용자 결정)
- 공개 범위: 전체 공개 (사용자 "다 표시되도록")
- Task D-Day 제거 (사용자 결정)

## 내 판단으로 정한 것 (빌더 재량 아님)
- **Chart.js 4.4.7** 을 jsdelivr CDN 에서 버전 고정 + `defer` 로 대시보드 페이지에서만 로드 (`marked` 와 같은 방식)
- 차트 색은 DaisyUI 테마 CSS 변수(`--color-primary`, `--color-base-content`, `--color-base-300` 등)를 런타임에 읽어 적용하고, 테마 토글 시 차트를 다시 그림
- 새 API `POST /api/dashboard/statistics` (파라미터 `periodDays` = 7|30|90, 그 외 값은 `INVALID_PARAMETER`), 공개 엔드포인트 등록, 기간별 Redis 5분 캐시. 기존 summary(누적값)는 유지하고 KPI 카드의 "전체 누적"에 사용
- 응답 DTO 이름: `DashboardStatisticsDto` (기존 `DashboardSummaryDto` 와 나란히, 세부 DTO는 독립 클래스)
- 유입 경로 분류: referrer 호스트 기준. 자기 사이트 호스트는 집계 제외(사이트 내 이동). 자기 사이트 호스트 목록은 `ServerOptionKey.STATISTICS_INTERNAL_REFERRER_HOSTS`(쉼표 구분, 기본 `lab.suhsaechan.kr,suh-project.synology.me,localhost`). 서브도메인 포함 일치(`host == h` 또는 `host` 가 `.h` 로 끝남)
- 기능 이름 표시: `FeatureType` 별 i18n 키 (`dashboard.feature.{TYPE}`)
- 증감률: 직전 기간 0이면 증감 배지 숨김
- 성능: `page_visit_log(visited_at)`, `feature_usage_log(used_at)` 인덱스를 Flyway(파일명 버전은 작업 시점 `version.yml` 값, 현재 `2.5.111` → `V2_5_111__add_statistics_time_indexes.sql`, 같은 prefix 파일 존재 시 확인)로 `CREATE INDEX IF NOT EXISTS` 추가

- 기존 숫자 타일 10개와 "기능별 상세 통계" 묶음은 모두 새 섹션으로 대체. 기존 `/api/dashboard/summary` API는 그대로 두고 KPI 카드의 "전체 누적" 값으로만 사용
- 새로고침 버튼은 기간 선택 옆에 유지, 로그인 사용자에게만 표시(`sec:authorize`). `POST /api/dashboard/statistics/refresh` 는 기존 `summary/refresh` 처럼 비공개(로그인 필요), summary 캐시와 현재 기간 statistics 캐시를 함께 갱신
- 기능 통계(KPI "기능 사용", "많이 쓴 기능")에서 페이지 조회 성격 3종 제외: `LOGIN`, `PROFILE_VIEW`, `NOTICE_VIEW`
- 인기 페이지: 현재 기록 대상(`PageVisitInterceptor.TRACK_PAGES`) 그대로, 첫 경로 단위로 합산(`/notice/12` → `/notice`, `/dashboard` → `/`). 기록 대상 추가는 하지 않음
- 유입 경로: 페이지뷰 건수 기준(단위 "회"), 사이트 내 이동 제외

## 빌더에게 맡기는 것
- 집계 쿼리 형태(네이티브 SQL 또는 JPQL), 0 채우기 위치
- 히트맵 색 단계 계산(4단계, 최댓값 기준 비율)
- 스켈레톤·빈 상태 문구 세부
