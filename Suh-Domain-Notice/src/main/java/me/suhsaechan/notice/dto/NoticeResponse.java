package me.suhsaechan.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.common.entity.SuhProjectUtilityNotice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 공지사항 관련 응답을 처리하는 통합 응답 DTO 클래스
 * 모듈의 모든 API 엔드포인트는 이 단일 응답 클래스를 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeResponse {
    
    // 단일 공지사항 정보
    private UUID noticeId;
    private String title;
    private String content;
    private String author;
    private LocalDateTime createdDate;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isImportant;
    private Boolean isActive;
    private Long viewCount;
    
    // 공지사항 목록
    @Builder.Default
    private List<SuhProjectUtilityNotice> notices = new ArrayList<>();
    
    // 댓글 관련 필드
    @Builder.Default
    private List<NoticeCommentDto> noticeCommentDtos = new ArrayList<>();
    private NoticeCommentDto noticeCommentDto;
    
    // 페이징 정보
    private Long totalCount;
}