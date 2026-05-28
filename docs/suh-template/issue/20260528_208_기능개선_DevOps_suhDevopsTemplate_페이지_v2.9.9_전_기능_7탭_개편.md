# 🚀[기능개선][DevOps] suhDevopsTemplate 페이지 v2.9.9 전 기능 7탭 개편

- 라벨: 작업전
- 담당자: Cassiiopeia

---

📝 현재 문제점
---

- `suhDevopsTemplate.html` 페이지가 SUH-DEVOPS-TEMPLATE **v2.7.12** 기준에 머물러 있어, 이후 추가된 기능 다수가 노출되지 않는다 (현재 템플릿 v2.9.9).
- **누락 항목**:
  - **Agent Skills (20개, 4플랫폼: Claude / Cursor / Gemini / Codex)** — 페이지에 전혀 없음. integrator `skills` 모드(대화형 메뉴 5번)도 미노출.
  - **마법사(wizard) 확장** — Flutter testflight / playstore / firebase + 공통(projects-sync / secrets-converter / projects-sync-worker)까지 존재하나 일부만 암시.
  - **워크플로우 전체** — Synology 무중단(nginx / traefik), Nexus, GitHub Packages, Firebase CICD, Test APK, Projects Sync Manager, Issue Helper(API+Module), 라벨 동기화 등 미노출.
  - **@suh-lab 댓글 명령어** — 정리/타입별 구분 필요.
- 정보량이 많은데 평면 나열이라 사용자가 "내 프로젝트에 무엇을 쓸 수 있는지" 한눈에 파악하기 어렵다.
- 하드코딩 Tailwind 색상이 많아 다크모드 대응용 커스텀 CSS(`common.css`의 `devops-*` 블록)가 누적돼 유지보수 부담이 있다.

🛠️ 해결 방안 / 제안 기능
---

- **7탭 단일 페이지**로 재구성: 개요 / 적용 / 명령어 / 마법사 / 워크플로우 / AI 스킬 / 설정.
- **프로젝트 타입별 필터** 도입: 명령어·마법사·워크플로우 탭 상단에서 타입(spring/flutter/react/next/node/python/react-native/expo/basic) 선택 시 해당 타입 항목만 노출.
- **마법사**는 htmlpreview 새 탭 링크 + docs 문서 링크로 연결(별도 호스팅 없음).
- **AI 스킬 탭**: 4플랫폼 설치 명령 + 20개 스킬 목록 표.
- **DaisyUI 5 semantic 토큰**(`bg-base-*`, `text-base-content`, `badge-*`, `mockup-code`, `alert`, `collapse`, `table`, `tabs tabs-lift`)으로 전환해 다크모드 자동 적응 → `common.css` 커스텀 `devops-*` 다크모드 블록 제거.
- CSP 준수(인라인 style 금지) 유지. 서버(Controller/Service/Entity) 변경 없음 — 순수 템플릿 + 정적 JS.

⚙️ 작업 내용
---

- `suhDevopsTemplate.html` 전면 재작성: 7탭 골격 + JS 데이터 객체(타입/명령어/마법사/워크플로우/스킬/설치명령) + 탭 전환·타입 필터 엔진.
- 각 탭 렌더 함수 구현(개요·적용·명령어·마법사·워크플로우·AI스킬·설정).
- `common.css` `devops-*` 다크모드 블록 제거(공용 오버라이드는 보존).
- Hero 버전 배지 v2.7.12 → v2.9.9.
- 설계: `docs/superpowers/specs/2026-05-28-devops-template-page-redesign-design.md`
- 계획: `docs/superpowers/plans/2026-05-28-devops-template-page-redesign.md`

🙋‍♂️ 담당자
---

- 프론트엔드: Cassiiopeia
