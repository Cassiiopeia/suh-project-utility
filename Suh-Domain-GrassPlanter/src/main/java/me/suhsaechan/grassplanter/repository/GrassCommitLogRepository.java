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

    // 페이징 처리된 커밋 로그 조회
    Page<GrassCommitLog> findByGrassProfile(GrassProfile grassProfile, Pageable pageable);

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
}
