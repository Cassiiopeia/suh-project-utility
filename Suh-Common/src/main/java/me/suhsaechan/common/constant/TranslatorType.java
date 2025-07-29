package me.suhsaechan.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TranslatorType {
  GOOGLE("구글 번역기"),
  PAPAGO("파파고 번역기");

  private final String description;
}