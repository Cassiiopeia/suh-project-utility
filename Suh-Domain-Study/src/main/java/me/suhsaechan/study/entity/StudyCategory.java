package me.suhsaechan.study.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.suhsaechan.common.entity.BasePostgresEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * StudyCategory는 스터디 자료를 카테고리별로 구성하는 엔티티입니다.
 * 사용자가 블로그나 위키처럼 다양한 카테고리로 스터디 자료를 정리할 수 있습니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyCategory extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID studyCategoryId;

    /**
     * 카테고리 이름
     */
    @Column(nullable = false)
    private String name;

    /**
     * 카테고리 설명
     */
    @Column(length = 1000)
    private String description;

    /**
     * 카테고리 아이콘 (아이콘 이름)
     */
    private String icon;

    /**
     * 카테고리 색상 (CSS 색상값)
     */
    private String color;

    /**
     * 카테고리 표시 순서
     */
    private Integer displayOrder;

    /**
     * 부모 카테고리 (계층형 구조 지원)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private StudyCategory parent;

    /**
     * 자식 카테고리 목록 (계층형 구조 지원)
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudyCategory> children = new ArrayList<>();

    /**
     * 카테고리에 속한 포스트 목록
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudyPost> posts = new ArrayList<>();

    /**
     * 카테고리 정보 업데이트
     */
    public void update(String name, String description, String icon, String color, Integer displayOrder) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.displayOrder = displayOrder;
    }

    /**
     * 자식 카테고리 추가
     */
    public void addChild(StudyCategory child) {
        if (child != null) {
            this.children.add(child);
            child.setParent(this);
        }
    }

    /**
     * 부모 카테고리 설정
     */
    public void setParent(StudyCategory parent) {
        this.parent = parent;
    }

    /**
     * 포스트 추가
     */
    public void addPost(StudyPost post) {
        if (post != null) {
            this.posts.add(post);
            post.setCategory(this);
        }
    }
    
    /**
     * 수정일자 조회 - BasePostgresEntity의 updatedDate를 반환
     */
    public java.time.LocalDateTime getModifiedDate() {
        return getUpdatedDate();
    }
}
