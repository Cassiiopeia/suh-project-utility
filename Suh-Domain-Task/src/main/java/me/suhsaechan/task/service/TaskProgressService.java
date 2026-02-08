package me.suhsaechan.task.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.task.dto.TaskProgressDto;
import me.suhsaechan.task.dto.TaskRequest;
import me.suhsaechan.task.dto.TaskResponse;
import me.suhsaechan.task.entity.TaskGoal;
import me.suhsaechan.task.entity.TaskProgress;
import me.suhsaechan.task.repository.TaskGoalRepository;
import me.suhsaechan.task.repository.TaskProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskProgressService {

  private final TaskProgressRepository taskProgressRepository;
  private final TaskGoalRepository taskGoalRepository;

  @Transactional
  public TaskResponse saveProgress(TaskRequest request) {
    TaskGoal goal = taskGoalRepository.findById(request.getTaskGoalId())
        .orElseThrow(() -> new CustomException(ErrorCode.TASK_GOAL_NOT_FOUND));

    LocalDate progressDate = request.getProgressDate() != null
        ? request.getProgressDate()
        : LocalDate.now();

    Optional<TaskProgress> existingProgress = taskProgressRepository
        .findByTaskGoalTaskGoalIdAndProgressDate(request.getTaskGoalId(), progressDate);

    TaskProgress progress;
    if (existingProgress.isPresent()) {
      progress = existingProgress.get();
      if (request.getContent() != null) {
        progress.setContent(request.getContent());
      }
      if (request.getStartAmount() != null) {
        progress.setStartAmount(request.getStartAmount());
      }
      if (request.getEndAmount() != null) {
        progress.setEndAmount(request.getEndAmount());
      }
      if (request.getMemo() != null) {
        progress.setMemo(request.getMemo());
      }
      if (request.getIsCompleted() != null) {
        progress.setIsCompleted(request.getIsCompleted());
      }
      log.info("Task Progress 수정: {} - {}", goal.getTitle(), progressDate);
    } else {
      progress = TaskProgress.builder()
          .taskGoal(goal)
          .progressDate(progressDate)
          .content(request.getContent())
          .startAmount(request.getStartAmount())
          .endAmount(request.getEndAmount())
          .memo(request.getMemo())
          .isCompleted(request.getIsCompleted() != null ? request.getIsCompleted() : false)
          .build();
      log.info("Task Progress 생성: {} - {}", goal.getTitle(), progressDate);
    }

    TaskProgress saved = taskProgressRepository.save(progress);

    return TaskResponse.builder()
        .progress(toProgressDto(saved))
        .build();
  }

  @Transactional(readOnly = true)
  public TaskResponse getProgressByGoal(UUID taskGoalId) {
    List<TaskProgress> progressList;

    if (taskGoalId != null) {
      taskGoalRepository.findById(taskGoalId)
          .orElseThrow(() -> new CustomException(ErrorCode.TASK_GOAL_NOT_FOUND));

      progressList = taskProgressRepository
          .findByTaskGoalTaskGoalIdOrderByProgressDateDesc(taskGoalId);
    } else {
      progressList = taskProgressRepository.findAllByOrderByProgressDateDesc();
    }

    List<TaskProgressDto> progressDtos = progressList.stream()
        .map(this::toProgressDto)
        .collect(Collectors.toList());

    return TaskResponse.builder()
        .progressList(progressDtos)
        .build();
  }

  @Transactional
  public void deleteProgress(UUID taskProgressId) {
    TaskProgress progress = taskProgressRepository.findById(taskProgressId)
        .orElseThrow(() -> new CustomException(ErrorCode.TASK_PROGRESS_NOT_FOUND));

    taskProgressRepository.delete(progress);
    log.info("Task Progress 삭제: {}", progress.getProgressDate());
  }

  private TaskProgressDto toProgressDto(TaskProgress progress) {
    Integer amountDone = 0;
    if (progress.getStartAmount() != null && progress.getEndAmount() != null) {
      amountDone = progress.getEndAmount() - progress.getStartAmount();
    }

    return TaskProgressDto.builder()
        .taskProgressId(progress.getTaskProgressId())
        .taskGoalId(progress.getTaskGoal().getTaskGoalId())
        .taskGoalTitle(progress.getTaskGoal().getTitle())
        .progressDate(progress.getProgressDate())
        .content(progress.getContent())
        .startAmount(progress.getStartAmount())
        .endAmount(progress.getEndAmount())
        .memo(progress.getMemo())
        .isCompleted(progress.getIsCompleted())
        .createdDate(progress.getCreatedDate())
        .amountDone(amountDone)
        .build();
  }
}
