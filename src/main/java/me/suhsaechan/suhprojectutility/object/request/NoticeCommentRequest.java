package me.suhsaechan.suhprojectutility.object.request;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCommentRequest {
    
    private UUID noticeId;
    private UUID commentId;
    private String author;
    private String content;
    private String authorIp;
    
    @Builder.Default
    private String authorName = "익명";
    // IP는 서버에서 자동으로 가져옴
} 