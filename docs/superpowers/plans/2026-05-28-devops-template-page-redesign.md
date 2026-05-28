# SUH DevOps Template 페이지 전면 개편 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `suhDevopsTemplate.html`을 7탭 단일 페이지로 재구성해 SUH-DEVOPS-TEMPLATE v2.9.9의 전 기능(Synology/서버/테스트빌드 CICD/전체 마법사/모든 @suh-lab 명령어/20개 AI 스킬)을 노출하고, DaisyUI semantic 토큰으로 다크모드 자동화해 커스텀 CSS를 제거한다.

**Architecture:** 정적 탭 페이지(서버 변경 0). 모든 데이터는 `<script>` 내 JS 객체로 선언 후 `escapeHtml`로 안전 렌더. 명령어/마법사/워크플로우 탭은 전역 프로젝트 타입 `select`로 필터링. DaisyUI 5 표준 컴포넌트(`tabs tabs-lift`, `select`, `table`, `mockup-code`, `alert`, `collapse`, `modal`, `badge`)만 사용.

**Tech Stack:** Thymeleaf, DaisyUI 5(CDN), Tailwind, jQuery, FontAwesome. 테스트는 브라우저 수동 검증(HTML 단위 테스트 프레임워크 없음).

---

## 사전 사실 (검증 완료)

- 라우트: `GET /suh-devops-template` → `PageController.java:85-87` → `pages/suhDevopsTemplate`. 공개 엔드포인트(`PublicEndpointConfig.java:54`).
- 페이지 파일: `Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html` (현재 751줄).
- 헤더/푸터 fragment: `~{fragments/header :: head(...)}`, `~{fragments/header :: header}`, `~{fragments/footer :: footer}`. jQuery·DaisyUI·FontAwesome·common.css는 header fragment에서 로드(기존 페이지가 `$(document).ready`, `fa-solid`, `card bg-base-100`, `tabs` 사용 중이므로 보장됨).
- 제거할 CSS: `common.css` **1525~1596줄** (`/* SUH DevOps Template 페이지 다크모드 */` 주석 + `.devops-*` 8개 dark 블록 + 1596 빈 줄). **1597줄 이후 `[data-theme="dark"] .bg-blue-50` 등 공용 오버라이드는 타 페이지 공유 → 절대 건드리지 않음.**
- 빌드/실행: `source ~/.zshrc && ./gradlew bootRun --args='--spring.profiles.active=dev'` → `http://localhost:8080/suh-devops-template`.
- 커밋 규칙: 사용자 명시 승인 없이 `git add`/`git commit` 금지. 각 Task 끝 "Commit" 스텝은 **사용자에게 diff 확인 요청 후 승인 시에만** 실행. Co-Authored-By 태그 금지.

## 데이터 인벤토리 (suh-github-template repo 기준, 하드코딩 값)

**프로젝트 타입(9)**: spring, flutter, react, next, node, python, react-native, react-native-expo, basic

**@suh-lab 명령어**:
- `@suh-lab server build [branch]` / `server destroy [branch]` / `server status [branch]` → spring, python
- `@suh-lab build app` / `apk build` / `ios build` → flutter
- `@suh-lab create qa` (키워드 순서 무관) → 전체

**마법사(wizard)** — htmlpreview URL = `https://htmlpreview.github.io/?https://github.com/Cassiiopeia/SUH-DEVOPS-TEMPLATE/blob/main/<path>`:
- flutter / `.github/util/flutter/testflight-wizard/testflight-wizard.html` — iOS TestFlight 설정 생성 — doc: docs/FLUTTER-TESTFLIGHT-WIZARD.md
- flutter / `.github/util/flutter/playstore-wizard/playstore-wizard.html` — Android Play Store 서명/Fastfile 생성 — doc: docs/FLUTTER-PLAYSTORE-WIZARD.md
- flutter / `.github/util/flutter/firebase-wizard/firebase-wizard.html` — Firebase 배포 설정 생성 — doc: docs/FLUTTER-CICD-OVERVIEW.md
- 공통(전체) / `.github/util/common/projects-sync-wizard/projects-sync-wizard.html` — GitHub Projects 동기화 Worker 설정 — doc: docs/GITHUB-PROJECTS-SYNC-WIZARD.md
- 공통(전체) / `.github/util/common/github-secrets-converter/secrets-converter.html` — GitHub Actions Secret 변환기 — doc: docs/TROUBLESHOOTING.md
- 공통(전체) / `.github/util/common/github-projects-sync-worker/` (HTML 없음, 가이드만) — Cloudflare Worker 소스 — doc: .github/util/common/github-projects-sync-worker/GITHUB_PROJECTS_SYNC_WORKER_GUIDE.md → **링크는 "문서 보기"만**

**워크플로우 — 공통**: 버전관리(VERSION-CONTROL), README동기화(README-VERSION-UPDATE), AutoChangelog(AUTO-CHANGELOG-CONTROL), QA봇(QA-ISSUE-CREATION-BOT), IssueHelper(SUH-ISSUE-HELPER-MODULE + SUH-ISSUE-HELPER-API), 라벨동기화(SYNC-ISSUE-LABELS), ProjectsSync매니저(PROJECTS-SYNC-MANAGER), 템플릿util버전동기화(TEMPLATE-UTIL-VERSION-SYNC), 플러그인버전동기화(TEMPLATE-PLUGIN-VERSION-SYNC)

**워크플로우 — 타입별**:
- spring: Synology nonstop nginx(PROJECT-SPRING-SYNOLOGY-NONSTOP-NGINX-CICD)[synology], Synology nonstop traefik(...-TRAEFIK-CICD)[synology], Synology simple(...-SYNOLOGY-SIMPLE-CICD)[synology], Synology PR Preview(...-SYNOLOGY-PR-PREVIEW)[synology], Nexus CI(...-NEXUS-CI)[synology], Nexus Publish(...-NEXUS-PUBLISH)[synology], GitHub Packages Publish(...-GITHUB-PACKAGES-PUBLISH)
- flutter: TestFlight(...-IOS-TESTFLIGHT), iOS Test TestFlight(...-IOS-TEST-TESTFLIGHT), Play Store(...-ANDROID-PLAYSTORE-CICD), Firebase(...-ANDROID-FIREBASE-CICD), Android Test APK(...-ANDROID-TEST-APK), Android Synology(...-ANDROID-SYNOLOGY-CICD)[synology], App Build Trigger(...-SUH-LAB-APP-BUILD-TRIGGER), CI(...-FLUTTER-CI)
- react: CI(PROJECT-REACT-CI), CICD(PROJECT-REACT-CICD)
- next: CI(PROJECT-NEXT-CI), CICD(PROJECT-NEXT-CICD)
- python: CI(PROJECT-PYTHON-CI), Synology CICD(...-SYNOLOGY-CICD)[synology], Synology PR Preview(...-SYNOLOGY-PR-PREVIEW)[synology]
- node/react-native/react-native-expo/basic: 타입 전용 CICD 없음(공통만)

**AI 스킬(20, `/cassiiopeia:<x>`)**: analyze, review, implement, plan, test, refactor, refactor-analyze, troubleshoot, document, report, issue, build, design, design-analyze, figma, ppt, testcase, suh-spring-test, init-worktree, (+ synology-expose 또는 동등 1개로 20 채움 — 실제 skills 폴더 기준: analyze/build/design/design-analyze/document/figma/implement/init-worktree/issue/plan/ppt/refactor/refactor-analyze/report/review/suh-spring-test/synology-expose/test/testcase/troubleshoot = 20)

