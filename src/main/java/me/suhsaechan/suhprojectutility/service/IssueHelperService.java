package me.suhsaechan.suhprojectutility.service;

import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.object.IssueHelperRequest;
import me.suhsaechan.suhprojectutility.object.IssueHelperResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IssueHelperService {

  private final OkHttpClient okHttpClient;
  // 기본 commit type (필요시 확장 가능)
  private static final String DEFAULT_COMMIT_TYPE = "feat";

  @Autowired
  public IssueHelperService(OkHttpClient okHttpClient) {
    this.okHttpClient = okHttpClient;
  }

  /**
   * GitHub 이슈 URL로부터 페이지를 읽어 <title> 태그를 파싱한 후,
   * 커스텀 규칙에 따라 브랜치명과 커밋 메시지를 생성합니다.
   */
  public IssueHelperResponse createIssueCommmitBranch(IssueHelperRequest request) {
    if (request == null || request.getIssueUrl() == null || request.getIssueUrl().trim().isEmpty()) {
      throw new IllegalArgumentException("Issue URL이 비어있습니다.");
    }

    String issueUrl = request.getIssueUrl().trim();
    String htmlContent = fetchPageContent(issueUrl);
    String fullTitle = parseTitleFromHtml(htmlContent);
    if (fullTitle == null || fullTitle.isEmpty()) {
      throw new IllegalArgumentException("페이지에서 제목을 추출할 수 없습니다.");
    }

    // 예제: "⚙️ [기능추가][대시보드] 로그인 이후 대시보드 페이지 구현 · Issue #1 · Cassiiopeia/suh-project-utility"
    // "· Issue" 이전까지의 부분을 원본 제목으로 사용
    String rawTitle = fullTitle.split("·\\s*Issue")[0].trim();

    // 이슈 번호 추출 (URL에서 "issues/숫자" 형식)
    String issueNumber = extractIssueNumber(issueUrl);
    if (issueNumber == null) {
      throw new IllegalArgumentException("URL에서 이슈 번호를 추출할 수 없습니다.");
    }

    // 현재 날짜 (yyyyMMdd)
    String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());

    // 브랜치명용 제목 가공: 맨 앞에 있는 대괄호 그룹과 이모지 등 불필요한 특수문자를 제거 후 언더스코어 처리
    String branchTitle = processTitleForBranch(rawTitle);
    String branchName = String.format("%s_#%s_%s", currentDate, issueNumber, branchTitle);

    // 커밋 메시지용 제목 가공: 대괄호 그룹 전체 제거 후 특수문자 제거, 공백 정리
    String commitTitle = processTitleForCommit(rawTitle);
    // "[변경 사항에 대한 설명]"에서 대괄호를 제외하고 고정 메시지를 사용
    String commitMessage = String.format("%s : %s : %s %s",
        commitTitle, DEFAULT_COMMIT_TYPE, "변경 사항에 대한 설명", issueUrl);

    return IssueHelperResponse.builder()
        .branchName(branchName)
        .commitMessage(commitMessage)
        .build();
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
   * - 맨 앞에 있는 대괄호 그룹(예, [기능추가], [대시보드] 등)과 이모지/특수문자 제거
   * - 남은 단어들은 언더스코어(_)로 연결
   * - 알파벳, 숫자, 한글, 그리고 언더스코어만 허용
   */
  private String processTitleForBranch(String title) {
    // 맨 앞에 있는 [~~] 그룹을 모두 제거 (여러 그룹일 경우 모두 제거)
    String withoutLeadingBrackets = title.replaceFirst("^(\\[[^\\]]*\\]\\s*)+", "");
    // 이모지나 기타 특수문자(알파벳, 숫자, 한글, 공백 제외) 제거
    withoutLeadingBrackets = withoutLeadingBrackets.replaceAll("^[^\\p{L}\\p{Nd}\\s]+", "");
    // 공백을 언더스코어로 변환
    String replaced = withoutLeadingBrackets.trim().replaceAll("\\s+", "_");
    // 알파벳, 숫자, 한글, 언더스코어 외의 문자는 제거
    return replaced.replaceAll("[^\\p{L}\\p{Nd}_]", "");
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
