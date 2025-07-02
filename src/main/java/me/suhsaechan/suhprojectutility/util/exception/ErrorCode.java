package me.suhsaechan.suhprojectutility.util.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
  // COMMON

  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다."),

  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

  ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근이 거부되었습니다."),

  // SCRIPT

  EMPTY_SCRIPT_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "스크립트 반환값이 없습니다"),

  // UTILS

  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),

  FILE_COPY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 복사에 실패했습니다."),

  // AUTHENTICATION

  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "ID와 비밀번호를 정확히 입력해 주십시오."),

  // FILE

  FILE_PATH_EMPTY(HttpStatus.BAD_REQUEST, "파일 경로가 비어 있거나 null입니다."),

  PERCENTILE_CALCULATION_ERROR(HttpStatus.BAD_REQUEST, "백분위 계산 오류: 전체 기준이 0 일 수 없습니다."),

  // NOTICE

  NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),

  INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "잘못된 파라미터입니다.");

  private final HttpStatus httpStatus;
  private final String message;
}
