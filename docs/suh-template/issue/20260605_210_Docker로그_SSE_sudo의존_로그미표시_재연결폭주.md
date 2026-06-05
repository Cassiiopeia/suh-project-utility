🗒️ 설명
---

`/docker-logs` 페이지에서 Docker 컨테이너 로그가 화면에 표시되지 않고, SSE 연결이 약 3초 간격으로 시작/종료를 무한 반복하는 현상.

- SSE 스트리밍이 `sudo -S` + PTY(`setPty(true)`) 기반으로 동작
- 서버가 sudo 없이 docker 명령을 실행 가능하도록 변경된 환경에서, sudo 비밀번호 주입 경로가 깨지며 채널이 즉시 닫힘
- 채널 즉시 종료 → 백엔드 스트리밍 루프 조기 탈출 → `emitter.complete()` → 프론트 `EventSource` 재연결 → 무한 반복
- PTY 에코로 인해 sudo 비밀번호가 로그/응답 스트림에 평문 노출 (관리자 전용 페이지라 영향도는 낮음)
- 결과적으로 실제 `docker logs` 출력을 읽기 전에 채널이 닫혀 로그가 화면에 표시되지 않음

🔄 재현 방법
---

1. `/docker-logs` 페이지 접속
2. 컨테이너 선택 후 스트리밍 시작
3. 로그가 표시되지 않고, 상태가 연결됨/끊김을 반복하며 서버 로그에 `SSE 스트리밍 시작 → 종료`가 3초 간격으로 누적됨

📸 참고 자료
---

```
SSE 스트리밍 시작 - 컨테이너: pickerpicker-back, tail: 100
SSE 스트리밍 종료 - 컨테이너: pickerpicker-back
(약 3초 후 반복)
```

관련 파일:
- `Suh-Domain-Docker/src/main/java/me/suhsaechan/docker/service/DockerLogService.java`
- `Suh-Web/src/main/java/me/suhsaechan/web/controller/api/DockerLogController.java`
- `Suh-Web/src/main/resources/templates/pages/dockerLogs.html`

✅ 예상 동작
---

- 스트리밍 시작 시 SSE 연결이 유지되고 `docker logs -f` 출력이 실시간으로 화면에 표시되어야 함
- 불필요한 재연결 폭주가 발생하지 않아야 함
- sudo가 필요 없는 환경에서는 sudo 없이 docker 명령을 실행해야 함

🛠️ 해결 방향
---

- 기존 sudo 기반 메소드는 `WithSudo` 접미사로 보존 (다른 환경 호환용)
- sudo 없는 기본 메소드를 추가하고 컨트롤러가 기본 메소드를 호출하도록 함
  - 스트리밍/폴링/컨테이너 목록 조회 모두 sudo 없는 기본 경로 사용
  - sudo 제거에 따라 PTY 및 비밀번호 주입 로직 제거
- 라인 제한 옵션을 100/500/1000/2000 → 100/200/500/1000 으로 조정

⚙️ 환경 정보
---

- **OS**: Linux (내부망 서버)
- **브라우저**: -
- **기기**: -

🙋‍♂️ 담당자
---

- **백엔드**: Cassiiopeia
- **프론트엔드**: Cassiiopeia
- **디자인**: -
