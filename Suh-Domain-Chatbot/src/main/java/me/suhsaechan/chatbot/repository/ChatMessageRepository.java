package me.suhsaechan.chatbot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import me.suhsaechan.chatbot.entity.ChatMessage;
import me.suhsaechan.chatbot.entity.ChatMessage.MessageRole;
import me.suhsaechan.chatbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    // 세션별 메시지 조회 (순서대로)
    List<ChatMessage> findByChatSessionOrderByMessageIndexAsc(ChatSession chatSession);

    // 세션 ID로 메시지 조회
    List<ChatMessage> findByChatSessionChatSessionIdOrderByMessageIndexAsc(UUID chatSessionId);

    // 세션별 메시지 수
    long countByChatSession(ChatSession chatSession);

    // 세션별 역할별 메시지 조회
    List<ChatMessage> findByChatSessionAndRoleOrderByMessageIndexAsc(ChatSession chatSession, MessageRole role);

    // 세션별 최근 N개 메시지 조회
    List<ChatMessage> findTop10ByChatSessionOrderByMessageIndexDesc(ChatSession chatSession);

    // 세션별 메시지 삭제
    void deleteByChatSession(ChatSession chatSession);

    // 세션 ID로 메시지 삭제
    void deleteByChatSessionChatSessionId(UUID chatSessionId);

    // 피드백이 있는 메시지 조회
    List<ChatMessage> findByIsHelpfulNotNull();

    // === 통계 관련 쿼리 ===

    // 총 메시지 수
    long count();

    // 기간별 메시지 수
    long countByCreatedDateAfter(LocalDateTime after);

    // 총 입력 토큰 합계
    @Query("SELECT COALESCE(SUM(m.inputTokens), 0) FROM ChatMessage m WHERE m.inputTokens IS NOT NULL")
    Long sumTotalInputTokens();

    // 총 출력 토큰 합계
    @Query("SELECT COALESCE(SUM(m.outputTokens), 0) FROM ChatMessage m WHERE m.outputTokens IS NOT NULL")
    Long sumTotalOutputTokens();

    // 기간별 입력 토큰 합계
    @Query("SELECT COALESCE(SUM(m.inputTokens), 0) FROM ChatMessage m WHERE m.inputTokens IS NOT NULL AND m.createdDate > :after")
    Long sumInputTokensAfter(@Param("after") LocalDateTime after);

    // 기간별 출력 토큰 합계
    @Query("SELECT COALESCE(SUM(m.outputTokens), 0) FROM ChatMessage m WHERE m.outputTokens IS NOT NULL AND m.createdDate > :after")
    Long sumOutputTokensAfter(@Param("after") LocalDateTime after);
}
