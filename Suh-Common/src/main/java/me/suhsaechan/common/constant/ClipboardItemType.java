package me.suhsaechan.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 웹 클립보드 항목 종류: TEXT 본문 DB 저장, IMAGE·FILE 본문 디스크 저장
@Getter
@RequiredArgsConstructor
public enum ClipboardItemType {

  TEXT("텍스트"),
  IMAGE("이미지"),
  FILE("파일");

  private final String description;
}
