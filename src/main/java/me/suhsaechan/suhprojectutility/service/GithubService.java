// java
package me.suhsaechan.suhprojectutility.service;

import lombok.RequiredArgsConstructor;
import me.suhsaechan.suhprojectutility.object.postgres.GithubIssueHelper;
import me.suhsaechan.suhprojectutility.object.postgres.GithubRepository;
import me.suhsaechan.suhprojectutility.object.request.IssueHelperRequest;
import me.suhsaechan.suhprojectutility.repository.GithubIssueHelperRepository;
import me.suhsaechan.suhprojectutility.repository.GithubRepositoryRepository;
import me.suhsaechan.suhprojectutility.util.AESUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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

    // 복호화
    String encryptedIp = request.getClientIpHash();
    String decryptedIp = aesUtil.decrypt(encryptedIp);

    String issueUrl = request.getIssueUrl().trim();

    // 먼저 issueUrl로 기존 이력이 있는지 조회
    GithubIssueHelper existing = issueHelperRepository.findByIssueUrl(issueUrl);
    if (existing != null) {
      return existing;
    }

    // GithubRepository 조회 : 파라미터로 전달받은 repositoryFullName 사용
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

    GithubIssueHelper newIssueHelper = GithubIssueHelper.builder()
        .issueUrl(issueUrl)
        .branchName(branchName)
        .commitMessage(commitMessage)
        .clientIp(decryptedIp)
        .githubRepository(githubRepository)
        .count(1L)
        .build();

    return issueHelperRepository.save(newIssueHelper);
  }
}