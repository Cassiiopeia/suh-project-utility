package me.suhsaechan.statistics.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatisticsDto {
  private Integer periodDays;
  private StatisticsPeriodTotalDto currentTotal;
  private StatisticsPeriodTotalDto previousTotal;
  private List<DailyStatisticsDto> dailyStatistics;
  private List<FeatureUsageStatisticsDto> featureUsages;
  private List<ReferrerSourceStatisticsDto> referrerSources;
  private List<PageViewStatisticsDto> topPages;
  private List<VisitHeatmapCellDto> heatmapCells;
}
