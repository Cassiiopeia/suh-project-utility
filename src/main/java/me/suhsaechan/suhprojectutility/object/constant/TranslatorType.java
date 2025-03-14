package me.suhsaechan.suhprojectutility.object.constant;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TranslatorType {
  GOOGLE("구글 번역기"),
  PAPAGO("파파고 번역기");

  private final String description;

  @JsonValue
  public String getCode(String str) {
    return str.toUpperCase();
  }
}