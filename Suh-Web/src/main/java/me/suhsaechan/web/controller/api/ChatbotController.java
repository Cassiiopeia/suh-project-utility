package me.suhsaechan.web.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.dto.ChatHistoryDto;
import me.suhsaechan.chatbot.dto.ChatbotRequest;
import me.suhsaechan.chatbot.dto.ChatbotResponse;
import me.suhsaechan.chatbot.service.ChatbotService;
import me.suhsaechan.chatbot.service.DocumentService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
   * 채팅 메시지 전송
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
}
