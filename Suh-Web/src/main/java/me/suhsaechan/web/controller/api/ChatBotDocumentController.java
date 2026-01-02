package me.suhsaechan.web.controller.api;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.dto.ChatBotDocumentDto;
import me.suhsaechan.chatbot.dto.ChatBotDocumentRequest;
import me.suhsaechan.chatbot.dto.ChatBotDocumentResponse;
import me.suhsaechan.chatbot.entity.ChatDocument;
import me.suhsaechan.chatbot.service.DocumentService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 챗봇 문서 관리 API 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot/document")
public class ChatBotDocumentController {

  private final DocumentService documentService;

  /**
   * 모든 문서 목록 조회
   */
  @PostMapping(value = "/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<ChatBotDocumentResponse> getAllDocuments() {
    List<ChatDocument> documents = documentService.getAllActiveDocuments();
    List<ChatBotDocumentDto> documentDtos = documents.stream()
        .map(ChatBotDocumentDto::from)
        .collect(Collectors.toList());

    return ResponseEntity.ok(ChatBotDocumentResponse.builder()
        .documents(documentDtos)
        .totalCount(documentDtos.size())
        .build());
  }

  /**
   * 문서 상세 조회
   */
  @PostMapping(value = "/detail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<ChatBotDocumentResponse> getDocument(@ModelAttribute ChatBotDocumentRequest request) {
    ChatDocument document = documentService.getDocument(request.getDocumentId());
    return ResponseEntity.ok(ChatBotDocumentResponse.builder()
        .document(ChatBotDocumentDto.from(document))
        .build());
  }

  /**
   * 문서 생성
   */
  @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<ChatBotDocumentResponse> createDocument(@ModelAttribute ChatBotDocumentRequest request) {
    ChatDocument document = documentService.createDocument(
        request.getTitle(),
        request.getCategory(),
        request.getContent(),
        request.getDescription()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(ChatBotDocumentResponse.builder()
        .document(ChatBotDocumentDto.from(document))
        .build());
  }

  /**
   * 문서 수정
   */
  @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<ChatBotDocumentResponse> updateDocument(@ModelAttribute ChatBotDocumentRequest request) {
    ChatDocument document = documentService.updateDocument(
        request.getDocumentId(),
        request.getTitle(),
        request.getCategory(),
        request.getContent(),
        request.getDescription()
    );
    return ResponseEntity.ok(ChatBotDocumentResponse.builder()
        .document(ChatBotDocumentDto.from(document))
        .build());
  }

  /**
   * 문서 삭제
   */
  @PostMapping(value = "/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> deleteDocument(@ModelAttribute ChatBotDocumentRequest request) {
    documentService.deleteDocument(request.getDocumentId());
    return ResponseEntity.noContent().build();
  }

  /**
   * 문서 활성화/비활성화 토글
   */
  @PostMapping(value = "/toggle-active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> toggleDocumentActive(@ModelAttribute ChatBotDocumentRequest request) {
    ChatDocument document = documentService.getDocument(request.getDocumentId());
    documentService.setDocumentActive(request.getDocumentId(), !document.getIsActive());
    return ResponseEntity.noContent().build();
  }

  /**
   * 문서 재처리 (청크 재생성 + 벡터화)
   */
  @PostMapping(value = "/reprocess", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<ChatBotDocumentResponse> reprocessDocument(@ModelAttribute ChatBotDocumentRequest request) {
    ChatDocument document = documentService.getDocument(request.getDocumentId());
    documentService.processDocument(document);
    ChatDocument reprocessed = documentService.getDocument(request.getDocumentId());
    return ResponseEntity.ok(ChatBotDocumentResponse.builder()
        .document(ChatBotDocumentDto.from(reprocessed))
        .build());
  }
}

