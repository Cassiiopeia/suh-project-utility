package me.suhsaechan.grassplanter.repository;

import me.suhsaechan.grassplanter.entity.GrassProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GrassProfileRepository extends JpaRepository<GrassProfile, UUID> {

    // GitHub 사용자명으로 프로필 조회
    Optional<GrassProfile> findByGithubUsername(String githubUsername);

    // 소유자 ID로 프로필 목록 조회
    List<GrassProfile> findByOwnerId(UUID ownerId);

    // 활성화된 프로필 조회
    List<GrassProfile> findByIsActiveTrue();

    // 자동 커밋이 활성화된 프로필 조회
    List<GrassProfile> findByIsAutoCommitEnabledTrue();

    // 활성화되고 자동 커밋이 활성화된 프로필 조회
    List<GrassProfile> findByIsActiveTrueAndIsAutoCommitEnabledTrue();

    // 특정 저장소를 기본으로 사용하는 프로필 조회
    @Query("SELECT gp FROM GrassProfile gp WHERE gp.defaultRepository.githubRepositoryId = :repositoryId")
    List<GrassProfile> findByDefaultRepositoryId(@Param("repositoryId") UUID repositoryId);

    // GitHub 사용자명 존재 여부 확인
    boolean existsByGithubUsername(String githubUsername);

    // 소유자 ID 존재 여부 확인
    boolean existsByOwnerId(UUID ownerId);
    
    // GitHub 사용자명과 기본 저장소 ID로 중복 체크
    @Query("SELECT COUNT(gp) > 0 FROM GrassProfile gp WHERE gp.githubUsername = :username AND gp.defaultRepository.githubRepositoryId = :repositoryId")
    boolean existsByGithubUsernameAndDefaultRepositoryId(@Param("username") String githubUsername, @Param("repositoryId") UUID repositoryId);
    
    // 프로필을 저장소 정보와 함께 조회
    @Query("SELECT gp FROM GrassProfile gp LEFT JOIN FETCH gp.defaultRepository WHERE gp.grassProfileId = :profileId")
    Optional<GrassProfile> findByIdWithRepository(@Param("profileId") UUID profileId);
}
