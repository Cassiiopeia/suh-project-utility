package me.suhsaechan.grassplanter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.grassplanter.dto.GrassCommitResult;
import me.suhsaechan.grassplanter.entity.GrassProfile;
import me.suhsaechan.github.entity.GithubRepository;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

/**
 * 잔디심기 GitHub 연동: 기여도·기여 수 조회, CHANGELOG 커밋, 응답 파싱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrassGithubClient {

    // CHANGELOG 없음 > 새 파일로 생성
    private static final int HTTP_NOT_FOUND = 404;

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

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

        String graphqlBody = objectMapper.writeValueAsString(Map.of(
                "query", query,
                "variables", Map.of("login", githubUsername, "from", from.toString(), "to", to.toString())));

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

    /**
     * CHANGELOG 커밋: 기본 저장소 확인 > 기존 파일 조회 > 내용 추가 > PUT (실패 시 사유 반환)
     */
    public GrassCommitResult commitChangelog(GrassProfile profile, String pat, String commitMessage) {
        Optional<String> repositoryFullName = Optional.ofNullable(profile.getDefaultRepository())
                .map(GithubRepository::getFullName);
        if (repositoryFullName.isEmpty()) {
            log.error("기본 저장소 정보가 없습니다. 프로필 ID: {}", profile.getGrassProfileId());
            return GrassCommitResult.failure("기본 저장소 없음");
        }

        String repoFullName = repositoryFullName.get();
        try {
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
                } else if (getResponse.code() != HTTP_NOT_FOUND) {
                    log.error("CHANGELOG 조회 실패 - 상태 코드: {}", getResponse.code());
                    return GrassCommitResult.failure("CHANGELOG 조회 실패 (상태 코드 " + getResponse.code() + ")");
                }
            }

            // 새로운 내용 생성
            String newContent = currentContent + "\n## [" + LocalDateTime.now() + "]\n- " + commitMessage + "\n";
            String encodedContent = Base64.getEncoder().encodeToString(newContent.getBytes(StandardCharsets.UTF_8));

            // 파일 업데이트 (branch 생략 > 저장소 기본 브랜치)
            Map<String, String> updateBody = new HashMap<>();
            updateBody.put("message", commitMessage);
            updateBody.put("content", encodedContent);
            if (sha != null) {
                updateBody.put("sha", sha);
            }
            String updateJson = objectMapper.writeValueAsString(updateBody);

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
                    return GrassCommitResult.success();
                }
                String errorBody = updateResponse.body() != null ? updateResponse.body().string() : "No error body";
                log.error("GitHub 커밋 실패 - 상태 코드: {}, 에러: {}", updateResponse.code(), errorBody);
                return GrassCommitResult.failure(
                        "GitHub 커밋 실패 (상태 코드 " + updateResponse.code() + "): " + errorBody);
            }

        } catch (Exception e) {
            log.error("GitHub 커밋 실행 중 오류: {}", e.getMessage(), e);
            return GrassCommitResult.failure("GitHub 커밋 중 오류: " + e.getMessage());
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
}
