# DaisyUI 네이티브 빌드타임 전환 + DevOps 페이지 완성 — 설계

- 관련 이슈: #216 (다크모드 DaisyUI 네이티브 테마 전환), #208 (suhDevopsTemplate 7탭 개편)
- 작성일: 2026-06-16
- PoC: 검증 완료 (이 문서 §3)

---

## 1. 배경 / 문제

### 1.1 두 이슈가 얽혀 있다
- **#208**: `suhDevopsTemplate.html`을 7탭으로 개편. 현재 working tree에 골격·데이터·필터엔진은 있으나 **렌더 함수 7개가 전부 빈 stub** → 페이지가 백지로 뜸. 미완성.
- **#216**: 다크모드를 DaisyUI 네이티브 테마로 전환. **거의 미착수.** `common.css` 3,737줄에 `[data-theme="dark"]` 수동 오버라이드 409개, `!important` 549개가 잔존.
- #208이 "DaisyUI semantic 토큰으로 작성하고 common.css의 devops-* 다크 블록 제거"를 요구하는데, 그 토큰이 다크모드에서 자동 적응하려면 #216(네이티브 테마)이 선행돼야 한다. → **#216을 먼저 확정하고 그 위에 #208을 구현한다.**

### 1.2 근본 원인 (git 이력으로 확정)
- 최초 도입(`8fc72fb`)은 `@tailwindcss/browser@4` + `daisyui@5` (정합 조합, 단 **브라우저 런타임**).
- 이후 `cdn.tailwindcss.com`(Tailwind **v3** Play CDN) + `daisyui@5.4.2`로 바뀜 → **비호환**. DaisyUI 5는 Tailwind v4의 `@theme`/`@plugin` 토큰 시스템 전제라, v3 CDN은 `base-100`·`base-content`·`btn-primary` 등 **색 토큰을 깔지 못함**.
- 이를 가리려고 `common.css`에 `[data-theme="dark"] .xxx { … !important }` 수동 오버라이드가 누적됨 → "마크업 추가할 때마다 다크모드 깨지고 !important로 막는" 악순환.
- v4 브라우저 런타임 복귀 시도(`5a50a36`)는 **revert**(`ee8b7bf`). 사유: **런타임 컴파일 FOUC + 내부망 검증 불가**.
- 결국 `8e2e35c`에서 common.css에 버튼 색만 수동으로 박아 임시 복구한 게 현재 상태.

**핵심 통찰: 과거 모든 시도는 "브라우저 런타임 컴파일"이었고 실패 원인은 항상 FOUC였다. 진짜 빌드타임(정적 CSS 사전 생성)은 한 번도 시도된 적 없다.** (`package.json`/`tailwind.config`가 레포에 커밋된 이력 0.)

---

## 2. 목표

1. DaisyUI 5가 정상 동작하는 **빌드타임 정적 CSS** 셋업으로 전환 → 런타임 컴파일 제거(FOUC 0).
2. 라이트/다크 테마가 DaisyUI 네이티브 테마 시스템으로 자동 적응.
3. 16개 페이지의 하드코딩 색상(494개)을 semantic 토큰으로 **전면 치환**.
4. `common.css`의 수동 다크모드 오버라이드·`!important` 대량 제거 → 색은 테마 단일 출처로.
5. `suhDevopsTemplate.html` 7탭 렌더 함수 완성 → 페이지 정상 동작.

> 사용자 결정: **전면 전환을 한 번에 진행**(점진 X). 셋업은 **로컬 빌드 + 산출 CSS 커밋**.

---

## 3. PoC 결과 (검증 완료)

이 PC(내부망)에서 사내 미러 `npm.mirror.lab.somansa.com`로 직접 컴파일해 확인:

