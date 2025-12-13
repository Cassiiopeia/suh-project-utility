package me.suhsaechan.chatbot.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.config.QdrantProperties;
import me.suhsaechan.chatbot.dto.ChatHistoryDto;
import me.suhsaechan.chatbot.dto.ChatbotRequest;
import me.suhsaechan.chatbot.dto.ChatbotResponse;
import me.suhsaechan.chatbot.dto.ChatbotResponse.ReferencedDocument;
import me.suhsaechan.chatbot.dto.VectorSearchResult;
import me.suhsaechan.chatbot.entity.ChatDocumentChunk;
import me.suhsaechan.chatbot.entity.ChatMessage;
import me.suhsaechan.chatbot.entity.ChatMessage.MessageRole;
import me.suhsaechan.chatbot.entity.ChatSession;
import me.suhsaechan.chatbot.repository.ChatDocumentChunkRepository;
import me.suhsaechan.chatbot.repository.ChatMessageRepository;
import me.suhsaechan.chatbot.repository.ChatSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 챗봇 메인 서비스
 * 대화 처리, 세션 관리, RAG 검색 등 핵심 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatDocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final QdrantProperties qdrantProperties;

    // 기본 설정
    private static final int DEFAULT_TOP_K = 3;
    private static final float DEFAULT_MIN_SCORE = 0.5f;

    /**
     * 채팅 메시지 처리
     * 현재는 LLM 연결 없이 RAG 검색 결과만 반환
     */
    @Transactional
    public ChatbotResponse chat(ChatbotRequest request, String userIp, String userAgent) {
        long startTime = System.currentTimeMillis();
        log.info("채팅 요청 처리 시작 - message: {}", request.getMessage());

        // 1. 세션 조회 또는 생성
        ChatSession session = getOrCreateSession(request.getSessionToken(), userIp, userAgent);

        // 2. 사용자 메시지 저장
        int messageIndex = (int) messageRepository.countByChatSession(session);
        ChatMessage userMessage = saveMessage(session, MessageRole.USER, request.getMessage(), messageIndex);

        // 3. RAG 검색
        int topK = request.getTopK() != null ? request.getTopK() : DEFAULT_TOP_K;
        float minScore = request.getMinScore() != null ? request.getMinScore() : DEFAULT_MIN_SCORE;

        List<VectorSearchResult> searchResults = searchRelevantDocuments(request.getMessage(), topK, minScore);

        // 4. 검색 결과로 응답 생성 (LLM 연결 전 임시 응답)
        String responseContent = generateTemporaryResponse(request.getMessage(), searchResults);

        // 5. AI 응답 저장
        List<String> referencedDocIds = searchResults.stream()
            .map(r -> r.getMetadata().get("documentId"))
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());

        ChatMessage assistantMessage = saveMessage(
            session,
            MessageRole.ASSISTANT,
            responseContent,
            messageIndex + 1
        );
        assistantMessage.setReferencedDocumentIds(String.join(",", referencedDocIds));
        messageRepository.save(assistantMessage);

        // 6. 세션 업데이트
        updateSessionActivity(session);

        long responseTime = System.currentTimeMillis() - startTime;

        // 7. 응답 생성
        List<ReferencedDocument> references = buildReferences(searchResults);

        log.info("채팅 응답 완료 - sessionId: {}, responseTime: {}ms",
            session.getChatSessionId(), responseTime);

        return ChatbotResponse.builder()
            .sessionToken(session.getSessionToken())
            .sessionId(session.getChatSessionId())
            .message(responseContent)
            .messageId(assistantMessage.getChatMessageId())
            .references(references)
            .responseTimeMs(responseTime)
            .build();
    }

    /**
     * 관련 문서 검색 (RAG)
     */
    public List<VectorSearchResult> searchRelevantDocuments(String query, int topK, float minScore) {
        log.debug("RAG 검색 시작 - query: {}, topK: {}, minScore: {}", query, topK, minScore);

        // 쿼리 임베딩 생성
        List<Float> queryVector = embeddingService.embed(query);

        // Qdrant 검색
        String collectionName = qdrantProperties.getCollectionName();
        List<VectorSearchResult> results = vectorStoreService.search(collectionName, queryVector, topK, minScore);

        log.debug("RAG 검색 완료 - 결과 수: {}", results.size());
        return results;
    }

    /**
     * 세션 조회 또는 생성
     */
    private ChatSession getOrCreateSession(String sessionToken, String userIp, String userAgent) {
        if (sessionToken != null && !sessionToken.isEmpty()) {
            return sessionRepository.findBySessionTokenAndIsActiveTrue(sessionToken)
                .orElseGet(() -> createNewSession(sessionToken, userIp, userAgent));
        }
        return createNewSession(UUID.randomUUID().toString(), userIp, userAgent);
    }

    /**
     * 새 세션 생성
     */
    private ChatSession createNewSession(String sessionToken, String userIp, String userAgent) {
        ChatSession session = ChatSession.builder()
            .sessionToken(sessionToken)
            .userIp(userIp)
            .userAgent(userAgent)
            .lastActivityAt(LocalDateTime.now())
            .isActive(true)
            .messageCount(0)
            .build();

        session = sessionRepository.save(session);
        log.info("새 세션 생성 - sessionId: {}", session.getChatSessionId());
        return session;
    }

    /**
     * 메시지 저장
     */
    private ChatMessage saveMessage(ChatSession session, MessageRole role, String content, int messageIndex) {
        ChatMessage message = ChatMessage.builder()
            .chatSession(session)
            .role(role)
            .content(content)
            .messageIndex(messageIndex)
            .build();

        return messageRepository.save(message);
    }

    /**
     * 세션 활동 업데이트
     */
    private void updateSessionActivity(ChatSession session) {
        session.setLastActivityAt(LocalDateTime.now());
        session.setMessageCount((int) messageRepository.countByChatSession(session));
        sessionRepository.save(session);
    }

    /**
     * 임시 응답 생성 (LLM 연결 전)
     */
    private String generateTemporaryResponse(String query, List<VectorSearchResult> searchResults) {
        if (searchResults.isEmpty()) {
            return "죄송합니다. 관련된 정보를 찾지 못했습니다. 다른 질문을 해주시거나, 더 구체적으로 질문해 주세요.";
        }

        StringBuilder response = new StringBuilder();
        response.append("다음은 관련된 정보입니다:\n\n");

        for (int i = 0; i < searchResults.size(); i++) {
            VectorSearchResult result = searchResults.get(i);
            String title = result.getMetadata().getOrDefault("title", "제목 없음");
            String category = result.getMetadata().getOrDefault("category", "기타");

            response.append(String.format("**[%s] %s**\n", category, title));
            response.append(result.getContent());
            response.append("\n\n");
        }

        response.append("---\n");
        response.append("*현재 LLM이 연결되지 않아 검색 결과만 표시됩니다. LLM 연결 후 자연스러운 대화가 가능합니다.*");

        return response.toString();
    }

    /**
     * 참조 문서 정보 생성
     */
    private List<ReferencedDocument> buildReferences(List<VectorSearchResult> searchResults) {
        List<ReferencedDocument> references = new ArrayList<>();

        for (VectorSearchResult result : searchResults) {
            String documentId = result.getMetadata().get("documentId");

            references.add(ReferencedDocument.builder()
                .documentId(documentId != null ? UUID.fromString(documentId) : null)
                .title(result.getMetadata().getOrDefault("title", ""))
                .category(result.getMetadata().getOrDefault("category", ""))
                .snippet(truncateContent(result.getContent(), 200))
                .score(result.getScore())
                .build());
        }

        return references;
    }

    /**
     * 내용 자르기
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 세션 대화 이력 조회
     */
    @Transactional(readOnly = true)
    public List<ChatHistoryDto> getChatHistory(String sessionToken) {
        ChatSession session = sessionRepository.findBySessionToken(sessionToken)
            .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다: " + sessionToken));

        List<ChatMessage> messages = messageRepository.findByChatSessionOrderByMessageIndexAsc(session);

        return messages.stream()
            .map(this::convertToHistoryDto)
            .collect(Collectors.toList());
    }

    /**
     * 세션 ID로 대화 이력 조회
     */
    @Transactional(readOnly = true)
    public List<ChatHistoryDto> getChatHistoryBySessionId(UUID sessionId) {
        List<ChatMessage> messages = messageRepository.findByChatSessionChatSessionIdOrderByMessageIndexAsc(sessionId);

        return messages.stream()
            .map(this::convertToHistoryDto)
            .collect(Collectors.toList());
    }

    /**
     * 메시지 피드백 저장
     */
    @Transactional
    public void saveMessageFeedback(UUID messageId, boolean isHelpful) {
        ChatMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다: " + messageId));

        message.setIsHelpful(isHelpful);
        messageRepository.save(message);

        log.info("메시지 피드백 저장 - messageId: {}, isHelpful: {}", messageId, isHelpful);
    }

    /**
     * 세션 종료
     */
    @Transactional
    public void endSession(String sessionToken) {
        ChatSession session = sessionRepository.findBySessionToken(sessionToken)
            .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다: " + sessionToken));

        session.setIsActive(false);
        sessionRepository.save(session);

        log.info("세션 종료 - sessionId: {}", session.getChatSessionId());
    }

    /**
     * 비활성 세션 정리 (스케줄러용)
     */
    @Transactional
    public int cleanupInactiveSessions(int hoursThreshold) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hoursThreshold);
        List<ChatSession> inactiveSessions = sessionRepository.findByIsActiveTrueAndLastActivityAtBefore(cutoff);

        for (ChatSession session : inactiveSessions) {
            session.setIsActive(false);
        }

        sessionRepository.saveAll(inactiveSessions);
        log.info("비활성 세션 정리 완료 - 정리 수: {}", inactiveSessions.size());

        return inactiveSessions.size();
    }

    private ChatHistoryDto convertToHistoryDto(ChatMessage message) {
        return ChatHistoryDto.builder()
            .messageId(message.getChatMessageId())
            .role(message.getRole())
            .content(message.getContent())
            .messageIndex(message.getMessageIndex())
            .createdAt(message.getCreatedDate())
            .isHelpful(message.getIsHelpful())
            .build();
    }
}
