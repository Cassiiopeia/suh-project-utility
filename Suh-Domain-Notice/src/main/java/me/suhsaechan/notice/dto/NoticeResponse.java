package me.suhsaechan.notice.object.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.suhsaechan.common.entity.NoticeComment;
import me.suhsaechan.common.entity.SuhProjectUtilityNotice;

/**
 * 공지사항 응답 객체
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private List<CommentDto> comments = new ArrayList<>();
    
    // 응답 상태
    private boolean success;
    private String message;
    private Long totalCount;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentDto {
        private UUID noticeCommentId;
        private String author;
        private String content;
        private String anonymizedIp;
        private LocalDateTime createdDate;
        private boolean canDelete;
    }
    
    /**
     * 댓글 목록 설정
     */
    public NoticeResponse withComments(List<NoticeComment> commentList, boolean canDelete) {
        if (commentList != null && !commentList.isEmpty()) {
            this.comments = commentList.stream()
                .map(comment -> CommentDto.builder()
                    .noticeCommentId(comment.getNoticeCommentId())
                    .author(comment.getAuthor())
                    .content(comment.getContent())
                    .anonymizedIp(comment.getAnonymizedIp())
                    .createdDate(comment.getCreatedDate())
                    .canDelete(canDelete)
                    .build())
                .collect(Collectors.toList());
        }
        return this;
    }
    
    /**
     * 공지사항 엔티티로부터 응답 객체 생성
     */
    public static NoticeResponse fromEntity(SuhProjectUtilityNotice notice) {
        return NoticeResponse.builder()
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
            .success(true)
            .build();
    }
    
    /**
     * 공지사항 목록 응답 생성
     */
    public static NoticeResponse ofList(List<SuhProjectUtilityNotice> notices, Long totalCount) {
        return NoticeResponse.builder()
            .notices(notices)
            .totalCount(totalCount)
            .success(true)
            .build();
    }
    
    /**
     * 성공 응답 생성
     */
    public static NoticeResponse success(String message) {
        return NoticeResponse.builder()
            .success(true)
            .message(message)
            .build();
    }
    
    /**
     * 실패 응답 생성
     */
    public static NoticeResponse fail(String message) {
        return NoticeResponse.builder()
            .success(false)
            .message(message)
            .build();
    }
} 