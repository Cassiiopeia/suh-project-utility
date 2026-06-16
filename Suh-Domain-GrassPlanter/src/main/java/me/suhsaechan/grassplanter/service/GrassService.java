package me.suhsaechan.grassplanter.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.util.GrassEncryptionUtil;
import me.suhsaechan.grassplanter.dto.CommitLogDto;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrassService {

    private final GrassProfileRepository grassProfileRepository;
    private final GrassCommitLogRepository grassCommitLogRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final GrassEncryptionUtil encryptionUtil;
    private final OkHttpClient okHttpClient;
    private final GithubService githubService;
    
    @Transactional
    public GrassResponse createProfile(GrassRequest request) {
        log.info("프로필 생성 시작 - GitHub 사용자: {}", request.getGithubUsername());

        // 중복 체크 (먼저 username 중복 확인)
        if (grassProfileRepository.existsByGithubUsername(request.getGithubUsername())) {
            log.error("이미 존재하는 GitHub 사용자명: {}", request.getGithubUsername());
            throw new RuntimeException("이미 존재하는 GitHub 사용자명입니다: " + request.getGithubUsername());
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
            throw new RuntimeException("이미 존재하는 프로필입니다.");
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

    @Transactional
    public GrassResponse executeCommit(GrassRequest request) {
        log.info("수동 커밋 실행 시작 - 프로필 ID: {}", request.getProfileId());

        GrassProfile profile = grassProfileRepository.findByIdWithRepository(request.getProfileId())
                .orElseThrow(() -> new CustomException(ErrorCode.GRASS_PROFILE_NOT_FOUND));

        try {
            // PAT 복호화
            String pat = encryptionUtil.decrypt(profile.getEncryptedPat());
            
            // GitHub API를 통한 커밋 실행
            boolean success = performGitHubCommit(profile, pat, request.getCommitMessage());
            
            // 커밋 로그 저장
            GrassCommitLog commitLog = GrassCommitLog.builder()
                    .grassProfile(profile)
                    .repositoryName(profile.getDefaultRepository() != null ? profile.getDefaultRepository().getFullName() : null)
                    .commitTime(LocalDateTime.now())
                    .commitMessage(request.getCommitMessage() != null ? 
                        request.getCommitMessage() : "Manual commit from GrassPlanter")
                    .isAutoCommit(false)  // 수동 커밋이므로 false
                    .isSuccess(success)
                    .errorMessage(success ? null : "커밋 실행 실패")
                    .build();

            GrassCommitLog savedLog = grassCommitLogRepository.save(commitLog);
            
            if (success) {
                return GrassResponse.builder()
                        .commitLog(convertToCommitLogDto(savedLog))
                        .build();
            } else {
                throw new CustomException(ErrorCode.GRASS_COMMIT_FAILED);
            }
                    
        } catch (Exception e) {
            log.error("커밋 실행 중 오류 발생: {}", e.getMessage(), e);
            
            // 오류 로그 저장
            GrassCommitLog errorLog = GrassCommitLog.builder()
                    .grassProfile(profile)
                    .repositoryName(profile.getDefaultRepository() != null ? profile.getDefaultRepository().getFullName() : null)
                    .commitTime(LocalDateTime.now())
                    .commitMessage(request.getCommitMessage())
                    .isAutoCommit(false)  // 수동 커밋이므로 false
                    .isSuccess(false)
                    .errorMessage(e.getMessage())
                    .build();

            grassCommitLogRepository.save(errorLog);
            
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
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

    public int checkContributionLevel(String githubUsername, LocalDate date) throws IOException {
        log.info("GitHub 기여도 확인 - 사용자: {}, 날짜: {}", githubUsername, date);

        String url = "https://github.com/" + githubUsername;
        Request request = new Request.Builder().url(url).build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("GitHub API 응답 실패 - 상태 코드: {}", response.code());
                throw new IOException("Unexpected code " + response);
            }

            String html = response.body().string();
            Document doc = Jsoup.parse(html);
            Element cell = doc.selectFirst("td.ContributionCalendar-day[data-date='" + date.toString() + "']");

            if (cell != null) {
                String level = cell.attr("data-level");
                int contributionLevel = Integer.parseInt(level);
                log.info("기여도 레벨 확인 완료 - 레벨: {}", contributionLevel);
                return contributionLevel;
            }

            log.info("해당 날짜의 기여도 정보 없음");
            return 0;
        }
    }

    public int getTodayContributionCount(String githubUsername, LocalDate date, String pat) throws IOException {
        log.info("GitHub 기여 수 확인(GraphQL) - 사용자: {}, 날짜: {}", githubUsername, date);

        String from = date.atStartOfDay().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime().toString();
        String to = date.atTime(LocalTime.MAX).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime().toString();

        String query = "query($login:String!,$from:DateTime!,$to:DateTime!){"
                + "user(login:$login){contributionsCollection(from:$from,to:$to){"
                + "contributionCalendar{weeks{contributionDays{date contributionCount}}}}}}";

        String graphqlBody = String.format(
                "{\"query\":\"%s\",\"variables\":{\"login\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}}",
                query.replace("\"", "\\\""), githubUsername, from, to);

        RequestBody body = RequestBody.create(graphqlBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.github.com/graphql")
                .header("Authorization", "Bearer " + pat)
                .header("Accept", "application/vnd.github.v3+json")
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("GitHub GraphQL 응답 실패 - 상태 코드: {}", response.code());
                throw new IOException("Unexpected code " + response);
            }

            String json = response.body().string();
            int count = extractContributionCountForDate(json, date.toString());
            log.info("기여 수 확인 완료 - 날짜: {}, 기여 수: {}", date, count);
            return count;
        }
    }

    int extractContributionCountForDate(String graphqlJson, String date) {
        String dateMarker = "\"date\":\"" + date + "\"";
        int dateIndex = graphqlJson.indexOf(dateMarker);
        if (dateIndex == -1) {
            return 0;
        }

        String countKey = "\"contributionCount\":";
        int countIndex = graphqlJson.indexOf(countKey, dateIndex);
        if (countIndex == -1) {
            return 0;
        }

        int start = countIndex + countKey.length();
        int end = start;
        while (end < graphqlJson.length() && Character.isDigit(graphqlJson.charAt(end))) {
            end++;
        }
        if (start == end) {
            return 0;
        }
        return Integer.parseInt(graphqlJson.substring(start, end));
    }

    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul") // 매일 23시 1회 실행
    @Transactional
    public void executeAutoCommits() {
        log.info("자동 커밋 스케줄 실행 시작");

        List<GrassProfile> activeProfiles = grassProfileRepository.findByIsAutoCommitEnabledTrue();

        for (GrassProfile profile : activeProfiles) {
            try {
                String pat = encryptionUtil.decrypt(profile.getEncryptedPat());

                // 오늘 기여 수 확인 (GraphQL - private 기여 포함, 잔디와 동일)
                int todayContributions = getTodayContributionCount(profile.getGithubUsername(), LocalDate.now(), pat);

                if (todayContributions <= 0) {
                    log.info("자동 커밋 실행 - 사용자: {}, 오늘 기여 없음(기여 수: {})",
                        profile.getGithubUsername(), todayContributions);

                    boolean success = performGitHubCommit(profile, pat, "Auto-commit: Daily contribution");

                    // 로그 저장
                    GrassCommitLog commitLog = GrassCommitLog.builder()
                            .grassProfile(profile)
                            .repositoryName(profile.getDefaultRepository() != null ? profile.getDefaultRepository().getFullName() : null)
                            .commitTime(LocalDateTime.now())
                            .commitMessage("Auto-commit: Daily contribution")
                            .isAutoCommit(true)  // 자동 커밋이므로 true
                            .isSuccess(success)
                            .commitLevel(todayContributions)
                            .build();

                    grassCommitLogRepository.save(commitLog);
                } else {
                    log.info("자동 커밋 생략 - 사용자: {}, 오늘 이미 기여 있음(기여 수: {})",
                        profile.getGithubUsername(), todayContributions);
                }
            } catch (Exception e) {
                log.error("자동 커밋 실행 중 오류 - 사용자: {}, 오류: {}", 
                    profile.getGithubUsername(), e.getMessage(), e);
            }
        }
        
        log.info("자동 커밋 스케줄 실행 완료");
    }

    private boolean performGitHubCommit(GrassProfile profile, String pat, String commitMessage) {
        try {
            // 저장소 정보 확인
            if (profile.getDefaultRepository() == null || profile.getDefaultRepository().getFullName() == null) {
                log.error("기본 저장소 정보가 없습니다. 프로필 ID: {}", profile.getGrassProfileId());
                return false;
            }
            
            String repoFullName = profile.getDefaultRepository().getFullName();
            log.info("GitHub 커밋 시작 - 저장소: {}", repoFullName);
            
            String apiUrl = String.format("https://api.github.com/repos/%s/contents/CHANGELOG.md", repoFullName);
            
            // 현재 파일 내용 가져오기
            Request getRequest = new Request.Builder()
                    .url(apiUrl)
                    .header("Authorization", "token " + pat)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();
            
            String sha = null;
            String currentContent = "";
            
            try (Response getResponse = okHttpClient.newCall(getRequest).execute()) {
                if (getResponse.isSuccessful()) {
                    String responseBody = getResponse.body().string();
                    sha = extractSha(responseBody);
                    currentContent = extractContent(responseBody);
                }
            }
            
            // 새로운 내용 생성
            String newContent = currentContent + "\n## [" + LocalDateTime.now() + "]\n- " + commitMessage + "\n";
            String encodedContent = Base64.getEncoder().encodeToString(newContent.getBytes(StandardCharsets.UTF_8));
            
            // 파일 업데이트
            String updateJson = String.format(
                "{\"message\":\"%s\",\"content\":\"%s\"%s,\"branch\":\"main\"}",
                commitMessage,
                encodedContent,
                sha != null ? ",\"sha\":\"" + sha + "\"" : ""
            );
            
            RequestBody body = RequestBody.create(updateJson, MediaType.parse("application/json"));
            Request updateRequest = new Request.Builder()
                    .url(apiUrl)
                    .header("Authorization", "token " + pat)
                    .header("Accept", "application/vnd.github.v3+json")
                    .put(body)
                    .build();
            
            try (Response updateResponse = okHttpClient.newCall(updateRequest).execute()) {
                if (updateResponse.isSuccessful()) {
                    log.info("GitHub 커밋 성공 - 저장소: {}", repoFullName);
                    return true;
                } else {
                    String errorBody = updateResponse.body() != null ? updateResponse.body().string() : "No error body";
                    log.error("GitHub 커밋 실패 - 상태 코드: {}, 에러: {}", updateResponse.code(), errorBody);
                    return false;
                }
            }
            
        } catch (Exception e) {
            log.error("GitHub 커밋 실행 중 오류: {}", e.getMessage(), e);
            return false;
        }
    }

    private String extractSha(String jsonResponse) {
        // 간단한 JSON 파싱 (실제로는 Jackson 등을 사용하는 것이 좋음)
        int shaIndex = jsonResponse.indexOf("\"sha\":\"");
        if (shaIndex != -1) {
            int start = shaIndex + 7;
            int end = jsonResponse.indexOf("\"", start);
            return jsonResponse.substring(start, end);
        }
        return null;
    }

    private String extractContent(String jsonResponse) {
        int contentIndex = jsonResponse.indexOf("\"content\":\"");
        if (contentIndex != -1) {
            int start = contentIndex + 11;
            int end = jsonResponse.indexOf("\"", start);
            String encodedContent = jsonResponse.substring(start, end).replace("\\n", "");
            return new String(Base64.getDecoder().decode(encodedContent), StandardCharsets.UTF_8);
        }
        return "";
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
