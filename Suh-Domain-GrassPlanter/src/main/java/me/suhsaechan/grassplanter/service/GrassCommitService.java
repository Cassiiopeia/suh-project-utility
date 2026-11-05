package me.suhsaechan.grassplanter.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.util.GrassEncryptionUtil;
import me.suhsaechan.grassplanter.dto.CommitLogDto;
import me.suhsaechan.grassplanter.dto.GrassCommitResult;
import me.suhsaechan.grassplanter.dto.GrassRequest;
import me.suhsaechan.grassplanter.dto.GrassResponse;
import me.suhsaechan.grassplanter.entity.GrassCommitLog;
import me.suhsaechan.grassplanter.entity.GrassProfile;
import me.suhsaechan.grassplanter.repository.GrassCommitLogRepository;
import me.suhsaechan.grassplanter.repository.GrassProfileRepository;
import me.suhsaechan.github.entity.GithubRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 잔디심기 커밋 실행·이력: 수동 커밋, 자동 커밋 스케줄, 커밋 로그 조회
 * 커밋 1건마다 로그 1건 save (성공·실패 공통, 트랜잭션 없이 건별 저장)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrassCommitService {

    // 프로필 템플릿이 비었을 때 자동 커밋 메시지
    static final String DEFAULT_AUTO_COMMIT_MESSAGE = "Auto-commit: Daily contribution";
    // 실패 사유 저장 상한 (GitHub 에러 본문 전체 적재 방지)
    static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final GrassProfileRepository grassProfileRepository;
    private final GrassCommitLogRepository grassCommitLogRepository;
    private final GrassEncryptionUtil encryptionUtil;
    private final GrassGithubClient grassGithubClient;

    /**
     * 수동 커밋: 프로필 조회 > PAT 복호화 > GitHub 커밋 > 로그 1회 저장 > 실패 시 GRASS_COMMIT_FAILED
     * 트랜잭션 없음: 실패 로그가 롤백되지 않고 남도록 save 단위 커밋
     */
    public GrassResponse executeCommit(GrassRequest request) {
        log.info("수동 커밋 실행 시작 - 프로필 ID: {}", request.getProfileId());

        GrassProfile profile = grassProfileRepository.findByIdWithRepository(request.getProfileId())
                .orElseThrow(() -> new CustomException(ErrorCode.GRASS_PROFILE_NOT_FOUND));

        GrassCommitResult commitResult;
        try {
            String pat = encryptionUtil.decrypt(profile.getEncryptedPat());
            commitResult = grassGithubClient.commitChangelog(profile, pat, request.getCommitMessage());
        } catch (Exception e) {
            log.error("커밋 실행 중 오류 발생: {}", e.getMessage(), e);
            commitResult = GrassCommitResult.failure(e.getMessage());
        }

        String loggedCommitMessage = Optional.ofNullable(request.getCommitMessage())
                .orElse("Manual commit from GrassPlanter");
        GrassCommitLog savedLog = grassCommitLogRepository.save(
                buildCommitLog(profile, loggedCommitMessage, commitResult, false, null));

        if (!Boolean.TRUE.equals(commitResult.getIsSuccess())) {
            throw new CustomException(ErrorCode.GRASS_COMMIT_FAILED);
        }
        return GrassResponse.builder()
                .commitLog(convertToCommitLogDto(savedLog))
                .build();
    }

    @Transactional(readOnly = true)
    public GrassResponse getCommitLogs(GrassRequest request) {
        log.info("커밋 로그 조회 - 프로필 ID: {}", request.getProfileId());

        if (request.getProfileId() == null) {
            // 전체 로그 조회 (페이징)
            Pageable pageable = PageRequest.of(
                    request.getPage() != null ? request.getPage() : 0,
                    request.getSize() != null ? request.getSize() : 20,
                    Sort.by("commitTime").descending()
            );

            Page<GrassCommitLog> logsPage = grassCommitLogRepository.findAll(pageable);
            List<CommitLogDto> logDtos = logsPage.getContent().stream()
                    .map(this::convertToCommitLogDto)
                    .collect(Collectors.toList());

            return GrassResponse.builder()
                    .commitLogs(logDtos)
                    .currentPage(logsPage.getNumber())
                    .totalPages(logsPage.getTotalPages())
                    .totalElements(logsPage.getTotalElements())
                    .build();
        } else {
            // 특정 프로필의 로그 조회
            GrassProfile profile = grassProfileRepository.findById(request.getProfileId())
                    .orElseThrow(() -> new CustomException(ErrorCode.GRASS_PROFILE_NOT_FOUND));

            List<GrassCommitLog> logs = grassCommitLogRepository.findByGrassProfile(profile);
            List<CommitLogDto> logDtos = logs.stream()
                    .map(this::convertToCommitLogDto)
                    .collect(Collectors.toList());

            return GrassResponse.builder()
                    .commitLogs(logDtos)
                    .totalElements((long) logDtos.size())
                    .build();
        }
    }

    /**
     * 자동 커밋: 프로필별 오늘 기록·기여 확인 > 기여 없으면 커밋 > 로그 저장 (프로필 단위 독립)
     */
    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul") // 매일 23시 1회 실행
    public void executeAutoCommits() {
        log.info("자동 커밋 스케줄 실행 시작");

        List<GrassProfile> activeProfiles = grassProfileRepository.findAutoCommitEnabledWithRepository();
        for (GrassProfile profile : activeProfiles) {
            executeAutoCommit(profile);
        }

        log.info("자동 커밋 스케줄 실행 완료");
    }

    /**
     * 프로필 1건 자동 커밋 (커밋을 시도했거나 예외가 나면 로그 1건 저장, 생략 시 저장 없음)
     */
    private void executeAutoCommit(GrassProfile profile) {
        String commitMessage = resolveAutoCommitMessage(profile);
        GrassCommitResult commitResult;
        Integer todayContributions = null;
        try {
            String pat = encryptionUtil.decrypt(profile.getEncryptedPat());

            // 오늘 이미 성공한 DB 커밋 기록이 있는지 확인
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
            Long todaySuccessCount = grassCommitLogRepository.countDailyCommits(profile, startOfDay, endOfDay);
            if (todaySuccessCount > 0) {
                log.info("자동 커밋 생략 - 사용자: {}, 오늘 이미 DB 커밋 기록 있음({}건)",
                    profile.getGithubUsername(), todaySuccessCount);
                return;
            }

            // 오늘 기여 수 확인 (GraphQL - private 기여 포함, 잔디와 동일)
            todayContributions = grassGithubClient.getTodayContributionCount(
                    profile.getGithubUsername(), LocalDate.now(), pat);
            if (todayContributions > 0) {
                log.info("자동 커밋 생략 - 사용자: {}, 오늘 이미 기여 있음(기여 수: {})",
                    profile.getGithubUsername(), todayContributions);
                return;
            }

            log.info("자동 커밋 실행 - 사용자: {}, 오늘 기여 없음(기여 수: {})",
                profile.getGithubUsername(), todayContributions);
            commitResult = grassGithubClient.commitChangelog(profile, pat, commitMessage);
        } catch (Exception e) {
            log.error("자동 커밋 실행 중 오류 - 사용자: {}, 오류: {}",
                profile.getGithubUsername(), e.getMessage(), e);
            commitResult = GrassCommitResult.failure("자동 커밋 중 오류: " + e.getMessage());
        }

        // 저장 실패가 다음 프로필 처리를 막지 않도록 분리
        try {
            grassCommitLogRepository.save(
                    buildCommitLog(profile, commitMessage, commitResult, true, todayContributions));
        } catch (Exception e) {
            log.error("자동 커밋 로그 저장 실패 - 사용자: {}, 오류: {}",
                profile.getGithubUsername(), e.getMessage(), e);
        }
    }

    /**
     * 자동 커밋 메시지: 프로필 템플릿 (비어 있으면 기본 문구)
     */
    String resolveAutoCommitMessage(GrassProfile profile) {
        return Optional.ofNullable(profile.getCommitMessageTemplate())
                .filter(template -> !template.isBlank())
                .orElse(DEFAULT_AUTO_COMMIT_MESSAGE);
    }

    private GrassCommitLog buildCommitLog(GrassProfile profile, String commitMessage,
            GrassCommitResult commitResult, boolean isAutoCommit, Integer commitLevel) {
        return GrassCommitLog.builder()
                .grassProfile(profile)
                .repositoryName(Optional.ofNullable(profile.getDefaultRepository())
                        .map(GithubRepository::getFullName)
                        .orElse(null))
                .commitTime(LocalDateTime.now())
                .commitMessage(commitMessage)
                .isAutoCommit(isAutoCommit)
                .isSuccess(Boolean.TRUE.equals(commitResult.getIsSuccess()))
                .errorMessage(truncateErrorMessage(commitResult.getFailureReason()))
                .commitLevel(commitLevel)
                .build();
    }

    private String truncateErrorMessage(String failureReason) {
        return Optional.ofNullable(failureReason)
                .map(reason -> reason.length() > MAX_ERROR_MESSAGE_LENGTH
                        ? reason.substring(0, MAX_ERROR_MESSAGE_LENGTH)
                        : reason)
                .orElse(null);
    }

    private CommitLogDto convertToCommitLogDto(GrassCommitLog log) {
        return CommitLogDto.builder()
                .grassCommitLogId(log.getGrassCommitLogId())
                .grassProfileId(log.getGrassProfile() != null ? log.getGrassProfile().getGrassProfileId() : null)
                .githubUsername(log.getGrassProfile() != null ? log.getGrassProfile().getGithubUsername() : null)
                .repositoryName(log.getRepositoryName())
                .commitTime(log.getCommitTime())
                .commitMessage(log.getCommitMessage())
                .commitSha(log.getCommitSha())
                .isSuccess(log.getIsSuccess())
                .isAutoCommit(log.getIsAutoCommit())
                .errorMessage(log.getErrorMessage())
                .commitLevel(log.getCommitLevel())
                .createdDate(log.getCreatedDate())
                .build();
    }
}
