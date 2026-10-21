📝 현재 문제점
---

- SUH-sejong-univ-auth 라이브러리 소개 페이지(https://cassiiopeia.github.io/SUH-sejong-univ-auth/)에서 방문자가 바로 학번/비밀번호로 인증을 체험할 수 있게 하려 함 (Cassiiopeia/SUH-sejong-univ-auth#17)
- 현재 세종대 인증 API(`/api/sejong-auth/authenticate`)는 lab 페이지 안에서만 호출 가능
  - 허용 출처(CORS)에 `https://cassiiopeia.github.io` 없음
  - CSRF 토큰을 요구해 외부 페이지에서 호출하면 403

🛠️ 해결 방안 / 제안 기능
---

- 세종대 인증 API 경로에 한해 `https://cassiiopeia.github.io` 출처 허용 (쿠키 등 자격 증명은 받지 않음)
- 세종대 인증 API 경로를 CSRF 검사에서 제외 (로그인 세션을 만들거나 바꾸지 않는 무상태 API, 학번/비밀번호를 요청마다 받음)
- 그 외 경로의 CORS, CSRF 정책은 그대로 유지

⚙️ 작업 내용
---

- CORS 설정: 세종대 인증 API 전용 규칙 추가
- 보안 설정: CSRF 제외 경로에 세종대 인증 API 추가
- 배포 후 github.io 출처로 실제 호출 확인

🙋‍♂️ 담당자
---

- 백엔드: Cassiiopeia
