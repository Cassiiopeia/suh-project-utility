# 전체 페이지 i18n 마이그레이션 — 설계 문서

- 작성일: 2026-06-16
- 대상: suh-project-utility (Spring Boot 3.4.2, Thymeleaf)
- 전제: i18n 기반 인프라(MessageSource, CookieLocaleResolver, 헤더 언어 토글)는 이미 구축·배포 완료

---

## 1. 배경 및 목표

i18n 기반은 이미 동작한다(header/footer/dashboard 일부 적용 검증 완료). 이제 **나머지 16개 페이지 + fragment의 모든 한글 UI 텍스트를 한/영 i18n으로 마이그레이션**한다.

HTML 정적 텍스트뿐 아니라 **JS 내부 한글 문자열(toast, alert, 동적 생성 텍스트)까지 전부** i18n화한다. 단, 고유명사·파일 경로·식별자는 제외한다.

### 현황 (한글 텍스트 분포)

| 페이지 | HTML 한글 | JS 한글 | 비고 |
|--------|----------|---------|------|
| dashboard | 109 (6 적용됨) | 62 | 학력/자격증/프로젝트명 다수 — 고유명사 제외 |
| suhRandom | 70 | 20 | |
| chatbotManagement | 62 | - | |
| taskTracker | 56 | 17 | |
| somansaBusDashboard | 50 | - | |
| aiServer | 45 | - | |
| grassPlanter | 43 | - | |
| sejongAuth | 42 | - | |
| noticeManagement | 35 | 46 | JS 한글 많음 |
| suhDevopsTemplate | 34 | - | |
| translator | 33 | 11 | 값 비교 로직 주의 |
| somansaBusMemberDetail | 31 | - | th:inline 이미 사용 |
| dockerLogs | 10 | - | |
| githubIssueHelper | 3 | - | |
| openAiChat | 2 | - | |
| login | 0 | - | 대상 없음 |

---

## 2. 핵심 아키텍처 — JS i18n 인프라

### 결정: 헤더 `head` fragment에 `window.MSG` 전역 주입

모든 페이지가 `~{fragments/header :: head(...)}`로 헤더를 가져오므로, 헤더 `head` fragment 안에 **Thymeleaf 인라인 표현식으로 메시지 키를 박은 `window.MSG` 객체**를 한 번 주입하면 모든 페이지 JS가 `window.MSG.키`로 접근할 수 있다.

```html
<!-- fragments/header.html, head fragment 내 common.js 로드 직후 -->
<script th:inline="javascript">
  window.MSG = {
    'common.toast.copySuccess': /*[[#{common.toast.copySuccess}]]*/ '복사되었습니다',
    'translator.toast.translateError': /*[[#{translator.toast.translateError}]]*/ '번역 중 오류가 발생했습니다',
    // ... 페이지별 JS에서 쓰는 키
  };
</script>
```

- Thymeleaf가 서버에서 현재 로케일에 맞는 값을 박아준다 — 별도 API 엔드포인트·런타임 fetch 불필요
- HTML(`#{}`)과 JS(`/*[[#{}]]*/`)가 **동일한 `messages_ko/en` 키 체계**를 공유
- JS 사용: `showToast(window.MSG['translator.toast.translateError'], 'negative')`
- `head`에서 정의되므로 모든 페이지 스크립트(DOM ready 이후 실행)보다 먼저 존재 — 타이밍 안전

### 대안 검토 (채택 안 함)
- `/api/i18n/messages` 엔드포인트 + fetch: 추가 API 호출·로딩 지연 발생. 인라인 주입이 더 단순.
- 페이지별 `<script th:inline>` 블록: 헤더 전역 주입이면 페이지마다 반복 불필요.

---

## 3. 작업 원칙 (페이지마다 적용)

### 3.1 번역 대상 선별 (가장 중요)

**i18n화 대상:**
- 화면에 보이는 UI 문구 (제목, 라벨, 버튼, 안내 문구)
- `placeholder`, `title` 속성의 한글
- JS의 toast/alert/동적 생성 UI 텍스트

**i18n화 제외 (절대 건드리지 않음):**
- 고유명사: 학력(`세종대학교`, `휘문고등학교`), 자격증(`정보처리기사`), 프로젝트명(`그녀를 구하라!`), 회사명(`데이원컴퍼니`)
- 파일 경로: `/images/ROMROM_로고.png`
- 식별자/이벤트명: `'대시보드-SAECHAN-LAB'`
- **값 비교 로직**: `text.includes('번역을 사용할 수 없습니다')` 처럼 문자열을 비교 조건으로 쓰는 경우 — i18n화하면 로케일 따라 비교가 깨진다. 이런 건 별도 판단(상수 키로 빼거나 그대로 두기).

### 3.2 HTML 치환
- 텍스트 노드: `<span th:text="#{key}">기존텍스트</span>` (기존 한글은 fallback 기본값으로 남김)
- 속성: `th:placeholder="#{key}"`, `th:title="#{key}"`
- 아이콘+텍스트 혼합: 텍스트만 `<span th:text>`로 감싼다

### 3.3 JS 치환
- `window.MSG['key']`로 교체
- 헤더 `head`의 `window.MSG` 객체에 해당 키 라인 추가

### 3.4 키 네이밍
`{페이지}.{영역}.{의미}` (예: `translator.placeholder.input`, `translator.toast.translateError`, `dashboard.card.totalVisitors`)
공통 재사용 텍스트는 `common.{영역}.{의미}` (예: `common.toast.copySuccess`)

---

## 4. 페이지 그룹 (단계적 진행)

자주 쓰는 핵심 페이지부터, 그룹 단위로 커밋·검증·배포한다.

- **그룹 1 (핵심)**: translator, suhRandom, taskTracker, noticeManagement
- **그룹 2 (관리)**: aiServer, chatbotManagement, grassPlanter, sejongAuth
- **그룹 3 (소만사/기타)**: somansaBusDashboard, somansaBusMemberDetail, suhDevopsTemplate, dockerLogs
- **그룹 4 (소량/마무리)**: githubIssueHelper, openAiChat, dashboard 나머지, header 나머지

각 그룹마다: messages_ko/en 키 추가 → HTML/JS 치환 → 정합성 검증 → 커밋. 그룹 단위 또는 전체 완료 후 배포.

---

## 5. 검증

각 페이지 작업 후:
1. 정합성: 페이지에서 쓰는 모든 `#{key}`·`window.MSG['key']`가 messages_ko/en 양쪽에 존재하는지 grep 검증
2. KO/EN 키 집합 일치 검증
3. 런타임(사용자 환경): `bootRun` 후 해당 페이지 한↔영 전환 확인 — 내부망 Gradle 제약상 사용자가 직접 수행

---

## 6. 엣지 케이스 / 리스크

- **JS 값 비교 로직** (§3.1): i18n화 시 비교 깨짐 → 신중 판단, 필요시 그대로 둠
- **th:inline 충돌**: 일부 페이지(login, somansaBusMemberDetail, conf)가 이미 `th:inline="javascript"` 사용 — 헤더 전역 `window.MSG`와 독립적이라 충돌 없음
- **고유명사 오역 방지**: §3.1 제외 목록 엄수
- **fallback**: 키 누락 시 messages.properties(한국어 기본 번들)로 fallback — 이미 구축됨
