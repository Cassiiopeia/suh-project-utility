package me.suhsaechan.common.exception;

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