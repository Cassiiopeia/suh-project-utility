package me.suhsaechan.task.repository;

import java.util.List;
import java.util.UUID;
import me.suhsaechan.task.entity.TaskGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskGoalRepository extends JpaRepository<TaskGoal, UUID> {

  List<TaskGoal> findByIsActiveTrueOrderByTargetDateAsc();

  List<TaskGoal> findByIsActiveTrueAndIsCompletedFalseOrderByTargetDateAsc();

  List<TaskGoal> findByIsActiveTrueOrderByPriorityAscTargetDateAsc();

  List<TaskGoal> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title);
}
