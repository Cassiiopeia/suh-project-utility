# DaisyUI 네이티브 빌드타임 전환 + DevOps 페이지 완성 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tailwind v4 + DaisyUI 5를 빌드타임으로 정적 컴파일해 다크모드를 네이티브 테마로 전환하고(#216), suhDevopsTemplate 7탭 렌더를 완성한다(#208).

**Architecture:** `Suh-Web/frontend/`에서 `@tailwindcss/cli`로 `static/css/tailwind.generated.css`를 사전 생성(커밋)하고, header.html의 `cdn.tailwindcss.com` 런타임을 이 정적 CSS로 교체한다. 그 위에서 16개 페이지의 하드코딩 색을 semantic 토큰으로 치환하고 common.css의 수동 다크 오버라이드를 제거한다.

**Tech Stack:** Tailwind CSS v4.3.1, DaisyUI 5.5.23, @tailwindcss/cli (npm, 사내 미러 `npm.mirror.lab.somansa.com`), Thymeleaf, Spring Boot 정적 서빙.

**검증 방식 주의:** 이 작업은 CSS/HTML 도메인이라 JUnit/pytest 단위테스트가 없다. 각 Task의 "검증"은 (a) `tailwind.generated.css` 재생성 성공, (b) grep으로 산출물·소스 상태 확인, (c) 로컬 구동 육안 확인으로 대체한다. **gradle 빌드는 내부망 불가** — Spring 구동 검증은 사용자가 수행하고, 에이전트는 CSS 빌드(npm)와 grep까지 책임진다.

**커밋 규칙(절대):** 사용자가 명시적으로 "커밋해줘"라고 요청하기 전에는 `git add`/`git commit`을 실행하지 않는다. 각 Task의 "Commit" 스텝은 **사용자 승인 시 실행할 메시지 초안**일 뿐, 자동 실행 금지. 커밋 메시지에 Claude/AI 흔적 금지.

---

## File Structure

생성:
- `Suh-Web/frontend/package.json` — 빌드 의존성·스크립트
- `Suh-Web/frontend/tailwind.input.css` — `@import "tailwindcss"` + `@plugin "daisyui"` + safelist
- `Suh-Web/src/main/resources/static/css/tailwind.generated.css` — **생성물(커밋)**
- `.gitignore` 수정 — `Suh-Web/frontend/node_modules/` 추가

수정:
- `Suh-Web/src/main/resources/templates/fragments/header.html` — CDN→정적 CSS
- `Suh-Web/src/main/resources/static/css/common.css` — 수동 다크 오버라이드/devops-* 제거
- `Suh-Web/src/main/resources/static/js/common.js` — 테마 토글 light 일관화
- `Suh-Web/src/main/resources/templates/pages/*.html` (16개) — 하드코딩 색 치환
- `Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html` — 렌더 함수 7개

---

## Task 1: 프론트엔드 빌드 셋업 + 1차 CSS 생성

**Files:**
- Create: `Suh-Web/frontend/package.json`
- Create: `Suh-Web/frontend/tailwind.input.css`
- Create: `Suh-Web/src/main/resources/static/css/tailwind.generated.css` (생성물)
- Modify: `.gitignore`

- [ ] **Step 1: `.gitignore`에 node_modules 추가**

`.gitignore` 끝에 추가:
```
# Frontend build
Suh-Web/frontend/node_modules/
```

- [ ] **Step 2: `package.json` 작성**

`Suh-Web/frontend/package.json`:
```json
{
  "name": "suh-web-frontend",
  "private": true,
  "version": "1.0.0",
  "scripts": {
    "build:css": "tailwindcss -i ./tailwind.input.css -o ../src/main/resources/static/css/tailwind.generated.css --content \"../src/main/resources/templates/**/*.html\" --minify"
  },
  "devDependencies": {
    "@tailwindcss/cli": "^4.3.1",
    "tailwindcss": "^4.3.1",
    "daisyui": "^5.5.23"
  }
}
```

- [ ] **Step 3: `tailwind.input.css` 작성 (safelist 포함)**

