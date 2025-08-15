package me.suhsaechan.notice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.entity.SuhProjectUtilityNotice;
import me.suhsaechan.common.repository.SuhProjectUtilityNoticeRepository;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {
  
  private final SuhProjectUtilityNoticeRepository noticeRepository;

  /**
   * 활성화된 공지사항 목록 조회
   */
  @Transactional(readOnly = true)
  public me.suhsaechan.notice.object.response.NoticeResponse getActiveNotices() {
    log.info("활성화된 공지사항 목록 조회");
    List<SuhProjectUtilityNotice> notices = noticeRepository.findActiveNotices(LocalDateTime.now());
    return me.suhsaechan.notice.object.response.NoticeResponse.builder()
        .notices(notices)
        .totalCount((long) notices.size())
        .build();
  }
  
  /**
   * 중요 공지사항 목록 조회
   */
  @Transactional(readOnly = true)
  public me.suhsaechan.notice.object.response.NoticeResponse getImportantNotices() {
    log.info("중요 공지사항 목록 조회");
    List<SuhProjectUtilityNotice> notices = 
        noticeRepository.findByIsImportantTrueAndIsActiveTrueOrderByCreatedDateDesc();
    return me.suhsaechan.notice.object.response.NoticeResponse.builder()
        .notices(notices)
        .totalCount((long) notices.size())
        .build();
  }
  
  /**
   * 모든 공지사항 목록 조회 (관리자용)
   */
  @Transactional(readOnly = true)
  public me.suhsaechan.notice.object.response.NoticeResponse getAllNotices() {
    log.info("모든 공지사항 목록 조회");
    List<SuhProjectUtilityNotice> notices = noticeRepository.findAll();
    return me.suhsaechan.notice.object.response.NoticeResponse.builder()
        .notices(notices)
        .totalCount((long) notices.size())
        .build();
  }
  
  /**
   * 공지사항 상세 조회
   */
  @Transactional
  public me.suhsaechan.notice.object.response.NoticeResponse getNoticeDetail(UUID noticeId) {
    log.info("공지사항 상세 조회: {}", noticeId);
    SuhProjectUtilityNotice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_NOTICE));
    
    // 조회수 증가
    notice.incrementViewCount();
    noticeRepository.save(notice);
    
    return me.suhsaechan.notice.object.response.NoticeResponse.builder()
        .noticeId(notice.getNoticeId())
        .title(notice.getTitle())
        .content(notice.getContent())
        .author(notice.getAuthor())
        .createdDate(notice.getCreatedDate())
        .startDate(notice.getStartDate())
        .endDate(notice.getEndDate())
        .isImportant(notice.getIsImportant())
        .isActive(notice.getIsActive())
        .viewCount(notice.getViewCount())
        .build();
  }
  
  /**
   * 공지사항 검색
   */
  @Transactional(readOnly = true)
  public me.suhsaechan.notice.object.response.NoticeResponse searchNotices(
      me.suhsaechan.notice.object.request.NoticeRequest request) {
    log.info("공지사항 검색: {}", request.getSearchKeyword());
    List<SuhProjectUtilityNotice> notices;
    
    if ("title".equals(request.getSearchType())) {
      notices = noticeRepository.findByTitleContainingOrderByCreatedDateDesc(request.getSearchKeyword());
    } else if ("content".equals(request.getSearchType())) {
      notices = noticeRepository.findByContentContainingOrderByCreatedDateDesc(request.getSearchKeyword());
    } else {
      throw new CustomException(ErrorCode.INVALID_PARAMETER);
    }
    
    return me.suhsaechan.notice.object.response.NoticeResponse.builder()
        .notices(notices)
        .totalCount((long) notices.size())
        .build();
  }
  
  /**
   * 공지사항 등록
   */
  @Transactional
  public me.suhsaechan.notice.object.response.NoticeResponse createNotice(
      me.suhsaechan.notice.object.request.NoticeRequest request) {
    log.info("공지사항 등록: {}", request.getTitle());
    
    // 유효성 검사
    validateNoticeRequest(request);
    
    SuhProjectUtilityNotice notice = SuhProjectUtilityNotice.builder()
        .title(request.getTitle())
        .content(request.getContent())
        .isImportant(request.getIsImportant())
        .startDate(request.getStartDate())
        .endDate(request.getEndDate())
        .isActive(request.getIsActive())
        .author(request.getAuthor())
        .viewCount(0L)
        .build();
    
    noticeRepository.save(notice);
    return me.suhsaechan.notice.object.response.NoticeResponse.builder().build();
  }
  
  /**
   * 공지사항 수정
   */
  @Transactional
  public me.suhsaechan.notice.object.response.NoticeResponse updateNotice(
      me.suhsaechan.notice.object.request.NoticeRequest request) {
    log.info("공지사항 수정: {}", request.getNoticeId());
    
    // 유효성 검사
    validateNoticeRequest(request);
    
    SuhProjectUtilityNotice notice = noticeRepository.findById(request.getNoticeId())
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_NOTICE));
    
    notice.setTitle(request.getTitle());
    notice.setContent(request.getContent());
    notice.setIsImportant(request.getIsImportant());
    notice.setStartDate(request.getStartDate());
    notice.setEndDate(request.getEndDate());
    notice.setIsActive(request.getIsActive());
    notice.setAuthor(request.getAuthor());
    
    noticeRepository.save(notice);
    return me.suhsaechan.notice.object.response.NoticeResponse.builder().build();
  }
  
  /**
   * 공지사항 삭제
   */
  @Transactional
  public me.suhsaechan.notice.object.response.NoticeResponse deleteNotice(UUID noticeId) {
    log.info("공지사항 삭제: {}", noticeId);
    
    SuhProjectUtilityNotice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_NOTICE));
    
    noticeRepository.delete(notice);
    return me.suhsaechan.notice.object.response.NoticeResponse.builder().build();
  }
  
  /**
   * 공지사항 활성화/비활성화
   */
  @Transactional
  public me.suhsaechan.notice.object.response.NoticeResponse toggleNoticeActive(UUID noticeId) {
    log.info("공지사항 활성화/비활성화: {}", noticeId);
    
    SuhProjectUtilityNotice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_NOTICE));
    
    notice.setIsActive(!notice.getIsActive());
    noticeRepository.save(notice);
    
    return me.suhsaechan.notice.object.response.NoticeResponse.builder().build();
  }
  
  /**
   * 공지사항 요청 유효성 검사
   */
  private void validateNoticeRequest(me.suhsaechan.notice.object.request.NoticeRequest request) {
    if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
      throw new CustomException(ErrorCode.INVALID_PARAMETER);
    }
    
    if (request.getContent() == null || request.getContent().trim().isEmpty()) {
      throw new CustomException(ErrorCode.INVALID_PARAMETER);
    }
    
    if (request.getIsActive() == null) {
      request.setIsActive(true);
    }
    
    if (request.getIsImportant() == null) {
      request.setIsImportant(false);
    }
  }
} 