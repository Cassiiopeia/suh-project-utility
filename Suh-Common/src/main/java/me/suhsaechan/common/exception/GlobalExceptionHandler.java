package me.suhsaechan.common.exception;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
    ErrorResponse response = ErrorResponse.builder()
        .errorCode(e.getErrorCode().name())
        .httpStatus(e.getErrorCode().getHttpStatus().name())
        .message(e.getErrorCode().getMessage())
        .build();
    return new ResponseEntity<>(response, e.getErrorCode().getHttpStatus());
  }

  /**
   * 클라이언트 연결 종료(SSE 화면 닫기·새로고침 등)로 발생하는 Broken pipe는 정상 상황으로 간주한다.
   * 응답 스트림이 이미 끊겼으므로 ErrorResponse 변환을 시도하지 않고 조용히 종료한다
   * (text/event-stream 응답에 JSON을 쓰려다 발생하던 2차 예외 방지).
   */
  @ExceptionHandler(IOException.class)
  public void handleClientAbort(IOException e) {
    log.debug("클라이언트 연결 종료 (정상): {}", e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    ErrorResponse response = ErrorResponse.builder()
        .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.name())
        .httpStatus(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().name())
        .message(e.getMessage())
        .build();
    log.error(e.getMessage(), e);
    return new ResponseEntity<>(response, ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus());
  }

}