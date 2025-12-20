package me.suhsaechan.translate.dto;

import kr.suhsaechan.ai.annotation.AiClass;
import kr.suhsaechan.ai.annotation.AiSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 번역 응답을 위한 구조화된 DTO
 * SUH-AIDER의 Structured Output 기능을 활용하여
 * AI가 JSON 형식으로 번역 결과를 반환하도록 합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AiClass(
    title = "Translation Result",
    description = "Structured translation result from AI model"
)
public class TranslationDto {

    @AiSchema(
        description = "번역된 텍스트 결과입니다. 원본 텍스트의 의미를 정확하게 목표 언어로 번역한 내용입니다.",
        required = true,
        minLength = 1,
        example = "안녕하세요"
    )
    private String translatedText;

    @AiSchema(
        description = "감지된 원본 언어 코드 (예: ko, en, ja). sourceLang이 AUTO인 경우 자동 감지된 언어입니다.",
        required = true,
        allowableValues = {"ko", "en", "ja", "zh-cn", "zh-tw", "es", "fr", "de", "ru", "pt", "it", "vi", "th", "id", "auto"},
        example = "en"
    )
    private String detectedLanguage;

    @AiSchema(
        description = "번역 품질 신뢰도 (0.0 ~ 1.0). 높을수록 번역 품질이 높습니다.",
        required = false,
        minimum = "0.0",
        maximum = "1.0",
        example = "0.95"
    )
    private Float confidence;
}
