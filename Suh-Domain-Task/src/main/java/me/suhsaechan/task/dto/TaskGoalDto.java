package me.suhsaechan.task.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskGoalDto {
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
  private LocalDateTime createdDate;
  private LocalDateTime updatedDate;

  // 계산된 필드
  private Long daysRemaining;
  private Integer currentAmount;
  private Double progressPercentage;
}
