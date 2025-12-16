package me.suhsaechan.statistics.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;

/**
 * 통계 응답 DTO
 * 통계 조회 관련 모든 응답 통합
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {

    // 방문자 통계
    private Long totalUniqueVisitors;
    private Long todayUniqueVisitors;
    private Long totalPageViews;
    private Long todayPageViews;

    // 기능별 사용 통계
    private Map<FeatureType, Long> featureUsageCounts;
    private Map<FeatureType, Long> todayFeatureUsageCounts;
}