**설치 명령(4플랫폼)**:
- Claude: `claude plugin marketplace add Cassiiopeia/SUH-DEVOPS-TEMPLATE` / `claude plugin install cassiiopeia@cassiiopeia-marketplace --scope user`
- Cursor: integrator `--mode skills` (파일 복사, 마켓플레이스 미지원)
- Gemini: `gemini extensions install https://github.com/Cassiiopeia/SUH-DEVOPS-TEMPLATE`
- Codex: `codex plugin marketplace add Cassiiopeia/SUH-DEVOPS-TEMPLATE`

---

## File Structure

- **Modify**: `Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html` — 전체 재작성(7탭 구조 + 데이터 객체 + 탭/필터 JS)
- **Modify**: `Suh-Web/src/main/resources/static/css/common.css` — 1525~1596줄 `.devops-*` dark 블록 제거
- Create: 없음
- Test: 없음(브라우저 수동 검증)

단일 파일 페이지이므로 Task는 "탭 단위 빌드 → 브라우저 검증 → 커밋"으로 분해. 모든 데이터 객체와 마크업이 같이 변하므로 한 파일 내에서 누적 작성.

---

### Task 1: HTML 골격 + 데이터 객체 + 탭/필터 인프라

**Files:**
- Modify: `Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html` (전체 재작성)

이 Task는 페이지의 뼈대(Hero, 7개 빈 탭, 전역 타입 select)와 모든 JS 데이터 객체 + 탭전환/타입필터 엔진을 만든다. 탭 내용은 Task 2~7에서 채운다.

- [ ] **Step 1: head/header/main 골격 + Hero + 탭 네비 작성**

기존 1~30줄(head/header/Hero)을 아래로 교체. Hero는 DaisyUI `hero` 사용, 버전 `v2.9.9`.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/header :: head(title='SUH DevOps Template - SAECHAN-LAB')}"></head>
<body class="min-h-screen">
<div th:replace="~{fragments/header :: header}"></div>

<main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
  <section class="hero bg-base-200 rounded-2xl mb-6">
    <div class="hero-content text-center flex-col py-10">
      <div class="flex justify-center gap-2 flex-wrap">
        <span class="badge badge-warning">v2.9.9</span>
        <span class="badge badge-ghost">GitHub Actions</span>
        <span class="badge badge-ghost">9 Project Types</span>
        <span class="badge badge-ghost">20 AI Skills</span>
      </div>
      <h1 class="text-3xl sm:text-4xl font-bold flex items-center justify-center gap-3">
        <i class="fa-solid fa-rocket text-warning"></i> SUH-DEVOPS-TEMPLATE
      </h1>
      <p class="text-base-content/70 text-sm sm:text-base">완전 자동화된 GitHub 프로젝트 관리 템플릿</p>
      <a href="https://github.com/Cassiiopeia/SUH-DEVOPS-TEMPLATE" target="_blank"
         class="btn btn-sm btn-neutral">
        <i class="fa-brands fa-github"></i> GitHub
      </a>
    </div>
  </section>

  <div role="tablist" class="tabs tabs-lift mb-6" id="mainTabs">
    <a role="tab" class="tab" data-tab="overview"><i class="fa-solid fa-star mr-1"></i> 개요</a>
    <a role="tab" class="tab" data-tab="apply"><i class="fa-solid fa-wand-magic-sparkles mr-1"></i> 적용</a>
    <a role="tab" class="tab" data-tab="commands"><i class="fa-solid fa-comment mr-1"></i> 명령어</a>
    <a role="tab" class="tab" data-tab="wizards"><i class="fa-solid fa-hat-wizard mr-1"></i> 마법사</a>
    <a role="tab" class="tab" data-tab="workflows"><i class="fa-solid fa-diagram-project mr-1"></i> 워크플로우</a>
    <a role="tab" class="tab" data-tab="skills"><i class="fa-solid fa-robot mr-1"></i> AI 스킬</a>
    <a role="tab" class="tab" data-tab="settings"><i class="fa-solid fa-gear mr-1"></i> 설정</a>
  </div>

  <section id="panel-overview" class="devops-panel"></section>
  <section id="panel-apply" class="devops-panel hide"></section>
  <section id="panel-commands" class="devops-panel hide"></section>
  <section id="panel-wizards" class="devops-panel hide"></section>
  <section id="panel-workflows" class="devops-panel hide"></section>
  <section id="panel-skills" class="devops-panel hide"></section>
  <section id="panel-settings" class="devops-panel hide"></section>
</main>
```

참고: `.hide`는 common.css 기존 유틸(`display:none`). `.devops-panel`은 마커용(스타일 없음, JS 셀렉터). hero 그라데이션 대신 `bg-base-200`로 다크 자동.

- [ ] **Step 2: 기능 상세 modal + footer + script 여는 태그**

```html
<dialog id="featureModal" class="modal">
  <div class="modal-box max-w-2xl">
    <form method="dialog"><button class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2">✕</button></form>
    <div id="modalHeader" class="flex items-center gap-3 mb-4"></div>
    <div id="modalContent" class="space-y-4"></div>
    <div class="modal-action">
      <a id="modalDocLink" href="#" target="_blank" class="btn btn-outline btn-sm"><i class="fa-solid fa-book"></i> 상세 문서 보기</a>
      <form method="dialog"><button class="btn">닫기</button></form>
    </div>
  </div>
  <form method="dialog" class="modal-backdrop"><button>close</button></form>
</dialog>

<div th:replace="~{fragments/footer :: footer}"></div>

<script>
const PREVIEW_BASE = 'https://htmlpreview.github.io/?https://github.com/Cassiiopeia/SUH-DEVOPS-TEMPLATE/blob/main/';
const REPO_BASE = 'https://github.com/Cassiiopeia/SUH-DEVOPS-TEMPLATE/blob/main/';
```

- [ ] **Step 3: 데이터 객체 선언 (script 내부)**

```javascript
const projectTypes = [
  { id: 'spring', label: 'Spring Boot', versionFile: 'build.gradle' },
  { id: 'flutter', label: 'Flutter', versionFile: 'pubspec.yaml' },
  { id: 'react', label: 'React', versionFile: 'package.json' },
  { id: 'next', label: 'Next.js', versionFile: 'package.json' },
  { id: 'node', label: 'Node.js', versionFile: 'package.json' },
  { id: 'python', label: 'Python', versionFile: 'pyproject.toml' },
  { id: 'react-native', label: 'React Native', versionFile: 'Info.plist + build.gradle' },
  { id: 'react-native-expo', label: 'React Native (Expo)', versionFile: 'app.json' },
  { id: 'basic', label: 'Basic (범용)', versionFile: 'version.yml' }
];

const commentCommands = [
  { cmd: '@suh-lab server build', desc: '임시 서버 빌드/배포 (브랜치 지정 가능)', icon: 'fa-server', types: ['spring','python'] },
  { cmd: '@suh-lab server destroy', desc: 'Preview 서버 삭제', icon: 'fa-trash', types: ['spring','python'] },
  { cmd: '@suh-lab server status', desc: '서버 상태 확인', icon: 'fa-circle-info', types: ['spring','python'] },
  { cmd: '@suh-lab build app', desc: 'iOS + Android 동시 빌드', icon: 'fa-mobile', types: ['flutter'] },
  { cmd: '@suh-lab apk build', desc: 'Android만 빌드', icon: 'fa-android', types: ['flutter'] },
  { cmd: '@suh-lab ios build', desc: 'iOS만 빌드', icon: 'fa-apple', types: ['flutter'] },
  { cmd: '@suh-lab create qa', desc: 'QA 이슈 자동 생성 (키워드 순서 무관)', icon: 'fa-clipboard-check', types: ['spring','flutter','react','next','node','python','react-native','react-native-expo','basic'] }
];

