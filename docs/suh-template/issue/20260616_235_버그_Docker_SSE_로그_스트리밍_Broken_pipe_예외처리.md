🗒️ 설명
---

Docker 컨테이너 로그 실시간 보기(SSE 스트리밍)에서 클라이언트 연결이 끊길 때 예외 처리가 잘못되어 서버 로그에 ERROR 스택트레이스가 쌓인다.

- 사용자가 로그 화면을 닫거나 새로고침하면 클라이언트가 SSE 연결을 끊는다 → 서버가 데이터를 보내려다 `Broken pipe`(정상적으로 발생할 수 있는 상황) 예외가 난다.
- 문제는 이 예외가 전역 예외 처리기(`GlobalExceptionHandler.handleException`)로 전파되어, SSE 응답(`Content-Type: text/event-stream`)에 JSON 형식의 `ErrorResponse`를 쓰려다 **2차 예외**(`No converter for ErrorResponse with preset Content-Type 'text/event-stream'`)가 발생한다.
- 즉 정상적인 연결 종료가 ERROR 2건으로 기록되어 로그가 오염되고, SSE 스트리밍의 안정성 우려가 있다.

참고: 챗봇도 같은 SSE(`SseEmitter`) 방식을 쓰므로, 동일 패턴의 예외가 챗봇 스트리밍에서도 잠재적으로 발생할 수 있다.

🔄 재현 방법
---

1. Docker 모니터링 페이지에서 특정 컨테이너의 실시간 로그 스트리밍을 연다
2. 로그가 흐르는 도중 화면을 닫거나 새로고침하여 연결을 끊는다
3. 서버 로그에서 `Broken pipe` ERROR와 뒤이은 `HttpMessageNotWritableException` 관찰

📸 참고 자료
---

운영 로그 (2026-06-16 13:14):

```
ERROR ... GlobalExceptionHandler : Broken pipe
java.io.IOException: Broken pipe
  ...
  at me.suhsaechan.docker.service.DockerLogService.streamContainerLogs(DockerLogService.java:234)
  at me.suhsaechan.web.controller.api.DockerLogController.lambda$streamContainerLogs$3(DockerLogController.java:117)

WARN ... ExceptionHandlerExceptionResolver : Failure in @ExceptionHandler ... handleException(Exception)
org.springframework.http.converter.HttpMessageNotWritableException:
  No converter for [class me.suhsaechan.common.exception.ErrorResponse] with preset Content-Type 'text/event-stream'
```

관련 파일:
- `Suh-Domain-Docker/src/main/java/me/suhsaechan/docker/service/DockerLogService.java` (`streamContainerLogs`, SSE send 부)
- `Suh-Web/src/main/java/me/suhsaechan/web/controller/api/DockerLogController.java` (SSE emitter 콜백)
- `Suh-Common/src/main/java/me/suhsaechan/common/exception/GlobalExceptionHandler.java` (`handleException` — SSE 응답에 JSON ErrorResponse를 쓰려다 실패)

✅ 예상 동작
---

- 클라이언트가 SSE 연결을 끊어 발생하는 `Broken pipe`는 정상 종료로 간주하여 조용히 처리(debug 로그 수준)하고, ERROR 스택트레이스를 남기지 않는다.
- SSE(`text/event-stream`) 엔드포인트에서 발생한 예외는 JSON `ErrorResponse`로 변환을 시도하지 않는다(2차 예외 방지).

⚙️ 환경 정보
---

- **OS**: -
- **브라우저**: -
- **기기**: -

🙋‍♂️ 담당자
---

- **백엔드**: 서새찬
- **프론트엔드**: -
- **디자인**: -
