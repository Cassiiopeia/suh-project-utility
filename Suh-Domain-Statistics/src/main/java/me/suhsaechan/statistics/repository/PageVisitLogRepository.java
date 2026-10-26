package me.suhsaechan.statistics.repository;

import java.time.LocalDateTime;
import java.util.List;
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

    // 기간 일별 방문 [날짜, 고유 방문자, 페이지뷰] (봇 제외, 끝 경계 미포함)
    @Query(value = """
        SELECT CAST(visited_at AS date) AS day,
               COUNT(DISTINCT client_hash) AS unique_visitors,
               COUNT(*) AS page_views
        FROM page_visit_log
        WHERE visited_at >= :start AND visited_at < :end AND is_bot = false
        GROUP BY CAST(visited_at AS date)
        """, nativeQuery = true)
    List<Object[]> countDailyVisitsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 기간 고유 방문자 수 (clientHash 기준, 봇 제외)
    @Query("SELECT COUNT(DISTINCT p.clientHash) FROM PageVisitLog p WHERE p.visitedAt >= :start AND p.visitedAt < :end AND p.isBot = false AND p.clientHash IS NOT NULL")
    long countDistinctClientHashBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 기간 페이지뷰 수 (봇 제외)
    @Query("SELECT COUNT(p) FROM PageVisitLog p WHERE p.visitedAt >= :start AND p.visitedAt < :end AND p.isBot = false")
    long countPageViewsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 기간 경로별 페이지뷰 [경로, 건수]
    @Query("SELECT p.pagePath, COUNT(p) FROM PageVisitLog p WHERE p.visitedAt >= :start AND p.visitedAt < :end AND p.isBot = false GROUP BY p.pagePath")
    List<Object[]> countGroupByPagePathBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 기간 referrer별 페이지뷰 [referrer(null 가능), 건수]
    @Query("SELECT p.referrer, COUNT(p) FROM PageVisitLog p WHERE p.visitedAt >= :start AND p.visitedAt < :end AND p.isBot = false GROUP BY p.referrer")
    List<Object[]> countGroupByReferrerBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 기간 요일 x 3시간 구간 페이지뷰 [ISO 요일(1 월~7 일), 구간(0~7), 건수]
    @Query(value = """
        SELECT EXTRACT(ISODOW FROM visited_at) AS iso_day_of_week,
               FLOOR(EXTRACT(HOUR FROM visited_at) / 3) AS hour_bucket,
               COUNT(*) AS page_views
        FROM page_visit_log
        WHERE visited_at >= :start AND visited_at < :end AND is_bot = false
        GROUP BY 1, 2
        """, nativeQuery = true)
    List<Object[]> countHeatmapBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