const wizards = [
  { name: 'iOS TestFlight 마법사', desc: 'TestFlight 배포 설정(ExportOptions.plist, Fastfile) 생성', icon: 'fa-apple', types: ['flutter'], html: '.github/util/flutter/testflight-wizard/testflight-wizard.html', doc: 'docs/FLUTTER-TESTFLIGHT-WIZARD.md' },
  { name: 'Android Play Store 마법사', desc: 'Play Store 서명/Fastfile/build.gradle 설정 생성', icon: 'fa-google-play', types: ['flutter'], html: '.github/util/flutter/playstore-wizard/playstore-wizard.html', doc: 'docs/FLUTTER-PLAYSTORE-WIZARD.md' },
  { name: 'Firebase 배포 마법사', desc: 'Firebase App Distribution 설정 생성', icon: 'fa-fire', types: ['flutter'], html: '.github/util/flutter/firebase-wizard/firebase-wizard.html', doc: 'docs/FLUTTER-CICD-OVERVIEW.md' },
  { name: 'GitHub Projects 동기화 마법사', desc: 'Projects 자동 동기화 Cloudflare Worker 설정 생성', icon: 'fa-diagram-project', types: ['spring','flutter','react','next','node','python','react-native','react-native-expo','basic'], html: '.github/util/common/projects-sync-wizard/projects-sync-wizard.html', doc: 'docs/GITHUB-PROJECTS-SYNC-WIZARD.md' },
  { name: 'GitHub Secrets 변환기', desc: 'Actions Secret 일괄 변환/인코딩 도구', icon: 'fa-key', types: ['spring','flutter','react','next','node','python','react-native','react-native-expo','basic'], html: '.github/util/common/github-secrets-converter/secrets-converter.html', doc: 'docs/TROUBLESHOOTING.md' },
  { name: 'Projects Sync Worker (소스)', desc: 'Cloudflare Worker 소스 — 가이드 문서 참고', icon: 'fa-cloud', types: ['spring','flutter','react','next','node','python','react-native','react-native-expo','basic'], html: '', doc: '.github/util/common/github-projects-sync-worker/GITHUB_PROJECTS_SYNC_WORKER_GUIDE.md' }
];

const ALL_TYPES = projectTypes.map(t => t.id);
const commonWorkflows = [
  { name: '버전 자동 관리', wf: 'PROJECT-COMMON-VERSION-CONTROL', desc: 'main 푸시 시 patch 버전 증가 + 태그' },
  { name: 'README 버전 동기화', wf: 'PROJECT-COMMON-README-VERSION-UPDATE', desc: 'README 버전 섹션 자동 갱신' },
  { name: 'AI 체인지로그', wf: 'PROJECT-COMMON-AUTO-CHANGELOG-CONTROL', desc: 'CodeRabbit 기반 CHANGELOG 자동 생성/머지' },
  { name: 'QA 이슈 봇', wf: 'PROJECT-COMMON-QA-ISSUE-CREATION-BOT', desc: '@suh-lab create qa 댓글로 QA 이슈 생성' },
  { name: 'Issue Helper (Module)', wf: 'PROJECT-COMMON-SUH-ISSUE-HELPER-MODULE', desc: '브랜치명/커밋 메시지 자동 제안' },
  { name: 'Issue Helper (API)', wf: 'PROJECT-COMMON-SUH-ISSUE-HELPER-API', desc: 'lab 사이트 API 연동 이슈 헬퍼' },
  { name: '이슈 라벨 동기화', wf: 'PROJECT-COMMON-SYNC-ISSUE-LABELS', desc: '표준 라벨 세트 자동 동기화' },
  { name: 'Projects 동기화 매니저', wf: 'PROJECT-COMMON-PROJECTS-SYNC-MANAGER', desc: '라벨↔Projects 상태 동기화' },
  { name: '템플릿 Util 버전 동기화', wf: 'PROJECT-COMMON-TEMPLATE-UTIL-VERSION-SYNC', desc: 'util 모듈 버전 자동 동기화' }
];

const typeWorkflows = [
  { name: 'Spring Synology 무중단 (Nginx)', wf: 'PROJECT-SPRING-SYNOLOGY-NONSTOP-NGINX-CICD', desc: 'Nginx 기반 Blue-Green 무중단 배포', types: ['spring'], synology: true },
  { name: 'Spring Synology 무중단 (Traefik)', wf: 'PROJECT-SPRING-SYNOLOGY-NONSTOP-TRAEFIK-CICD', desc: 'Traefik 기반 Blue-Green 무중단 배포', types: ['spring'], synology: true },
  { name: 'Spring Synology Simple', wf: 'PROJECT-SPRING-SYNOLOGY-SIMPLE-CICD', desc: 'Synology 단순 배포', types: ['spring'], synology: true },
  { name: 'Spring PR Preview', wf: 'PROJECT-SPRING-SYNOLOGY-PR-PREVIEW', desc: '@suh-lab server 명령으로 임시 서버 배포', types: ['spring'], synology: true },
  { name: 'Spring Nexus CI', wf: 'PROJECT-SPRING-NEXUS-CI', desc: 'Nexus 빌드 검증', types: ['spring'], synology: true },
  { name: 'Spring Nexus Publish', wf: 'PROJECT-SPRING-NEXUS-PUBLISH', desc: 'Nexus 아티팩트 배포', types: ['spring'], synology: true },
  { name: 'Spring GitHub Packages', wf: 'PROJECT-SPRING-GITHUB-PACKAGES-PUBLISH', desc: 'GitHub Packages 라이브러리 배포', types: ['spring'], synology: false },
  { name: 'Flutter iOS TestFlight', wf: 'PROJECT-FLUTTER-IOS-TESTFLIGHT', desc: 'TestFlight 자동 업로드', types: ['flutter'], synology: false },
  { name: 'Flutter iOS Test TestFlight', wf: 'PROJECT-FLUTTER-IOS-TEST-TESTFLIGHT', desc: '테스트 빌드 TestFlight 업로드', types: ['flutter'], synology: false },
  { name: 'Flutter Play Store', wf: 'PROJECT-FLUTTER-ANDROID-PLAYSTORE-CICD', desc: 'Play Store 내부 테스트 배포', types: ['flutter'], synology: false },
  { name: 'Flutter Firebase', wf: 'PROJECT-FLUTTER-ANDROID-FIREBASE-CICD', desc: 'Firebase App Distribution 배포', types: ['flutter'], synology: false },
  { name: 'Flutter Android Test APK', wf: 'PROJECT-FLUTTER-ANDROID-TEST-APK', desc: '테스트 APK 빌드/아티팩트', types: ['flutter'], synology: false },
  { name: 'Flutter Android Synology', wf: 'PROJECT-FLUTTER-ANDROID-SYNOLOGY-CICD', desc: 'Synology APK 배포', types: ['flutter'], synology: true },
  { name: 'Flutter App Build Trigger', wf: 'PROJECT-FLUTTER-SUH-LAB-APP-BUILD-TRIGGER', desc: '@suh-lab build app 댓글 트리거', types: ['flutter'], synology: false },
  { name: 'Flutter CI', wf: 'PROJECT-FLUTTER-CI', desc: 'PR 빌드 검증', types: ['flutter'], synology: false },
  { name: 'React CI', wf: 'PROJECT-REACT-CI', desc: 'PR 빌드 검증', types: ['react'], synology: false },
  { name: 'React CICD', wf: 'PROJECT-REACT-CICD', desc: 'Docker 빌드/배포', types: ['react'], synology: false },
  { name: 'Next.js CI', wf: 'PROJECT-NEXT-CI', desc: 'PR 빌드 검증', types: ['next'], synology: false },
  { name: 'Next.js CICD', wf: 'PROJECT-NEXT-CICD', desc: 'Docker 빌드/배포', types: ['next'], synology: false },
  { name: 'Python CI', wf: 'PROJECT-PYTHON-CI', desc: 'PR 빌드 검증', types: ['python'], synology: false },
  { name: 'Python Synology CICD', wf: 'PROJECT-PYTHON-SYNOLOGY-CICD', desc: 'Synology Docker 배포', types: ['python'], synology: true },
  { name: 'Python PR Preview', wf: 'PROJECT-PYTHON-SYNOLOGY-PR-PREVIEW', desc: '@suh-lab server 명령으로 임시 서버 배포', types: ['python'], synology: true }
];

