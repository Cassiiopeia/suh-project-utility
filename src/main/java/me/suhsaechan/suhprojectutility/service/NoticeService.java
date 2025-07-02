package me.suhsaechan.suhprojectutility.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.object.postgres.SuhProjectUtilityNotice;
import me.suhsaechan.suhprojectutility.object.request.NoticeRequest;
import me.suhsaechan.suhprojectutility.object.response.NoticeResponse;
import me.suhsaechan.suhprojectutility.repository.SuhProjectUtilityNoticeRepository;
import me.suhsaechan.suhprojectutility.util.exception.CustomException;
import me.suhsaechan.suhprojectutility.util.exception.ErrorCode;
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
  public NoticeResponse getActiveNotices() {
    log.info("활성화된 공지사항 목록 조회");
    List<SuhProjectUtilityNotice> notices = noticeRepository.findActiveNotices(LocalDateTime.now());
    return NoticeResponse.ofList(notices, (long) notices.size());
  }
  
  /**
   * 중요 공지사항 목록 조회
   */
  @Transactional(readOnly = true)
  public NoticeResponse getImportantNotices() {
    log.info("중요 공지사항 목록 조회");
    List<SuhProjectUtilityNotice> notices = 
        noticeRepository.findByIsImportantTrueAndIsActiveTrueOrderByCreatedDateDesc();
    return NoticeResponse.ofList(notices, (long) notices.size());
  }
  
  /**
   * 모든 공지사항 목록 조회 (관리자용)
   */
  @Transactional(readOnly = true)
  public NoticeResponse getAllNotices() {
    log.info("모든 공지사항 목록 조회");
    List<SuhProjectUtilityNotice> notices = noticeRepository.findAll();
    return NoticeResponse.ofList(notices, (long) notices.size());
  }
  
  /**
   * 공지사항 상세 조회
   */
  @Transactional
  public NoticeResponse getNoticeDetail(UUID noticeId) {
    log.info("공지사항 상세 조회: {}", noticeId);
    SuhProjectUtilityNotice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));
    
    // 조회수 증가
    notice.incrementViewCount();
    noticeRepository.save(notice);
    
    return NoticeResponse.ofSingle(notice);
  }
  
  /**
   * 공지사항 검색
   */
  @Transactional(readOnly = true)
  public NoticeResponse searchNotices(NoticeRequest request) {
    log.info("공지사항 검색: {}", request.getSearchKeyword());
    List<SuhProjectUtilityNotice> notices;
    
    if ("title".equals(request.getSearchType())) {
      notices = noticeRepository.findByTitleContainingOrderByCreatedDateDesc(request.getSearchKeyword());
    } else if ("content".equals(request.getSearchType())) {
      notices = noticeRepository.findByContentContainingOrderByCreatedDateDesc(request.getSearchKeyword());
    } else {
      throw new CustomException(ErrorCode.INVALID_PARAMETER);
    }
    
    return NoticeResponse.ofList(notices, (long) notices.size());
  }
  
  /**
   * 공지사항 등록
   */
  @Transactional
  public NoticeResponse createNotice(NoticeRequest request) {
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
    return NoticeResponse.ofSuccess("공지사항이 등록되었습니다.");
  }
  
  /**
   * 공지사항 수정
   */
  @Transactional
  public NoticeResponse updateNotice(NoticeRequest request) {
    log.info("공지사항 수정: {}", request.getNoticeId());
    
    // 유효성 검사
    validateNoticeRequest(request);
    
    SuhProjectUtilityNotice notice = noticeRepository.findById(request.getNoticeId())
        .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));
    
    notice.setTitle(request.getTitle());
    notice.setContent(request.getContent());
    notice.setIsImportant(request.getIsImportant());
    notice.setStartDate(request.getStartDate());
    notice.setEndDate(request.getEndDate());
    notice.setIsActive(request.getIsActive());
    notice.setAuthor(request.getAuthor());
    
    noticeRepository.save(notice);
    return NoticeResponse.ofSuccess("공지사항이 수정되었습니다.");
  }
  
  /**
   * 공지사항 삭제
   */
  @Transactional
  public NoticeResponse deleteNotice(UUID noticeId) {
    log.info("공지사항 삭제: {}", noticeId);
    
    SuhProjectUtilityNotice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));
    
    noticeRepository.delete(notice);
    return NoticeResponse.ofSuccess("공지사항이 삭제되었습니다.");
  }
  
  /**
   * 공지사항 활성화/비활성화
   */
  @Transactional
  public NoticeResponse toggleNoticeActive(UUID noticeId) {
    log.info("공지사항 활성화/비활성화: {}", noticeId);
    
    SuhProjectUtilityNotice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));
    
    notice.setIsActive(!notice.getIsActive());
    noticeRepository.save(notice);
    
    String message = notice.getIsActive() ? "공지사항이 활성화되었습니다." : "공지사항이 비활성화되었습니다.";
    return NoticeResponse.ofSuccess(message);
  }
  
  /**
   * 공지사항 요청 유효성 검사
   */
  private void validateNoticeRequest(NoticeRequest request) {
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