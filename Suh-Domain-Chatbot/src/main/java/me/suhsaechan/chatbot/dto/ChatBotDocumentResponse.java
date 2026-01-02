package me.suhsaechan.chatbot.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇 문서 관리 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotDocumentResponse {
    private List<ChatBotDocumentDto> documents;
    private ChatBotDocumentDto document;
    private Integer totalCount;
}
