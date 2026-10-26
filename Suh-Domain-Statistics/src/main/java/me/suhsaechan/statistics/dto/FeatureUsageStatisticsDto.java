package me.suhsaechan.statistics.dto;

import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUsageStatisticsDto {
  private FeatureType featureName;
  private Long usageCount;
}
