package me.suhsaechan.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.web.config.UserAuthority;
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
}