- `tailwindcss@4.3.1` + `daisyui@5.5.23` **설치 성공**.
- `tailwindcss -i input.css -o output.css --content "**/*.html"` → **44KB 정적 CSS, 154ms**.
- input.css:
  ```css
  @import "tailwindcss";
  @plugin "daisyui" { themes: light --default, dark --prefersdark; }
  @custom-variant dark (&:where([data-theme="dark"], [data-theme="dark"] *));
  ```
- 산출물 검증:
  - ✅ `--color-primary/base-100/base-200/base-content/warning/error/info/success/neutral` 정적 정의
  - ✅ `.btn-primary { --btn-color: var(--color-primary) }` — 색 토큰 연결
  - ✅ `[data-theme=dark] { --color-base-100: oklch(25%…); --color-base-content: oklch(97%…) }` — 라이트와 다른 값
  - ✅ `input.theme-controller[value=dark]:checked` 지원 (기존 토글 호환)
  - ✅ 출력 CSS 내 `<script>`/런타임 0개 = **순수 정적 = FOUC 없음**
  - ✅ btn/badge/tabs/hero/card/alert/mockup-code/select/modal 컴포넌트 전부 생성

**결론: 빌드타임 방식은 동작하며, 과거 실패 원인(FOUC)을 근본 제거한다.**

---

## 4. 아키텍처 / 셋업 설계

### 4.1 빌드 도구 (프로젝트 루트 또는 Suh-Web 하위에 프론트 빌드 디렉토리)
```
Suh-Web/frontend/                 # 신규 (또는 프로젝트 루트)
├── package.json                  # tailwindcss@4, daisyui@5, @tailwindcss/cli
├── tailwind.input.css            # @import + @plugin daisyui + @custom-variant
└── (스캔 소스 = ../src/main/resources/templates/**/*.html)
```
- 빌드 명령: `npx @tailwindcss/cli -i tailwind.input.css -o ../src/main/resources/static/css/tailwind.generated.css --content "../src/main/resources/templates/**/*.html" --minify`
- **산출물 `tailwind.generated.css`를 git에 커밋**(로컬 빌드 + 커밋 방식).
- `package.json`에 `"build:css"` 스크립트로 고정. README/문서에 재생성 절차 명시.

### 4.2 header.html 변경
- 제거: `<script src="https://cdn.tailwindcss.com">`, `daisyui@5.4.2/daisyui.css`, `tailwind.config` JS.
- 추가: `<link th:href="@{/css/tailwind.generated.css}">` (common.css보다 **먼저** 로드).
- FOUC 스크립트(테마 즉시 적용)는 유지하되 §4.4 일관화 반영.

