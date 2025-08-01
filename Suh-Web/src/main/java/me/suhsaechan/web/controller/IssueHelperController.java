package me.suhsaechan.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.github.service.IssueHelperService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
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
  @PostMapping(value = "/create/commmit-branch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<me.suhsaechan.github.dto.response.IssueHelperResponse> createIssueCommmitBranch(
      @ModelAttribute me.suhsaechan.github.dto.request.IssueHelperRequest request) {
    try {
      me.suhsaechan.github.dto.response.IssueHelperResponse response = issueHelperService.createIssueCommmitBranch(request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      log.error("입력 오류: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("서버 오류: {}", e.getMessage());
      return ResponseEntity.internalServerError().build();
    }
  }
}
