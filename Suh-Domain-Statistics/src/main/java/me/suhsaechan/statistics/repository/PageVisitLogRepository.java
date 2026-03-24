package me.suhsaechan.statistics.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import me.suhsaechan.statistics.entity.PageVisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PageVisitLogRepository extends JpaRepository<PageVisitLog, UUID> {

    long countByIsBotFalse();

    long countByVisitedAtAfterAndIsBotFalse(LocalDateTime after);

    @Query("SELECT COUNT(DISTINCT p.clientHash) FROM PageVisitLog p WHERE p.isBot = false AND p.clientHash IS NOT NULL")
    long countDistinctClientHash();

    @Query("SELECT COUNT(DISTINCT p.clientHash) FROM PageVisitLog p WHERE p.visitedAt > :after AND p.isBot = false AND p.clientHash IS NOT NULL")
    long countDistinctClientHashAfter(@Param("after") LocalDateTime after);

    long countByPagePathAndIsBotFalse(String pagePath);

    long countByPagePathAndVisitedAtAfterAndIsBotFalse(String pagePath, LocalDateTime after);

    @Query(value = "SELECT DATE(p.visited_at) as date, COUNT(DISTINCT p.client_hash) as count "
        + "FROM page_visit_log p WHERE p.is_bot = false AND p.visited_at >= :since "
        + "GROUP BY DATE(p.visited_at) ORDER BY date", nativeQuery = true)
    List<Object[]> countDailyUniqueVisitorsSince(@Param("since") LocalDateTime since);

    @Query(value = "SELECT DATE(p.visited_at) as date, COUNT(p.page_visit_log_id) as count "
        + "FROM page_visit_log p WHERE p.is_bot = false AND p.visited_at >= :since "
        + "GROUP BY DATE(p.visited_at) ORDER BY date", nativeQuery = true)
    List<Object[]> countDailyPageViewsSince(@Param("since") LocalDateTime since);
}
