package me.suhsaechan.study.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.suhsaechan.common.entity.BasePostgresEntity;

import java.util.UUID;

/**
 * StudyAttachment는 스터디 포스트에 첨부되는 파일 정보를 관리하는 엔티티입니다.
 * 이미지, 동영상, 문서 등 다양한 파일을 포스트에 첨부할 수 있습니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyAttachment extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID studyAttachmentId;

    /**
     * 원본 파일명
     */
    @Column(nullable = false)
    private String originalFilename;

    /**
     * 저장된 파일명 (UUID 기반)
     */
    @Column(nullable = false)
    private String storedFilename;

    /**
     * 파일 URL
     */
    @Column(nullable = false)
    private String fileUrl;

    /**
     * 파일 크기 (바이트)
     */
    private Long fileSize;

    /**
     * 파일 타입 (MIME 타입)
     */
    private String fileType;

    /**
     * 파일 확장자
     */
    private String fileExtension;

    /**
     * 속한 포스트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private StudyPost post;

    /**
     * 이미지 파일 여부
     */
    @Builder.Default
    private Boolean isImage = false;

    /**
     * 비디오 파일 여부
     */
    @Builder.Default
    private Boolean isVideo = false;

    /**
     * 마크다운 내 표시 순서
     */
    private Integer displayOrder;

    /**
     * 포스트 설정
     */
    public void setPost(StudyPost post) {
        this.post = post;
    }

    /**
     * 파일 타입 업데이트
     */
    public void updateFileType(String fileType, String fileExtension) {
        this.fileType = fileType;
        this.fileExtension = fileExtension;
        
        // 파일 타입에 따라 이미지/비디오 구분
        if (fileType != null) {
            this.isImage = fileType.startsWith("image/");
            this.isVideo = fileType.startsWith("video/");
        }
    }
}
