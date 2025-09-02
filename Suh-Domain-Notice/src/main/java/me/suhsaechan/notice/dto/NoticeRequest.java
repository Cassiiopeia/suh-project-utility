package me.suhsaechan.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 공지사항 관련 요청을 처리하는 통합 요청 DTO 클래스
 * 모듈의 모든 API 엔드포인트는 이 단일 요청 클래스를 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    
    // 댓글 관련 필드
    private UUID commentId;
    private String commentAuthor;
    private String commentContent;
    private String commentAuthorIp;
    private String clientHash;
    private String authorIp;
    
    @Builder.Default
    private String authorName = "익명";
}