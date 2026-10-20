package me.suhsaechan.somansabus.repository;

import java.time.LocalDateTime;
import java.util.UUID;
import me.suhsaechan.somansabus.entity.SomansaBusSchedulerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SomansaBusSchedulerStateRepository
    extends JpaRepository<SomansaBusSchedulerState, UUID> {

  /**
   * 발화권 선점: nextFireAt 이 읽은 값 그대로일 때만 다음 발화 시각으로 갱신 (1 선점 성공, 0 다른 인스턴스가 먼저 선점)
   */
  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE SomansaBusSchedulerState s SET s.nextFireAt = :claimedNextFireAt "
      + "WHERE s.somansaBusSchedulerStateId = :stateId AND s.nextFireAt = :expectedNextFireAt")
  int claimFire(@Param("stateId") UUID stateId,
      @Param("expectedNextFireAt") LocalDateTime expectedNextFireAt,
      @Param("claimedNextFireAt") LocalDateTime claimedNextFireAt);
}
