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

    // 기간별 전체 기능 사용 횟수
    long countByUsedAtAfter(LocalDateTime after);

    // 전체 기능별 사용 횟수 (GROUP BY - N+1 방지)
    @Query("SELECT f.featureName, COUNT(f) FROM FeatureUsageLog f GROUP BY f.featureName")
    List<Object[]> countGroupByFeatureName();

    // 기간별 기능별 사용 횟수 (GROUP BY - N+1 방지)
    @Query("SELECT f.featureName, COUNT(f) FROM FeatureUsageLog f WHERE f.usedAt > :after GROUP BY f.featureName")
    List<Object[]> countGroupByFeatureNameAfter(@Param("after") LocalDateTime after);
}
