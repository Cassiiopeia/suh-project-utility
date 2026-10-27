📝 현재 문제점
---

- 여러 레포에 GitHub Pages(github.io) 소개 페이지를 새로 만들었지만, 대시보드 프로젝트 카드는 대부분 GitHub 레포로만 연결된다.
- 세종대 인증 카드의 소개 페이지 링크가 404다 (레포 이름이 `pro-sejong-auth`로 바뀜).
- EarLocAlert 앱은 소개 페이지와 스토어 출시가 끝났는데 대시보드에 카드가 없다.

🛠️ 해결 방안 / 제안 기능
---

- github.io 소개 페이지가 있는 프로젝트는 소개 페이지를 첫 번째 버튼으로, GitHub를 그 뒤에 둔다.
- 대상 (Cassiiopeia, Twin-Fang 공개 레포 37개 중 github.io 응답 200인 페이지 기준)
  - passQL: 소개 페이지 버튼 추가
  - 세종대 인증: 소개 페이지·GitHub 링크를 `pro-sejong-auth`로 수정
  - suh-api-log 모듈 카드: GitHub 대신 소개 페이지로 연결
  - suh-random-engine, sejong-univ-auth 모듈 카드: 사이트 내 데모는 유지하고 "소개 페이지" 링크 추가
  - EarLocAlert 카드 신규 추가 (소개 페이지, Google Play, App Store, GitHub)
- 제외: Cassiiopeia.github.io (app-ads.txt 호스팅 용도)

⚙️ 작업 내용
---

- `templates/pages/dashboard.html` 카드 링크 수정, EarLocAlert 카드 추가
- `messages*.properties` EarLocAlert 설명 문구 (한국어·영어)
- EarLocAlert 대표 이미지·로고 추가
- 라이트·다크, 모바일 폭 화면 확인

🙋‍♂️ 담당자
---

- 백엔드: Cassiiopeia
- 프론트엔드: Cassiiopeia
