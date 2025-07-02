package me.suhsaechan.suhprojectutility.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.suhprojectutility.object.request.NoticeRequest;
import me.suhsaechan.suhprojectutility.object.response.NoticeResponse;
import me.suhsaechan.suhprojectutility.service.NoticeService;
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
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

  private final NoticeService noticeService;

  /**
   * 모든 공지사항 조회
   */
  @PostMapping(value = "/get/all", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<NoticeResponse> getAllNotices() {
    return ResponseEntity.ok(noticeService.getAllNotices());
  }

  /**
   * 활성화된 공지사항 조회
   */
  @PostMapping(value = "/get/active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<NoticeResponse> getActiveNotices() {
    return ResponseEntity.ok(noticeService.getActiveNotices());
  }

  /**
   * 공지사항 상세 조회
   */
  @PostMapping(value = "/get/detail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<NoticeResponse> getNoticeDetail(@ModelAttribute NoticeRequest request) {
    return ResponseEntity.ok(noticeService.getNoticeDetail(request.getNoticeId()));
  }

  /**
   * 공지사항 검색
   */
  @PostMapping(value = "/search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NoticeResponse> searchNotices(@ModelAttribute NoticeRequest request) {
    return ResponseEntity.ok(noticeService.searchNotices(request));
  }

  /**
   * 공지사항 생성
   */
  @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NoticeResponse> createNotice(@ModelAttribute NoticeRequest request) {
    return ResponseEntity.ok(noticeService.createNotice(request));
  }

  /**
   * 공지사항 수정
   */
  @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NoticeResponse> updateNotice(@ModelAttribute NoticeRequest request) {
    return ResponseEntity.ok(noticeService.updateNotice(request));
  }

  /**
   * 공지사항 삭제
   */
  @PostMapping(value = "/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NoticeResponse> deleteNotice(@ModelAttribute NoticeRequest request) {
    return ResponseEntity.ok(noticeService.deleteNotice(request.getNoticeId()));
  }

  /**
   * 공지사항 활성화/비활성화 토글
   */
  @PostMapping(value = "/toggle-active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NoticeResponse> toggleNoticeActive(@ModelAttribute NoticeRequest request) {
    return ResponseEntity.ok(noticeService.toggleNoticeActive(request.getNoticeId()));
  }
}