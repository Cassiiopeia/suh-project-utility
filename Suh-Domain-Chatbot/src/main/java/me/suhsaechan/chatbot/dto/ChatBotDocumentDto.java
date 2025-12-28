package me.suhsaechan.chatbot.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.chatbot.entity.ChatDocument;

/**
 * 챗봇 문서 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotDocumentDto {
    private UUID documentId;
    private String title;
    private String category;
    private String content;
    private String description;
    private Boolean isActive;
    private Boolean isProcessed;
    private Integer chunkCount;
    private String createdDate;
    private String updatedDate;

    /**
     * Entity -> DTO 변환
     */
    public static ChatBotDocumentDto from(ChatDocument document) {
        return ChatBotDocumentDto.builder()
            .documentId(document.getChatDocumentId())
            .title(document.getTitle())
            .category(document.getCategory())
            .content(document.getContent())
            .description(document.getDescription())
            .isActive(document.getIsActive())
            .isProcessed(document.getIsProcessed())
            .chunkCount(document.getChunkCount())
            .createdDate(document.getCreatedDate() != null ? document.getCreatedDate().toString() : null)
            .updatedDate(document.getUpdatedDate() != null ? document.getUpdatedDate().toString() : null)
            .build();
    }
}
