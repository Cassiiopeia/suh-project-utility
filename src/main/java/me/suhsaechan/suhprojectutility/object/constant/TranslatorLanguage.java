package me.suhsaechan.suhprojectutility.object.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TranslatorLanguage {
  AUTO("auto", "auto", "언어 감지"),
  KO("ko", "ko", "한국어"),
  EN("en", "en", "영어"),
  JA("ja", "ja", "일본어"),
  ZH_CN("zh-CN", "zh-CN", "중국어 (간체)"),
  ZH_TW("zh-TW", "zh-TW", "중국어 (번체)"),
  ES("es", "es", "스페인어"),
  FR("fr", "fr", "프랑스어"),
  DE("de", "de", "독일어"),
  RU("ru", "ru", "러시아어"),
  PT("pt", "pt", "포르투갈어"),
  IT("it", "it", "이탈리아어"),
  VI("vi", "vi", "베트남어"),
  TH("th", "th", "태국어"),
  ID("id", "id", "인도네시아어");

  private final String googleCode;
  private final String papagoCode;
  private final String name;

  @JsonCreator
  public static TranslatorLanguage fromCode(String code) {
    if (code == null) {
      return AUTO;
    }
    for (TranslatorLanguage language : values()) {
      if (language.getPapagoCode().equalsIgnoreCase(code)) {
        return language;
      }
    }
    return AUTO;
  }
}