const skills = [
  { cmd: 'analyze', desc: '코드 분석 (구현 없이 분석만)' },
  { cmd: 'review', desc: '코드 리뷰 (버그/보안/개선점)' },
  { cmd: 'implement', desc: '기능 구현 (코드 스타일 자동 감지)' },
  { cmd: 'plan', desc: '구현 계획 수립' },
  { cmd: 'test', desc: '테스트 코드 생성' },
  { cmd: 'refactor', desc: '리팩토링 적용' },
  { cmd: 'refactor-analyze', desc: '리팩토링 분석 (Plan only)' },
  { cmd: 'troubleshoot', desc: '체계적 디버깅' },
  { cmd: 'document', desc: '문서화' },
  { cmd: 'report', desc: '구현 보고서 생성' },
  { cmd: 'issue', desc: 'GitHub 이슈 초안 작성' },
  { cmd: 'build', desc: '빌드 자동화' },
  { cmd: 'design', desc: '시스템 설계 + 구현' },
  { cmd: 'design-analyze', desc: '설계 분석 (Plan only)' },
  { cmd: 'figma', desc: 'Figma → 반응형 코드 변환' },
  { cmd: 'ppt', desc: '기술 발표 자료 작성' },
  { cmd: 'testcase', desc: 'QA 테스트케이스 생성' },
  { cmd: 'suh-spring-test', desc: 'Spring 테스트 코드 생성' },
  { cmd: 'init-worktree', desc: 'Git worktree 자동 생성' },
  { cmd: 'synology-expose', desc: 'Synology 서비스 외부 노출 가이드' }
];

const installCommands = {
  claude: 'claude plugin marketplace add Cassiiopeia/SUH-DEVOPS-TEMPLATE\nclaude plugin install cassiiopeia@cassiiopeia-marketplace --scope user',
  cursor: 'bash <(curl -fsSL "https://raw.githubusercontent.com/Cassiiopeia/SUH-DEVOPS-TEMPLATE/main/template_integrator.sh") --mode skills',
  gemini: 'gemini extensions install https://github.com/Cassiiopeia/SUH-DEVOPS-TEMPLATE',
  codex: 'codex plugin marketplace add Cassiiopeia/SUH-DEVOPS-TEMPLATE'
};

const integratorCommand = {
  unix: 'bash <(curl -fsSL "https://raw.githubusercontent.com/Cassiiopeia/SUH-DEVOPS-TEMPLATE/main/template_integrator.sh")',
  windows: '$wc=New-Object Net.WebClient;$wc.Encoding=[Text.Encoding]::UTF8;iex $wc.DownloadString("https://raw.githubusercontent.com/Cassiiopeia/SUH-DEVOPS-TEMPLATE/main/template_integrator.ps1")'
};

const featureDetails = {
  version: { title: '버전 자동화', icon: 'fa-tag', bgColor: 'bg-primary', description: 'main 브랜치에 푸시하면 patch 버전이 자동 증가하고 Git 태그가 생성됩니다.', details: ['version.yml에서 현재 버전 관리', 'main 푸시 시 patch 자동 증가 (1.0.0 → 1.0.1)', 'Git 태그 자동 생성/푸시', 'deploy PR에 버전 정보 포함'], docLink: REPO_BASE + 'docs/VERSION-CONTROL.md' },
  changelog: { title: 'AI 체인지로그', icon: 'fa-robot', bgColor: 'bg-secondary', description: 'CodeRabbit AI가 PR 변경사항을 분석해 CHANGELOG.md를 자동 생성합니다.', details: ['CodeRabbit 연동 AI 코드 리뷰', 'PR 머지 시 CHANGELOG 자동 업데이트', '한국어 요약 생성', '버전별 변경 이력 정리'], docLink: REPO_BASE + 'docs/CHANGELOG-AUTOMATION.md' },
  preview: { title: 'PR Preview', icon: 'fa-server', bgColor: 'bg-success', description: 'Issue/PR 댓글로 임시 서버를 배포하고, 닫으면 자동 삭제합니다.', details: ['@suh-lab server build 댓글로 생성', 'Synology NAS Docker 배포', '고유 포트 자동 할당', 'Issue/PR 닫히면 자동 삭제'], docLink: REPO_BASE + 'docs/PR-PREVIEW.md' },
  issue: { title: '이슈 자동화', icon: 'fa-code-branch', bgColor: 'bg-warning', description: '이슈 생성 시 브랜치명/커밋 메시지를 제안하고 QA 이슈를 생성합니다.', details: ['Issue Helper 브랜치명 생성', '커밋 메시지 형식 제안', '@suh-lab create qa로 QA 이슈 생성', '4종 이슈 템플릿 제공'], docLink: REPO_BASE + 'docs/ISSUE-AUTOMATION.md' },
  flutter: { title: 'Flutter CI/CD', icon: 'fa-mobile', bgColor: 'bg-info', description: 'Flutter 앱을 TestFlight/Play Store/Firebase에 자동 배포합니다.', details: ['@suh-lab build app으로 iOS+Android 빌드', 'TestFlight 자동 업로드', 'Play Store 내부 테스트 배포', 'Firebase App Distribution / Test APK'], docLink: REPO_BASE + 'docs/FLUTTER-CICD-OVERVIEW.md' },
  synology: { title: 'Synology 배포', icon: 'fa-hard-drive', bgColor: 'bg-accent', description: 'Synology NAS에 Docker 기반 무중단 배포를 수행합니다.', details: ['deploy PR 머지 시 자동 배포', 'Docker 이미지 빌드/푸시', 'Container Manager SSH 연동', 'Blue-Green 무중단 (Nginx/Traefik)'], docLink: REPO_BASE + 'docs/SYNOLOGY-DEPLOYMENT-GUIDE.md' }
};
```

- [ ] **Step 4: 공용 유틸 + 탭/필터 엔진 + 렌더 디스패처 작성**

```javascript
let currentTab = (location.hash || '').replace('#','') || localStorage.getItem('devopsTab') || 'overview';
let currentType = localStorage.getItem('devopsType') || 'spring';
const VALID_TABS = ['overview','apply','commands','wizards','workflows','skills','settings'];
if (!VALID_TABS.includes(currentTab)) currentTab = 'overview';

function escapeHtml(text){ const d=document.createElement('div'); d.textContent=text; return d.innerHTML; }

function copyText(text, label){
  navigator.clipboard.writeText(text).then(()=>showToast((label||'명령어')+'가 복사되었습니다!','positive'))
    .catch(()=>showToast('복사에 실패했습니다.','negative'));
}

function typeFilterSelectHtml(){
  const opts = projectTypes.map(t=>`<option value="${t.id}"${t.id===currentType?' selected':''}>${escapeHtml(t.label)}</option>`).join('');
  return `<label class="form-control w-full max-w-xs mb-4">
    <span class="label-text text-sm font-medium mb-1 block">프로젝트 타입 선택</span>
    <select class="select select-bordered select-sm devops-type-select">${opts}</select>
  </label>`;
}

function switchTab(tab){
  currentTab = tab;
  localStorage.setItem('devopsTab', tab);
  history.replaceState(null, '', '#'+tab);
  document.querySelectorAll('#mainTabs .tab').forEach(t=>t.classList.toggle('tab-active', t.dataset.tab===tab));
  document.querySelectorAll('.devops-panel').forEach(p=>p.classList.add('hide'));
  document.getElementById('panel-'+tab).classList.remove('hide');
  renderTab(tab);
}

