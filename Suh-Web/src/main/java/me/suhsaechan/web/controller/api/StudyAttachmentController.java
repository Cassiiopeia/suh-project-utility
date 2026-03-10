package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.properties.MinioProperties;
import me.suhsaechan.study.dto.AttachmentDto;
import me.suhsaechan.study.dto.StudyRequest;
import me.suhsaechan.study.dto.StudyResponse;
import me.suhsaechan.study.entity.StudyPost;
import me.suhsaechan.study.repository.StudyPostRepository;
import me.suhsaechan.study.service.StudyFileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/study/attachment")
@RequiredArgsConstructor
public class StudyAttachmentController {

    private final StudyFileService fileService;
    private final StudyPostRepository postRepository;
    private final MinioProperties minioProperties;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam(value = "files") MultipartFile file,
                                                        @RequestParam(value = "id", required = false) UUID postId) {
        Map<String, String> response = new HashMap<>();
        String fileUrlPath;

        if (postId != null) {
            StudyPost post = postRepository.findById(postId).orElse(null);
            if (post != null) {
                List<MultipartFile> files = new ArrayList<>();
                files.add(file);
                List<AttachmentDto> attachments = fileService.processAndSaveFiles(files, post);

                if (!attachments.isEmpty()) {
                    fileUrlPath = attachments.get(0).getFileUrl();
                } else {
                    throw new RuntimeException("첨부 파일 저장 실패");
                }
            } else {
                fileUrlPath = fileService.uploadFile(file);
            }
        } else {
            fileUrlPath = fileService.uploadFile(file);
        }

        response.put("fileUrl", fileUrlPath);

        String fullFileUrl = minioProperties.getPublicDomain() + fileUrlPath;
        log.info("업로드 파일 경로: {}", fileUrlPath);
        log.info("업로드 파일 전체 URL: {}", fullFileUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyResponse> getAttachmentsByPostId(@ModelAttribute StudyRequest request) {
        return ResponseEntity.ok(fileService.getAttachmentsByPostId(request.getPostId()));
    }

    @PostMapping(value = "/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> deleteAttachment(@ModelAttribute StudyRequest request) {
        fileService.deleteAttachment(request.getId());
        return ResponseEntity.noContent().build();
    }
}
