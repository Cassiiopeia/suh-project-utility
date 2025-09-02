package me.suhsaechan.study.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 포스트 정보를 담는 독립적인 DTO 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private UUID id;
    private String title;
    private String content;
    private String summary;
    private String author;
    private Integer viewCount;
    private Boolean isPublic;
    private String tags;
    private List<String> tagList;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private UUID categoryId;
    private String categoryName;
    @Builder.Default
    private List<AttachmentDto> attachments = new ArrayList<>();
}