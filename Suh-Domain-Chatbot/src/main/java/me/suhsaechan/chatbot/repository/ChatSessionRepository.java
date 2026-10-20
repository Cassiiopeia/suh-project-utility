package me.suhsaechan.chatbot.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.chatbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    // 세션 토큰으로 활성 세션 조회
    Optional<ChatSession> findBySessionTokenAndIsActiveTrue(String sessionToken);

    // 세션 토큰으로 세션 조회
    Optional<ChatSession> findBySessionToken(String sessionToken);

    // === 통계 관련 쿼리 ===

    // 총 세션 수
    long count();

    // 기간별 세션 수 (오늘 이후)
    @Query("SELECT COUNT(s) FROM ChatSession s WHERE s.createdDate >= :after")
    long countByCreatedDateAfter(@Param("after") LocalDateTime after);
}
