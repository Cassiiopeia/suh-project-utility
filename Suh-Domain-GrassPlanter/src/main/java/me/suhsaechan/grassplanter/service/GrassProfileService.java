package me.suhsaechan.grassplanter.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.util.GrassEncryptionUtil;
import me.suhsaechan.grassplanter.dto.GrassRequest;
import me.suhsaechan.grassplanter.dto.GrassResponse;
import me.suhsaechan.grassplanter.dto.ProfileDto;
import me.suhsaechan.grassplanter.entity.GrassCommitLog;
import me.suhsaechan.grassplanter.entity.GrassProfile;
import me.suhsaechan.grassplanter.repository.GrassCommitLogRepository;
import me.suhsaechan.grassplanter.repository.GrassProfileRepository;
import me.suhsaechan.github.entity.GithubRepository;
import me.suhsaechan.github.repository.GithubRepositoryRepository;
import me.suhsaechan.github.service.GithubService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 잔디심기 프로필 관리: 생성, 수정, 삭제, 목록
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrassProfileService {

    private final GrassProfileRepository grassProfileRepository;
    private final GrassCommitLogRepository grassCommitLogRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final GrassEncryptionUtil encryptionUtil;
    private final GithubService githubService;

    @Transactional
    public GrassResponse createProfile(GrassRequest request) {
        log.info("프로필 생성 시작 - GitHub 사용자: {}", request.getGithubUsername());

        // 중복 체크 (먼저 username 중복 확인)
        if (grassProfileRepository.existsByGithubUsername(request.getGithubUsername())) {
            log.error("이미 존재하는 GitHub 사용자명: {}", request.getGithubUsername());
            throw new CustomException(ErrorCode.GRASS_PROFILE_DUPLICATE_USERNAME);
        }

        // GitHub Repository 조회 또는 웹 스크래핑으로 자동 생성
        GithubRepository githubRepository = null;
        if (request.getRepositoryFullName() != null && !request.getRepositoryFullName().isEmpty()) {
            // Repository가 DB에 있는지 확인하고, 없으면 스크래핑으로 가져오기
            githubRepository = githubRepositoryRepository.findByFullName(request.getRepositoryFullName())
                    .orElseGet(() -> {
                        log.info("GitHub 저장소 정보를 스크래핑합니다: {}", request.getRepositoryFullName());
                        return githubService.fetchAndSaveGithubRepository(request.getRepositoryFullName());
                    });
        }

        // 중복 체크
        if (githubRepository != null && grassProfileRepository.existsByGithubUsernameAndDefaultRepositoryId(
                request.getGithubUsername(), githubRepository.getGithubRepositoryId())) {
            log.error("이미 존재하는 프로필 - 사용자: {}, 저장소: {}",
                request.getGithubUsername(), githubRepository.getFullName());
            throw new CustomException(ErrorCode.GRASS_PROFILE_DUPLICATE);
        }

        // PAT 암호화
        String encryptedPat = encryptionUtil.encrypt(request.getPersonalAccessToken());

        GrassProfile profile = GrassProfile.builder()
                .githubUsername(request.getGithubUsername())
                .encryptedPat(encryptedPat)
                .defaultRepository(githubRepository)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isAutoCommitEnabled(request.getIsAutoCommitEnabled() != null ? request.getIsAutoCommitEnabled() : false)
                .targetCommitLevel(request.getTargetCommitLevel() != null ? request.getTargetCommitLevel() : 1)
                .commitMessageTemplate(request.getCommitMessageTemplate())
                .dailyCommitGoal(request.getDailyCommitGoal())
                .ownerId(request.getOwnerId())
                .ownerNickname(request.getOwnerNickname())
                .build();

        GrassProfile saved = grassProfileRepository.save(profile);
        log.info("프로필 생성 완료 - ID: {}", saved.getGrassProfileId());

        return GrassResponse.builder()
                .profile(convertToProfileDto(saved))
                .build();
    }

    @Transactional
    public GrassResponse updateProfile(GrassRequest request) {
        log.info("프로필 업데이트 시작 - ID: {}", request.getProfileId());

        GrassProfile profile = grassProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new CustomException(ErrorCode.GRASS_PROFILE_NOT_FOUND));

        // PAT 업데이트 (새로운 PAT가 제공된 경우만)
        if (request.getPersonalAccessToken() != null && !request.getPersonalAccessToken().isEmpty()) {
            profile.setEncryptedPat(encryptionUtil.encrypt(request.getPersonalAccessToken()));
        }

        if (request.getIsActive() != null) {
            profile.setIsActive(request.getIsActive());
        }
        if (request.getIsAutoCommitEnabled() != null) {
            profile.setIsAutoCommitEnabled(request.getIsAutoCommitEnabled());
        }
        if (request.getTargetCommitLevel() != null) {
            profile.setTargetCommitLevel(request.getTargetCommitLevel());
        }
        if (request.getOwnerNickname() != null) {
            profile.setOwnerNickname(request.getOwnerNickname());
        }
        if (request.getCommitMessageTemplate() != null) {
            profile.setCommitMessageTemplate(request.getCommitMessageTemplate());
        }
        if (request.getDailyCommitGoal() != null) {
            profile.setDailyCommitGoal(request.getDailyCommitGoal());
        }

        // 저장소 변경 (DB에 없으면 스크래핑으로 자동 등록)
        if (request.getRepositoryFullName() != null && !request.getRepositoryFullName().isEmpty()) {
            GithubRepository githubRepository = githubRepositoryRepository
                    .findByFullName(request.getRepositoryFullName())
                    .orElseGet(() -> githubService.fetchAndSaveGithubRepository(request.getRepositoryFullName()));
            profile.setDefaultRepository(githubRepository);
        }

        GrassProfile updated = grassProfileRepository.save(profile);
        log.info("프로필 업데이트 완료 - ID: {}", updated.getGrassProfileId());

        return GrassResponse.builder()
                .profile(convertToProfileDto(updated))
                .build();
    }

    @Transactional
    public GrassResponse deleteProfile(UUID profileId) {
        log.info("프로필 삭제 시작 - ID: {}", profileId);

        GrassProfile profile = grassProfileRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ErrorCode.GRASS_PROFILE_NOT_FOUND));

        // 관련된 커밋 로그 먼저 삭제
        List<GrassCommitLog> commitLogs = grassCommitLogRepository.findByGrassProfile(profile);
        if (!commitLogs.isEmpty()) {
            log.info("관련 커밋 로그 {} 개 삭제", commitLogs.size());
            grassCommitLogRepository.deleteAll(commitLogs);
        }

        // 프로필 삭제
        grassProfileRepository.delete(profile);
        log.info("프로필 삭제 완료 - ID: {}", profileId);

        return GrassResponse.builder()
                .build();
    }

    @Transactional(readOnly = true)
    public GrassResponse getProfiles(GrassRequest request) {
        log.info("프로필 목록 조회");

        List<GrassProfile> profiles;
        if (request.getOwnerId() != null) {
            profiles = grassProfileRepository.findByOwnerId(request.getOwnerId());
        } else {
            profiles = grassProfileRepository.findAll();
        }

        List<ProfileDto> profileDtos = profiles.stream()
                .map(this::convertToProfileDto)
                .collect(Collectors.toList());

        return GrassResponse.builder()
                .profiles(profileDtos)
                .totalElements((long) profileDtos.size())
                .build();
    }

    private ProfileDto convertToProfileDto(GrassProfile profile) {
        return ProfileDto.builder()
                .grassProfileId(profile.getGrassProfileId())
                .githubUsername(profile.getGithubUsername())
                .defaultRepositoryId(profile.getDefaultRepository() != null ? profile.getDefaultRepository().getGithubRepositoryId() : null)
                .defaultRepositoryName(profile.getDefaultRepository() != null ? profile.getDefaultRepository().getFullName() : null)
                .isActive(profile.getIsActive())
                .isAutoCommitEnabled(profile.getIsAutoCommitEnabled())
                .targetCommitLevel(profile.getTargetCommitLevel())
                .commitMessageTemplate(profile.getCommitMessageTemplate())
                .dailyCommitGoal(profile.getDailyCommitGoal())
                .streakDays(profile.getStreakDays())
                .ownerId(profile.getOwnerId())
                .ownerNickname(profile.getOwnerNickname())
                .createdDate(profile.getCreatedDate())
                .updatedDate(profile.getUpdatedDate())
                .build();
    }
}
