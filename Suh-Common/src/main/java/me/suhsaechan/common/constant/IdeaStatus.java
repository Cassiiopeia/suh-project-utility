package me.suhsaechan.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 아이디어 보따리 항목 상태 (기본 IDEA)
@Getter
@RequiredArgsConstructor
public enum IdeaStatus {

  IDEA("아이디어"),
  IN_PROGRESS("진행중"),
  DONE("완료"),
  DROPPED("보류");

  private final String description;
}
