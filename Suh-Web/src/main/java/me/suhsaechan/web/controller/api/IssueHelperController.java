package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.github.dto.IssueHelperRequest;
import me.suhsaechan.github.dto.IssueHelperResponse;
import me.suhsaechan.github.service.IssueHelperService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
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

  @LogMonitor
  @PostMapping(value = "/create/commit-branch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<IssueHelperResponse> createIssueCommmitBranch(
      @ModelAttribute IssueHelperRequest request) {
      IssueHelperResponse response = issueHelperService.createIssueCommmitBranch(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // GITHUB_WORKFLOW 에서 호출되는 API
  @LogMonitor
  @PostMapping(value = "/create/commit-branch/github-workflow", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<IssueHelperResponse> createIssueCommmitBranchByGithubWorkflow(
      @ModelAttribute IssueHelperRequest request) {
      IssueHelperResponse response = issueHelperService.createIssueCommitBranchByGithubWorkflow(request);
      return ResponseEntity.status(HttpStatus.OK).body(response);
  }
}