function setType(type){
  currentType = type;
  localStorage.setItem('devopsType', type);
  ['commands','wizards','workflows'].forEach(t=>{ if(t===currentTab) renderTab(t); });
}

function renderTab(tab){
  const el = document.getElementById('panel-'+tab);
  if (el.dataset.rendered === 'true' && !['commands','wizards','workflows'].includes(tab)) return;
  switch(tab){
    case 'overview': renderOverview(el); break;
    case 'apply': renderApply(el); break;
    case 'commands': renderCommands(el); break;
    case 'wizards': renderWizards(el); break;
    case 'workflows': renderWorkflows(el); break;
    case 'skills': renderSkills(el); break;
    case 'settings': renderSettings(el); break;
  }
  el.dataset.rendered = 'true';
}

function bindTypeSelects(scope){
  scope.querySelectorAll('.devops-type-select').forEach(sel=>{
    sel.addEventListener('change', e=>setType(e.target.value));
  });
}

function openFeatureModal(key){
  const f = featureDetails[key];
  document.getElementById('modalHeader').innerHTML =
    `<div class="w-12 h-12 rounded-lg ${f.bgColor} flex items-center justify-center"><i class="fa-solid ${f.icon} text-white text-xl"></i></div><h3 class="font-bold text-xl">${escapeHtml(f.title)}</h3>`;
  document.getElementById('modalContent').innerHTML =
    `<p class="text-base-content/70">${escapeHtml(f.description)}</p><ul class="list-disc list-inside space-y-2 text-sm text-base-content/80">${f.details.map(d=>`<li>${escapeHtml(d)}</li>`).join('')}</ul>`;
  document.getElementById('modalDocLink').href = f.docLink;
  document.getElementById('featureModal').showModal();
}

// 렌더 함수 stub (Task 2~7에서 구현)
function renderOverview(el){ el.innerHTML=''; }
function renderApply(el){ el.innerHTML=''; }
function renderCommands(el){ el.innerHTML=''; }
function renderWizards(el){ el.innerHTML=''; }
function renderWorkflows(el){ el.innerHTML=''; }
function renderSkills(el){ el.innerHTML=''; }
function renderSettings(el){ el.innerHTML=''; }

$(document).ready(function(){
  document.querySelectorAll('#mainTabs .tab').forEach(tab=>{
    tab.addEventListener('click', function(e){ e.preventDefault(); switchTab(this.dataset.tab); });
  });
  switchTab(currentTab);
});
</script>
</body>
</html>
```

- [ ] **Step 5: 빌드 후 브라우저 검증**

Run: `source ~/.zshrc && ./gradlew bootRun --args='--spring.profiles.active=dev'` (백그라운드), `http://localhost:8080/suh-devops-template` 열기.
Expected: Hero(v2.9.9), 7개 탭 클릭 시 활성 표시 전환, 패널이 빈 채로 토글됨, 콘솔 에러 없음, hash(`#commands`) 갱신, 새로고침 시 탭 유지.

- [ ] **Step 6: Commit** (사용자에게 diff 확인 요청 후 승인 시에만)

```bash
git add Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html
git commit -m "$(cat <<'EOF'
DevOps Template 페이지 개편 : feat : 7탭 골격 + 데이터 객체 + 탭/타입필터 엔진 구축
EOF
)"
```

---

### Task 2: 개요 탭 (renderOverview)

**Files:**
- Modify: `suhDevopsTemplate.html` (`renderOverview` 함수 본문 교체)

- [ ] **Step 1: renderOverview 구현**

`function renderOverview(el){ el.innerHTML=''; }`를 아래로 교체.

```javascript
function renderOverview(el){
  const features = [
    { key:'version', icon:'fa-tag', color:'bg-primary', title:'버전 자동화', desc:'main 푸시 시 자동 버전 증가 + Git 태그' },
    { key:'changelog', icon:'fa-robot', color:'bg-secondary', title:'AI 체인지로그', desc:'CodeRabbit 리뷰 기반 CHANGELOG 자동 생성' },
    { key:'preview', icon:'fa-server', color:'bg-success', title:'PR Preview', desc:'Issue/PR 댓글로 임시 서버 배포' },
    { key:'issue', icon:'fa-code-branch', color:'bg-warning', title:'이슈 자동화', desc:'브랜치명/커밋 제안, QA 이슈 생성' },
    { key:'flutter', icon:'fa-mobile', color:'bg-info', title:'Flutter CI/CD', desc:'TestFlight/Play Store/Firebase 배포' },
    { key:'synology', icon:'fa-hard-drive', color:'bg-accent', title:'Synology 배포', desc:'Docker 기반 NAS 무중단 배포' }
  ];
  const cards = features.map(f=>`
    <div class="card bg-base-100 border border-base-300 cursor-pointer hover:shadow-lg transition-all" data-feature="${f.key}">
      <div class="card-body p-4">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-10 h-10 rounded-lg ${f.color} flex items-center justify-center"><i class="fa-solid ${f.icon} text-white"></i></div>
          <h3 class="font-bold">${escapeHtml(f.title)}</h3>
        </div>
        <p class="text-sm text-base-content/70">${escapeHtml(f.desc)}</p>
        <div class="text-right mt-2"><span class="text-xs text-primary">자세히 보기 →</span></div>
      </div>
    </div>`).join('');

  const flow = ['main 푸시','버전 증가','deploy PR 생성','AI 체인지로그','자동 머지','CI/CD 배포'];
  const flowHtml = flow.map((f,i)=>`<div class="badge badge-lg badge-primary">${escapeHtml(f)}</div>${i<flow.length-1?'<i class="fa-solid fa-arrow-right text-base-content/40"></i>':''}`).join('');

  const rows = projectTypes.map(t=>`<tr><td><code class="bg-base-200 px-2 py-1 rounded">${escapeHtml(t.id)}</code></td><td>${escapeHtml(t.label)}</td><td>${escapeHtml(t.versionFile)}</td></tr>`).join('');

  el.innerHTML = `
    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-star text-warning"></i> 핵심 기능</h2>
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">${cards}</div>
    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-diagram-project text-success"></i> 자동화 흐름</h2>
    <div class="card bg-base-100 border border-base-300 p-6 mb-8"><div class="flex flex-wrap items-center justify-center gap-2 text-sm">${flowHtml}</div></div>
    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-layer-group text-info"></i> 지원 프로젝트 타입</h2>
    <div class="card bg-base-100 border border-base-300"><div class="card-body p-0"><div class="overflow-x-auto"><table class="table table-zebra w-full text-sm"><thead><tr><th>타입</th><th>설명</th><th>버전 파일</th></tr></thead><tbody>${rows}</tbody></table></div></div></div>`;

  el.querySelectorAll('[data-feature]').forEach(c=>c.addEventListener('click',()=>openFeatureModal(c.dataset.feature)));
}
```

- [ ] **Step 2: 브라우저 검증**

개요 탭: 6장 카드 그리드, 카드 클릭 시 modal(제목/설명/리스트/문서링크), 흐름 badge 체인, 타입 9행 표. 다크 토글 시 색 정상(`bg-base-*`).

- [ ] **Step 3: Commit** (승인 시에만)

```bash
git add Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html
git commit -m "DevOps Template 페이지 개편 : feat : 개요 탭(핵심기능/흐름/타입표) 구현"
```

---

### Task 3: 적용 탭 (renderApply)

**Files:**
- Modify: `suhDevopsTemplate.html` (`renderApply` 함수 본문 교체)

- [ ] **Step 1: renderApply 구현**

