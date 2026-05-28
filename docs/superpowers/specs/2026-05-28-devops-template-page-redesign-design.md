# SUH DevOps Template 페이지 전면 개편 — 설계 문서

- 작성일: 2026-05-28
- 대상 파일: `Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html`
- 보조 변경: `Suh-Web/src/main/resources/static/css/common.css` (devops-* 다크모드 블록 제거)
- 데이터 출처: `suh-github-template` (SUH-DEVOPS-TEMPLATE v2.9.9)

## 1. 배경 / 문제

현재 `suhDevopsTemplate.html`은 SUH-DEVOPS-TEMPLATE **v2.7.12** 기준이며, 그 이후 추가된 기능이 페이지에 반영되지 않았다. 누락/불완전 항목:

- **Agent Skills (20개, 4플랫폼)** — 페이지에 전혀 없음. integrator `skills` 모드(메뉴 5번)도 미노출.
- **마법사(wizard) 확장** — 현재 Flutter testflight/playstore만 암시. 실제로는 Flutter(testflight/playstore/firebase) + 공통(projects-sync-wizard, github-secrets-converter, github-projects-sync-worker)까지 존재.
- **워크플로우 전체 목록** — Synology(nonstop nginx/traefik), Nexus, GitHub Packages, Firebase CICD, Test APK/IPA, Projects Sync Manager, Issue Helper(API+Module), 라벨 동기화, 플러그인/util 버전 동기화 등 미노출.
- **버전 표기** — Hero 배지 `v2.7.12` → `v2.9.9`.

사용자가 "전체적으로 다 볼 수 있어야 한다"고 요구. 정보량이 많아 한 화면에서 탐색 가능한 구조가 필요하다. 동시에 DaisyUI 5와 잘 호환되게, 커스텀 CSS는 최대한 배제한다.

## 2. 목표

1. 템플릿의 **모든 기능**(Synology/서버지원/테스트빌드 CICD/전체 마법사/모든 명령어/AI 스킬)을 한 페이지에서 탐색 가능하게 노출.
2. **DaisyUI semantic 토큰** 사용으로 다크모드 자동 적응 → 커스텀 다크모드 오버라이드 CSS 제거.
3. CSP 준수(인라인 style 금지) 유지.
4. 서버(Controller/Service/Entity) 변경 없음 — 순수 템플릿 + 정적 JS만.

비목표: wizard HTML을 lab 사이트에 직접 임베드/호스팅하지 않는다(htmlpreview 외부 링크로 연결). 새 프로젝트 타입 추가나 백엔드 API 변경 없음.

## 3. 아키텍처

탭 기반 단일 페이지(SPA-like, 정적). 서버 라운드트립 없음.

```
[ Hero: v2.9.9 배지 + GitHub 링크 ]
[ DaisyUI tabs (tabs-lift) 7개:
   개요 | 적용 | 명령어 | 마법사 | 워크플로우 | AI 스킬 | 설정 ]
[ 각 탭 패널(tab-content) ]
```

- 활성 탭: URL hash(`#commands` 등) + `localStorage('devopsTab')`로 유지.
- **전역 프로젝트 타입 필터**: 명령어/마법사/워크플로우 탭 상단 공통 `select`. 선택 타입에 해당하는 항목만 노출. `localStorage('devopsType')` 공유.
- 모든 데이터는 `<script>` 내 JS 객체로 선언, `escapeHtml` 통해 안전 렌더(기존 패턴 유지).

### 데이터 객체

| 객체 | 내용 |
|------|------|
| `projectTypes` | 9종: spring, flutter, react, next, node, python, react-native, react-native-expo, basic. 각 {버전파일, 지원기능 배지[]} |
| `commentCommands` | `@suh-lab` 명령어 [{cmd, desc, icon, types[]}]. server build/destroy/status, build app, apk build, ios build, create qa |
| `wizards` | [{name, desc, types[], previewUrl, docUrl, icon}]. flutter 3종 + 공통 3종 |
| `workflows` | 공통 + 타입별 CICD [{name, desc, types[], synology:bool}] |
| `skills` | 20개 [{cmd, desc}] |
| `installCommands` | 4플랫폼 {claude, cursor, gemini, codex} 설치 명령 |
| `featureDetails` | 개요 modal 데이터(기존 유지, flutter/synology 등 6종) |

## 4. 탭별 내용

### 탭 1 · 개요
- 핵심 기능 6장 카드(클릭 → modal): 버전자동화 / AI체인지로그 / PR Preview / 이슈자동화 / Flutter CICD / Synology배포
- 자동화 흐름 badge 체인: main푸시 → 버전증가 → deploy PR → AI체인지로그 → 자동머지 → CICD배포
- 지원 타입 9종 표 (타입/설명/버전파일/지원기능 배지)

### 탭 2 · 적용 (integrator)
- OS 선택 radio(unix/windows) → 명령어(`mockup-code`) + 복사
- integrator 6모드 카드: full / version / workflows / issues / skills / interactive
- 빠른시작 2카드: 새 프로젝트("Use this template") / 기존(마법사 명령)

### 탭 3 · 명령어 (`@suh-lab`, 타입필터)
- 전역 타입 select
- 명령어 카드: `server build/destroy/status [branch]`, `build app`, `apk build`, `ios build`, `create qa`. 복사 버튼 + 대상타입 배지
- 필터 동작 예: react 선택 → `create qa`만 노출. spring/python → server 명령. flutter → app/apk/ios 명령

