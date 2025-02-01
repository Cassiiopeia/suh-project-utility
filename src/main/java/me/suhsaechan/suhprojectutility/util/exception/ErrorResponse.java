package me.suhsaechan.suhprojectutility.util.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ErrorResponse {
  private final String errorCode;
  private final String httpStatus;
  private final String message;
}
