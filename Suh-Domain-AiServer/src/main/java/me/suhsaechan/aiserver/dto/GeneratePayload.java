package me.suhsaechan.aiserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 서버 Generate API용 페이로드 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePayload {

    /**
     * AI 모델명
     */
    private String model;

    /**
     * AI에게 전달할 프롬프트 텍스트
     */
    private String prompt;

    /**
     * 스트림 모드 사용 여부 (기본값: false)
     */
    private Boolean stream;
}