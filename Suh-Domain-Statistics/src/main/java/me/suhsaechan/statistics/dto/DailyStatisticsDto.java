package me.suhsaechan.statistics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatisticsDto {
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate date;
  private Long uniqueVisitors;
  private Long pageViews;
  private Long featureUsages;
  private Long chatTokens;
}
