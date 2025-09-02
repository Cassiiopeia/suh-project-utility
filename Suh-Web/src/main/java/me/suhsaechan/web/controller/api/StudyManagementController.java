package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.study.dto.StudyRequest;
import me.suhsaechan.study.dto.StudyResponse;
import me.suhsaechan.study.service.StudyCategoryService;
import me.suhsaechan.study.service.StudyFileService;
import me.suhsaechan.study.service.StudyPostService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스터디 관리 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyManagementController {

    private final StudyCategoryService categoryService;
    private final StudyPostService postService;
    private final StudyFileService fileService;

    // 카테고리 관련 엔드포인트 =================================================

    /**
     * 모든 카테고리 조회
     */
    @PostMapping(value = "/category/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * 특정 카테고리 조회
     */
    @PostMapping(value = "/category/get", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> getCategory(@ModelAttribute StudyRequest request) {
        return ResponseEntity.ok(categoryService.getCategory(request.getId()));
    }

    /**
     * 카테고리 생성
     */
    @PostMapping(value = "/category/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> createCategory(@ModelAttribute StudyRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    /**
     * 카테고리 수정
     */
    @PostMapping(value = "/category/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> updateCategory(@ModelAttribute StudyRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(request.getId(), request));
    }

    /**
     * 카테고리 삭제
     */
    @PostMapping(value = "/category/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> deleteCategory(@ModelAttribute StudyRequest request) {
        categoryService.deleteCategory(request.getId());
        return ResponseEntity.ok().build();
    }

    // 포스트 관련 엔드포인트 =================================================

    /**
     * 특정 카테고리의 포스트 목록 조회
     */
    @PostMapping(value = "/post/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> getPostsByCategoryId(@ModelAttribute StudyRequest request) {
        return ResponseEntity.ok(postService.getPostsByCategoryId(request.getCategoryId(), request));
    }

    /**
     * 특정 포스트 조회 (조회수 증가)
     */
    @PostMapping(value = "/post/get", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> getPost(@ModelAttribute StudyRequest request) {
        return ResponseEntity.ok(postService.getPost(request.getId()));
    }

    /**
     * 포스트 생성
     */
    @PostMapping(value = "/post/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> createPost(@ModelAttribute StudyRequest request) {
        return ResponseEntity.ok(postService.createPost(request));
    }

    /**
     * 포스트 수정
     */
    @PostMapping(value = "/post/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> updatePost(@ModelAttribute StudyRequest request) {
        return ResponseEntity.ok(postService.updatePost(request.getId(), request));
    }

    /**
     * 포스트 삭제
     */
    @PostMapping(value = "/post/delete")
    public ResponseEntity<Void> deletePost(@RequestBody StudyRequest request) {
        postService.deletePost(request.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 포스트 검색
     */
    @PostMapping(value = "/post/search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> searchPosts(@ModelAttribute StudyRequest request) {
        return ResponseEntity.ok(postService.searchPosts(request));
    }

    /**
     * 최근 포스트 조회
     */
    @PostMapping(value = "/post/recent", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> getRecentPosts() {
        return ResponseEntity.ok(postService.getRecentPosts());
    }

    /**
     * 인기 포스트 조회 (조회수 기준)
     */
    @PostMapping(value = "/post/popular", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> getPopularPosts() {
        return ResponseEntity.ok(postService.getPopularPosts());
    }

    // 첨부 파일 관련 엔드포인트는 StudyAttachmentController로 이동
}
