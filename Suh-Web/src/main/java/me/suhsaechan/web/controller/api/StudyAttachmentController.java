package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.study.dto.AttachmentDto;
import me.suhsaechan.study.dto.StudyRequest;
import me.suhsaechan.study.dto.StudyResponse;
import me.suhsaechan.study.entity.StudyPost;
import me.suhsaechan.study.repository.StudyPostRepository;
import me.suhsaechan.study.service.StudyFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 스터디 첨부 파일 관리 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/study/attachment")
@RequiredArgsConstructor
public class StudyAttachmentController {

    private final StudyFileService fileService;
    private final StudyPostRepository postRepository;
    
    @Value("${file.domain}")
    private String fileDomain; // https://suh-project.synology.me
    
    @Value("${file.dir}")
    private String fileDir;

    /**
     * 파일 업로드 엔드포인트
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam(value = "files") MultipartFile file,
                                                        @RequestParam(value = "id", required = false) UUID postId) {
        Map<String, String> response = new HashMap<>();
        
        // 파일 확장자 처리
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        
        // UUID 생성 (StudyFileService와 동일한 파일명 사용)
        String storedFilename = UUID.randomUUID().toString() + "." + extension;
        
        // 특정 포스트에 파일 첨부 (포스트 ID가 있는 경우)
        String fileUrlPath = "/" + fileDir + "/" + storedFilename;
        
        if (postId != null) {
            StudyPost post = postRepository.findById(postId).orElse(null);
            if (post != null) {
                List<MultipartFile> files = new ArrayList<>();
                files.add(file);
                List<AttachmentDto> attachments = fileService.processAndSaveFiles(files, post);
                
                // StudyFileService에서 생성한 파일 URL 사용
                if (!attachments.isEmpty()) {
                    fileUrlPath = attachments.get(0).getFileUrl();
                } else {
                    throw new RuntimeException("첨부 파일 저장 실패");
                }
            }
        } else {
            // 포스트 없는 경우 (임시 업로드)
            log.info("포스트 ID 없이 파일 업로드 - 임시 저장: {}", originalFilename);
        }
        
        // URL 응답에 추가
        response.put("fileUrl", fileUrlPath);
        
        // 디버깅용
        String fullFileUrl = fileDomain + fileUrlPath;
        log.info("업로드 파일 경로: {}", fileUrlPath);
        log.info("업로드 파일 전체 URL: {}", fullFileUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 포스트의 첨부 파일 목록 조회
     */
    @PostMapping(value = "/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> getAttachmentsByPostId(@ModelAttribute StudyRequest request) {
        return ResponseEntity.ok(fileService.getAttachmentsByPostId(request.getPostId()));
    }

    /**
     * 첨부 파일 삭제
     */
    @PostMapping(value = "/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> deleteAttachment(@ModelAttribute StudyRequest request) {
        fileService.deleteAttachment(request.getId());
        return ResponseEntity.noContent().build();
    }
}
