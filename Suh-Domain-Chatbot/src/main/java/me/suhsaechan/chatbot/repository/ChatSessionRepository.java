package me.suhsaechan.chatbot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.chatbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    // 세션 토큰으로 활성 세션 조회
    Optional<ChatSession> findBySessionTokenAndIsActiveTrue(String sessionToken);

    // 세션 토큰으로 세션 조회
    Optional<ChatSession> findBySessionToken(String sessionToken);

    // 활성 세션 목록 (최근 활동 순)
    List<ChatSession> findByIsActiveTrueOrderByLastActivityAtDesc();

    // 비활성화 대상 세션 조회 (활성 상태이면서 특정 시간 이전에 마지막 활동)
    List<ChatSession> findByIsActiveTrueAndLastActivityAtBefore(LocalDateTime cutoffTime);

    // IP로 최근 세션 조회
    List<ChatSession> findByUserIpOrderByCreatedDateDesc(String userIp);

    // === 통계 관련 쿼리 ===

    // 총 세션 수
    long count();

    // 기간별 세션 수
    long countByCreatedDateAfter(LocalDateTime after);
}
