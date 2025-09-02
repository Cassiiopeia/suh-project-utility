package me.suhsaechan.web.controller.view;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.study.dto.StudyRequest;
import me.suhsaechan.study.dto.StudyResponse;
import me.suhsaechan.study.service.StudyCategoryService;
import me.suhsaechan.study.service.StudyPostService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import java.util.UUID;

/**
 * 스터디 관리 뷰 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/study-management")
public class StudyViewController {

    private final StudyCategoryService categoryService;
    private final StudyPostService postService;

    /**
     * 스터디 관리 메인 페이지 - 카테고리 목록 표시
     */
    @GetMapping({"", "/"})
    public String getStudyManagementPage(Model model) {
        StudyResponse categoriesResponse = categoryService.getAllCategories();
        model.addAttribute("categoriesResponse", categoriesResponse);
        
        // 최근 포스트와 인기 포스트 추가
        model.addAttribute("recentPosts", postService.getRecentPosts().getPosts());
        model.addAttribute("popularPosts", postService.getPopularPosts().getPosts());
        
        return "pages/study/studyManagementMain";
    }

    /**
     * 카테고리 상세 페이지 - 포스트 목록 표시
     */
    @GetMapping("/category/{categoryId}")
    public String getCategoryPage(@PathVariable UUID categoryId, 
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  Model model) {
        // 카테고리 정보 조회
        StudyResponse categoryResponse = categoryService.getCategory(categoryId);
        model.addAttribute("category", categoryResponse.getCategory());
        
        // 포스트 목록 조회 (페이징)
        StudyRequest request = StudyRequest.builder()
                .categoryId(categoryId)
                .page(page)
                .size(size)
                .build();
        StudyResponse postsResponse = postService.getPostsByCategoryId(categoryId, request);
        model.addAttribute("posts", postsResponse.getPosts());
        model.addAttribute("totalPages", postsResponse.getTotalPages());
        model.addAttribute("currentPage", page);
        
        // 전체 카테고리 목록도 함께 전달
        model.addAttribute("categoriesResponse", categoryService.getAllCategories());
        
        return "pages/study/category";
    }

    /**
     * 포스트 상세 조회 페이지
     */
    @GetMapping("/post/{postId}")
    public String getPostPage(@PathVariable UUID postId, Model model) {
        // 포스트 정보 조회
        StudyResponse postResponse = postService.getPost(postId);
        model.addAttribute("post", postResponse.getPost());
        
        // 전체 카테고리 목록도 함께 전달
        model.addAttribute("categoriesResponse", categoryService.getAllCategories());
        
        return "pages/study/post-view";
    }

    /**
     * 포스트 작성 페이지
     */
    @GetMapping("/post/create")
    public String getPostCreatePage(@RequestParam(required = false) UUID categoryId, Model model) {
        if (categoryId != null) {
            // 카테고리 정보 조회
            StudyResponse categoryResponse = categoryService.getCategory(categoryId);
            model.addAttribute("category", categoryResponse.getCategory());
        }
        
        // 전체 카테고리 목록
        model.addAttribute("categoriesResponse", categoryService.getAllCategories());
        
        // 편집 모드 설정 (새 포스트 작성)
        model.addAttribute("isEdit", false);
        
        return "pages/study/post-form";
    }

    /**
     * 포스트 수정 페이지
     */
    @GetMapping("/post/{postId}/edit")
    public String getPostEditPage(@PathVariable UUID postId, Model model) {
        // 포스트 정보 조회
        StudyResponse postResponse = postService.getPost(postId);
        model.addAttribute("post", postResponse.getPost());
        model.addAttribute("isEdit", true);
        
        // 전체 카테고리 목록
        model.addAttribute("categoriesResponse", categoryService.getAllCategories());
        
        return "pages/study/post-form";
    }

    /**
     * 포스트 검색 결과 페이지
     */
    @GetMapping("/search")
    public String getSearchResultsPage(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String tag,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       Model model) {
        // 키워드와 태그가 모두 없는 경우 빈 결과 표시
        if ((keyword == null || keyword.trim().isEmpty()) && (tag == null || tag.trim().isEmpty())) {
            model.addAttribute("posts", List.of());
            model.addAttribute("totalPages", 0);
            model.addAttribute("currentPage", 0);
            model.addAttribute("keyword", "");
            model.addAttribute("tag", "");
            model.addAttribute("searchError", "검색 키워드나 태그를 입력해주세요");
        } else {
            StudyRequest request = StudyRequest.builder()
                    .keyword(keyword)
                    .tag(tag)
                    .page(page)
                    .size(size)
                    .build();
                    
            try {
                StudyResponse searchResponse = postService.searchPosts(request);
                model.addAttribute("posts", searchResponse.getPosts());
                model.addAttribute("totalPages", searchResponse.getTotalPages());
                model.addAttribute("currentPage", page);
                model.addAttribute("keyword", keyword);
                model.addAttribute("tag", tag);
            } catch (CustomException e) {
                model.addAttribute("posts", List.of());
                model.addAttribute("totalPages", 0);
                model.addAttribute("currentPage", 0);
                model.addAttribute("keyword", keyword);
                model.addAttribute("tag", tag);
                model.addAttribute("searchError", e.getMessage());
            }
        }
        
        // 전체 카테고리 목록
        model.addAttribute("categoriesResponse", categoryService.getAllCategories());
        
        return "pages/study/search-results";
    }

    /**
     * 카테고리 관리 페이지
     */
    @GetMapping("/category-management")
    public String getCategoryManagementPage(Model model) {
        StudyResponse categoriesResponse = categoryService.getAllCategories();
        model.addAttribute("categoriesResponse", categoriesResponse);
        
        return "pages/study/category-management";
    }
}
