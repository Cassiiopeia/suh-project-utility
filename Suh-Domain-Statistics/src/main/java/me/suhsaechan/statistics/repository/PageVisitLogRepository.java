package me.suhsaechan.statistics.repository;

import java.time.LocalDateTime;
import java.util.UUID;
import me.suhsaechan.statistics.entity.PageVisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 페이지 방문 기록 Repository
 */
@Repository
public interface PageVisitLogRepository extends JpaRepository<PageVisitLog, UUID> {

    // 총 페이지뷰 (봇 제외)
    long countByIsBotFalse();

    // 기간별 페이지뷰 (봇 제외)
    long countByVisitedAtAfterAndIsBotFalse(LocalDateTime after);

    // 총 고유 방문자 수 (clientHash 기준, 봇 제외)
    @Query("SELECT COUNT(DISTINCT p.clientHash) FROM PageVisitLog p WHERE p.isBot = false AND p.clientHash IS NOT NULL")
    long countDistinctClientHash();

    // 기간별 고유 방문자 수 (clientHash 기준, 봇 제외)
    @Query("SELECT COUNT(DISTINCT p.clientHash) FROM PageVisitLog p WHERE p.visitedAt > :after AND p.isBot = false AND p.clientHash IS NOT NULL")
    long countDistinctClientHashAfter(@Param("after") LocalDateTime after);

    // 특정 페이지 조회수
    long countByPagePathAndIsBotFalse(String pagePath);

    // 특정 페이지 기간별 조회수
    long countByPagePathAndVisitedAtAfterAndIsBotFalse(String pagePath, LocalDateTime after);
}
