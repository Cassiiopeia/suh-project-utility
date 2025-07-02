package me.suhsaechan.suhprojectutility.object.request;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 공지사항 요청 객체
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class NoticeRequest {
  
  // 공지사항 ID (수정, 삭제 시 사용)
  private UUID noticeId;
  
  // 공지사항 제목
  private String title;
  
  // 공지사항 내용
  private String content;
  
  // 중요 공지 여부
  private Boolean isImportant;
  
  // 게시 시작일
  private LocalDateTime startDate;
  
  // 게시 종료일
  private LocalDateTime endDate;
  
  // 활성화 여부
  private Boolean isActive;
  
  // 작성자
  private String author;
  
  // 검색어 (조회 시 사용)
  private String searchKeyword;
  
  // 검색 타입 (제목, 내용)
  private String searchType;
} 