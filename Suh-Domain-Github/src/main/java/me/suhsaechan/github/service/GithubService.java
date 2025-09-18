// java
package me.suhsaechan.github.service;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.github.entity.GithubIssueHelper;
import me.suhsaechan.github.entity.GithubRepository;
import me.suhsaechan.github.repository.GithubIssueHelperRepository;
import me.suhsaechan.github.repository.GithubRepositoryRepository;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.util.AESUtil;
import me.suhsaechan.github.dto.IssueHelperRequest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubService {

  private final GithubIssueHelperRepository issueHelperRepository;
  private final GithubRepositoryRepository githubRepositoryRepository;
  private final AESUtil aesUtil;
  private final OkHttpClient okHttpClient;

  /**
   * issueUrl 및 repositoryFullName(요청 객체에 추가된 필드) 기반으로 기존 이력이 있는지 먼저 조회합니다.
   * 없으면, 브랜치명, 커밋 메시지 등 IssueHelperService에서 생성한 내용을 포함하여
   * 새로 생성 후 저장합니다.
   */
  @Transactional
  public GithubIssueHelper processIssueHelper(IssueHelperRequest request, String branchName,
      String commitMessage, String repositoryFullName) {

    // clientHash -> clientIp
    String clientHash = request.getClientHash();
    String clientIp = (clientHash == null || clientHash.isBlank())
        ? "GITHUB_WORKFLOW"
        : aesUtil.decrypt(clientHash);

    log.info("Client IP: {}", clientIp);

    String issueUrl = request.getIssueUrl().trim();

    // 먼저 issueUrl로 기존 이력이 있는지 조회
    return issueHelperRepository.findByIssueUrl(issueUrl)
        .map(issueHelper -> {
          // 기존 이력이 있는 경우, clientIp가 다르면 업데이트
          if (!issueHelper.getClientIp().equals(clientIp)) {
            issueHelper.setClientIp(clientIp);
          }
          // 카운트 증가
          issueHelper.incrementCount();
          return issueHelperRepository.save(issueHelper);
        })
        .orElseGet(() -> {
          // 기존 이슈가 없는경우 -> 새로 생성
          GithubRepository githubRepository = githubRepositoryRepository.findByFullName(repositoryFullName)
              .orElseGet(() -> {
                    log.info("새 레포지토리 등록: {} (웹 UI에서 첫 요청이므로 자동 허용)", repositoryFullName);
                    GithubRepository newRepo = GithubRepository.builder()
                        .fullName(repositoryFullName)
                        .starCount(0L)
                        .forkCount(0L)
                        .watcherCount(0L)
                        .description(null)
                        .isGithubWorkflowResponseAllowed(Boolean.TRUE) // 웹 UI에서의 첫 호출은 자동으로 허용
                        .build();
                    return githubRepositoryRepository.save(newRepo);
              });

          // 웹 UI에서 호출 시에도, 명시적으로 차단된 저장소는 사용 불가
          if (Boolean.FALSE.equals(githubRepository.getIsGithubWorkflowResponseAllowed())) {
            log.warn("차단된 레포지토리에 대한 접근 시도: {}", repositoryFullName);
            throw new CustomException(ErrorCode.ACCESS_DENIED);
          }

          // 이슈 Helper 생성
          GithubIssueHelper newIssueHelper = GithubIssueHelper.builder()
              .issueUrl(issueUrl)
              .branchName(branchName)
              .commitMessage(commitMessage)
              .clientIp(clientIp)
              .githubRepository(githubRepository)
              .count(1L)
              .build();

          return issueHelperRepository.save(newIssueHelper);
        });
  }

  /**
   * GitHub Repository 페이지를 스크래핑하여 정보를 가져오고 저장합니다.
   * @param repositoryFullName username/repository 형식
   * @return 저장된 GithubRepository 엔티티
   */
  @Transactional
  public GithubRepository fetchAndSaveGithubRepository(String repositoryFullName) {
    log.info("GitHub Repository 정보 스크래핑 시작: {}", repositoryFullName);
    
    // 이미 존재하는지 확인
    return githubRepositoryRepository.findByFullName(repositoryFullName)
        .orElseGet(() -> {
          String url = "https://github.com/" + repositoryFullName;
          
          try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .build();
            
            Response response = okHttpClient.newCall(request).execute();
            
            if (!response.isSuccessful()) {
              log.error("GitHub 페이지 로드 실패: {} - 응답 코드: {}", url, response.code());
              throw new CustomException(ErrorCode.GITHUB_API_ERROR);
            }
            
            String html = response.body().string();
            Document doc = Jsoup.parse(html);
            
            // 스타, 포크, 워처 수 파싱
            Long starCount = 0L;
            Long forkCount = 0L;
            String description = null;
            
            // 스타 수 파싱
            Element starElement = doc.select("a[href*='/stargazers'] strong").first();
            if (starElement != null) {
              String starText = starElement.text().replace(",", "").replace("k", "000");
              try {
                starCount = Long.parseLong(starText);
              } catch (NumberFormatException e) {
                log.warn("스타 수 파싱 실패: {}", starText);
              }
            }
            
            // 포크 수 파싱
            Element forkElement = doc.select("a[href*='/forks'] strong").first();
            if (forkElement != null) {
              String forkText = forkElement.text().replace(",", "").replace("k", "000");
              try {
                forkCount = Long.parseLong(forkText);
              } catch (NumberFormatException e) {
                log.warn("포크 수 파싱 실패: {}", forkText);
              }
            }
            
            // 설명 파싱
            Element descElement = doc.select("p.f4.my-3").first();
            if (descElement != null) {
              description = descElement.text();
            }
            
            // Repository 엔티티 생성 및 저장
            GithubRepository githubRepository = GithubRepository.builder()
                .fullName(repositoryFullName)
                .starCount(starCount)
                .forkCount(forkCount)
                .watcherCount(starCount) // GitHub에서 워처와 스타가 통합됨
                .description(description)
                .isGithubWorkflowResponseAllowed(Boolean.TRUE)
                .build();
            
            githubRepository = githubRepositoryRepository.save(githubRepository);
            log.info("GitHub Repository 정보 저장 완료: {}", repositoryFullName);
            
            return githubRepository;
            
          } catch (IOException e) {
            log.error("GitHub 페이지 스크래핑 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.GITHUB_API_ERROR);
          }
        });
  }

}