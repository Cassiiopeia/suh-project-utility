package me.suhsaechan.task.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.task.entity.TaskProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskProgressRepository extends JpaRepository<TaskProgress, UUID> {

  List<TaskProgress> findByTaskGoalTaskGoalIdOrderByProgressDateDesc(UUID taskGoalId);

  List<TaskProgress> findAllByOrderByProgressDateDesc();

  void deleteByTaskGoalTaskGoalId(UUID taskGoalId);

  Optional<TaskProgress> findByTaskGoalTaskGoalIdAndProgressDate(UUID taskGoalId, LocalDate progressDate);

  Optional<TaskProgress> findTopByTaskGoalTaskGoalIdOrderByProgressDateDesc(UUID taskGoalId);

  List<TaskProgress> findByTaskGoalTaskGoalIdAndProgressDateBetweenOrderByProgressDateDesc(
      UUID taskGoalId, LocalDate startDate, LocalDate endDate);

  @Query("SELECT COALESCE(MAX(p.endAmount), 0) FROM TaskProgress p WHERE p.taskGoal.taskGoalId = :taskGoalId")
  Integer findMaxEndAmountByTaskGoalId(@Param("taskGoalId") UUID taskGoalId);
}
