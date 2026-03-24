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

    List<ChatMessage> findByChatSessionOrderByMessageIndexAsc(ChatSession chatSession);

    List<ChatMessage> findByChatSessionChatSessionIdOrderByMessageIndexAsc(UUID chatSessionId);

    long countByChatSession(ChatSession chatSession);

    List<ChatMessage> findByChatSessionAndRoleOrderByMessageIndexAsc(ChatSession chatSession, MessageRole role);

    List<ChatMessage> findTop10ByChatSessionOrderByMessageIndexDesc(ChatSession chatSession);

    void deleteByChatSession(ChatSession chatSession);

    void deleteByChatSessionChatSessionId(UUID chatSessionId);

    List<ChatMessage> findByIsHelpfulNotNull();

    // === 통계 관련 쿼리 ===

    long count();

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.createdDate >= :after")
    long countByCreatedDateAfter(@Param("after") LocalDateTime after);

    @Query("SELECT COALESCE(SUM(m.inputTokens), 0) FROM ChatMessage m WHERE m.inputTokens IS NOT NULL")
    Long sumTotalInputTokens();

    @Query("SELECT COALESCE(SUM(m.outputTokens), 0) FROM ChatMessage m WHERE m.outputTokens IS NOT NULL")
    Long sumTotalOutputTokens();

    @Query("SELECT COALESCE(SUM(m.inputTokens), 0) FROM ChatMessage m WHERE m.inputTokens IS NOT NULL AND m.createdDate > :after")
    Long sumInputTokensAfter(@Param("after") LocalDateTime after);

    @Query("SELECT COALESCE(SUM(m.outputTokens), 0) FROM ChatMessage m WHERE m.outputTokens IS NOT NULL AND m.createdDate > :after")
    Long sumOutputTokensAfter(@Param("after") LocalDateTime after);

    @Query(value = "SELECT DATE(m.created_date) as date, COUNT(m.chat_message_id) as count "
        + "FROM chat_message m WHERE m.created_date >= :since "
        + "GROUP BY DATE(m.created_date) ORDER BY date", nativeQuery = true)
    List<Object[]> countDailyMessagesSince(@Param("since") LocalDateTime since);

    @Query(value = "SELECT DATE(m.created_date) as date, COALESCE(SUM(COALESCE(m.input_tokens, 0) + COALESCE(m.output_tokens, 0)), 0) as count "
        + "FROM chat_message m WHERE m.created_date >= :since "
        + "GROUP BY DATE(m.created_date) ORDER BY date", nativeQuery = true)
    List<Object[]> countDailyTokensSince(@Param("since") LocalDateTime since);
}
