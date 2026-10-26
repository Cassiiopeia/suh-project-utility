package me.suhsaechan.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitHeatmapCellDto {
  private Integer dayOfWeek;
  private Integer hourBucket;
  private Long pageViews;
}