`Suh-Web/frontend/tailwind.input.css`:
```css
@import "tailwindcss";

@plugin "daisyui" {
  themes: light --default, dark --prefersdark;
}

@custom-variant dark (&:where([data-theme="dark"], [data-theme="dark"] *));

/* JS 데이터 객체에서 문자열로 생성되어 정적 스캔이 누락할 수 있는 클래스 보강 */
@source inline("badge-{primary,secondary,info,success,warning,error,ghost,neutral}");
@source inline("btn-{primary,secondary,info,success,warning,error,ghost,outline,neutral}");
@source inline("bg-{primary,secondary,info,success,warning,error,neutral,accent}");
@source inline("alert-{info,success,warning,error}");
```

- [ ] **Step 4: 의존성 설치**

Run (Bash):
```bash
cd "d:/0-suh/project/suh-project-utility/Suh-Web/frontend" && npm install
```
Expected: `added N packages`, 에러 없음. (사내 미러로 다운로드)

- [ ] **Step 5: CSS 생성**

Run:
```bash
cd "d:/0-suh/project/suh-project-utility/Suh-Web/frontend" && npm run build:css
```
Expected: `🌼 daisyUI 5.5.23` 출력, `Done in ...ms`, 에러 없음.

- [ ] **Step 6: 생성물 검증**

Run:
```bash
cd "d:/0-suh/project/suh-project-utility"
grep -c 'data-theme=dark' Suh-Web/src/main/resources/static/css/tailwind.generated.css
grep -oE '\-\-color-(primary|base-100|base-content|warning|error)\b' Suh-Web/src/main/resources/static/css/tailwind.generated.css | sort -u
grep -c '<script\|cdn.tailwindcss' Suh-Web/src/main/resources/static/css/tailwind.generated.css
```
Expected: data-theme=dark ≥1, 색 변수 5개 모두 출력, script/cdn = 0.

- [ ] **Step 7: Commit (사용자 승인 시)**

```
DaisyUI 네이티브 빌드타임 전환 : feat : Tailwind v4 CLI 빌드 셋업 추가 및 정적 CSS 생성 https://github.com/Cassiiopeia/suh-project-utility/issues/216
```

---

## Task 2: header.html — CDN 런타임 제거, 정적 CSS 연결 (체크포인트)

**Files:**
- Modify: `Suh-Web/src/main/resources/templates/fragments/header.html`

- [ ] **Step 1: 현재 로드부 확인**

Read `header.html` 의 `<head th:fragment>` 블록. 대상: `cdn.tailwindcss.com` script, `daisyui@5.4.2/daisyui.css` link, `tailwind.config` script.

- [ ] **Step 2: CDN 3개 라인 제거 + 정적 CSS link 추가**

기존:
```html
  <!-- Tailwind CSS + DaisyUI 5 -->
  <script src="https://cdn.tailwindcss.com"></script>
  <link href="https://cdn.jsdelivr.net/npm/daisyui@5.4.2/daisyui.css" rel="stylesheet" type="text/css"/>
  <script>
    tailwind.config = {
      darkMode: ['class', '[data-theme="dark"]']
    }
  </script>
```
변경 후:
```html
  <!-- Tailwind v4 + DaisyUI 5 (빌드타임 생성, 자동 생성물) -->
  <link rel="stylesheet" type="text/css" th:href="@{/css/tailwind.generated.css}"/>
```
(`tailwind.generated.css`는 `common.css`보다 **먼저** 로드되어야 한다 — head 내 common.css link보다 위에 둔다.)

- [ ] **Step 3: FOUC 스크립트 일관화 (light도 명시)**

기존 FOUC 스크립트의 else 분기 `setAttribute('data-theme','light')`는 유지(이미 light 명시). 변경 불필요면 그대로 둔다. 확인만.

- [ ] **Step 4: grep 검증**

Run:
```bash
cd "d:/0-suh/project/suh-project-utility"
grep -n "cdn.tailwindcss\|daisyui@5.4.2\|tailwind.config" Suh-Web/src/main/resources/templates/fragments/header.html || echo "CDN 잔존 없음 OK"
grep -n "tailwind.generated.css" Suh-Web/src/main/resources/templates/fragments/header.html
```
Expected: CDN 잔존 없음, generated.css link 1개.

- [ ] **Step 5: 체크포인트 — 로컬 구동 육안 확인 (사용자 수행)**

사용자에게 요청: dev 프로파일로 구동해 임의 페이지 1개(예: 대시보드)에서 (a) btn 색 표시, (b) 다크 토글 동작, (c) FOUC 없음 확인. **여기서 깨지면 멈추고 재점검** (input.css 테마 설정, link 순서).

