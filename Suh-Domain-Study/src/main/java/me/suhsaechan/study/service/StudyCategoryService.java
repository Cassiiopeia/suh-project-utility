package me.suhsaechan.study.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.study.dto.CategoryDto;
import me.suhsaechan.study.dto.StudyRequest;
import me.suhsaechan.study.dto.StudyResponse;
import me.suhsaechan.study.entity.StudyCategory;
import me.suhsaechan.study.repository.StudyCategoryRepository;
import me.suhsaechan.study.repository.StudyPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyCategoryService {

    private final StudyCategoryRepository categoryRepository;
    private final StudyPostRepository postRepository;

    /**
     * 모든 카테고리를 계층 구조로 조회
     */
    public StudyResponse getAllCategories() {
        List<StudyCategory> rootCategories = categoryRepository.findByParentIsNullOrderByDisplayOrderAsc();
        List<CategoryDto> categoryDtos = rootCategories.stream()
                .map(this::convertToCategoryDtoWithChildren)
                .collect(Collectors.toList());
                
        return StudyResponse.builder()
                .categories(categoryDtos)
                .build();
    }

    /**
     * 특정 카테고리 조회
     */
    public StudyResponse getCategory(UUID categoryId) {
        StudyCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORIES_NOT_FOUND));
                
        CategoryDto categoryDto = convertToCategoryDto(category);
        
        // 포스트 수 조회
        long postCount = postRepository.countByCategoryStudyCategoryId(categoryId);
        categoryDto.setPostCount(postCount);
        
        return StudyResponse.builder()
                .category(categoryDto)
                .build();
    }

    /**
     * 새 카테고리 생성
     */
    @Transactional
    public StudyResponse createCategory(StudyRequest request) {
        StudyCategory parent = null;
        
        // 부모 카테고리가 지정된 경우 조회
        if (request.getParentCategoryId() != null) {
            parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        
        // 이름 중복 체크
        Optional<StudyCategory> existingCategory = categoryRepository.findByName(request.getCategoryName());
        if (existingCategory.isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }
        
        // 새 카테고리 생성
        StudyCategory category = StudyCategory.builder()
                .name(request.getCategoryName())
                .description(request.getCategoryDescription())
                .icon(request.getCategoryIcon())
                .color(request.getCategoryColor())
                .displayOrder(request.getCategoryDisplayOrder())
                .parent(parent)
                .build();
                
        StudyCategory savedCategory = categoryRepository.save(category);
        
        return StudyResponse.builder()
                .category(convertToCategoryDto(savedCategory))
                .build();
    }

    /**
     * 카테고리 수정
     */
    @Transactional
    public StudyResponse updateCategory(UUID categoryId, StudyRequest request) {
        StudyCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORIES_NOT_FOUND));
                
        // 이름 변경 시 중복 체크
        if (!category.getName().equals(request.getCategoryName())) {
            Optional<StudyCategory> existingCategory = categoryRepository.findByName(request.getCategoryName());
            if (existingCategory.isPresent() && !existingCategory.get().getStudyCategoryId().equals(categoryId)) {
                throw new CustomException(ErrorCode.DUPLICATE_CATEGORIES);
            }
        }
        
        // 카테고리 정보 수정
        category.update(
            request.getCategoryName(),
            request.getCategoryDescription(),
            request.getCategoryIcon(),
            request.getCategoryColor(),
            request.getCategoryDisplayOrder()
        );
        
        StudyCategory updatedCategory = categoryRepository.save(category);
        
        return StudyResponse.builder()
                .category(convertToCategoryDto(updatedCategory))
                .build();
    }

    /**
     * 카테고리 삭제
     */
    @Transactional
    public void deleteCategory(UUID categoryId) {
        StudyCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORIES_NOT_FOUND));
                
        // 자식 카테고리 확인
        if (!category.getChildren().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_DELETE_CHILD_CATEGORIES_EXISTS);
        }
        
        // 포스트 확인
        long postCount = postRepository.countByCategoryStudyCategoryId(categoryId);
        if (postCount > 0) {
            throw new CustomException(ErrorCode.INVALID_DELETE_CATEGORIES_POST_EXISTS);
        }
        
        categoryRepository.delete(category);
    }

    /**
     * StudyCategory 엔티티를 CategoryDto로 변환
     */
    private CategoryDto convertToCategoryDto(StudyCategory category) {
        CategoryDto dto = CategoryDto.builder()
                .id(category.getStudyCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .color(category.getColor())
                .displayOrder(category.getDisplayOrder())
                .createdDate(category.getCreatedDate())
                .modifiedDate(category.getModifiedDate())
                .build();
                
        // 부모 정보 설정
        if (category.getParent() != null) {
            dto.setParentId(category.getParent().getStudyCategoryId());
            dto.setParentName(category.getParent().getName());
        }
        
        return dto;
    }

    /**
     * 카테고리와 하위 카테고리를 포함한 DTO로 변환
     */
    private CategoryDto convertToCategoryDtoWithChildren(StudyCategory category) {
        CategoryDto dto = convertToCategoryDto(category);
        
        // 하위 카테고리 변환 및 추가
        List<CategoryDto> childrenDtos = new ArrayList<>();
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            childrenDtos = category.getChildren().stream()
                    .map(this::convertToCategoryDtoWithChildren)
                    .collect(Collectors.toList());
        }
        dto.setChildren(childrenDtos);
        
        // 포스트 수 설정
        long postCount = postRepository.countByCategoryStudyCategoryId(category.getStudyCategoryId());
        dto.setPostCount(postCount);
        
        return dto;
    }
}
