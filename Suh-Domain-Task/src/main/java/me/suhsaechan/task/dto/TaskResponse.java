package me.suhsaechan.task.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

  @Builder.Default
  private List<TaskGoalDto> goals = new ArrayList<>();
  private TaskGoalDto goal;

  @Builder.Default
  private List<TaskProgressDto> progressList = new ArrayList<>();
  private TaskProgressDto progress;

  private Long totalGoals;
  private Long activeGoals;
  private Double progressPercentage;
}
