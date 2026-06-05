🗒️ 설명
---

전 페이지에서 DaisyUI 버튼·배지의 색이 표시되지 않는다.

- `btn-primary`(예: 번역하기)가 파란 배경 없이 테두리만 살짝 보임
- `btn-outline`(예: 복사하기)은 거의 보이지 않음
- 특정 페이지만이 아니라 공통 header를 쓰는 모든 페이지에서 동일하게 발생

원인은 공통 헤더의 CDN 셋업이 비호환 조합이기 때문이다.

- `cdn.tailwindcss.com` → Tailwind v3 Play CDN
- `daisyui@5.4.2/daisyui.css` → DaisyUI 5 (Tailwind v4 전용)

DaisyUI 5는 Tailwind v4의 토큰 시스템으로 색을 적용하는데, Tailwind v3 CDN은 이를 처리하지 못해 컴포넌트의 모양(테두리·패딩)만 적용되고 색 토큰이 누락된다.

🔄 재현 방법
---

1. 임의 페이지 접속 (번역, 대시보드, Docker 로그 등)
2. 화면의 버튼 확인
3. `btn-primary`/`btn-error`/`btn-outline` 버튼에 배경색이 없고 테두리만 보임

📸 참고 자료
---

관련 파일:
- `Suh-Web/src/main/resources/templates/fragments/header.html` (CDN 셋업)
- `Suh-Web/src/main/resources/static/css/common.css` (색 변수 정의)

✅ 예상 동작
---

- 번역하기 버튼은 파란 배경, 복사하기 버튼은 회색 테두리로 표시되어야 함
- 다크모드 토글이 정상 동작해야 함
- 기존 레이아웃이 깨지지 않아야 함

🛠️ 해결 방향
---

- 공통 헤더의 CDN 셋업을 Tailwind v4 + DaisyUI 5 정합 조합으로 전환
  - `@tailwindcss/browser@4` 스크립트 + 인라인 `@plugin "daisyui"` 방식
  - 기존 `tailwind.config` JS 설정 제거, 다크모드는 CSS 변형으로 처리
- Tailwind v4에서 폐기된 `flex-shrink-*`를 `shrink-*`로 치환 (11개)
- `common.css`는 색 변수가 이미 v4 친화적이라 손대지 않음

설계 문서: `docs/superpowers/specs/2026-06-05-daisyui5-tailwindv4-color-fix-design.md`

⚙️ 환경 정보
---

- **OS**: -
- **브라우저**: Chrome
- **기기**: -

🙋‍♂️ 담당자
---

- **백엔드**: -
- **프론트엔드**: Cassiiopeia
- **디자인**: -
