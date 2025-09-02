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
 * 카테고리 정보를 담는 독립적인 DTO 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private UUID id;
    private String name;
    private String description;
    private String icon;
    private String color;
    private Integer displayOrder;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private UUID parentId;
    private String parentName;
    @Builder.Default
    private List<CategoryDto> children = new ArrayList<>();
    private Long postCount;
}