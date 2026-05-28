📝 현재 문제점
---

- 대시보드 Team Projects 섹션의 트립게더 카드가 GitHub Org 페이지로만 연결되어, 실제 서비스 앱(tripgether.app)과 앱스토어 다운로드 경로로 바로 진입할 수 없다.
- Projects 섹션에는 Mapsy, 세종말싸미 등 프로젝트별 리소스 그룹이 정리되어 있으나 트립게더 항목이 누락되어 BE/FE/AI 레포 및 앱 메인 링크를 한곳에서 확인할 수 없다.

🛠️ 해결 방안 / 제안 기능
---

- Team Projects 트립게더 카드 클릭 시 `https://tripgether.app/` 로 연결되도록 변경하고, 하단 액션 버튼을 GitHub / Instagram / iOS / Android 4개로 확장한다.
  - iOS: `https://apps.apple.com/app/tripgether/id6746592757`
  - Android: `https://play.google.com/store/apps/details?id=app.tripgether`
- Projects 섹션 Mapsy 그룹 바로 위에 Tripgether 그룹 박스를 신규 추가하고, 다음 4개 서브카드를 배치한다.
  - 앱 메인: `https://tripgether.app/`
  - BE 레포: `https://github.com/Cassiiopeia/Tripgether-BE`
  - FE 레포: `https://github.com/Cassiiopeia/tripgether-FE`
  - AI 레포: `https://github.com/TEAM-Tripgether/tripgether-python` (Private 배지 표기)

⚙️ 작업 내용
---

- 대상 파일: `Suh-Web/src/main/resources/templates/pages/dashboard.html`
- CSP 준수(인라인 style 미사용) 및 Tailwind 표준 클래스만 사용, 기존 카드 패턴 재사용

🙋‍♂️ 담당자
---

- 프론트엔드: Cassiiopeia