- [ ] **Step 6: Commit (사용자 승인 시)**

```
DaisyUI 네이티브 빌드타임 전환 : refactor : header CDN 런타임 제거 후 생성 CSS 연결 https://github.com/Cassiiopeia/suh-project-utility/issues/216
```

---

## Task 3: common.css — DaisyUI 색 오버라이드·버튼 보강·devops-* 제거

**Files:**
- Modify: `Suh-Web/src/main/resources/static/css/common.css`

- [ ] **Step 1: 제거 대상 식별 (읽고 목록화)**

Read 후 분류 (spec §4.3 기준):
- 29~100줄 부근 `.btn-primary/.btn-error/.btn-success/.btn-info/.btn-outline/.btn-ghost` 색 수동 보강 → 제거
- `[data-theme="dark"]`로 DaisyUI 컴포넌트(btn/badge/toast/alert/navbar/input/textarea/select/modal-box/divider/collapse) 색을 박은 규칙 → 제거
- `devops-*` 블록(약 1628~1697) → 제거
- **보존**: `.version-badge`, `.hide`, `.text-rotate-item`, `.sejong-auth-*`, `.thinking-*`, `.grass-cell-*`, badge-soft 색 정의, 페이지 레이아웃/애니메이션

- [ ] **Step 2: 버튼 색 수동 보강 제거**

29~100줄의 `.btn-primary{...}` 등 색 강제 블록 삭제. (DaisyUI 네이티브가 처리)

- [ ] **Step 3: DaisyUI 컴포넌트 다크 오버라이드 제거**

식별된 `[data-theme="dark"] .btn-*/.badge-*/.alert-*/.navbar/.input/.select/.modal-box/...` 색 규칙 삭제. 보존 목록 클래스는 건드리지 않는다.

- [ ] **Step 4: devops-* 블록 제거**

`.devops-hero`, `.devops-command-box`, `.devops-feature-card`, `.devops-flow-card`, `.devops-quick-start-card` 등 전부 삭제 (라이트/다크 양쪽). #208 새 마크업이 semantic 토큰을 쓰므로 불필요.

- [ ] **Step 5: CSS 재생성 + 검증**

Run:
```bash
cd "d:/0-suh/project/suh-project-utility/Suh-Web/frontend" && npm run build:css
cd "d:/0-suh/project/suh-project-utility"
echo "common.css 줄 수:"; wc -l Suh-Web/src/main/resources/static/css/common.css
echo "남은 data-theme dark:"; grep -c 'data-theme="dark"' Suh-Web/src/main/resources/static/css/common.css
echo "남은 devops-:"; grep -c 'devops-' Suh-Web/src/main/resources/static/css/common.css
```
Expected: 줄 수 유의미 감소, devops- = 0, data-theme="dark" 대폭 감소(보존분만 남음).

- [ ] **Step 6: 체크포인트 — 육안 (사용자)**

대시보드·번역·notice 페이지 라이트/다크 확인. 색 깨지면 어떤 클래스가 네이티브로 안 잡히는지 식별 후 보존 목록 재검토.

- [ ] **Step 7: Commit (사용자 승인 시)**

```
DaisyUI 네이티브 빌드타임 전환 : refactor : common.css 수동 다크 오버라이드 및 devops 블록 제거 https://github.com/Cassiiopeia/suh-project-utility/issues/216
```

---

## Task 4: 하드코딩 색상 → semantic 토큰 치환 (16개 페이지)

**Files:**
- Modify: `pages/dashboard.html`(182), `sejongAuth.html`(68), `suhRandom.html`(45), `somansaBusDashboard.html`(45), `taskTracker.html`(26), 그 외 11개

**치환 매핑 (spec §4.5):**
| 하드코딩 | → semantic |
|---|---|
| `bg-white`,`bg-gray-50/100` | `bg-base-100`/`bg-base-200` |
| `bg-gray-800/900` | `bg-base-300`/`bg-neutral` |
| `text-gray-500/600/700` | `text-base-content/70` |
| `border-gray-200/300` | `border-base-300` |
| `bg-blue-*`/`text-blue-*` | `bg-primary`/`text-primary` |
| `bg-green-*`→`bg-success` `bg-red-*`→`bg-error` `bg-amber/yellow-*`→`bg-warning` `bg-cyan-*`→`bg-info` |

