package me.suhsaechan.suhprojectutility.util.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  // Common
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다."),

  // UTILS
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
  FILE_COPY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 복사에 실패했습니다."),

  // AUTHENTICATION
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "ID와 비밀번호를 정확히 입력해 주십시오.");

  private final HttpStatus httpStatus;
  private final String message;

  ErrorCode(HttpStatus httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }
}
