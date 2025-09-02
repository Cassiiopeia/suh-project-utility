package me.suhsaechan.study.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.study.dto.AttachmentDto;
import me.suhsaechan.study.dto.StudyResponse;
import me.suhsaechan.study.entity.StudyAttachment;
import me.suhsaechan.study.entity.StudyPost;
import me.suhsaechan.study.repository.StudyAttachmentRepository;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyFileService {

    private final StudyAttachmentRepository attachmentRepository;
    
    // 파일 저장 관리자
    @Autowired
    private FileStorageProvider fileStorageProvider;
    
    // 파일 업로드 디렉토리 및 URL 기본 경로
    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/upload";
    private final String FILE_URL_BASE = "/upload/";
    
    // 파일 접근을 위한 도메인 URL
    private final String FILE_DOMAIN = "http://suh-project.synology.me";

    /**
     * 파일 업로드 처리 및 저장
     * @param files 업로드할 파일 목록
     * @param post 첨부 대상 포스트
     * @return 저장된 첨부 파일 목록
     */
    @Transactional
    public List<AttachmentDto> processAndSaveFiles(List<MultipartFile> files, StudyPost post) {
        List<AttachmentDto> savedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            
            try {
                // 파일 정보 추출
                String originalFilename = file.getOriginalFilename();
                String extension = FilenameUtils.getExtension(originalFilename);
                String storedFilename = UUID.randomUUID().toString() + "." + extension;
                
                // 파일 메타데이터 저장
                StudyAttachment attachment = StudyAttachment.builder()
                        .originalFilename(originalFilename)
                        .storedFilename(storedFilename)
                        .fileUrl(FILE_URL_BASE + storedFilename)
                        .fileSize(file.getSize())
                        .fileType(file.getContentType())
                        .fileExtension(extension)
                        .post(post)
                        .build();
                
                // 파일 타입에 따라 이미지/비디오 여부 설정
                attachment.updateFileType(file.getContentType(), extension);
                
                // DB에 첨부 파일 정보 저장
                StudyAttachment savedAttachment = attachmentRepository.save(attachment);
                
                // 로컬 파일 시스템에 파일 저장
                saveFileToLocalSystem(file, storedFilename);
                
                // 저장된 첨부 파일 정보 변환
                savedFiles.add(convertToAttachmentDto(savedAttachment));
                
            } catch (IOException e) {
                log.error("파일 업로드 중 오류 발생: {}", e.getMessage(), e);
                throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
            }
        }
        
        return savedFiles;
    }

    /**
     * 로컬 파일 시스템에 파일 저장
     */
    private void saveFileToLocalSystem(MultipartFile file, String storedFilename) throws IOException {
        try {
            // FileStorageProvider를 사용하여 파일 저장 (SMB 사용)
            fileStorageProvider.saveFile(file, storedFilename);
            
            // 로깅
            log.info("파일이 시놀로지에 저장되었습니다: {}", storedFilename);
        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 공개 메서드 - 컨트롤러에서 직접 호출
     */
    @Transactional
    public void saveFile(MultipartFile file, String storedFilename) throws IOException {
        saveFileToLocalSystem(file, storedFilename);
    }

    /**
     * 첨부 파일 조회
     */
    public StudyResponse getAttachmentsByPostId(UUID postId) {
        List<StudyAttachment> attachments = attachmentRepository.findByPostStudyPostId(postId);
        List<AttachmentDto> attachmentDtos = attachments.stream()
                .map(this::convertToAttachmentDto)
                .collect(Collectors.toList());
                
        return StudyResponse.builder()
                .attachments(attachmentDtos)
                .build();
    }

    /**
     * 첨부 파일 삭제
     */
    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        StudyAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ATTACHMENT_NOT_FOUND));
                
        // 로컬 파일 시스템에서 파일 삭제
        try {
            deleteFileFromLocalSystem(attachment.getStoredFilename());
        } catch (Exception e) {
            log.error("로컬 시스템에서 파일 삭제 중 오류 발생: {}", e.getMessage(), e);
        }
        
        // DB에서 첨부 파일 정보 삭제
        attachmentRepository.delete(attachment);
    }

    /**
     * 로컬 파일 시스템에서 파일 삭제
     */
    private void deleteFileFromLocalSystem(String storedFilename) {
        try {
            boolean deleted = fileStorageProvider.deleteFile(storedFilename);
            if (deleted) {
                log.info("파일이 시놀로지에서 삭제되었습니다: {}", storedFilename);
            } else {
                log.warn("시놀로지 파일 삭제 실패: {}", storedFilename);
            }
        } catch (Exception e) {
            log.error("파일 삭제 중 오류 발생: {}", e.getMessage(), e);
        }
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