package me.suhsaechan.github.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueHelperService {

  private final OkHttpClient okHttpClient;

  private final GithubService githubService;

  // 기본 commit type (필요시 확장 가능)
  private static final String DEFAULT_COMMIT_TYPE = "feat";

  /**
   * GitHub 이슈 URL로부터 페이지를 읽어 <title> 태그를 파싱한 후,
   * 커스텀 규칙에 따라 브랜치명과 커밋 메시지를 생성합니다.
   */
  public me.suhsaechan.github.dto.response.IssueHelperResponse createIssueCommmitBranch(
      me.suhsaechan.github.dto.request.IssueHelperRequest request) {
    log.debug("이슈 도우미 요청 시작: {}", request);

    if (request == null || request.getIssueUrl() == null || request.getIssueUrl().trim().isEmpty()) {
      log.error("잘못된 요청: request={}", request);
      throw new IllegalArgumentException("Issue URL이 비어있습니다.");
    }

    String issueUrl = request.getIssueUrl().trim();
    log.debug("처리할 이슈 URL: {}", issueUrl);

    String repositoryFullName = extractRepositoryFullName(issueUrl);
    log.debug("추출된 저장소 이름: {}", repositoryFullName);

    try {
      String htmlContent = fetchPageContent(issueUrl);
      log.debug("페이지 내용 크기: {} bytes", htmlContent.length());

      String fullTitle = parseTitleFromHtml(htmlContent);
      log.debug("파싱된 전체 제목: {}", fullTitle);

      String rawTitle = fullTitle.split("·\\s*Issue")[0].trim();
      log.debug("정제된 제목: {}", rawTitle);

      String issueNumber = extractIssueNumber(issueUrl);
      log.debug("추출된 이슈 번호: {}", issueNumber);

      String branchName = generateBranchName(rawTitle, issueNumber);
      log.debug("생성된 브랜치명: {}", branchName);

      String commitMessage = generateCommitMessage(rawTitle, issueUrl);
      log.debug("생성된 커밋 메시지: {}", commitMessage);

      // DB 저장 추가
      githubService.processIssueHelper(request, branchName, commitMessage, repositoryFullName);

      me.suhsaechan.github.dto.response.IssueHelperResponse response = me.suhsaechan.github.dto.response.IssueHelperResponse.builder()
          .branchName(branchName)
          .commitMessage(commitMessage)
          .build();

      log.info("이슈 도우미 처리 완료: {}", response);
      return response;

    } catch (Exception e) {
      log.error("이슈 도우미 처리 중 오류 발생: {}", e.getMessage(), e);
      throw new RuntimeException("이슈 도우미 처리 실패", e);
    }
  }
  private String generateBranchName(String rawTitle, String issueNumber) {
    String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
    String branchTitle = processTitleForBranch(rawTitle);
    return String.format("%s_#%s_%s", currentDate, issueNumber, branchTitle);
  }

  private String generateCommitMessage(String rawTitle, String issueUrl) {
    String commitTitle = processTitleForCommit(rawTitle);
    return String.format("%s : %s : %s %s",
        commitTitle, DEFAULT_COMMIT_TYPE, "변경 사항에 대한 설명", issueUrl);
  }

  // URL에서 owner/repository 추출
  private String extractRepositoryFullName(String url) {
    Pattern pattern = Pattern.compile("github\\.com/([^/]+/[^/]+)/issues/\\d+");
    Matcher matcher = pattern.matcher(url);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  /**
   * OkHttp를 이용해 지정된 URL의 HTML을 가져옵니다.
   */
  private String fetchPageContent(String url) {
    Request request = new Request.Builder()
        .url(url)
        .get()
        .build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Unexpected code " + response);
      }
      return response.body().string();
    } catch (IOException e) {
      log.error("페이지 로드 실패: {}", e.getMessage());
      throw new RuntimeException("페이지를 불러오는데 실패했습니다.", e);
    }
  }

  /**
   * jsoup을 이용하여 HTML의 <title> 태그 내용을 파싱합니다.
   */
  private String parseTitleFromHtml(String html) {
    Document doc = Jsoup.parse(html);
    return doc.title();
  }

  /**
   * URL에서 이슈 번호 추출 (예: https://github.com/owner/repo/issues/2)
   */
  private String extractIssueNumber(String url) {
    Pattern pattern = Pattern.compile("issues/(\\d+)");
    Matcher matcher = pattern.matcher(url);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  /**
   * 브랜치명용 제목 가공
   * - 인접한 대괄호 사이에 공백이 없으면 먼저 공백을 삽입한 후,
   * 대괄호는 제거하고 내부 텍스트는 유지합니다.
   * - 특수문자는 제거하고, 남은 단어들은 공백을 언더스코어(_)로 변환합니다.
   * - 허용 문자: 알파벳, 숫자, 한글, 공백 (최종 변환 시 공백은 언더스코어로 대체)
   */
  private String processTitleForBranch(String title) {
    if (title == null || title.isEmpty()) {
      return "";
    }

    // 인접한 대괄호 사이에 공백 삽입 (예: "[기능추가][깃헙이슈도우미]" → "[기능추가] [깃헙이슈도우미]")
    title = title.replaceAll("\\]\\s*\\[", "] [");

    // 모든 대괄호를 제거하고 내부 텍스트만 남김 (예: "[기능추가]" → "기능추가")
    String withoutBrackets = title.replaceAll("\\[([^\\]]+)\\]", "$1");

    // 특수문자 제거 (알파벳, 숫자, 한글, 공백 외의 모든 문자 제거)
    withoutBrackets = withoutBrackets.replaceAll("[^\\p{L}\\p{Nd}\\s]", "");

    // 앞뒤 공백 제거 후, 공백을 언더스코어(_)로 변환
    return withoutBrackets.trim().replaceAll("\\s+", "_");
  }

  /**
   * 커밋 메시지용 제목 가공
   * - 모든 대괄호 그룹을 제거
   * - 나머지 특수문자(알파벳, 숫자, 한글, 공백 제외) 제거하고, 여러 공백은 하나의 공백으로 정리
   */
  private String processTitleForCommit(String title) {
    // 모든 대괄호 그룹 제거
    String removedBracketGroups = title.replaceAll("\\[[^\\]]*\\]", "").trim();
    // 특수문자(알파벳, 숫자, 한글, 공백 제외) 제거
    String cleaned = removedBracketGroups.replaceAll("[^\\p{L}\\p{Nd}\\s]", "").trim();
    // 중복 공백을 하나로
    return cleaned.replaceAll("\\s+", " ");
  }
}
