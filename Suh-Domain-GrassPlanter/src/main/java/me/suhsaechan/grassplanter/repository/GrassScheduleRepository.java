package me.suhsaechan.grassplanter.repository;

import me.suhsaechan.grassplanter.entity.GrassProfile;
import me.suhsaechan.grassplanter.entity.GrassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GrassScheduleRepository extends JpaRepository<GrassSchedule, UUID> {

    // 프로필별 스케줄 조회
    List<GrassSchedule> findByGrassProfile(GrassProfile grassProfile);
    
    // 프로필 ID로 스케줄 조회
    List<GrassSchedule> findByGrassProfileGrassProfileId(UUID grassProfileId);

    // 활성화된 스케줄만 조회
    List<GrassSchedule> findByIsActiveTrue();

    // 프로필의 활성화된 스케줄 조회
    List<GrassSchedule> findByGrassProfileAndIsActiveTrue(GrassProfile grassProfile);

    // 반복 유형별 스케줄 조회
    List<GrassSchedule> findByRecurrenceType(String recurrenceType);

    // 실행 예정인 스케줄 조회
    @Query("SELECT gs FROM GrassSchedule gs " +
           "WHERE gs.isActive = true " +
           "AND gs.nextExecutionTime <= :currentTime " +
           "AND (gs.endDate IS NULL OR gs.endDate > :currentTime)")
    List<GrassSchedule> findSchedulesToExecute(@Param("currentTime") LocalDateTime currentTime);

    // 특정 저장소를 대상으로 하는 스케줄 조회
    @Query("SELECT gs FROM GrassSchedule gs WHERE gs.repository.githubRepositoryId = :repositoryId")
    List<GrassSchedule> findByRepositoryId(@Param("repositoryId") UUID repositoryId);

    // 기간 내 활성 스케줄 조회
    @Query("SELECT gs FROM GrassSchedule gs " +
           "WHERE gs.isActive = true " +
           "AND gs.startDate <= :date " +
           "AND (gs.endDate IS NULL OR gs.endDate >= :date)")
    List<GrassSchedule> findActiveSchedulesAt(@Param("date") LocalDateTime date);

    // 만료된 스케줄 조회
    @Query("SELECT gs FROM GrassSchedule gs " +
           "WHERE gs.isActive = true " +
           "AND gs.endDate IS NOT NULL " +
           "AND gs.endDate < :currentTime")
    List<GrassSchedule> findExpiredSchedules(@Param("currentTime") LocalDateTime currentTime);

    // 스케줄 이름 존재 여부 확인
    boolean existsByGrassProfileAndScheduleName(GrassProfile grassProfile, String scheduleName);
}
