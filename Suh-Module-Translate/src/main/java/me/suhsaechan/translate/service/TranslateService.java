package me.suhsaechan.translate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.suhsaechan.ai.model.JsonSchema;
import kr.suhsaechan.ai.model.SuhAiderRequest;
import kr.suhsaechan.ai.model.SuhAiderResponse;
import kr.suhsaechan.ai.service.SuhAiderEngine;
import kr.suhsaechan.ai.util.JsonSchemaClassParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.CommonStatus;
import me.suhsaechan.common.constant.TranslatorLanguage;
import me.suhsaechan.translate.dto.TranslationDto;
import me.suhsaechan.translate.dto.TranslationRequest;
import me.suhsaechan.translate.dto.TranslationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI 기반 번역 서비스
 * SUH-AIDER 엔진을 사용하여 직접 번역을 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateService {

    private final SuhAiderEngine suhAiderEngine;
    private final ObjectMapper objectMapper;

    @Value("${suh.aider.translation-model:gemma3:1b}")
    private String translationModel;

    /**
     * AI 기반 번역 수행
     */
    public TranslationResponse translate(TranslationRequest request) {
        // 1. 입력 검증
        String text = request.getText();
        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("번역할 텍스트가 비어있습니다.");
        }

        log.info("AI 번역 시작 - 모델: {}, 원본 언어: {}, 목표 언어: {}",
            translationModel, request.getSourceLang(), request.getTargetLang());

        try {
            // 2. 프롬프트 생성
            String prompt = buildTranslationPrompt(
                text,
                request.getSourceLang(),
                request.getTargetLang()
            );

            // 3. AI 호출 (Structured Output)
            JsonSchema schema = JsonSchemaClassParser.parse(TranslationDto.class);
            log.debug("생성된 JSON Schema: {}", schema);

            SuhAiderRequest suhAiderRequest = SuhAiderRequest.builder()
                .model(translationModel)
                .prompt(prompt)
                .stream(false)
                .responseSchema(schema)  // Structured Output 활성화
                .build();

            SuhAiderResponse suhAiderResponse = suhAiderEngine.generate(suhAiderRequest);
            String aiResponse = suhAiderResponse.getResponse();

            log.debug("AI 응답: {}", aiResponse);

            // 4. JSON 파싱
            TranslationDto aiResult = objectMapper.readValue(aiResponse, TranslationDto.class);

            // 5. 응답 변환
            TranslatorLanguage detectedLang = TranslatorLanguage.fromEnglishNameOrCode(aiResult.getDetectedLanguage());

            log.info("AI 번역 완료 - 감지 언어: {}, 신뢰도: {}",
                detectedLang, aiResult.getConfidence());

            return TranslationResponse.builder()
                .translatedText(aiResult.getTranslatedText())
                .originalText(text)
                .translatorType(request.getTranslatorType())
                .detectedLang(detectedLang)
                .sourceLang(detectedLang)
                .targetLang(request.getTargetLang())
                .result(CommonStatus.SUCCESS)
                .build();

        } catch (Exception e) {
            log.error("AI 번역 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("번역 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 번역 프롬프트 생성
     */
    private String buildTranslationPrompt(String text, TranslatorLanguage sourceLang, TranslatorLanguage targetLang) {
        String sourceDisplay = sourceLang.getEnglishName();
        String targetDisplay = targetLang.getEnglishName();

        String prompt = "You are a professional translator AI.\n\n" +
            "Task: Translate the following text from " + sourceDisplay + " to " + targetDisplay + ".\n\n" +
            "Original Text:\n" + text + "\n\n" +
            "Requirements:\n" +
            "- Provide accurate and natural translation\n" +
            "- Preserve the original meaning and tone\n" +
            "- If source language is \"auto-detect\", automatically detect the language and provide the detected language code (ko, en, ja, etc.)\n" +
            "- Provide translation confidence score (0.0 to 1.0)\n\n" +
            "IMPORTANT: You must respond ONLY with a valid JSON object matching this format:\n" +
            "{\n" +
            "  \"translatedText\": \"번역된 텍스트\",\n" +
            "  \"detectedLanguage\": \"ko\",\n" +
            "  \"confidence\": 0.95\n" +
            "}\n\n" +
            "Do not include any explanation or additional text outside the JSON object.";

        return prompt;
    }
}
