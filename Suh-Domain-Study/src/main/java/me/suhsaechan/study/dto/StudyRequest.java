package me.suhsaechan.study.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 스터디 관련 요청을 처리하는 통합 요청 DTO 클래스
 * 모듈의 모든 API 엔드포인트는 이 단일 요청 클래스를 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyRequest {
    
    // 공통 필드
    private UUID id;
    
    // 카테고리 관련 필드
    private String categoryName;
    private String categoryDescription;
    private String categoryIcon;
    private String categoryColor;
    private Integer categoryDisplayOrder;
    private UUID parentCategoryId;
    
    // 포스트 관련 필드
    private String postTitle;
    private String postContent;
    private String postSummary;
    private String postAuthor;
    private String postTags;
    private Boolean isPublic;
    private UUID categoryId;
    
    // 첨부 파일 관련 필드
    private List<MultipartFile> files;
    private UUID postId;
    private List<UUID> attachmentIds;
    
    // 페이지네이션
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
    
    // 검색
    private String keyword;
    private String tag;
}