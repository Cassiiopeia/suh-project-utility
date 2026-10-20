package me.suhsaechan.web.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.service.UserAuthority;
import me.suhsaechan.common.util.AESUtil;
import me.suhsaechan.github.dto.IssueHelperRequest;
import me.suhsaechan.github.dto.IssueHelperResponse;
import me.suhsaechan.github.service.IssueHelperService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.web.config.ServerInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/issue-helper")
public class IssueHelperController {

  private final IssueHelperService issueHelperService;
  private final AESUtil aesUtil;
  private final UserAuthority userAuthority;

  @LogMonitor
  @PostMapping(value = "/create/commit-branch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<IssueHelperResponse> createIssueCommmitBranch(
      @ModelAttribute IssueHelperRequest request, HttpServletRequest httpRequest) {
      // clientHash 는 서버에서 결정 (클라이언트 값 무시): 세션 값 > 없으면 요청 IP 고정 IV 암호화
      String sessionClientHash = (String) httpRequest.getSession().getAttribute(ServerInfo.CLIENT_HASH_SESSION_KEY);
      request.setClientHash(Optional.ofNullable(sessionClientHash)
          .orElseGet(() -> aesUtil.encryptWithFixedIv(userAuthority.extractIpFromRequest(httpRequest))));
      IssueHelperResponse response = issueHelperService.createIssueCommmitBranch(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // GITHUB_WORKFLOW 에서 호출되는 API
  @LogMonitor
  @PostMapping(value = "/create/commit-branch/github-workflow", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<IssueHelperResponse> createIssueCommmitBranchByGithubWorkflow(
      @ModelAttribute IssueHelperRequest request) {
      // 워크플로우 호출은 clientHash 없음 > 이력 clientIp "GITHUB_WORKFLOW"
      request.setClientHash(null);
      IssueHelperResponse response = issueHelperService.createIssueCommitBranchByGithubWorkflow(request);
      return ResponseEntity.status(HttpStatus.OK).body(response);
  }
}
