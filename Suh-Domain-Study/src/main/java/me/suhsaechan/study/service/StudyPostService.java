package me.suhsaechan.study.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.study.dto.AttachmentDto;
import me.suhsaechan.study.dto.PostDto;
import me.suhsaechan.study.dto.StudyRequest;
import me.suhsaechan.study.dto.StudyResponse;
import me.suhsaechan.study.entity.StudyAttachment;
import me.suhsaechan.study.entity.StudyCategory;
import me.suhsaechan.study.entity.StudyPost;
import me.suhsaechan.study.repository.StudyAttachmentRepository;
import me.suhsaechan.study.repository.StudyCategoryRepository;
import me.suhsaechan.study.repository.StudyPostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyPostService {

    private final StudyPostRepository postRepository;
    private final StudyCategoryRepository categoryRepository;
    private final StudyAttachmentRepository attachmentRepository;
    private final StudyFileService fileService;

    /**
     * 특정 카테고리의 포스트 목록 조회 (페이징)
     */
    public StudyResponse getPostsByCategoryId(UUID categoryId, StudyRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 10;
        String sortBy = StringUtils.hasText(request.getSortBy()) ? request.getSortBy() : "createdDate";
        String direction = StringUtils.hasText(request.getSortDirection()) ? request.getSortDirection() : "DESC";
        
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<StudyPost> postsPage = postRepository.findByCategoryStudyCategoryId(categoryId, pageable);
        List<PostDto> postDtos = postsPage.getContent().stream()
                .map(this::convertToPostDto)
                .collect(Collectors.toList());
                
        return StudyResponse.builder()
                .posts(postDtos)
                .totalPosts(postsPage.getTotalElements())
                .totalPages(postsPage.getTotalPages())
                .build();
    }

    /**
     * 특정 포스트 조회 (조회수 증가)
     */
    @Transactional
    public StudyResponse getPost(UUID postId) {
        StudyPost post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
                
        // 조회수 증가
        post.incrementViewCount();
        postRepository.save(post);
        
        return StudyResponse.builder()
                .post(convertToPostDto(post))
                .build();
    }

    /**
     * 새 포스트 생성
     */
    @Transactional
    public StudyResponse createPost(StudyRequest request) {
        StudyCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORIES_NOT_FOUND));
                
        // 포스트 생성
        StudyPost post = StudyPost.builder()
                .title(request.getPostTitle())
                .content(request.getPostContent())
                .summary(request.getPostSummary())
                .author(request.getPostAuthor())
                .tags(request.getPostTags())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .category(category)
                .viewCount(0)
                .build();
                
        StudyPost savedPost = postRepository.save(post);
        
        // 첨부 파일 처리
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            fileService.processAndSaveFiles(request.getFiles(), savedPost);
        }
        
        return StudyResponse.builder()
                .post(convertToPostDto(savedPost))
                .build();
    }

    /**
     * 포스트 수정
     */
    @Transactional
    public StudyResponse updatePost(UUID postId, StudyRequest request) {
        StudyPost post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
                
        // 포스트 정보 수정
        post.update(
            request.getPostTitle(),
            request.getPostContent(),
            request.getPostSummary(),
            request.getPostTags(),
            request.getIsPublic()
        );
        
        // 카테고리 변경이 필요한 경우
        if (request.getCategoryId() != null && !post.getCategory().getStudyCategoryId().equals(request.getCategoryId())) {
            StudyCategory newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CustomException(ErrorCode.CATEGORIES_NOT_FOUND));
            post.setCategory(newCategory);
        }
        
        StudyPost updatedPost = postRepository.save(post);
        
        // 새 첨부 파일 처리
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            fileService.processAndSaveFiles(request.getFiles(), updatedPost);
        }
        
        // 삭제할 첨부 파일 처리
        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            for (UUID attachmentId : request.getAttachmentIds()) {
                fileService.deleteAttachment(attachmentId);
            }
        }
        
        return StudyResponse.builder()
                .post(convertToPostDto(updatedPost))
                .build();
    }

    /**
     * 포스트 삭제
     */
    @Transactional
    public void deletePost(UUID postId) {
        StudyPost post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
                
        // 첨부 파일 삭제
        List<StudyAttachment> attachments = attachmentRepository.findByPostStudyPostId(postId);
        for (StudyAttachment attachment : attachments) {
            fileService.deleteAttachment(attachment.getStudyAttachmentId());
        }
        
        postRepository.delete(post);
    }

    /**
     * 키워드로 포스트 검색
     */
    public StudyResponse searchPosts(StudyRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 10;
        String sortBy = StringUtils.hasText(request.getSortBy()) ? request.getSortBy() : "createdDate";
        String direction = StringUtils.hasText(request.getSortDirection()) ? request.getSortDirection() : "DESC";
        
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<StudyPost> postsPage;
        if (StringUtils.hasText(request.getKeyword())) {
            postsPage = postRepository.searchByKeyword(request.getKeyword(), pageable);
        } else if (StringUtils.hasText(request.getTag())) {
            postsPage = postRepository.findByTag(request.getTag(), pageable);
        } else {
            throw new CustomException(ErrorCode.INVALID_STUDY_REQUEST_PARAMETER);
        }
        
        List<PostDto> postDtos = postsPage.getContent().stream()
                .map(this::convertToPostDto)
                .collect(Collectors.toList());
                
        return StudyResponse.builder()
                .posts(postDtos)
                .totalPosts(postsPage.getTotalElements())
                .totalPages(postsPage.getTotalPages())
                .build();
    }

    /**
     * 최근 포스트 조회
     */
    public StudyResponse getRecentPosts() {
        List<StudyPost> recentPosts = postRepository.findTop5ByIsPublicTrueOrderByCreatedDateDesc();
        List<PostDto> postDtos = recentPosts.stream()
                .map(this::convertToPostDto)
                .collect(Collectors.toList());
                
        return StudyResponse.builder()
                .posts(postDtos)
                .build();
    }

    /**
     * 인기 포스트 조회 (조회수 기준)
     */
    public StudyResponse getPopularPosts() {
        List<StudyPost> popularPosts = postRepository.findTop5ByIsPublicTrueOrderByViewCountDesc();
        List<PostDto> postDtos = popularPosts.stream()
                .map(this::convertToPostDto)
                .collect(Collectors.toList());
                
        return StudyResponse.builder()
                .posts(postDtos)
                .build();
    }

    /**
     * StudyPost 엔티티를 PostDto로 변환
     */
    private PostDto convertToPostDto(StudyPost post) {
        // 태그 목록 변환
        List<String> tagList = StringUtils.hasText(post.getTags()) ? 
                Arrays.asList(post.getTags().split(",")) : 
                List.of();
                
        // 첨부 파일 목록 조회 및 변환
        List<StudyAttachment> attachments = attachmentRepository.findByPostStudyPostIdOrderByDisplayOrderAsc(post.getStudyPostId());
        List<AttachmentDto> attachmentDtos = attachments.stream()
                .map(this::convertToAttachmentDto)
                .collect(Collectors.toList());
                
        return PostDto.builder()
                .id(post.getStudyPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .summary(post.getSummary())
                .author(post.getAuthor())
                .viewCount(post.getViewCount())
                .isPublic(post.getIsPublic())
                .tags(post.getTags())
                .tagList(tagList)
                .createdDate(post.getCreatedDate())
                .categoryId(post.getCategory().getStudyCategoryId())
                .categoryName(post.getCategory().getName())
                .attachments(attachmentDtos)
                .build();
    }

    /**
     * StudyAttachment 엔티티를 AttachmentDto로 변환
     */
    private AttachmentDto convertToAttachmentDto(StudyAttachment attachment) {
        return AttachmentDto.builder()
                .id(attachment.getStudyAttachmentId())
                .originalFilename(attachment.getOriginalFilename())
                .storedFilename(attachment.getStoredFilename())
                .fileUrl(attachment.getFileUrl())
                .fileSize(attachment.getFileSize())
                .fileType(attachment.getFileType())
                .fileExtension(attachment.getFileExtension())
                .isImage(attachment.getIsImage())
                .isVideo(attachment.getIsVideo())
                .displayOrder(attachment.getDisplayOrder())
                .createdDate(attachment.getCreatedDate())
                .build();
    }
}
