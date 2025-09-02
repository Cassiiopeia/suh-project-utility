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
 * StudyPost는 스터디 포스트(글) 정보를 관리하는 엔티티입니다.
 * 사용자는 마크다운으로 작성된 내용을 포함한 스터디 노트를 작성할 수 있습니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyPost extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID studyPostId;

    /**
     * 포스트 제목
     */
    @Column(nullable = false)
    private String title;

    /**
     * 포스트 내용 (마크다운 형식)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 포스트 요약
     */
    @Column(length = 500)
    private String summary;

    /**
     * 작성자 정보
     */
    private String author;

    /**
     * 조회수
     */
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * 공개 여부
     */
    @Builder.Default
    private Boolean isPublic = true;

    /**
     * 포스트 태그 (쉼표로 구분)
     */
    @Column(length = 500)
    private String tags;

    /**
     * 속한 카테고리
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private StudyCategory category;

    /**
     * 포스트에 첨부된 파일 목록
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudyAttachment> attachments = new ArrayList<>();

    /**
     * 포스트 내용 업데이트
     */
    public void update(String title, String content, String summary, String tags, Boolean isPublic) {
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.tags = tags;
        this.isPublic = isPublic;
    }

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount += 1;
    }

    /**
     * 카테고리 설정
     */
    public void setCategory(StudyCategory category) {
        this.category = category;
    }

    /**
     * 첨부파일 추가
     */
    public void addAttachment(StudyAttachment attachment) {
        if (attachment != null) {
            this.attachments.add(attachment);
            attachment.setPost(this);
        }
    }

    /**
     * 특정 첨부파일 제거
     */
    public void removeAttachment(StudyAttachment attachment) {
        if (attachment != null) {
            this.attachments.remove(attachment);
            attachment.setPost(null);
        }
    }
    
    /**
     * 수정일자 조회 - BasePostgresEntity의 updatedDate를 반환
     */
    public java.time.LocalDateTime getModifiedDate() {
        return getUpdatedDate();
    }
}
