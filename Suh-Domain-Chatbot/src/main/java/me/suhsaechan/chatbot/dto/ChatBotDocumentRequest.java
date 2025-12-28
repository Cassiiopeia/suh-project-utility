package me.suhsaechan.chatbot.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇 문서 관리 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotDocumentRequest {
    private UUID documentId;
    private String title;
    private String category;
    private String content;
    private String description;
}
