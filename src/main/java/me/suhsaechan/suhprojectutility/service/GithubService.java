// java
package me.suhsaechan.suhprojectutility.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.object.postgres.GithubIssueHelper;
import me.suhsaechan.suhprojectutility.object.postgres.GithubRepository;
import me.suhsaechan.suhprojectutility.object.request.IssueHelperRequest;
import me.suhsaechan.suhprojectutility.repository.GithubIssueHelperRepository;
import me.suhsaechan.suhprojectutility.repository.GithubRepositoryRepository;
import me.suhsaechan.suhprojectutility.util.security.AESUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubService {

  private final GithubIssueHelperRepository issueHelperRepository;
  private final GithubRepositoryRepository repositoryRepository;
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
    String clientIp = aesUtil.decrypt(request.getClientHash());
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
          GithubRepository githubRepository = repositoryRepository.findByFullName(repositoryFullName);
          if (githubRepository == null) {
            githubRepository = GithubRepository.builder()
                .fullName(repositoryFullName)
                .starCount(0L)
                .forkCount(0L)
                .watcherCount(0L)
                .description(null)
                .build();
            githubRepository = repositoryRepository.save(githubRepository);
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