package me.suhsaechan.statistics.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import me.suhsaechan.statistics.entity.FeatureUsageLog;
import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureUsageLogRepository extends JpaRepository<FeatureUsageLog, UUID> {

    long countByFeatureName(FeatureType featureName);

    long countByFeatureNameAndUsedAtAfter(FeatureType featureName, LocalDateTime after);

    long count();

    long countByUsedAtAfter(LocalDateTime after);

    @Query("SELECT f.featureName, COUNT(f) FROM FeatureUsageLog f GROUP BY f.featureName")
    List<Object[]> countGroupByFeatureName();

    @Query("SELECT f.featureName, COUNT(f) FROM FeatureUsageLog f WHERE f.usedAt > :after GROUP BY f.featureName")
    List<Object[]> countGroupByFeatureNameAfter(@Param("after") LocalDateTime after);

    @Query(value = "SELECT DATE(f.used_at) as date, COUNT(f.feature_usage_log_id) as count "
        + "FROM feature_usage_log f WHERE f.used_at >= :since "
        + "GROUP BY DATE(f.used_at) ORDER BY date", nativeQuery = true)
    List<Object[]> countDailyUsageSince(@Param("since") LocalDateTime since);
}
