package me.suhsaechan.chatbot.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.chatbot.entity.ChatbotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatbotConfigRepository extends JpaRepository<ChatbotConfig, UUID> {

    // 설정 키로 조회
    Optional<ChatbotConfig> findByConfigKey(String configKey);

    // 활성화된 설정 키로 조회
    Optional<ChatbotConfig> findByConfigKeyAndIsActiveTrue(String configKey);

    // 모든 활성화된 설정 조회
    List<ChatbotConfig> findByIsActiveTrueOrderByOrderIndexAsc();

    // 모든 설정 조회 (정렬)
    List<ChatbotConfig> findAllByOrderByOrderIndexAsc();
}
