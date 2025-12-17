package me.suhsaechan.common.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TranslatorLanguage {
  AUTO("auto", "auto-detect", "언어 감지"),
  KO("ko", "Korean", "한국어"),
  EN("en", "English", "영어"),
  JA("ja", "Japanese", "일본어"),
  ZH_CN("zh-CN", "Chinese (Simplified)", "중국어 (간체)"),
  ZH_TW("zh-TW", "Chinese (Traditional)", "중국어 (번체)"),
  ES("es", "Spanish", "스페인어"),
  FR("fr", "French", "프랑스어"),
  DE("de", "German", "독일어"),
  RU("ru", "Russian", "러시아어"),
  PT("pt", "Portuguese", "포르투갈어"),
  IT("it", "Italian", "이탈리아어"),
  VI("vi", "Vietnamese", "베트남어"),
  TH("th", "Thai", "태국어"),
  ID("id", "Indonesian", "인도네시아어");

  private final String code;
  private final String englishName;
  private final String koreanName;

  @JsonCreator
  public static TranslatorLanguage fromCode(String code) {
    if (code == null) {
      return AUTO;
    }
    for (TranslatorLanguage language : values()) {
      if (language.getCode().equalsIgnoreCase(code)) {
        return language;
      }
    }
    return AUTO;
  }

  public static TranslatorLanguage fromEnglishNameOrCode(String value) {
    if (value == null) {
      return AUTO;
    }
    String lowerValue = value.toLowerCase();
    for (TranslatorLanguage language : values()) {
      if (language.getCode().equalsIgnoreCase(value) ||
          language.getEnglishName().equalsIgnoreCase(value)) {
        return language;
      }
    }
    return AUTO;
  }
}