### 탭 4 · 마법사 (wizard, 타입필터)
- wizard 카드: Flutter(testflight, playstore, firebase), 공통(projects-sync, secrets-converter, projects-sync-worker)
- 각 카드: 용도 + "브라우저에서 열기"(htmlpreview 새 탭) + "문서"(docs 링크)
- htmlpreview URL 패턴: `https://htmlpreview.github.io/?https://github.com/Cassiiopeia/SUH-DEVOPS-TEMPLATE/blob/main/.github/util/<path>/<wizard>.html`

### 탭 5 · 워크플로우 (타입필터)
- 공통 워크플로우 표: 버전관리, README동기화, AutoChangelog, QA봇, IssueHelper(API+Module), 라벨동기화, ProjectsSync매니저, 플러그인/util버전동기화
- 타입별 CICD 표: Spring(Synology nonstop nginx·traefik, Nexus CI/Publish, GHPackages), Flutter(TestFlight, PlayStore, Firebase, TestAPK, IOS Test), React/Next(CI+CICD), Python(CI, Synology CICD/PR-Preview)
- Synology 워크플로우는 `badge badge-info` "Synology"로 구분 표시

### 탭 6 · AI 스킬
- 4플랫폼 설치명령 세그먼트(`tabs` 또는 카드): Claude(2줄 marketplace+install) / Cursor(파일복사) / Gemini(extension install) / Codex(marketplace add)
- integrator 연동 안내: `--mode skills` (대화형 메뉴 5번)
- 20개 스킬 표 (`/cassiiopeia:<skill>` / 용도)

### 탭 7 · 설정
- 필수 Secret: `_GITHUB_PAT_TOKEN` (scope: repo, workflow)
- Org 설정 체크리스트(Allow Actions PR / Read·Write)
- 배포 전 3대 작업: PAT 설정 / deploy 브랜치 생성 / CodeRabbit 활성화
- 문서 목록 표 8종(docs 링크)

## 5. DaisyUI / CSS 전략 (커스텀 최소화)

하드코딩 Tailwind 색 → DaisyUI semantic 토큰 교체. 다크모드 자동 적응으로 `devops-*` 오버라이드 CSS 제거.

| 현재(하드코딩) | 변경(semantic) |
|---|---|
| `bg-base-100 border-gray-200` | `card bg-base-100 border-base-300` |
| `bg-gray-50` `bg-gray-100` | `bg-base-200` |
| `text-gray-600` `text-gray-500` | `text-base-content/70` `text-base-content/60` |
| `bg-blue-50` `bg-green-50` 카드 | `alert alert-info` / `alert alert-success` |
| 명령어 박스 `bg-gray-100` | `mockup-code` |
| hero `bg-amber-200` 그라데이션 | `bg-base-200` 기반(그라데이션 유지 필요시 클래스 1~2줄만) |

### 사용 DaisyUI 컴포넌트(전부 표준)
`tabs tabs-lift` · `tab-content` · `select select-bordered` · `table table-zebra` · `badge badge-*` · `modal`/`dialog` · `collapse collapse-arrow` · `mockup-code` · `alert` · `card bg-base-100`

### common.css 변경
- **제거**: `[data-theme="dark"] .devops-hero`, `-overlay`, `-command-box`, `-command-item`, `-feature-card`, `-project-code`, `-flow-card`, `-quick-start-card` 블록 (현재 약 1525~1595줄). semantic 토큰 사용으로 불필요.
- **추가**: 없음 목표. hero 그라데이션 유지 시에만 최소 클래스.
- 그 외 `[data-theme="dark"] .bg-blue-50` 등 공용 오버라이드는 다른 페이지가 공유하므로 **건드리지 않음**(surgical scope).

### CSP / JS
- 인라인 `style=""` 금지 유지. 모든 스타일 클래스.
- JS: 탭 전환 + 타입 필터 로직 추가. 기존 `escapeHtml`/clipboard 복사/`showToast`/modal 패턴 유지. `data-tab`·`data-type` 속성 + 이벤트 위임.

## 6. 단위 경계 / 테스트 관점

- 각 탭 패널은 독립 — 한 탭 렌더 로직이 다른 탭에 영향 없음.
- 타입 필터는 순수 함수: `(allItems, selectedType) → visibleItems`. 데이터 객체 기반이라 항목 추가가 곧 노출(하드코딩 마크업 아님).
- 검증: 정적 페이지이므로 브라우저에서 dev 서버 띄워 직접 확인 — (1) 탭 전환, (2) 타입 필터 동작, (3) 복사 버튼, (4) wizard 링크 새 탭, (5) modal, (6) 라이트/다크 토글 시 색 정상.

## 7. 리스크 / 고려사항

- **htmlpreview 의존**: wizard HTML이 외부 htmlpreview 렌더에 의존. 깨질 경우 대비해 "문서" 링크(docs/*.md)를 항상 함께 제공.
- **버전 하드코딩**: Hero 배지 `v2.9.9`는 수동 값. 추후 버전업 시 갱신 필요(현 페이지도 동일 한계).
- **데이터 동기화**: 명령어/워크플로우 목록은 템플릿 repo와 수동 동기화. 데이터 객체로 분리해 갱신 용이하게.
- common.css 공용 오버라이드(`.bg-blue-50` 등)는 타 페이지 공유 → 제거 금지.

## 8. 예상 결과

- 7탭 단일 페이지에서 템플릿 전 기능 탐색 가능.
- 타입 선택만으로 "내 프로젝트에 쓸 수 있는 것"만 필터링.
- 커스텀 다크모드 CSS ~70줄 제거, DaisyUI semantic 토큰으로 다크모드 자동.
- 서버 변경 0.
