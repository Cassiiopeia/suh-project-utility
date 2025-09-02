package me.suhsaechan.study.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 첨부 파일 정보를 담는 독립적인 DTO 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    private UUID id;
    private String originalFilename;
    private String storedFilename;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String fileExtension;
    private Boolean isImage;
    private Boolean isVideo;
    private Integer displayOrder;
    private LocalDateTime createdDate;
}