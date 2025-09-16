package me.suhsaechan.grassplanter.repository;

import me.suhsaechan.grassplanter.entity.GrassCommitLog;
import me.suhsaechan.grassplanter.entity.GrassProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GrassCommitLogRepository extends JpaRepository<GrassCommitLog, UUID> {

    // 프로필별 커밋 로그 조회
    List<GrassCommitLog> findByGrassProfile(GrassProfile grassProfile);
    
    // 프로필 ID로 커밋 로그 조회
    List<GrassCommitLog> findByGrassProfileGrassProfileId(UUID grassProfileId);

    // 기간별 커밋 로그 조회
    List<GrassCommitLog> findByCommitTimeBetween(LocalDateTime start, LocalDateTime end);

    // 프로필과 기간별 커밋 로그 조회
    List<GrassCommitLog> findByGrassProfileAndCommitTimeBetween(
        GrassProfile grassProfile, 
        LocalDateTime start, 
        LocalDateTime end
    );

    // 성공한 커밋만 조회
    List<GrassCommitLog> findByGrassProfileAndIsSuccessTrue(GrassProfile grassProfile);

    // 실패한 커밋만 조회
    List<GrassCommitLog> findByGrassProfileAndIsSuccessFalse(GrassProfile grassProfile);

    // 자동 커밋만 조회
    List<GrassCommitLog> findByGrassProfileAndIsAutoCommitTrue(GrassProfile grassProfile);

    // 페이징 처리된 커밋 로그 조회
    Page<GrassCommitLog> findByGrassProfile(GrassProfile grassProfile, Pageable pageable);

    // 특정 저장소의 커밋 로그 조회
    @Query("SELECT gcl FROM GrassCommitLog gcl WHERE gcl.repository.githubRepositoryId = :repositoryId")
    List<GrassCommitLog> findByRepositoryId(@Param("repositoryId") UUID repositoryId);

    // 일일 커밋 수 통계
    @Query("SELECT COUNT(gcl) FROM GrassCommitLog gcl " +
           "WHERE gcl.grassProfile = :profile " +
           "AND gcl.commitTime >= :startOfDay " +
           "AND gcl.commitTime < :endOfDay " +
           "AND gcl.isSuccess = true")
    Long countDailyCommits(
        @Param("profile") GrassProfile profile,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    // 최근 커밋 조회
    List<GrassCommitLog> findTop10ByGrassProfileOrderByCommitTimeDesc(GrassProfile grassProfile);
}