**주의: 자동 sed 일괄치환 금지.** 파일별로 Read 후 맥락 보고 치환 (의미 색 오인 방지).

- [ ] **Step 1: dashboard.html 치환**

Read 후 위 매핑대로 Edit. 의미 불명확한 임의 색은 보존(주석 사유). 치환 후:
```bash
cd "d:/0-suh/project/suh-project-utility"
grep -cE 'bg-white|bg-gray-|text-gray-|border-gray-|(bg|text)-(blue|green|red|amber|yellow|cyan)-[0-9]' Suh-Web/src/main/resources/templates/pages/dashboard.html
```
Expected: 잔존 수가 의도적 보존분만 남도록 감소(182→소수).

- [ ] **Step 2: sejongAuth.html 치환** (동일 절차, grep 검증)
- [ ] **Step 3: suhRandom.html 치환** (동일)
- [ ] **Step 4: somansaBusDashboard.html 치환** (동일)
- [ ] **Step 5: taskTracker.html 치환** (동일)
- [ ] **Step 6: 나머지 11개 치환** (somansaBusMemberDetail, chatbotManagement, aiServer, openAiChat, noticeManagement, login, translator, dockerLogs, githubIssueHelper, error/403·500). 각 파일 Read→Edit→grep.

- [ ] **Step 7: 전체 CSS 재생성 + 전수 grep**

```bash
cd "d:/0-suh/project/suh-project-utility/Suh-Web/frontend" && npm run build:css
cd "d:/0-suh/project/suh-project-utility"
grep -rcE 'bg-white|text-gray-[0-9]|border-gray-[0-9]' Suh-Web/src/main/resources/templates/pages/ | grep -v ':0'
```
Expected: 잔존이 의도 보존분만(거의 0).

- [ ] **Step 8: 체크포인트 육안 (사용자)** — 치환한 페이지 라이트/다크 확인.

- [ ] **Step 9: Commit (사용자 승인 시)**

```
DaisyUI 네이티브 빌드타임 전환 : refactor : 전 페이지 하드코딩 색상 semantic 토큰 치환 https://github.com/Cassiiopeia/suh-project-utility/issues/216
```

---

## Task 5: suhDevopsTemplate.html — 렌더 함수 7개 구현 (#208)

**Files:**
- Modify: `Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html`

데이터 객체(`projectTypes`, `commentCommands`, `wizards`, `commonWorkflows`, `typeWorkflows`, `skills`, `featureDetails`, `installCommands`, `integratorCommand`)는 이미 존재. 244~251줄 빈 stub 7개를 실제 구현으로 교체. **CSP 준수: 인라인 style 금지, 클래스만. semantic 토큰 사용.**

- [ ] **Step 1: renderOverview** — 핵심 기능 카드(featureDetails 6개)를 `card bg-base-100` 그리드로, 클릭 시 `openFeatureModal(key)`. 버전/배지 요약.

- [ ] **Step 2: renderApply** — OS별(unix/windows) `integratorCommand`를 `mockup-code`로 표시 + 복사 버튼(`copyText`). "Use this template" 안내.

- [ ] **Step 3: renderCommands** — `typeFilterSelectHtml()` + `currentType` 필터로 `commentCommands` 중 해당 타입만 목록 렌더. `bindTypeSelects(el)` 호출.

- [ ] **Step 4: renderWizards** — 타입 필터 + `wizards` 목록. htmlpreview(`PREVIEW_BASE+html`) 링크 + docs(`REPO_BASE+doc`) 링크. html 없으면 docs만.

- [ ] **Step 5: renderWorkflows** — 타입 필터 + `commonWorkflows`(공통) & `typeWorkflows`(타입별) 섹션. wf명을 `table` 또는 `collapse`로.

- [ ] **Step 6: renderSkills** — 4플랫폼 설치명령(`installCommands`) `mockup-code` + 복사, `skills` 20개 `table`.

- [ ] **Step 7: renderSettings** — 필수 Secret(`_GITHUB_PAT_TOKEN`)·Org 설정 안내를 `alert`/`mockup-code`로.

- [ ] **Step 8: 필터 select 바인딩 연결 확인**

`renderCommands/Wizards/Workflows`가 `typeFilterSelectHtml()`을 포함하고, 렌더 직후 `bindTypeSelects(el)`를 호출하는지 확인. (setType→해당 탭 재렌더 경로 이미 존재)

