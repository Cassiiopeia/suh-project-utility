package me.suhsaechan.chatbot.dto;

import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 벡터 검색 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchResult {
    private UUID pointId;
    private Float score;
    private String content;
    private Map<String, String> metadata;
}
