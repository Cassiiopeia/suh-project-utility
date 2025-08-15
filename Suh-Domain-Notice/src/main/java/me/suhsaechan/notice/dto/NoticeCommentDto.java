package me.suhsaechan.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCommentDto {
    private UUID noticeCommentId;
    private String author;
    private String content;
    private String anonymizedIp;
    private String clientHash;  // 클라이언트 해시값
    private LocalDateTime createdDate;
    private boolean canDelete;
}
