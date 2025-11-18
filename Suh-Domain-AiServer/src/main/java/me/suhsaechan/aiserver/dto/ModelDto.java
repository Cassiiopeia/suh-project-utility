package me.suhsaechan.aiserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 모델 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDto {

    /**
     * 모델 이름
     */
    private String name;

    /**
     * 모델 식별자
     */
    private String model;

    /**
     * 수정 일시
     */
    @JsonProperty("modified_at")
    private String modifiedAt;

    /**
     * 모델 크기 (바이트)
     */
    private Long size;

    /**
     * 다이제스트 (해시)
     */
    private String digest;

    /**
     * 모델 상세 정보
     */
    private ModelDetailsDto details;
}

