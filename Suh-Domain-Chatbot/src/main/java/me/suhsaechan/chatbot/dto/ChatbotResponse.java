package me.suhsaechan.chatbot.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {

    // 세션 토큰
    private String sessionToken;

    // 세션 ID
    private UUID sessionId;

    // AI 응답 메시지
    private String message;

    // 메시지 ID
    private UUID messageId;

    // 참조한 문서 정보
    private List<ReferencedDocument> references;

    // 응답 생성 시간 (ms)
    private Long responseTimeMs;

    /**
     * 참조 문서 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferencedDocument {
        private UUID documentId;
        private String title;
        private String category;
        private String snippet;
        private Float score;
    }
}
