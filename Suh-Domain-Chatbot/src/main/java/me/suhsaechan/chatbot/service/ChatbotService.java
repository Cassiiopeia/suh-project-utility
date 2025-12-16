package me.suhsaechan.chatbot.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import kr.suhsaechan.ai.exception.SuhAiderException;
import kr.suhsaechan.ai.service.StreamCallback;
import kr.suhsaechan.ai.service.SuhAiderEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.config.QdrantProperties;
import me.suhsaechan.chatbot.dto.ChatHistoryDto;
import me.suhsaechan.chatbot.dto.ChatbotRequest;
import me.suhsaechan.chatbot.dto.ChatbotResponse;
import me.suhsaechan.chatbot.dto.ChatbotResponse.ReferencedDocument;
import me.suhsaechan.chatbot.dto.VectorSearchResult;
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
    private final SuhAiderEngine suhAiderEngine;

    // 기본 설정
    private static final int DEFAULT_TOP_K = 3;
    private static final float DEFAULT_MIN_SCORE = 0.5f;
    private static final String LLM_MODEL = "granite4:1b-h";
    private static final int MAX_HISTORY_MESSAGES = 30;  // 최근 대화 이력 포함 수
    private static final int TOKEN_ESTIMATE_DIVISOR = 3;  // 한글 기준 약 3자 = 1토큰

    /**
     * 채팅 메시지 처리
     * RAG 검색 + LLM 응답 생성
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

        // 4. 최근 대화 이력 조회 (현재 사용자 메시지 제외)
        List<ChatMessage> recentHistory = getRecentHistory(session, MAX_HISTORY_MESSAGES, messageIndex);

        // 5. 프롬프트 구성 및 LLM 응답 생성
        String fullPrompt = buildFullPrompt(request.getMessage(), searchResults, recentHistory);
        String responseContent = generateAiResponseFromPrompt(fullPrompt);

        // 6. AI 응답 저장
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
        assistantMessage.setInputTokens(estimateTokenCount(fullPrompt));
        assistantMessage.setOutputTokens(estimateTokenCount(responseContent));
        long responseTime = System.currentTimeMillis() - startTime;
        assistantMessage.setResponseTimeMs(responseTime);
        messageRepository.save(assistantMessage);

        // 7. 세션 업데이트
        updateSessionActivity(session);

        // 8. 응답 생성
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
     * 스트리밍 채팅 메시지 처리
     * RAG 검색 + LLM 스트리밍 응답 생성
     */
    @Transactional
    public void chatStream(String sessionToken, String message, int topK, float minScore,
                           String userIp, String userAgent, StreamCallback callback) {
        log.info("스트리밍 채팅 요청 처리 시작 - message: {}", message);
        final long streamStartTime = System.currentTimeMillis();

        try {
            // 1. 세션 조회 또는 생성
            ChatSession session = getOrCreateSession(sessionToken, userIp, userAgent);

            // 2. 사용자 메시지 저장
            int messageIndex = (int) messageRepository.countByChatSession(session);
            saveMessage(session, MessageRole.USER, message, messageIndex);

            // 3. RAG 검색
            List<VectorSearchResult> searchResults = searchRelevantDocuments(message, topK, minScore);

            // 4. 최근 대화 이력 조회 (현재 사용자 메시지 제외)
            List<ChatMessage> recentHistory = getRecentHistory(session, MAX_HISTORY_MESSAGES, messageIndex);

            // 5. 프롬프트 구성
            String fullPrompt = buildFullPrompt(message, searchResults, recentHistory);
            final int inputTokens = estimateTokenCount(fullPrompt);

            // 6. 스트리밍 응답 생성
            StringBuilder fullResponse = new StringBuilder();
            final ChatSession finalSession = session;
            final int finalMessageIndex = messageIndex;
            final List<String> referencedDocIds = searchResults.stream()
                .map(r -> r.getMetadata().get("documentId"))
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

            suhAiderEngine.generateStreamAsync(LLM_MODEL, fullPrompt, new StreamCallback() {
                @Override
                public void onNext(String chunk) {
                    fullResponse.append(chunk);
                    callback.onNext(chunk);
                }

                @Override
                public void onComplete() {
                    // AI 응답 저장 (토큰 및 응답 시간 포함)
                    ChatMessage assistantMessage = saveMessage(
                        finalSession,
                        MessageRole.ASSISTANT,
                        fullResponse.toString(),
                        finalMessageIndex + 1
                    );
                    assistantMessage.setReferencedDocumentIds(String.join(",", referencedDocIds));
                    assistantMessage.setInputTokens(inputTokens);
                    assistantMessage.setOutputTokens(estimateTokenCount(fullResponse.toString()));
                    assistantMessage.setResponseTimeMs(System.currentTimeMillis() - streamStartTime);
                    messageRepository.save(assistantMessage);

                    // 세션 업데이트
                    updateSessionActivity(finalSession);

                    log.info("스트리밍 채팅 완료 - sessionId: {}, 응답 길이: {}, 입력토큰: {}, 출력토큰: {}",
                        finalSession.getChatSessionId(), fullResponse.length(),
                        inputTokens, estimateTokenCount(fullResponse.toString()));

                    callback.onComplete();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("스트리밍 응답 생성 중 오류: {}", error.getMessage(), error);
                    callback.onError(error);
                }
            });

        } catch (Exception e) {
            log.error("스트리밍 채팅 처리 중 오류: {}", e.getMessage(), e);
            callback.onError(e);
        }
    }

    /**
     * 최근 대화 이력 조회 (현재 사용자 메시지 제외)
     *
     * @param session 세션
     * @param maxMessages 최대 이력 메시지 수
     * @param currentMessageIndex 현재 사용자 메시지의 인덱스 (이 메시지는 제외됨)
     * @return 최근 대화 이력 (현재 메시지 제외)
     */
    private List<ChatMessage> getRecentHistory(ChatSession session, int maxMessages, int currentMessageIndex) {
        List<ChatMessage> allMessages = messageRepository.findByChatSessionOrderByMessageIndexAsc(session);

        // 현재 사용자 메시지 제외 (messageIndex가 currentMessageIndex 미만인 메시지만 포함)
        List<ChatMessage> historyWithoutCurrent = allMessages.stream()
            .filter(msg -> msg.getMessageIndex() < currentMessageIndex)
            .collect(Collectors.toList());

        log.debug("대화 이력 조회 - 전체 메시지 수: {}, 현재 메시지 인덱스: {}, 제외 후 메시지 수: {}, 최대 이력: {}",
            allMessages.size(), currentMessageIndex, historyWithoutCurrent.size(), maxMessages);

        // 최대 개수 제한
        if (historyWithoutCurrent.size() <= maxMessages) {
            log.debug("반환할 이력 메시지 수: {}", historyWithoutCurrent.size());
            return historyWithoutCurrent;
        }

        List<ChatMessage> result = historyWithoutCurrent.subList(
            historyWithoutCurrent.size() - maxMessages,
            historyWithoutCurrent.size()
        );
        log.debug("반환할 이력 메시지 수: {} (최대 {}개로 제한)", result.size(), maxMessages);
        return result;
    }

    /**
     * LLM 응답 생성 (프롬프트 직접 전달)
     */
    private String generateAiResponseFromPrompt(String fullPrompt) {
        try {
            log.debug("LLM 응답 생성 시작 - 모델: {}, 프롬프트 길이: {}", LLM_MODEL, fullPrompt.length());
            String response = suhAiderEngine.generate(LLM_MODEL, fullPrompt);
            log.debug("LLM 응답 생성 완료 - 응답 길이: {}", response.length());
            return response;
        } catch (SuhAiderException e) {
            log.error("LLM 응답 생성 실패: {} - {}", e.getErrorCode(), e.getMessage());
            return "죄송합니다. AI 서비스에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.";
        } catch (Exception e) {
            log.error("LLM 응답 생성 중 예외 발생: {}", e.getMessage(), e);
            return "죄송합니다. 응답 생성 중 오류가 발생했습니다.";
        }
    }

    /**
     * 토큰 수 추정 (한글 기준 약 3자 = 1토큰)
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / TOKEN_ESTIMATE_DIVISOR);
    }

    /**
     * 전체 프롬프트 구성
     */
    private String buildFullPrompt(String userMessage, List<VectorSearchResult> searchResults,
                                   List<ChatMessage> recentHistory) {
        StringBuilder prompt = new StringBuilder();

        // 시스템 프롬프트
        prompt.append(buildSystemPrompt(searchResults));

        // 대화 이력
        if (!recentHistory.isEmpty()) {
            prompt.append("\n\n### 이전 대화:\n");
            for (ChatMessage msg : recentHistory) {
                String role = msg.getRole() == MessageRole.USER ? "사용자" : "서니";
                prompt.append(role).append(": ").append(msg.getContent()).append("\n");
            }
        }

        // 현재 질문
        prompt.append("\n### 사용자 질문:\n");
        prompt.append(userMessage);

        // 응답 시작 유도
        prompt.append("\n\n### 서니의 응답:\n");

        String fullPrompt = prompt.toString();
        log.debug("프롬프트 구성 완료 - 이력 메시지 수: {}, 전체 프롬프트 길이: {} 문자",
            recentHistory.size(), fullPrompt.length());
        log.trace("전체 프롬프트 내용:\n{}", fullPrompt);

        return fullPrompt;
    }

    /**
     * 시스템 프롬프트 구성
     */
    private String buildSystemPrompt(List<VectorSearchResult> searchResults) {
        StringBuilder systemPrompt = new StringBuilder();

        systemPrompt.append("당신은 SUH Project Utility의 친절한 도우미 'SuhNi(서니)'입니다.\n");
        systemPrompt.append("사용자의 질문에 친절하고 간결하게 답변해주세요.\n");
        systemPrompt.append("한국어로 답변하며, 존댓말을 사용합니다.\n");

        if (searchResults.isEmpty()) {
            systemPrompt.append("\n관련 참고 문서를 찾지 못했습니다. 일반적인 안내를 제공해주세요.\n");
        } else {
            systemPrompt.append("\n### 참고 문서:\n");
            for (int i = 0; i < searchResults.size(); i++) {
                VectorSearchResult result = searchResults.get(i);
                String title = result.getMetadata().getOrDefault("title", "제목 없음");
                systemPrompt.append(String.format("[%d] %s\n%s\n\n",
                    i + 1, title, result.getContent()));
            }
            systemPrompt.append("위 참고 문서를 바탕으로 사용자 질문에 답변해주세요.\n");
        }

        return systemPrompt.toString();
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
