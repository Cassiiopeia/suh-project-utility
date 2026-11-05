package me.suhsaechan.web.controller.api;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.clipboard.dto.ClipboardFileDto;
import me.suhsaechan.clipboard.dto.ClipboardRequest;
import me.suhsaechan.clipboard.dto.ClipboardResponse;
import me.suhsaechan.clipboard.service.ClipboardService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clipboard")
public class ClipboardController {

  private final ClipboardService clipboardService;

  @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<ClipboardResponse> createItem(@ModelAttribute ClipboardRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(clipboardService.createItem(request));
  }

  @PostMapping(value = "/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<ClipboardResponse> getItems() {
    return ResponseEntity.ok(clipboardService.getItems());
  }

  @PostMapping(value = "/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> deleteItem(@ModelAttribute ClipboardRequest request) {
    clipboardService.deleteItem(request.getClipboardItemId());
    return ResponseEntity.noContent().build();
  }

  // <img src>·내려받기 링크에서 직접 쓰도록 GET (이미지 inline, 그 외 attachment)
  @GetMapping("/file/{clipboardItemId}")
  public ResponseEntity<Resource> getFile(@PathVariable UUID clipboardItemId) {
    ClipboardFileDto file = clipboardService.loadFile(clipboardItemId);
    ContentDisposition contentDisposition = (Boolean.TRUE.equals(file.getIsImage())
        ? ContentDisposition.inline()
        : ContentDisposition.attachment())
        .filename(file.getOriginalFileName(), StandardCharsets.UTF_8)
        .build();
    // 사용자 업로드 본문: 스니핑 차단 + 직접 열어도 스크립트 실행 차단 (SVG 등)
    return ResponseEntity.ok()
        .contentType(toMediaType(file.getContentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "sandbox")
        .body(new FileSystemResource(file.getFilePath()));
  }

  // 저장된 contentType > MediaType (형식 오류면 octet-stream)
  private MediaType toMediaType(String contentType) {
    try {
      return MediaType.parseMediaType(contentType);
    } catch (InvalidMediaTypeException e) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }
}
