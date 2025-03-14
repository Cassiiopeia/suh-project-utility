package me.suhsaechan.suhprojectutility.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.suhsaechan.suhprojectutility.object.constant.TranslatorLanguage;
import me.suhsaechan.suhprojectutility.object.constant.TranslatorType;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class TranslationRequest {

  private String text;         // 번역할 원본 텍스트

  private TranslatorType translatorType;

  @Builder.Default
  private TranslatorLanguage sourceLang = TranslatorLanguage.AUTO; // 원본 언어 (기본값: AUTO)

  private TranslatorLanguage targetLang; // 목표 언어
}