- [ ] **Step 9: CSS 재생성(새 클래스 스캔) + grep 검증**

```bash
cd "d:/0-suh/project/suh-project-utility/Suh-Web/frontend" && npm run build:css
cd "d:/0-suh/project/suh-project-utility"
grep -c "innerHTML=''" Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html
grep -c "el.innerHTML *= *`" Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html
```
Expected: 빈 stub `innerHTML=''` = 0, 실제 템플릿 렌더 7개.

- [ ] **Step 10: 체크포인트 육안 (사용자)** — 7탭 클릭 시 내용 표시, 타입 select 변경 시 commands/wizards/workflows 필터 동작, 다크/라이트 확인.

- [ ] **Step 11: Commit (사용자 승인 시)**

```
suhDevopsTemplate 7탭 개편 : feat : 탭별 렌더 함수 7개 구현 https://github.com/Cassiiopeia/suh-project-utility/issues/208
```

---

## Task 6: 테마 토글 일관화 + 최종 검증

**Files:**
- Modify: `Suh-Web/src/main/resources/static/js/common.js`
- Modify: `Suh-Web/src/main/resources/static/css/tailwind.generated.css` (재생성)

- [ ] **Step 1: common.js initTheme light 명시 일관화**

`initTheme()`에서 light일 때 `removeAttribute('data-theme')` → `setAttribute('data-theme','light')`로 변경(header FOUC와 일치). 토글 change 핸들러의 else도 동일하게:
```javascript
if (isDark) {
  document.documentElement.setAttribute('data-theme', 'dark');
} else {
  document.documentElement.setAttribute('data-theme', 'light');
}
```
(change 핸들러 내부 `this.checked` 분기도 동일 패턴으로.)

- [ ] **Step 2: 생성물 상단에 자동생성 주석 확인**

`tailwind.generated.css` 최상단에 "자동 생성물 — 직접 수정 금지, frontend에서 npm run build:css로 재생성" 주석이 없으면, input.css 상단에 `/*! ... */` 주석 추가 후 재생성. (Tailwind는 input 주석을 보존)

- [ ] **Step 3: 최종 CSS 재생성**

```bash
cd "d:/0-suh/project/suh-project-utility/Suh-Web/frontend" && npm run build:css
```

- [ ] **Step 4: 전수 최종 검증**

```bash
cd "d:/0-suh/project/suh-project-utility"
echo "=== CDN 런타임 전 페이지 잔존 0 확인 ==="
grep -rn "cdn.tailwindcss" Suh-Web/src/main/resources/templates/ || echo "OK 0"
echo "=== generated css 존재 + 다크 토큰 ==="
grep -c 'data-theme=dark' Suh-Web/src/main/resources/static/css/tailwind.generated.css
echo "=== devops- 잔존 0 ==="
grep -rc 'devops-' Suh-Web/src/main/resources/static/css/common.css
```
Expected: cdn 0, 다크 토큰 ≥1, devops- 0.

- [ ] **Step 5: 최종 체크포인트 — 전 페이지 육안 (사용자)**

16개 페이지 전부 라이트/다크 토글, FOUC, 색 일관성 확인. (#216 성공 기준)

- [ ] **Step 6: Commit (사용자 승인 시)**

```
DaisyUI 네이티브 빌드타임 전환 : fix : 테마 토글 light 속성 일관화 및 생성 CSS 최종본 https://github.com/Cassiiopeia/suh-project-utility/issues/216
```

---

## Self-Review 결과

- **Spec 커버리지**: §4.1 빌드도구→Task1, §4.2 header→Task2, §4.3 common.css→Task3, §4.5 치환→Task4, §4.6 렌더→Task5, §4.4 토글일관화→Task6. 전부 매핑됨.
- **Placeholder**: 치환 매핑·코드·grep 명령 모두 구체 기재. "적절히 처리" 류 없음.
- **타입 일관성**: 함수명(`build:css`, `bindTypeSelects`, `typeFilterSelectHtml`, `openFeatureModal`, `copyText`, `setType`)을 기존 코드에서 확인된 이름으로 통일.
- gradle 빌드 불가 제약 반영 — Spring 구동은 사용자 체크포인트로 위임, 에이전트는 npm 빌드·grep 책임.