### 4.3 common.css 정리 (3,737줄 → 대폭 축소)
- **제거**: DaisyUI 컴포넌트 색 오버라이드(~51개 btn/badge/toast/alert/navbar/input/modal 등), Tailwind 유틸 다크 오버라이드(~30개), 그에 딸린 `!important` 다수. §1.2 버튼 색 수동 보강(29~100줄)도 제거 — 네이티브가 처리.
- **보존**: 순수 커스텀(`.version-badge`, `.hide`, `.text-rotate-item`, `.sejong-auth-*`, `.thinking-*`, 페이지별 레이아웃/애니메이션). 이들의 다크모드 색이 네이티브 토큰으로 대체 가능하면 토큰 참조로 바꾸고, 아니면 유지.
- **제거(#208)**: `devops-*` 블록(1628~1697) — 새 7탭 마크업이 semantic 토큰을 쓰므로 불필요.

### 4.4 테마 토글 일관화 (함정 수정)
- 현재 불일치: header FOUC는 light일 때 `setAttribute('data-theme','light')`, common.js `initTheme()`는 light일 때 `removeAttribute('data-theme')`.
- DaisyUI를 `light --default`로 깔면 속성 없어도 라이트지만, **일관성을 위해 양쪽 다 light일 때도 `data-theme="light"`를 명시**하도록 통일. localStorage 키 `theme`는 유지.

### 4.5 하드코딩 색상 치환 매핑 (494개)
| 하드코딩 | semantic 토큰 |
|---|---|
| `bg-white`, `bg-gray-50/100` | `bg-base-100` / `bg-base-200` |
| `bg-gray-800/900` | `bg-neutral` / `bg-base-300` |
| `text-gray-500/600/700` | `text-base-content/70` 등 |
| `text-black` / `text-white`(맥락) | `text-base-content` / `text-neutral-content` |
| `border-gray-200/300` | `border-base-300` |
| `bg-blue-500`,`text-blue-*` | `bg-primary` / `text-primary` |
| `bg-green-500` | `bg-success` | `bg-red-500` → `bg-error` | `bg-amber/yellow` → `bg-warning` | `bg-cyan` → `bg-info` |
- 의미가 불명확한 임의 색은 보존 또는 케이스별 판단(자동 일괄치환 금지 — 오인 치환 위험).

### 4.6 #208 렌더 함수 7개 구현
- 이미 정의된 데이터 객체(`projectTypes`, `commentCommands`, `wizards`, `commonWorkflows`, `typeWorkflows`, `skills`, `featureDetails`, `installCommands`, `integratorCommand`)를 사용.
- `renderOverview/Apply/Commands/Wizards/Workflows/Skills/Settings` 구현. CSP 준수(인라인 style 금지, 클래스만). commands/wizards/workflows는 타입 필터(`currentType`) 반영.

---

## 5. 작업 순서 (한 번에, 단 검증 체크포인트 둠)

1. 프론트 빌드 디렉토리 + package.json + tailwind.input.css 생성, `tailwind.generated.css` 1차 생성.
2. header.html 셋업 교체(CDN 제거 → 생성 CSS link). **체크포인트: 로컬 구동해 한 페이지 색/다크 확인.**
3. common.css에서 DaisyUI 색 오버라이드·버튼 보강·`!important` 색강제·devops-* 제거. CSS 재생성.
4. 16개 페이지 하드코딩 색 → semantic 토큰 전면 치환(dashboard 우선). 매 치환 후 CSS 재생성(스캔 반영).
5. suhDevopsTemplate.html 렌더 함수 7개 구현.
6. 테마 토글 일관화(header + common.js).
7. **전 페이지 라이트/다크 육안 검증**(내부망 직접 구동). CSS 재생성 최종본 커밋.

---

## 6. 리스크 / 대응

| 리스크 | 대응 |
|---|---|
| 생성 CSS가 스캔 못 한 동적 클래스 누락(JS가 문자열로 만든 클래스) | safelist 또는 input.css `@source inline(...)`, JS 생성 클래스는 스캔 소스에 포함되도록 확인 |
| 494개 일괄 치환 중 오인 치환(의미 색을 잘못 매핑) | 자동 sed 금지. 파일별로 읽고 맥락 판단해 치환 |
| common.css 대량 삭제로 보존 대상까지 제거 | §4.3 보존 목록 기준. 삭제 전 grep으로 사용처 확인 |
| 빌드 산출물과 소스 불일치(누가 css만 수정) | "color는 HTML 클래스로, 재생성은 build:css로"를 문서화. 생성 파일 상단에 "자동 생성 — 직접 수정 금지" 주석 |
| gradle 빌드 내부망 불가 | CSS 빌드는 npm(미러 OK)으로 독립. Spring은 정적 파일 서빙만 |

## 7. 성공 기준
- 전 페이지에서 `cdn.tailwindcss.com` 런타임 제거, `tailwind.generated.css` 정적 서빙.
- btn-primary 등 DaisyUI 색이 정상 표시(파란 배경 등), 다크/라이트 토글이 전 페이지 일관 동작, FOUC 없음.
- suhDevopsTemplate 7탭 모두 내용 렌더 + 타입 필터 동작.
- common.css `!important`/`[data-theme="dark"]` 규칙 수가 유의미하게 감소(색은 테마 단일 출처).
- 하드코딩 색상 잔존이 최소화(불가피한 케이스만, 사유 주석).
