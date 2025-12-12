package me.suhsaechan.common.util;

import kr.suhsaechan.ai.service.SuhAiderEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SUH-AIDER AI 유틸리티 클래스
 * SuhAiderEngine을 사용하여 AI 응답을 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuhAiUtil {

    private final SuhAiderEngine suhAiderEngine;

    @Value("${suh.aider.default-model:llama2}")
    private String defaultModel;
    private final String GEMMA_4B = "gemma3:4b";
    private final String GEMMA_1B = "gemma3:1b";

    /**
     * 기본 모델을 사용하여 AI 응답 생성
     *
     * @param prompt 프롬프트 텍스트
     * @return 생성된 응답 텍스트
     */
    public String generateResponse(String prompt) {
        log.debug("AI 응답 생성 요청 - 모델: {}, 프롬프트 길이: {}", defaultModel, prompt != null ? prompt.length() : 0);
        return suhAiderEngine.generate(GEMMA_1B, prompt);
    }

    /**
     * 지정된 모델을 사용하여 AI 응답 생성
     *
     * @param model 모델명 (예: "llama2", "mistral")
     * @param prompt 프롬프트 텍스트
     * @return 생성된 응답 텍스트
     */
    public String generateResponse(String model, String prompt) {
        log.debug("AI 응답 생성 요청 - 모델: {}, 프롬프트 길이: {}", model, prompt != null ? prompt.length() : 0);
        return suhAiderEngine.generate(GEMMA_1B, prompt);
    }
}
