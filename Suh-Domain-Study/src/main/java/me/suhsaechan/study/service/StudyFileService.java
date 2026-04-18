package me.suhsaechan.study.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.properties.MinioProperties;
import me.suhsaechan.common.util.MinioUtil;
import me.suhsaechan.study.dto.AttachmentDto;
import me.suhsaechan.study.dto.StudyResponse;
import me.suhsaechan.study.entity.StudyAttachment;
import me.suhsaechan.study.entity.StudyPost;
import me.suhsaechan.study.repository.StudyAttachmentRepository;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyFileService {

    private final StudyAttachmentRepository attachmentRepository;
    private final MinioUtil minioUtil;
    private final MinioProperties minioProperties;

    @Transactional
    public List<AttachmentDto> processAndSaveFiles(List<MultipartFile> files, StudyPost post) {
        List<AttachmentDto> savedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);
            String storedFilename = UUID.randomUUID() + "." + extension;
            String objectName = MinioUtil.PUBLIC_IMAGES_PATH + storedFilename;

            minioUtil.uploadFile(minioProperties.getBucket(), objectName, file);

            String fileUrl = "/" + minioProperties.getBucket() + "/" + objectName;

            StudyAttachment attachment = StudyAttachment.builder()
                    .originalFilename(originalFilename)
                    .storedFilename(storedFilename)
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .fileExtension(extension)
                    .post(post)
                    .build();

            attachment.updateFileType(file.getContentType(), extension);

            StudyAttachment savedAttachment = attachmentRepository.save(attachment);
            savedFiles.add(convertToAttachmentDto(savedAttachment));
        }

        return savedFiles;
    }

    @Transactional
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + "." + extension;
        String objectName = MinioUtil.PUBLIC_IMAGES_PATH + storedFilename;

        minioUtil.uploadFile(minioProperties.getBucket(), objectName, file);

        return "/" + minioProperties.getBucket() + "/" + objectName;
    }

    public StudyResponse getAttachmentsByPostId(UUID postId) {
        List<StudyAttachment> attachments = attachmentRepository.findByPostStudyPostId(postId);
        List<AttachmentDto> attachmentDtos = attachments.stream()
                .map(this::convertToAttachmentDto)
                .collect(Collectors.toList());

        return StudyResponse.builder()
                .attachments(attachmentDtos)
                .build();
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        StudyAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ATTACHMENT_NOT_FOUND));

        String objectName = MinioUtil.PUBLIC_IMAGES_PATH + attachment.getStoredFilename();
        try {
            minioUtil.deleteFile(minioProperties.getBucket(), objectName);
        } catch (Exception e) {
            log.error("MinIO 파일 삭제 실패 - object: {}, error: {}", objectName, e.getMessage(), e);
        }

        attachmentRepository.delete(attachment);
    }

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
