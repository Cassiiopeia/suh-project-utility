package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.task.dto.TaskRequest;
import me.suhsaechan.task.dto.TaskResponse;
import me.suhsaechan.task.service.TaskGoalService;
import me.suhsaechan.task.service.TaskProgressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/task")
public class TaskController {

  private final TaskGoalService taskGoalService;
  private final TaskProgressService taskProgressService;

  // === Goal 관련 ===

  @PostMapping(value = "/goal/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<TaskResponse> createGoal(@ModelAttribute TaskRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(taskGoalService.createGoal(request));
  }

  @PostMapping(value = "/goal/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<TaskResponse> getActiveGoals() {
    return ResponseEntity.ok(taskGoalService.getActiveGoals());
  }

  @PostMapping(value = "/goal/detail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<TaskResponse> getGoalDetail(@ModelAttribute TaskRequest request) {
    return ResponseEntity.ok(taskGoalService.getGoalDetail(request.getTaskGoalId()));
  }

  @PostMapping(value = "/goal/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<TaskResponse> updateGoal(@ModelAttribute TaskRequest request) {
    return ResponseEntity.ok(taskGoalService.updateGoal(request));
  }

  @PostMapping(value = "/goal/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> deleteGoal(@ModelAttribute TaskRequest request) {
    taskGoalService.deleteGoal(request.getTaskGoalId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/goal/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<TaskResponse> completeGoal(@ModelAttribute TaskRequest request) {
    return ResponseEntity.ok(taskGoalService.completeGoal(request.getTaskGoalId()));
  }

  @PostMapping(value = "/goal/cancel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<TaskResponse> cancelGoal(@ModelAttribute TaskRequest request) {
    return ResponseEntity.ok(taskGoalService.cancelGoal(request.getTaskGoalId()));
  }

  // === Progress 관련 ===

  @PostMapping(value = "/progress/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<TaskResponse> saveProgress(@ModelAttribute TaskRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(taskProgressService.saveProgress(request));
  }

  @PostMapping(value = "/progress/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<TaskResponse> getProgressList(@ModelAttribute TaskRequest request) {
    return ResponseEntity.ok(taskProgressService.getProgressByGoal(request.getTaskGoalId()));
  }

  @PostMapping(value = "/progress/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> deleteProgress(@ModelAttribute TaskRequest request) {
    taskProgressService.deleteProgress(request.getTaskProgressId());
    return ResponseEntity.noContent().build();
  }
}
