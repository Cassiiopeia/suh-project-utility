package me.suhsaechan.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRequest {
  // 조회 기간 일수 (7, 30, 90)
  private Integer periodDays;
}
