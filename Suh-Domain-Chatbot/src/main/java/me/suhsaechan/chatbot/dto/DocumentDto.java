package me.suhsaechan.chatbot.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 문서 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {
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
}
