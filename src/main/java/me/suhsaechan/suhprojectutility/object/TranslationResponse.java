package me.suhsaechan.suhprojectutility.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.suhsaechan.suhprojectutility.object.constant.CommonStatus;
import me.suhsaechan.suhprojectutility.object.constant.TranslatorLanguage;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class TranslationResponse {

  private String translatedText;                   // 번역된 텍스트

  private TranslatorLanguage detectedLang;   // 감지된 원본 언어 (예외: AUTO)

  private TranslatorLanguage targetLang;     // 목표 언어

  private CommonStatus result;  // SUCCESS, FAIL

  private String errorMessage;                     // 에러 메시지 (실패 시)
}
