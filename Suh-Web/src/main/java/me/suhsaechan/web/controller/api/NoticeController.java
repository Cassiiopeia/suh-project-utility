package me.suhsaechan.web.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.service.UserAuthority;
import me.suhsaechan.notice.object.request.NoticeRequest;
import me.suhsaechan.notice.object.response.NoticeResponse;
import me.suhsaechan.notice.service.NoticeCommentService;
import me.suhsaechan.notice.service.NoticeService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공지사항 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notice")
public class NoticeController {

  private final NoticeService noticeService;
  private final NoticeCommentService noticeCommentService;
  private final UserAuthority userAuthority;

  /**
   * 활성화된 공지사항 목록 조회
   */
  @PostMapping(value = "/get/active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<me.suhsaechan.notice.object.response.NoticeResponse> getActiveNotices() {
    return ResponseEntity.ok(noticeService.getActiveNotices());
  }

  /**
   * 모든 공지사항 목록 조회
   */
  @PostMapping(value = "/get/all", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<me.suhsaechan.notice.object.response.NoticeResponse> getAllNotices() {
    return ResponseEntity.ok(noticeService.getAllNotices());
  }

  /**
   * 공지사항 상세 조회
   */
  @PostMapping(value = "/get/detail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<me.suhsaechan.notice.object.response.NoticeResponse> getNoticeDetail(@ModelAttribute me.suhsaechan.notice.object.request.NoticeRequest request) {
    return ResponseEntity.ok(noticeService.getNoticeDetail(request.getNoticeId()));
  }

  /**
   * 공지사항 검색
   */
  @PostMapping(value = "/search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<me.suhsaechan.notice.object.response.NoticeResponse> searchNotices(@ModelAttribute me.suhsaechan.notice.object.request.NoticeRequest request) {
    return ResponseEntity.ok(noticeService.searchNotices(request));
  }

  /**
   * 공지사항 등록
   */
  @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<me.suhsaechan.notice.object.response.NoticeResponse> createNotice(@ModelAttribute me.suhsaechan.notice.object.request.NoticeRequest request) {
    return ResponseEntity.ok(noticeService.createNotice(request));
  }

  /**
   * 공지사항 수정
   */
  @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<me.suhsaechan.notice.object.response.NoticeResponse> updateNotice(@ModelAttribute me.suhsaechan.notice.object.request.NoticeRequest request) {
    return ResponseEntity.ok(noticeService.updateNotice(request));
  }

  /**
   * 공지사항 삭제
   */
  @PostMapping(value = "/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<me.suhsaechan.notice.object.response.NoticeResponse> deleteNotice(@ModelAttribute me.suhsaechan.notice.object.request.NoticeRequest request) {
    return ResponseEntity.ok(noticeService.deleteNotice(request.getNoticeId()));
  }

  /**
   * 공지사항 활성화/비활성화
   */
  @PostMapping(value = "/toggle-active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<me.suhsaechan.notice.object.response.NoticeResponse> toggleNoticeActive(@ModelAttribute me.suhsaechan.notice.object.request.NoticeRequest request) {
    return ResponseEntity.ok(noticeService.toggleNoticeActive(request.getNoticeId()));
  }

  /**
   * 공지사항 댓글 목록 조회
   */
  @PostMapping(value = "/comment/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<NoticeResponse> getComments(@ModelAttribute NoticeRequest request) {
    return ResponseEntity.ok(noticeCommentService.getCommentsByNoticeId(request.getNoticeId()));
  }

  /**
   * 공지사항 댓글 등록
   */
  @PostMapping(value = "/comment/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<NoticeResponse> createComment(@ModelAttribute NoticeRequest request,
                                                     HttpServletRequest httpRequest) {
    // IP 주소 설정 (프록시 등을 고려한 실제 클라이언트 IP 추출)
    String clientIp = userAuthority.extractIpFromRequest(httpRequest);
    request.setAuthorIp(clientIp);
    
    // 클라이언트 해시 설정
    String clientHash = (String) httpRequest.getSession().getAttribute("clientHash");
    if (clientHash != null) {
      request.setClientHash(clientHash);
    }
    
    return ResponseEntity.ok(noticeCommentService.createComment(request));
  }

  /**
   * 공지사항 댓글 삭제
   */
  @PostMapping(value = "/comment/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<NoticeResponse> deleteComment(@ModelAttribute NoticeRequest request) {
    return ResponseEntity.ok(noticeCommentService.deleteComment(request.getCommentId()));
  }
}