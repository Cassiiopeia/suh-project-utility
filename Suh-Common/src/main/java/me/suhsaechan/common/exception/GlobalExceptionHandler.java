package me.suhsaechan.common.exception;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
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

  // 요청 파라미터 바인딩·형식 변환 실패 (UUID·날짜 형식 오류 등) > 400
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
    log.warn("요청 파라미터 바인딩 실패: {}", e.getMessage());
    ErrorResponse response = ErrorResponse.builder()
        .errorCode(ErrorCode.INVALID_PARAMETER.name())
        .httpStatus(ErrorCode.INVALID_PARAMETER.getHttpStatus().name())
        .message(ErrorCode.INVALID_PARAMETER.getMessage())
        .build();
    return new ResponseEntity<>(response, ErrorCode.INVALID_PARAMETER.getHttpStatus());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    ErrorResponse response = ErrorResponse.builder()
        .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.name())
        .httpStatus(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().name())
        // 내부 예외 메시지(SQL·스택 정보) 노출 차단: 고정 문구만 응답, 원문은 로그
        .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
        .build();
    log.error(e.getMessage(), e);
    return new ResponseEntity<>(response, ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus());
  }

}