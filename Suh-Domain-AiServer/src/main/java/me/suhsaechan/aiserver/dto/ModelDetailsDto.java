package me.suhsaechan.aiserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 모델 상세 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDetailsDto {

    /**
     * 부모 모델명
     */
    @JsonProperty("parent_model")
    private String parentModel;

    /**
     * 포맷 (예: gguf)
     */
    private String format;

    /**
     * 모델 패밀리 (예: gemma3, qwen3)
     */
    private String family;

    /**
     * 모델 패밀리 목록
     */
    private List<String> families;

    /**
     * 파라미터 크기 (예: 4.3B, 8.0B)
     */
    @JsonProperty("parameter_size")
    private String parameterSize;

    /**
     * 양자화 레벨 (예: Q4_K_M, Q8_0)
     */
    @JsonProperty("quantization_level")
    private String quantizationLevel;
}

