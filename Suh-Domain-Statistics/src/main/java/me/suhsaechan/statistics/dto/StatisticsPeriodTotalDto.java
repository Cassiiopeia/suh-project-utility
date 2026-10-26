package me.suhsaechan.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsPeriodTotalDto {
  private Long uniqueVisitors;
  private Long pageViews;
  private Long featureUsages;
  private Long chatTokens;
}
