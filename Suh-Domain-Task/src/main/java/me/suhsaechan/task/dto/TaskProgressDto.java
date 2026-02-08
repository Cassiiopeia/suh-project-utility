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
public class TaskProgressDto {
  private UUID taskProgressId;
  private UUID taskGoalId;
  private String taskGoalTitle;
  private LocalDate progressDate;
  private String content;
  private Integer startAmount;
  private Integer endAmount;
  private String memo;
  private Boolean isCompleted;
  private LocalDateTime createdDate;

  // 계산된 필드
  private Integer amountDone;
}
