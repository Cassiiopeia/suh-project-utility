package me.suhsaechan.task.dto;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

  // Goal 관련 필드
  private UUID taskGoalId;
  private String title;
  private String description;
  private LocalDate targetDate;
  private Integer totalAmount;
  private String unit;
  private String icon;
  private String color;
  private Boolean isActive;
  private Boolean isCompleted;
  private Integer priority;

  // Progress 관련 필드
  private UUID taskProgressId;
  private LocalDate progressDate;
  private String content;
  private Integer startAmount;
  private Integer endAmount;
  private String memo;

  // 조회 필터
  private Boolean includeCompleted;
  private LocalDate startDateFilter;
  private LocalDate endDateFilter;
}
