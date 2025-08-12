// java
package me.suhsaechan.github.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubService {

  private final GithubIssueHelperRepository issueHelperRepository;
  public final GithubRepositoryRepository githubRepositoryRepository;
  private final AESUtil aesUtil;

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
          GithubRepository githubRepository = githubRepositoryRepository.findByFullName(repositoryFullName);
          if (githubRepository == null) {
            // 웹 UI에서 처음 호출된 경우 자동으로 레포지토리를 등록하고 허용 상태로 설정
            log.info("새 레포지토리 등록: {} (웹 UI에서 첫 요청이므로 자동 허용)", repositoryFullName);
            githubRepository = GithubRepository.builder()
                .fullName(repositoryFullName)
                .starCount(0L)
                .forkCount(0L)
                .watcherCount(0L)
                .description(null)
                .isGithubWorkflowResponseAllowed(Boolean.TRUE) // 웹 UI에서의 첫 호출은 자동으로 허용
                .build();
            githubRepository = githubRepositoryRepository.save(githubRepository);
          }

          // 웹 UI에서 호출 시에도, 명시적으로 차단된 저장소는 사용 불가
          if (githubRepository.getIsGithubWorkflowResponseAllowed() != null && !githubRepository.getIsGithubWorkflowResponseAllowed()) {
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

}