```javascript
function renderApply(el){
  const modes = [
    { m:'full', d:'전체 통합 (버전관리 + 워크플로우 + 이슈템플릿)' },
    { m:'version', d:'버전 관리 시스템만 (version.yml + scripts)' },
    { m:'workflows', d:'GitHub Actions 워크플로우만' },
    { m:'issues', d:'이슈/PR 템플릿만' },
    { m:'skills', d:'Agent Skill 설치만 (Claude/Cursor/Gemini/Codex)' },
    { m:'interactive', d:'대화형 선택 (기본값, 추천)' }
  ];
  const modeRows = modes.map(x=>`<tr><td><code class="bg-base-200 px-2 py-1 rounded">--mode ${escapeHtml(x.m)}</code></td><td>${escapeHtml(x.d)}</td></tr>`).join('');

  el.innerHTML = `
    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-wand-magic-sparkles text-warning"></i> 기존 프로젝트에 적용</h2>
    <fieldset class="fieldset mb-3">
      <legend class="fieldset-legend text-sm font-medium mb-2">운영체제 선택</legend>
      <div class="flex gap-4 flex-wrap">
        <label class="flex items-center gap-2 cursor-pointer"><input type="radio" name="apply-os" value="unix" class="radio radio-primary" checked/><span>macOS / Linux</span></label>
        <label class="flex items-center gap-2 cursor-pointer"><input type="radio" name="apply-os" value="windows" class="radio radio-primary"/><span>Windows PowerShell</span></label>
      </div>
    </fieldset>
    <div class="mockup-code mb-1"><pre data-prefix="$"><code id="applyCmd"></code></pre></div>
    <button id="applyCopyBtn" class="btn btn-primary btn-sm mb-2"><i class="fa-regular fa-copy"></i> 복사</button>
    <p class="text-xs text-base-content/60 mb-8">대화형 모드로 프로젝트 타입과 버전을 자동 감지합니다.</p>

    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-sliders text-info"></i> 통합 모드</h2>
    <div class="card bg-base-100 border border-base-300 mb-8"><div class="card-body p-0"><div class="overflow-x-auto"><table class="table table-zebra w-full text-sm"><thead><tr><th>모드</th><th>설명</th></tr></thead><tbody>${modeRows}</tbody></table></div></div></div>

    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-bolt text-warning"></i> 빠른 시작</h2>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="alert alert-success"><i class="fa-solid fa-plus"></i><div><h3 class="font-bold">새 프로젝트</h3><p class="text-sm">GitHub에서 <strong>"Use this template"</strong> 클릭 → 1분 내 자동 초기화</p></div></div>
      <div class="alert alert-info"><i class="fa-solid fa-download"></i><div><h3 class="font-bold">기존 프로젝트</h3><p class="text-sm">위 <strong>통합 명령어</strong>를 터미널에서 실행</p></div></div>
    </div>`;

  const cmdEl = el.querySelector('#applyCmd');
  const setCmd = ()=>{ const os = el.querySelector('input[name="apply-os"]:checked').value; cmdEl.textContent = integratorCommand[os]; };
  setCmd();
  el.querySelectorAll('input[name="apply-os"]').forEach(r=>r.addEventListener('change', setCmd));
  el.querySelector('#applyCopyBtn').addEventListener('click', ()=>copyText(cmdEl.textContent,'명령어'));
}
```

- [ ] **Step 2: 브라우저 검증**

적용 탭: OS radio 전환 시 mockup-code 명령어 변경(unix↔windows), 복사 버튼 동작(토스트), 6모드 표, 빠른시작 alert 2개.

- [ ] **Step 3: Commit** (승인 시에만)

```bash
git add Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html
git commit -m "DevOps Template 페이지 개편 : feat : 적용 탭(integrator 명령/모드/빠른시작) 구현"
```

---

### Task 4: 명령어 탭 (renderCommands, 타입필터)

**Files:**
- Modify: `suhDevopsTemplate.html` (`renderCommands` 함수 본문 교체)

- [ ] **Step 1: renderCommands 구현**

```javascript
function renderCommands(el){
  const visible = commentCommands.filter(c=>c.types.includes(currentType));
  const items = visible.length ? visible.map(c=>`
    <div class="card bg-base-100 border border-base-300">
      <div class="card-body p-3 flex-row items-center justify-between">
        <div class="flex items-center gap-3">
          <i class="fa-solid ${c.icon} text-base-content/50 w-5"></i>
          <div>
            <code class="font-mono text-sm text-primary">${escapeHtml(c.cmd)}</code>
            <p class="text-xs text-base-content/60">${escapeHtml(c.desc)}</p>
          </div>
        </div>
        <button class="btn btn-ghost btn-xs devops-cmd-copy" data-cmd="${escapeHtml(c.cmd)}"><i class="fa-regular fa-copy"></i></button>
      </div>
    </div>`).join('')
    : `<div class="alert"><i class="fa-solid fa-circle-info"></i><span>이 프로젝트 타입에는 전용 댓글 명령어가 없습니다. (공통: @suh-lab create qa)</span></div>`;

  el.innerHTML = `
    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-comment text-primary"></i> 댓글 명령어 <span class="badge badge-soft badge-info text-xs">Issue/PR</span></h2>
    ${typeFilterSelectHtml()}
    <p class="text-sm text-base-content/60 mb-4">Issue나 PR에 댓글로 입력하면 자동화가 실행됩니다.</p>
    <div class="space-y-2">${items}</div>`;

  bindTypeSelects(el);
  el.querySelectorAll('.devops-cmd-copy').forEach(b=>b.addEventListener('click',()=>copyText(b.dataset.cmd,'댓글 명령어')));
}
```

- [ ] **Step 2: 브라우저 검증**

명령어 탭: 타입 select 변경 시 목록 즉시 필터(spring/python→server 3개+create qa, flutter→app/apk/ios+create qa, react/node 등→create qa만), 복사 버튼 동작, 타입 선택이 마법사/워크플로우 탭과 공유(localStorage).

- [ ] **Step 3: Commit** (승인 시에만)

```bash
git add Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html
git commit -m "DevOps Template 페이지 개편 : feat : 명령어 탭(@suh-lab 타입필터) 구현"
```

---

### Task 5: 마법사 탭 (renderWizards, 타입필터)

**Files:**
- Modify: `suhDevopsTemplate.html` (`renderWizards` 함수 본문 교체)

- [ ] **Step 1: renderWizards 구현**

