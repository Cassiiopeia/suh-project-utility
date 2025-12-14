package me.suhsaechan.chatbot.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇 설정 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConfigDto {
    private UUID configId;
    private String configKey;
    private String configName;
    private String configValue;
    private String description;
    private Boolean isActive;
    private Integer orderIndex;
    private String createdDate;
    private String updatedDate;
}
