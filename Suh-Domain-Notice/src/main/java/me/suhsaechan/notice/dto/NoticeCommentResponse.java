package me.suhsaechan.notice.object.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.suhsaechan.common.entity.NoticeComment;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCommentResponse {
    private UUID noticeCommentId;
    private String author;
    private String content;
    private String anonymizedIp;
    private LocalDateTime createdDate;
    private boolean canDelete;
    
    public static NoticeCommentResponse from(NoticeComment comment, boolean canDelete) {
        return NoticeCommentResponse.builder()
                .noticeCommentId(comment.getNoticeCommentId())
                .author(comment.getAuthor())
                .content(comment.getContent())
                .anonymizedIp(comment.getAnonymizedIp())
                .createdDate(comment.getCreatedDate())
                .canDelete(canDelete)
                .build();
    }
} 