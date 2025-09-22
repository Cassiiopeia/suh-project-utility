package me.suhsaechan.aiserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 서버 Embeddings API용 페이로드 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingsPayload {

    /**
     * AI 모델명
     */
    private String model;

    /**
     * 임베딩할 입력 텍스트
     */
    private String input;
}