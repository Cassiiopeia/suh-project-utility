package me.suhsaechan.web.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import kr.suhsaechan.ai.service.StreamCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.dto.ChatHistoryDto;
import me.suhsaechan.chatbot.dto.ChatbotRequest;
import me.suhsaechan.chatbot.dto.ChatbotResponse;
import me.suhsaechan.chatbot.dto.ChatbotConfigDto;
import me.suhsaechan.chatbot.dto.ChatbotConfigRequest;
import me.suhsaechan.chatbot.dto.DocumentDto;
import me.suhsaechan.chatbot.dto.DocumentRequest;
import me.suhsaechan.chatbot.dto.DocumentResponse;
import me.suhsaechan.chatbot.entity.ChatDocument;
import me.suhsaechan.chatbot.service.ChatbotConfigService;
import me.suhsaechan.chatbot.service.ChatbotService;
import me.suhsaechan.chatbot.service.DocumentService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 챗봇 API 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatbotController {

  private final ChatbotService chatbotService;
  private final DocumentService documentService;

  /**
   * 채팅 메시지 전송 (비스트리밍)
   */
  @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<ChatbotResponse> chat(@ModelAttribute ChatbotRequest request,
                                           HttpServletRequest httpRequest) {
    String userIp = extractClientIp(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");

    ChatbotResponse response = chatbotService.chat(request, userIp, userAgent);
    return ResponseEntity.ok(response);
  }

  /**
   * 스트리밍 채팅 (SSE)
   * 토큰 단위로 실시간 응답 전송
   */
  @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter chatStream(
          @RequestParam(required = false) String sessionToken,
          @RequestParam String message,
          @RequestParam(defaultValue = "3") int topK,
          @RequestParam(defaultValue = "0.5") float minScore,
          HttpServletRequest httpRequest) {

    log.info("스트리밍 채팅 요청 - message: {}, sessionToken: {}", message, sessionToken);

    String userIp = extractClientIp(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");

    // SSE 타임아웃 2분 (LLM 응답 시간 고려)
    SseEmitter emitter = new SseEmitter(120000L);

    // 연결 끊김 처리
    emitter.onCompletion(() -> log.debug("SSE 연결 완료"));
    emitter.onTimeout(() -> log.warn("SSE 타임아웃"));
    emitter.onError(e -> log.error("SSE 오류: {}", e.getMessage()));

    // 스트리밍 응답 시작 (세션 토큰 콜백 포함)
    chatbotService.chatStream(sessionToken, message, topK, minScore, userIp, userAgent,
        new StreamCallback() {
          @Override
          public void onNext(String chunk) {
            try {
              log.debug("SSE 청크 전송: [{}] ({} bytes)", chunk, chunk.length());
              // JSON 형식으로 감싸서 공백 보존
              String jsonData = "{\"text\":\"" + chunk.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"}";
              emitter.send(SseEmitter.event()
                  .name("message")
                  .data(jsonData));
            } catch (IOException e) {
              log.error("SSE 전송 오류: {}", e.getMessage());
              emitter.completeWithError(e);
            }
          }

          @Override
          public void onComplete() {
            try {
              log.debug("SSE 완료 이벤트 전송");
              emitter.send(SseEmitter.event()
                  .name("done")
                  .data(""));
              emitter.complete();
            } catch (IOException e) {
              log.error("SSE 완료 전송 오류: {}", e.getMessage());
              emitter.completeWithError(e);
            }
          }

          @Override
          public void onError(Throwable error) {
            log.error("스트리밍 오류: {}", error.getMessage());
            try {
              emitter.send(SseEmitter.event()
                  .name("error")
                  .data(error.getMessage()));
            } catch (IOException e) {
              log.error("SSE 오류 전송 실패: {}", e.getMessage());
            }
            emitter.completeWithError(error);
          }
        },
        // 세션 토큰 콜백 - SSE 연결 직후 세션 토큰을 프론트엔드에 전달
        (newSessionToken) -> {
          try {
            log.info("세션 토큰 전송 - sessionToken: {}", newSessionToken);
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("{\"sessionToken\":\"" + newSessionToken + "\"}"));
          } catch (IOException e) {
            log.error("세션 토큰 SSE 전송 실패: {}", e.getMessage());
          }
        });

    return emitter;
  }

  /**
   * 대화 이력 조회
   */
  @PostMapping(value = "/history", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<List<ChatHistoryDto>> getChatHistory(@ModelAttribute ChatbotRequest request) {
    List<ChatHistoryDto> history = chatbotService.getChatHistory(request.getSessionToken());
    return ResponseEntity.ok(history);
  }

  /**
   * 메시지 피드백 (좋아요/싫어요)
   */
  @PostMapping(value = "/feedback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> saveFeedback(@ModelAttribute FeedbackRequest request) {
    if (request.getMessageId() == null || request.getIsHelpful() == null) {
      return ResponseEntity.badRequest().build();
    }
    chatbotService.saveMessageFeedback(request.getMessageId(), request.getIsHelpful());
    return ResponseEntity.noContent().build();
  }

  /**
   * 세션 종료
   */
  @PostMapping(value = "/session/end", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> endSession(@ModelAttribute ChatbotRequest request) {
    chatbotService.endSession(request.getSessionToken());
    return ResponseEntity.noContent().build();
  }

  /**
   * 벡터 컬렉션 초기화 (관리자용)
   */
  @PostMapping(value = "/admin/init-collection", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> initializeCollection() {
    documentService.initializeCollection();
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * 클라이언트 IP 추출
   */
  private String extractClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    // 여러 IP가 있을 경우 첫 번째 IP 사용
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    return ip;
  }

  /**
   * 피드백 요청 DTO (내부 클래스)
   */
  @lombok.Data
  public static class FeedbackRequest {
    private UUID messageId;
    private Boolean isHelpful;
  }

  // ========== 문서 관리 API (관리자용) ==========

  /**
   * 모든 문서 목록 조회
   */
  @PostMapping(value = "/document/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<DocumentResponse> getAllDocuments() {
    List<ChatDocument> documents = documentService.getAllActiveDocuments();
    List<DocumentDto> documentDtos = documents.stream()
        .map(this::toDocumentDto)
        .collect(Collectors.toList());

    return ResponseEntity.ok(DocumentResponse.builder()
        .documents(documentDtos)
        .totalCount(documentDtos.size())
        .build());
  }

  /**
   * 문서 상세 조회
   */
  @PostMapping(value = "/document/detail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<DocumentDto> getDocument(@ModelAttribute DocumentRequest request) {
    ChatDocument document = documentService.getDocument(request.getDocumentId());
    return ResponseEntity.ok(toDocumentDto(document));
  }

  /**
   * 문서 생성
   */
  @PostMapping(value = "/document/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<DocumentDto> createDocument(@ModelAttribute DocumentRequest request) {
    ChatDocument document = documentService.createDocument(
        request.getTitle(),
        request.getCategory(),
        request.getContent(),
        request.getDescription()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(toDocumentDto(document));
  }

  /**
   * 문서 수정
   */
  @PostMapping(value = "/document/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<DocumentDto> updateDocument(@ModelAttribute DocumentRequest request) {
    ChatDocument document = documentService.updateDocument(
        request.getDocumentId(),
        request.getTitle(),
        request.getCategory(),
        request.getContent(),
        request.getDescription()
    );
    return ResponseEntity.ok(toDocumentDto(document));
  }

  /**
   * 문서 삭제
   */
  @PostMapping(value = "/document/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> deleteDocument(@ModelAttribute DocumentRequest request) {
    documentService.deleteDocument(request.getDocumentId());
    return ResponseEntity.noContent().build();
  }

  /**
   * 문서 활성화/비활성화 토글
   */
  @PostMapping(value = "/document/toggle-active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> toggleDocumentActive(@ModelAttribute DocumentRequest request) {
    ChatDocument document = documentService.getDocument(request.getDocumentId());
    documentService.setDocumentActive(request.getDocumentId(), !document.getIsActive());
    return ResponseEntity.noContent().build();
  }

  /**
   * 문서 재처리 (청크 재생성 + 벡터화)
   */
  @PostMapping(value = "/document/reprocess", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<DocumentDto> reprocessDocument(@ModelAttribute DocumentRequest request) {
    ChatDocument document = documentService.getDocument(request.getDocumentId());
    documentService.processDocument(document);
    return ResponseEntity.ok(toDocumentDto(documentService.getDocument(request.getDocumentId())));
  }

  /**
   * ChatDocument -> DocumentDto 변환
   */
  private DocumentDto toDocumentDto(ChatDocument document) {
    return DocumentDto.builder()
        .documentId(document.getChatDocumentId())
        .title(document.getTitle())
        .category(document.getCategory())
        .content(document.getContent())
        .description(document.getDescription())
        .isActive(document.getIsActive())
        .isProcessed(document.getIsProcessed())
        .chunkCount(document.getChunkCount())
        .createdDate(document.getCreatedDate() != null ? document.getCreatedDate().toString() : null)
        .updatedDate(document.getUpdatedDate() != null ? document.getUpdatedDate().toString() : null)
        .build();
  }
}
