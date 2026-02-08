package me.suhsaechan.task.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.task.dto.TaskGoalDto;
import me.suhsaechan.task.dto.TaskRequest;
import me.suhsaechan.task.dto.TaskResponse;
import me.suhsaechan.task.entity.TaskGoal;
import me.suhsaechan.task.repository.TaskGoalRepository;
import me.suhsaechan.task.repository.TaskProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGoalService {

  private final TaskGoalRepository taskGoalRepository;
  private final TaskProgressRepository taskProgressRepository;

  @Transactional
  public TaskResponse createGoal(TaskRequest request) {
    TaskGoal taskGoal = TaskGoal.builder()
        .title(request.getTitle())
        .description(request.getDescription())
        .targetDate(request.getTargetDate())
        .totalAmount(request.getTotalAmount())
        .unit(request.getUnit() != null ? request.getUnit() : "페이지")
        .icon(request.getIcon() != null ? request.getIcon() : "fa-solid fa-bullseye")
        .color(request.getColor() != null ? request.getColor() : "blue")
        .isActive(true)
        .isCompleted(false)
        .priority(request.getPriority() != null ? request.getPriority() : 0)
        .build();

    TaskGoal saved = taskGoalRepository.save(taskGoal);
    log.info("Task Goal 생성 완료: {}", saved.getTitle());

    return TaskResponse.builder()
        .goal(toGoalDto(saved))
        .build();
  }

  @Transactional(readOnly = true)
  public TaskResponse getActiveGoals() {
    List<TaskGoal> goals = taskGoalRepository.findByIsActiveTrueOrderByPriorityAscTargetDateAsc();
    List<TaskGoalDto> goalDtos = goals.stream()
        .map(this::toGoalDto)
        .collect(Collectors.toList());

    return TaskResponse.builder()
        .goals(goalDtos)
        .totalGoals((long) goalDtos.size())
        .activeGoals(goalDtos.stream().filter(g -> Boolean.FALSE.equals(g.getIsCompleted())).count())
        .build();
  }

  @Transactional(readOnly = true)
  public TaskResponse getGoalDetail(UUID taskGoalId) {
    TaskGoal goal = taskGoalRepository.findById(taskGoalId)
        .orElseThrow(() -> new CustomException(ErrorCode.TASK_GOAL_NOT_FOUND));

    return TaskResponse.builder()
        .goal(toGoalDto(goal))
        .build();
  }

  @Transactional
  public TaskResponse updateGoal(TaskRequest request) {
    TaskGoal goal = taskGoalRepository.findById(request.getTaskGoalId())
        .orElseThrow(() -> new CustomException(ErrorCode.TASK_GOAL_NOT_FOUND));

    if (request.getTitle() != null) {
      goal.setTitle(request.getTitle());
    }
    if (request.getDescription() != null) {
      goal.setDescription(request.getDescription());
    }
    if (request.getTargetDate() != null) {
      goal.setTargetDate(request.getTargetDate());
    }
    if (request.getTotalAmount() != null) {
      goal.setTotalAmount(request.getTotalAmount());
    }
    if (request.getUnit() != null) {
      goal.setUnit(request.getUnit());
    }
    if (request.getIcon() != null) {
      goal.setIcon(request.getIcon());
    }
    if (request.getColor() != null) {
      goal.setColor(request.getColor());
    }
    if (request.getIsActive() != null) {
      goal.setIsActive(request.getIsActive());
    }
    if (request.getIsCompleted() != null) {
      goal.setIsCompleted(request.getIsCompleted());
    }
    if (request.getPriority() != null) {
      goal.setPriority(request.getPriority());
    }

    TaskGoal saved = taskGoalRepository.save(goal);
    log.info("Task Goal 수정 완료: {}", saved.getTitle());

    return TaskResponse.builder()
        .goal(toGoalDto(saved))
        .build();
  }

  @Transactional
  public void deleteGoal(UUID taskGoalId) {
    TaskGoal goal = taskGoalRepository.findById(taskGoalId)
        .orElseThrow(() -> new CustomException(ErrorCode.TASK_GOAL_NOT_FOUND));

    taskProgressRepository.deleteByTaskGoalTaskGoalId(taskGoalId);
    taskGoalRepository.delete(goal);
    log.info("Task Goal 삭제 완료: {}", goal.getTitle());
  }

  @Transactional
  public TaskResponse completeGoal(UUID taskGoalId) {
    TaskGoal goal = taskGoalRepository.findById(taskGoalId)
        .orElseThrow(() -> new CustomException(ErrorCode.TASK_GOAL_NOT_FOUND));

    goal.setIsCompleted(true);
    TaskGoal saved = taskGoalRepository.save(goal);
    log.info("Task Goal 완료 처리: {}", saved.getTitle());

    return TaskResponse.builder()
        .goal(toGoalDto(saved))
        .build();
  }

  @Transactional
  public TaskResponse cancelGoal(UUID taskGoalId) {
    TaskGoal goal = taskGoalRepository.findById(taskGoalId)
        .orElseThrow(() -> new CustomException(ErrorCode.TASK_GOAL_NOT_FOUND));

    goal.setIsActive(false);
    TaskGoal saved = taskGoalRepository.save(goal);
    log.info("Task Goal 취소 처리: {}", saved.getTitle());

    return TaskResponse.builder()
        .goal(toGoalDto(saved))
        .build();
  }

  private Long calculateDaysRemaining(TaskGoal goal) {
    if (goal.getTargetDate() == null) {
      return null;
    }
    return ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
  }

  private TaskGoalDto toGoalDto(TaskGoal goal) {
    Integer currentAmount = taskProgressRepository.findMaxEndAmountByTaskGoalId(goal.getTaskGoalId());

    Double progressPercentage = 0.0;
    if (goal.getTotalAmount() != null && goal.getTotalAmount() > 0 && currentAmount != null) {
      progressPercentage = (currentAmount.doubleValue() / goal.getTotalAmount()) * 100;
    }

    return TaskGoalDto.builder()
        .taskGoalId(goal.getTaskGoalId())
        .title(goal.getTitle())
        .description(goal.getDescription())
        .targetDate(goal.getTargetDate())
        .totalAmount(goal.getTotalAmount())
        .unit(goal.getUnit())
        .icon(goal.getIcon())
        .color(goal.getColor())
        .isActive(goal.getIsActive())
        .isCompleted(goal.getIsCompleted())
        .priority(goal.getPriority())
        .createdDate(goal.getCreatedDate())
        .updatedDate(goal.getUpdatedDate())
        .daysRemaining(calculateDaysRemaining(goal))
        .currentAmount(currentAmount)
        .progressPercentage(progressPercentage)
        .build();
  }
}
