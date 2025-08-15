package me.suhsaechan.notice.object.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.suhsaechan.common.entity.SuhProjectUtilityNotice;
import me.suhsaechan.notice.dto.NoticeCommentDto;

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
    private List<NoticeCommentDto> noticeCommentDtos = new ArrayList<>();

    private NoticeCommentDto noticeCommentDto;


    // 페이징 정보
    private Long totalCount;
}