```javascript
function renderWizards(el){
  const visible = wizards.filter(w=>w.types.includes(currentType));
  const cards = visible.length ? visible.map(w=>{
    const openBtn = w.html
      ? `<a href="${PREVIEW_BASE+encodeURI(w.html)}" target="_blank" class="btn btn-primary btn-sm"><i class="fa-solid fa-up-right-from-square"></i> 브라우저에서 열기</a>`
      : '';
    return `
    <div class="card bg-base-100 border border-base-300">
      <div class="card-body p-4">
        <h3 class="font-bold flex items-center gap-2"><i class="fa-brands ${w.icon} text-primary"></i><i class="fa-solid ${w.icon} text-primary"></i> ${escapeHtml(w.name)}</h3>
        <p class="text-sm text-base-content/70">${escapeHtml(w.desc)}</p>
        <div class="card-actions justify-end mt-2">
          ${openBtn}
          <a href="${REPO_BASE+encodeURI(w.doc)}" target="_blank" class="btn btn-outline btn-sm"><i class="fa-solid fa-book"></i> 문서</a>
        </div>
      </div>
    </div>`;
  }).join('')
    : `<div class="alert"><i class="fa-solid fa-circle-info"></i><span>이 프로젝트 타입에는 전용 마법사가 없습니다.</span></div>`;

  el.innerHTML = `
    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-hat-wizard text-secondary"></i> 설정 마법사</h2>
    ${typeFilterSelectHtml()}
    <p class="text-sm text-base-content/60 mb-4">브라우저에서 마법사를 열어 배포/설정 파일을 생성하세요.</p>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">${cards}</div>`;

  bindTypeSelects(el);
}
```

참고: 아이콘은 wizard마다 `fa-brands` 또는 `fa-solid` 중 하나만 실제 렌더됨(존재 않는 클래스는 빈 글리프). 단순화를 위해 둘 다 출력하되 시각 노이즈 없음. 정확도 원하면 데이터에 `iconStyle` 필드 추가 가능하나 YAGNI — 현 범위 제외.

- [ ] **Step 2: 브라우저 검증**

마법사 탭: flutter 선택 → 6개(testflight/playstore/firebase + 공통 3), 비-flutter → 공통 3개(projects-sync/secrets-converter/worker). "브라우저에서 열기"가 htmlpreview 새 탭으로 열림(worker는 버튼 없음, 문서만). "문서"는 repo blob 새 탭.

- [ ] **Step 3: Commit** (승인 시에만)

```bash
git add Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html
git commit -m "DevOps Template 페이지 개편 : feat : 마법사 탭(wizard htmlpreview 타입필터) 구현"
```

---

### Task 6: 워크플로우 탭 (renderWorkflows, 타입필터)

**Files:**
- Modify: `suhDevopsTemplate.html` (`renderWorkflows` 함수 본문 교체)

- [ ] **Step 1: renderWorkflows 구현**

```javascript
function renderWorkflows(el){
  const commonRows = commonWorkflows.map(w=>`
    <tr><td><code class="text-xs">${escapeHtml(w.wf)}</code></td><td class="font-medium">${escapeHtml(w.name)}</td><td class="text-base-content/70">${escapeHtml(w.desc)}</td></tr>`).join('');

  const typed = typeWorkflows.filter(w=>w.types.includes(currentType));
  const typeRows = typed.length ? typed.map(w=>`
    <tr><td><code class="text-xs">${escapeHtml(w.wf)}</code></td><td class="font-medium">${escapeHtml(w.name)}${w.synology?' <span class="badge badge-info badge-xs">Synology</span>':''}</td><td class="text-base-content/70">${escapeHtml(w.desc)}</td></tr>`).join('')
    : `<tr><td colspan="3" class="text-center text-base-content/60">이 타입은 전용 워크플로우가 없습니다 (공통 워크플로우만 사용).</td></tr>`;

  el.innerHTML = `
    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-diagram-project text-success"></i> GitHub Actions 워크플로우</h2>
    ${typeFilterSelectHtml()}
    <h3 class="font-bold mb-2 mt-2">공통 워크플로우 <span class="text-xs text-base-content/60">(모든 타입)</span></h3>
    <div class="card bg-base-100 border border-base-300 mb-6"><div class="card-body p-0"><div class="overflow-x-auto"><table class="table table-zebra table-sm w-full"><thead><tr><th>워크플로우</th><th>이름</th><th>설명</th></tr></thead><tbody>${commonRows}</tbody></table></div></div></div>
    <h3 class="font-bold mb-2">타입별 CI/CD <span class="text-xs text-base-content/60">(선택된 타입)</span></h3>
    <div class="card bg-base-100 border border-base-300"><div class="card-body p-0"><div class="overflow-x-auto"><table class="table table-zebra table-sm w-full"><thead><tr><th>워크플로우</th><th>이름</th><th>설명</th></tr></thead><tbody>${typeRows}</tbody></table></div></div></div>`;

  bindTypeSelects(el);
}
```

- [ ] **Step 2: 브라우저 검증**

워크플로우 탭: 공통 9행 항상 노출, 타입별 표는 select 따라 변경(spring 7행 with Synology 배지 6개, flutter 8행, react/next/python 각, node/basic/RN→"전용 없음"). 가로 스크롤 정상.

- [ ] **Step 3: Commit** (승인 시에만)

```bash
git add Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html
git commit -m "DevOps Template 페이지 개편 : feat : 워크플로우 탭(공통+타입별 CICD, Synology 배지) 구현"
```

---

### Task 7: AI 스킬 탭 + 설정 탭 (renderSkills, renderSettings)

**Files:**
- Modify: `suhDevopsTemplate.html` (`renderSkills`, `renderSettings` 함수 본문 교체)

- [ ] **Step 1: renderSkills 구현**

```javascript
function renderSkills(el){
  const platforms = [
    { id:'claude', label:'Claude Code', icon:'fa-robot', note:'마켓플레이스 등록 후 설치 (추천)' },
    { id:'cursor', label:'Cursor', icon:'fa-i-cursor', note:'integrator skills 모드로 파일 복사 (마켓플레이스 미지원)' },
    { id:'gemini', label:'Gemini CLI', icon:'fa-gem', note:'extension 설치' },
    { id:'codex', label:'Codex CLI', icon:'fa-terminal', note:'plugin marketplace 등록' }
  ];
  const installCards = platforms.map(p=>`
    <div class="card bg-base-100 border border-base-300">
      <div class="card-body p-4">
        <h3 class="font-bold flex items-center gap-2"><i class="fa-solid ${p.icon} text-primary"></i> ${escapeHtml(p.label)}</h3>
        <p class="text-xs text-base-content/60 mb-1">${escapeHtml(p.note)}</p>
        <div class="mockup-code text-xs"><pre><code>${escapeHtml(installCommands[p.id])}</code></pre></div>
        <div class="card-actions justify-end mt-2"><button class="btn btn-ghost btn-xs devops-install-copy" data-cmd="${escapeHtml(installCommands[p.id])}"><i class="fa-regular fa-copy"></i> 복사</button></div>
      </div>
    </div>`).join('');

  const skillRows = skills.map(s=>`<tr><td><code class="text-primary">/cassiiopeia:${escapeHtml(s.cmd)}</code></td><td class="text-base-content/70">${escapeHtml(s.desc)}</td></tr>`).join('');

  el.innerHTML = `
    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-robot text-secondary"></i> AI Agent Skills <span class="badge badge-soft badge-secondary text-xs">20개</span></h2>
    <p class="text-sm text-base-content/60 mb-4">4개 AI 코딩 도구에서 DevOps 자동화 스킬을 바로 사용하세요.</p>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">${installCards}</div>
    <h3 class="font-bold mb-2">스킬 목록</h3>
    <div class="card bg-base-100 border border-base-300"><div class="card-body p-0"><div class="overflow-x-auto"><table class="table table-zebra table-sm w-full"><thead><tr><th>스킬</th><th>용도</th></tr></thead><tbody>${skillRows}</tbody></table></div></div></div>`;

  el.querySelectorAll('.devops-install-copy').forEach(b=>b.addEventListener('click',()=>copyText(b.dataset.cmd,'설치 명령어')));
}
```

- [ ] **Step 2: renderSettings 구현**

```javascript
function renderSettings(el){
  const docs = [
    { t:'통합 스크립트 가이드', f:'docs/TEMPLATE-INTEGRATOR.md', d:'기존 프로젝트에 템플릿 통합' },
    { t:'버전 관리', f:'docs/VERSION-CONTROL.md', d:'version.yml, 자동 버전 증가' },
    { t:'체인지로그 자동화', f:'docs/CHANGELOG-AUTOMATION.md', d:'CodeRabbit 연동, AI 문서화' },
    { t:'PR Preview', f:'docs/PR-PREVIEW.md', d:'임시 서버 배포 시스템' },
    { t:'Flutter CI/CD', f:'docs/FLUTTER-CICD-OVERVIEW.md', d:'iOS/Android 자동 배포' },
    { t:'Synology 배포', f:'docs/SYNOLOGY-DEPLOYMENT-GUIDE.md', d:'Docker 기반 NAS 배포' },
    { t:'이슈 자동화', f:'docs/ISSUE-AUTOMATION.md', d:'Issue Helper, QA 봇' },
    { t:'트러블슈팅', f:'docs/TROUBLESHOOTING.md', d:'자주 발생하는 문제 해결' }
  ];
  const docRows = docs.map(d=>`<tr><td><a href="${REPO_BASE+encodeURI(d.f)}" target="_blank" class="link link-primary">${escapeHtml(d.t)}</a></td><td class="text-base-content/70">${escapeHtml(d.d)}</td></tr>`).join('');

  el.innerHTML = `
    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-gear text-info"></i> 필수 설정</h2>
    <div class="alert alert-info mb-4"><i class="fa-solid fa-circle-info"></i><span>자동 체인지로그, PR 머지 등을 사용하려면 아래 설정이 필요합니다.</span></div>
    <div class="mockup-code mb-4 text-xs"><pre><code>Repository Settings → Secrets → Actions → New repository secret
Name:  _GITHUB_PAT_TOKEN
Value: [Personal Access Token - repo, workflow 권한]</code></pre></div>

    <div class="collapse collapse-arrow bg-base-100 border border-base-300 mb-4">
      <input type="checkbox"/>
      <div class="collapse-title font-medium"><i class="fa-solid fa-building mr-2"></i>Organization 설정 (필요 시)</div>
      <div class="collapse-content"><ul class="list-disc list-inside space-y-1 text-sm"><li>Settings → Actions → General</li><li>✅ Allow GitHub Actions to create and approve pull requests</li><li>✅ Read and write permissions</li></ul></div>
    </div>

    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-list-check text-warning"></i> 배포 전 3대 작업</h2>
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
      <div class="card bg-base-100 border border-base-300"><div class="card-body p-4"><h3 class="font-bold text-sm">1. PAT 토큰</h3><p class="text-xs text-base-content/70">_GITHUB_PAT_TOKEN (repo, workflow)</p></div></div>
      <div class="card bg-base-100 border border-base-300"><div class="card-body p-4"><h3 class="font-bold text-sm">2. deploy 브랜치</h3><code class="text-xs">git checkout -b deploy && git push -u origin deploy</code></div></div>
      <div class="card bg-base-100 border border-base-300"><div class="card-body p-4"><h3 class="font-bold text-sm">3. CodeRabbit</h3><p class="text-xs text-base-content/70">coderabbit.ai에서 저장소 활성화</p></div></div>
    </div>

    <h2 class="text-xl font-bold mb-4 flex items-center gap-2"><i class="fa-solid fa-book text-secondary"></i> 문서</h2>
    <div class="card bg-base-100 border border-base-300"><div class="card-body p-0"><div class="overflow-x-auto"><table class="table table-zebra w-full text-sm"><thead><tr><th>문서</th><th>설명</th></tr></thead><tbody>${docRows}</tbody></table></div></div></div>`;
}
```

- [ ] **Step 3: 브라우저 검증**

AI 스킬 탭: 4플랫폼 설치 카드(mockup-code + 복사), 20행 스킬 표. 설정 탭: Secret mockup-code, Org collapse 펼침, 3대 작업 카드, 문서 8행 링크 새 탭.

- [ ] **Step 4: Commit** (승인 시에만)

```bash
git add Suh-Web/src/main/resources/templates/pages/suhDevopsTemplate.html
git commit -m "DevOps Template 페이지 개편 : feat : AI 스킬 탭 + 설정 탭 구현"
```

---

### Task 8: 커스텀 다크모드 CSS 제거

**Files:**
- Modify: `Suh-Web/src/main/resources/static/css/common.css` (1525~1596줄 제거)

- [ ] **Step 1: devops-* 다크모드 블록 제거**

`common.css`에서 아래 범위를 삭제(주석 한 줄 + 8개 dark 규칙 + 직후 빈 줄). **1597줄 `[data-theme="dark"] .bg-blue-50`부터는 공용이므로 보존.**

제거 시작: `/* SUH DevOps Template 페이지 다크모드 */` (1525줄)
제거 끝: `[data-theme="dark"] .devops-quick-start-card p { ... }` 닫는 `}` (1595줄) + 1596 빈 줄

Edit old_string(블록 전체)을 new_string(빈 문자열에 가깝게 — 위아래 빈 줄 1개 유지)로 교체. 삭제 대상 8개 셀렉터: `.devops-hero`, `.devops-hero-overlay`, `.devops-hero h1/p`, `.devops-command-box`, `.devops-command-box code`, `.devops-command-item`, `.devops-command-item:hover`, `.devops-feature-card`, `.devops-feature-card:hover`, `.devops-feature-card p`, `.devops-feature-card .text-blue-500`, `.devops-project-code`, `.devops-flow-card`, `.devops-quick-start-card`, `.devops-quick-start-card h3`, `.devops-quick-start-card p`.

- [ ] **Step 2: 빌드 후 다크모드 검증**

bootRun 재시작 후 `/suh-devops-template`에서 테마 토글(라이트↔다크) 반복. 모든 탭에서 카드/표/배지/mockup-code/alert 색이 정상(깨짐·흰배경 잔존 없음). `.devops-*` 클래스가 더 이상 마크업에 없으므로(신규 페이지는 semantic 토큰만 사용) 회귀 없음 확인.

- [ ] **Step 3: 잔존 참조 grep 확인**

Run(Grep tool): 패턴 `devops-(hero|command-box|command-item|feature-card|project-code|flow-card|quick-start-card)` 를 `Suh-Web/src/main/resources/` 전체에서 검색.
Expected: 매치 0건 (CSS·HTML 모두). `.devops-panel`/`.devops-type-select`/`.devops-cmd-copy`/`.devops-install-copy`는 신규 JS 마커이므로 매치되어도 정상(위 패턴엔 안 걸림).

- [ ] **Step 4: Commit** (승인 시에만)

```bash
git add Suh-Web/src/main/resources/static/css/common.css
git commit -m "DevOps Template 페이지 개편 : refactor : DaisyUI semantic 전환으로 devops-* 커스텀 다크모드 CSS 제거"
```

---

## Self-Review

**1. Spec coverage:**
- 7탭(개요/적용/명령어/마법사/워크플로우/AI스킬/설정) → Task 1(골격)+2~7(내용). ✓
- 타입 필터(명령어/마법사/워크플로우) → Task 1(엔진)+4/5/6. ✓
- wizard htmlpreview 새탭 + docs → Task 5. ✓
- Synology/서버/테스트빌드 CICD 전부 노출 → Task 6 typeWorkflows(Spring Synology 4종+Nexus+GHPackages, Flutter TestFlight/Firebase/TestAPK 등). ✓
- 20 AI 스킬 + 4플랫폼 설치 + integrator skills 모드 → Task 7 + Task 3(모드표). ✓
- v2.7.12 → v2.9.9 → Task 1 Hero. ✓
- DaisyUI semantic 토큰 + 커스텀 CSS 제거 → 전 Task 마크업 semantic + Task 8. ✓
- CSP(인라인 style 금지) → 전 Task 클래스만, JS classList/textContent. ✓
- 서버 변경 0 → HTML/CSS만. ✓
- 공용 `.bg-blue-50` 등 오버라이드 보존 → Task 8 범위 1525~1596 한정. ✓

**2. Placeholder scan:** 모든 step에 실제 코드/명령 포함. wizard 아이콘 단순화는 명시적 결정(YAGNI). 누락 없음.

**3. Type consistency:** `renderTab` switch가 7개 render 함수 호출 ↔ Task 2~7이 동일 시그니처(`render<Tab>(el)`)로 구현. `currentType`/`currentTab`/`setType`/`switchTab`/`typeFilterSelectHtml`/`bindTypeSelects`/`copyText`/`escapeHtml`/`openFeatureModal` 모두 Task 1에서 정의 후 후속 Task에서 사용. `PREVIEW_BASE`/`REPO_BASE`는 Task 1 Step 2 선언 후 Task 5/7 사용. 데이터 객체 필드명(`types`/`synology`/`html`/`doc`/`cmd`/`wf`) 일관. ✓

---

## Execution Handoff
```