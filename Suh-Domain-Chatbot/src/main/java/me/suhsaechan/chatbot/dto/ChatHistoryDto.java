package me.suhsaechan.chatbot.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.chatbot.entity.ChatMessage.MessageRole;

/**
 * 채팅 히스토리 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryDto {

    private UUID messageId;
    private MessageRole role;
    private String content;
    private Integer messageIndex;
    private LocalDateTime createdAt;
    private Boolean isHelpful;
}
