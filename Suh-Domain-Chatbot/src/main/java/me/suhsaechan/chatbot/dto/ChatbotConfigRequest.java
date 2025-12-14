package me.suhsaechan.chatbot.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇 설정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConfigRequest {
    private UUID configId;
    private String configKey;
    private String configName;
    private String configValue;
    private String description;
}
