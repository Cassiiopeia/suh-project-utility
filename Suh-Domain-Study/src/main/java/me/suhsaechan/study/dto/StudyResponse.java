package me.suhsaechan.study.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 스터디 관련 응답을 처리하는 통합 응답 DTO 클래스
 * 모듈의 모든 API 엔드포인트는 이 단일 응답 클래스를 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyResponse {
    
    // 카테고리 응답
    private List<CategoryDto> categories;
    private CategoryDto category;
    
    // 포스트 응답
    private List<PostDto> posts;
    private PostDto post;
    private Long totalPosts;
    private Integer totalPages;
    
    // 첨부 파일 응답
    private List<AttachmentDto> attachments;
    private AttachmentDto attachment;
    
    // 파일 업로드 응답
    private String fileUrl;
}