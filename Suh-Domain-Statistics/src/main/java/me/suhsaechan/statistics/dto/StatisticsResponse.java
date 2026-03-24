package me.suhsaechan.statistics.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.common.dto.DailyStatDto;
import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {

    private Long totalUniqueVisitors;
    private Long todayUniqueVisitors;
    private Long totalPageViews;
    private Long todayPageViews;

    private Map<FeatureType, Long> featureUsageCounts;
    private Map<FeatureType, Long> todayFeatureUsageCounts;

    private List<DailyStatDto> dailyVisitors;
    private List<DailyStatDto> dailyPageViews;
    private List<DailyStatDto> dailyFeatureUsage;
    private List<DailyStatDto> dailyChatMessages;
    private List<DailyStatDto> dailyChatTokens;
}
