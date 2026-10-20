package me.suhsaechan.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{
  private final ErrorCode errorCode;

  public CustomException(ErrorCode errorCode){
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  // 원인 예외 보존 (스택 추적용)
  public CustomException(ErrorCode errorCode, Throwable cause){
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }
}
