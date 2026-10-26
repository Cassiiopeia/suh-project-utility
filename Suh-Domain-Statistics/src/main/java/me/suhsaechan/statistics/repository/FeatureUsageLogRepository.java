package me.suhsaechan.statistics.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import me.suhsaechan.statistics.entity.FeatureUsageLog;
import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 기능 사용 기록 Repository
 */
@Repository
public interface FeatureUsageLogRepository extends JpaRepository<FeatureUsageLog, UUID> {

    // 특정 기능 총 사용 횟수
    long countByFeatureName(FeatureType featureName);

    // 특정 기능 기간별 사용 횟수
    long countByFeatureNameAndUsedAtAfter(FeatureType featureName, LocalDateTime after);

    // 전체 기능 사용 횟수
    long count();

    // 전체 기능별 사용 횟수 (GROUP BY - N+1 방지)
    @Query("SELECT f.featureName, COUNT(f) FROM FeatureUsageLog f GROUP BY f.featureName")
    List<Object[]> countGroupByFeatureName();

    // 기간별 기능별 사용 횟수 (GROUP BY - N+1 방지)
    @Query("SELECT f.featureName, COUNT(f) FROM FeatureUsageLog f WHERE f.usedAt > :after GROUP BY f.featureName")
    List<Object[]> countGroupByFeatureNameAfter(@Param("after") LocalDateTime after);

    // 기간 일별 기능 사용 [날짜, 건수] (제외 기능 이름 목록 제외, 끝 경계 미포함)
    @Query(value = """
        SELECT CAST(used_at AS date) AS day, COUNT(*) AS usage_count
        FROM feature_usage_log
        WHERE used_at >= :start AND used_at < :end AND feature_name NOT IN (:excludedFeatureNames)
        GROUP BY CAST(used_at AS date)
        """, nativeQuery = true)
    List<Object[]> countDailyUsageBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                          @Param("excludedFeatureNames") List<String> excludedFeatureNames);

    // 기간 기능 사용 횟수 (제외 기능 제외)
    @Query("SELECT COUNT(f) FROM FeatureUsageLog f WHERE f.usedAt >= :start AND f.usedAt < :end AND f.featureName NOT IN :excludedFeatures")
    long countUsageBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                           @Param("excludedFeatures") Collection<FeatureType> excludedFeatures);

    // 기간 기능별 사용 횟수 [기능, 건수] (제외 기능 제외)
    @Query("SELECT f.featureName, COUNT(f) FROM FeatureUsageLog f WHERE f.usedAt >= :start AND f.usedAt < :end AND f.featureName NOT IN :excludedFeatures GROUP BY f.featureName")
    List<Object[]> countGroupByFeatureNameBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                                  @Param("excludedFeatures") Collection<FeatureType> excludedFeatures);
}
