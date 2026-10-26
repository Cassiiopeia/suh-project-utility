package me.suhsaechan.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageViewStatisticsDto {
  private String pagePath;
  private Long viewCount;
}
