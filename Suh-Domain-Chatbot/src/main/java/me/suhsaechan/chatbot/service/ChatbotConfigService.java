package me.suhsaechan.chatbot.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.dto.ChatbotConfigDto;
import me.suhsaechan.chatbot.entity.ChatbotConfig;
import me.suhsaechan.chatbot.repository.ChatbotConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 챗봇 설정 서비스
 * 시스템 프롬프트 등 챗봇 설정 관리 (인메모리 캐시 적용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotConfigService {

    private final ChatbotConfigRepository configRepository;

    // 인메모리 캐시 (configKey -> configValue)
    private final ConcurrentHashMap<String, String> configCache = new ConcurrentHashMap<>();

    // 기본 시스템 프롬프트 (DB에 없을 경우 사용)
    private static final String DEFAULT_SYSTEM_PROMPT = """
        ## 당신은 'SuhNi(서니)'입니다.

        SUH Project Utility 사이트의 AI 도우미입니다.

        ### 서니 소개
        - 이름: 서니 (SuhNi)
        - 역할: SUH Project Utility 사이트 이용자를 돕는 친절한 AI 어시스턴트
        - 성격: 친절하고 차분하며, 사용자의 질문에 성실하게 답변합니다.

        ### 응답 가이드라인
        - 한국어로 친절하게 존댓말을 사용하세요.
        - 질문에 직접적이고 간결하게 답변하세요.
        - "너는 누구야?"라는 질문에는 서니로서 자기소개를 해주세요.
        - 욕설이나 부적절한 언어에는 정중하게 대화 방향 전환을 유도하세요.
        - 모르는 내용은 솔직하게 "아직 해당 정보가 준비되지 않았습니다"라고 답변하세요.
        """;

    /**
     * 설정 값 조회 (캐시 우선)
     */
    public String getConfigValue(String configKey) {
        // 캐시에서 먼저 조회
        String cachedValue = configCache.get(configKey);
        if (cachedValue != null) {
            log.debug("캐시에서 설정 조회 - key: {}", configKey);
            return cachedValue;
        }

        // DB에서 조회
        return configRepository.findByConfigKeyAndIsActiveTrue(configKey)
            .map(config -> {
                // 캐시에 저장
                configCache.put(configKey, config.getConfigValue());
                log.debug("DB에서 설정 조회 후 캐시 저장 - key: {}", configKey);
                return config.getConfigValue();
            })
            .orElseGet(() -> {
                log.warn("설정을 찾을 수 없음, 기본값 사용 - key: {}", configKey);
                // 시스템 프롬프트인 경우 기본값 반환
                if ("system_prompt".equals(configKey)) {
                    return DEFAULT_SYSTEM_PROMPT;
                }
                return "";
            });
    }

    /**
     * 시스템 프롬프트 조회 (편의 메서드)
     */
    public String getSystemPrompt() {
        return getConfigValue("system_prompt");
    }

    /**
     * 모든 설정 조회
     */
    @Transactional(readOnly = true)
    public List<ChatbotConfigDto> getAllConfigs() {
        return configRepository.findAllByOrderByOrderIndexAsc()
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * 설정 ID로 조회
     */
    @Transactional(readOnly = true)
    public ChatbotConfigDto getConfig(UUID configId) {
        ChatbotConfig config = configRepository.findById(configId)
            .orElseThrow(() -> new RuntimeException("설정을 찾을 수 없습니다: " + configId));
        return toDto(config);
    }

    /**
     * 설정 키로 조회
     */
    @Transactional(readOnly = true)
    public ChatbotConfigDto getConfigByKey(String configKey) {
        ChatbotConfig config = configRepository.findByConfigKey(configKey)
            .orElseThrow(() -> new RuntimeException("설정을 찾을 수 없습니다: " + configKey));
        return toDto(config);
    }

    /**
     * 설정 수정 (캐시 갱신)
     */
    @Transactional
    public ChatbotConfigDto updateConfig(UUID configId, String configName, String configValue, String description) {
        ChatbotConfig config = configRepository.findById(configId)
            .orElseThrow(() -> new RuntimeException("설정을 찾을 수 없습니다: " + configId));

        config.setConfigName(configName);
        config.setConfigValue(configValue);
        config.setDescription(description);
        config = configRepository.save(config);

        // 캐시 갱신
        configCache.put(config.getConfigKey(), configValue);
        log.info("설정 수정 및 캐시 갱신 - key: {}", config.getConfigKey());

        return toDto(config);
    }

    /**
     * 설정 활성화/비활성화 (캐시 무효화)
     */
    @Transactional
    public void toggleConfigActive(UUID configId) {
        ChatbotConfig config = configRepository.findById(configId)
            .orElseThrow(() -> new RuntimeException("설정을 찾을 수 없습니다: " + configId));

        config.setIsActive(!config.getIsActive());
        configRepository.save(config);

        // 캐시 무효화
        configCache.remove(config.getConfigKey());
        log.info("설정 활성화 상태 변경 - key: {}, isActive: {}", config.getConfigKey(), config.getIsActive());
    }

    /**
     * 캐시 초기화 (전체)
     */
    public void clearCache() {
        configCache.clear();
        log.info("설정 캐시 전체 초기화");
    }

    /**
     * 캐시 초기화 (특정 키)
     */
    public void clearCache(String configKey) {
        configCache.remove(configKey);
        log.info("설정 캐시 초기화 - key: {}", configKey);
    }

    /**
     * Entity -> DTO 변환
     */
    private ChatbotConfigDto toDto(ChatbotConfig config) {
        return ChatbotConfigDto.builder()
            .configId(config.getChatbotConfigId())
            .configKey(config.getConfigKey())
            .configName(config.getConfigName())
            .configValue(config.getConfigValue())
            .description(config.getDescription())
            .isActive(config.getIsActive())
            .orderIndex(config.getOrderIndex())
            .createdDate(config.getCreatedDate() != null ? config.getCreatedDate().toString() : null)
            .updatedDate(config.getUpdatedDate() != null ? config.getUpdatedDate().toString() : null)
            .build();
    }
}
