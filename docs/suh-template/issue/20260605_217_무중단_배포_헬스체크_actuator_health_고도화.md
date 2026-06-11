📝 현재 문제점
---

이슈 #205 로 Traefik Blue-Green 무중단 배포가 완성되었으나, 배포 헬스체크 방식이 느슨하다.

- 현재 배포 헬스체크는 루트 경로 `/` 를 호출하고 HTTP `200|301|302|308` 을 통과로 인정한다.
- Spring Security 로그인 리다이렉트(302)도 통과로 처리되므로, **"컨테이너가 HTTP 응답을 한다"** 까지만 보장된다.
- 애플리케이션이 실제로 정상 기동(UP)했는지, DB/외부 의존성 연결이 살아있는지는 검증하지 못한다.
- 그 결과, Spring 컨텍스트가 일부만 떠 로그인 페이지만 응답하는 반쪽 상태에서도 헬스체크가 통과해 트래픽이 전환될 위험이 있다.

이슈 #205 의 후속 작업으로 명시된 `/actuator/health` 노출 및 헬스체크 path 개선 항목이다.

🛠️ 해결 방안 / 제안 기능
---

배포 헬스체크를 `/actuator/health` 기반으로 교체하여 실제 애플리케이션 UP 상태를 정확히 검증한다.

- `/actuator/health` 를 인증 없이 접근 가능하도록 공개 경로에 추가한다. (현재 Spring Security 가 302 로 가로채 헬스체크에 사용할 수 없음)
- actuator health 엔드포인트만 노출하도록 설정을 명시한다.
- 배포 워크플로우의 헬스체크 대상 경로와 통과 인정 코드를 강화한다. (`/actuator/health` 호출 → HTTP 200(UP)만 통과)
- 더불어, 이슈 #205 의 Mixed Content 수정분(ForwardedHeaderFilter)이 원격 main 에 반영되지 않은 상태로 발견되어 함께 정리한다.

⚙️ 작업 내용
---

- `PublicEndpointConfig.java`: 공개 API 경로에 `/actuator/health` 추가 (Security 302 가로채기 제거)
- GitHub Secret `APPLICATION_YML`: actuator health 노출 설정 추가 (application.yml 이 gitignore 되어 secret 으로 주입되므로 secret 직접 갱신)
- `SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml`: 헬스체크 경로를 `/` → `/actuator/health`, 통과 인정 코드를 `200|301|302|308` → `200` 으로 변경
- `WebSecurityConfig.java`: ForwardedHeaderFilter 빈 복원 (이슈 #205 Mixed Content 수정분, 원격 미반영분 정리)
- 배포 후 검증: 배포 중 `lab.suhsaechan.kr` 무중단 유지, `/actuator/health` 가 `{"status":"UP"}` 응답, 헬스체크 실패 시 롤백 동작 확인

🙋‍♂️ 담당자
---

- 백엔드: Cassiiopeia
- 프론트엔드: -
- 디자인: -
