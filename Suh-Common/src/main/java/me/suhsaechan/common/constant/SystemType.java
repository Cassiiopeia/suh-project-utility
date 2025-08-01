package me.suhsaechan.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum SystemType {
  WINDOWS("윈도우 환경"),
  MAC("MAC 환경"),
  LINUX("리눅스 환경"),
  OTHER("기타 환경");
  private final String description;
}
