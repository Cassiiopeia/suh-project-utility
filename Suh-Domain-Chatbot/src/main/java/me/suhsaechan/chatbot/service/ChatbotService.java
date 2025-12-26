package me.suhsaechan.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import kr.suhsaechan.ai.exception.SuhAiderException;
import kr.suhsaechan.ai.model.JsonSchema;
import kr.suhsaechan.ai.model.SuhAiderRequest;
import kr.suhsaechan.ai.model.SuhAiderResponse;
import kr.suhsaechan.ai.service.StreamCallback;
import kr.suhsaechan.ai.service.SuhAiderEngine;
import kr.suhsaechan.ai.util.JsonSchemaClassParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.config.ChatbotProperties;
import me.suhsaechan.chatbot.config.QdrantProperties;
import me.suhsaechan.chatbot.dto.ChatHistoryDto;
import me.suhsaechan.chatbot.dto.ChatbotRequest;
import me.suhsaechan.chatbot.dto.ChatbotResponse;
import me.suhsaechan.chatbot.dto.ChatbotResponse.ReferencedDocument;
import me.suhsaechan.chatbot.dto.IntentClassificationDto;
import me.suhsaechan.chatbot.dto.ThinkingEventDto;
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
 * 챗봇 메인 서비스 (Agent-LLM 스타일)
 *
 * <p>Agent-LLM 멀티스텝 처리 흐름:</p>
 * <pre>
 * Step 1: [의도 분류] 경량 LLM (gemma3:1b) → IntentClassificationDto
 *         - 질문 유형 분류 (KNOWLEDGE_QUERY, GREETING, CHITCHAT, CLARIFICATION)
 *         - RAG 검색 필요 여부 판단
 *         - 질문 요약 (다음 스텝에 전달)
 *
 * Step 2: [RAG 검색] (Step 1에서 needsRagSearch=true인 경우에만)
 *         - 벡터 임베딩 생성
 *         - Qdrant 유사도 검색
 *         - 관련 문서 청크 반환
 *
 * Step 3: [응답 생성] 고품질 LLM (rnj-1:8b)
 *         - Step 1의 요약 + Step 2의 검색 결과 + 대화 이력
 *         - 컨텍스트 기반 최종 응답 생성
 * </pre>
 *
 * <p>이러한 멀티스텝 방식은:</p>
 * <ul>
 *   <li>불필요한 RAG 검색을 제거하여 응답 속도 개선</li>
 *   <li>경량 모델과 고품질 모델을 적재적소에 활용하여 비용 최적화</li>
 *   <li>각 스텝의 로깅으로 Agent 동작 추적 가능</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final float CONFIDENCE_THRESHOLD = 0.7f;

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatDocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final QdrantProperties qdrantProperties;
    private final SuhAiderEngine suhAiderEngine;
    private final ChatbotProperties chatbotProperties;
    private final ObjectMapper objectMapper;

    /**
     * 채팅 메시지 처리 (Agent-LLM 멀티스텝)
     *
     * <p>Agent 흐름:</p>
     * <pre>
     * Step 1: 의도 분류 (경량 LLM)
     * Step 2: RAG 검색 (조건부)
     * Step 3: 응답 생성 (고품질 LLM)
     * </pre>
     */
    @Transactional
    public ChatbotResponse chat(ChatbotRequest request, String userIp, String userAgent) {
        long startTime = System.currentTimeMillis();
        log.info("[Agent-LLM] 채팅 요청 처리 시작 - message: {}", request.getMessage());

        // 1. 세션 조회 또는 생성
        ChatSession session = getOrCreateSession(request.getSessionToken(), userIp, userAgent);

        // 2. 사용자 메시지 저장
        int messageIndex = (int) messageRepository.countByChatSession(session);
        ChatMessage userMessage = saveMessage(session, MessageRole.USER, request.getMessage(), messageIndex);

        // 3. 최근 대화 이력 조회 (의도 분류 전에 미리 조회)
        List<ChatMessage> recentHistory = getRecentHistory(session, chatbotProperties.getAgent().getHistory().getMaxMessages(), messageIndex);

        // ===== Agent Step 1: 의도 분류 =====
        log.info("[Agent Step 1/3] 의도 분류 시작");
        IntentClassificationDto intent = classifyUserIntent(request.getMessage(), recentHistory);
        log.info("[Agent Step 1/3] 의도 분류 완료 - type: {}, needsRAG: {}, confidence: {}, summary: {}",
            intent.getIntentType(), intent.getNeedsRagSearch(), intent.getConfidence(), intent.getSummary());

        // ===== Agent Step 2: RAG 검색 (조건부) =====
        List<VectorSearchResult> searchResults = new ArrayList<>();
        if (Boolean.TRUE.equals(intent.getNeedsRagSearch())) {
            log.info("[Agent Step 2/3] RAG 검색 시작");
            int topK = request.getTopK() != null ? request.getTopK() : chatbotProperties.getAgent().getRag().getTopK();
            float minScore = request.getMinScore() != null ? request.getMinScore() : chatbotProperties.getAgent().getRag().getMinScore();

            // 검색 쿼리는 원본 메시지 또는 요약 사용
            String searchQuery = intent.getSummary() != null && !intent.getSummary().isEmpty()
                ? intent.getSummary()
                : request.getMessage();

            searchResults = searchRelevantDocuments(searchQuery, topK, minScore);
            log.info("[Agent Step 2/3] RAG 검색 완료 - 결과 수: {}, 쿼리: {}", searchResults.size(), searchQuery);
        } else {
            log.info("[Agent Step 2/3] RAG 검색 생략 - 의도: {} (일반 대화)", intent.getIntentType());
        }

        // ===== Agent Step 3: 응답 생성 (고품질 LLM) =====
        log.info("[Agent Step 3/3] 응답 생성 시작");
        String fullPrompt = buildFullPrompt(request.getMessage(), searchResults, recentHistory, intent);
        String responseContent = generateAiResponse(request.getMessage(), searchResults, recentHistory, intent);

        // 6. AI 응답 저장 (토큰 정보 포함 - 임시 계산)
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
        // 토큰 사용량 저장 (임시: 문자열 길이 기반 추정, 한글 약 2자당 1토큰, 영문 약 4자당 1토큰)
        assistantMessage.setInputTokens(estimateTokenCount(fullPrompt));
        assistantMessage.setOutputTokens(estimateTokenCount(responseContent));
        messageRepository.save(assistantMessage);

        // 7. 세션 업데이트
        updateSessionActivity(session);

        long responseTime = System.currentTimeMillis() - startTime;

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
     * 스트리밍 채팅 메시지 처리 (Agent-LLM) - 기존 호환성 유지
     */
    public void chatStream(String sessionToken, String message, int topK, float minScore,
                           String userIp, String userAgent, StreamCallback callback,
                           Consumer<String> sessionTokenCallback) {
        chatStream(sessionToken, message, topK, minScore, userIp, userAgent, callback, sessionTokenCallback, null);
    }

    /**
     * 스트리밍 채팅 메시지 처리 (Agent-LLM) - Thinking 콜백 지원
     *
     * <p>Agent 흐름:</p>
     * <pre>
     * Step 1: 의도 분류 (경량 LLM) + 재시도
     * Step 2: RAG 검색 (조건부)
     * Step 3: 응답 생성 (고품질 LLM, 스트리밍)
     * </pre>
     *
     * @param sessionTokenCallback 세션 토큰을 받을 콜백 (null 가능)
     * @param thinkingCallback 생각 과정을 받을 콜백 (null 가능)
     */
    public void chatStream(String sessionToken, String message, int topK, float minScore,
                           String userIp, String userAgent, StreamCallback callback,
                           Consumer<String> sessionTokenCallback, ThinkingCallback thinkingCallback) {
        log.info("[Agent-LLM Stream] 스트리밍 채팅 요청 처리 시작 - message: {}", message);

        try {
            // 초기 DB 작업 (트랜잭션으로 처리)
            StreamingContext context = initializeStreamingContext(sessionToken, message, userIp, userAgent, sessionTokenCallback);
            ChatSession session = context.getSession();
            int messageIndex = context.getMessageIndex();
            List<ChatMessage> recentHistory = context.getRecentHistory();

            // ===== Agent Step 1: 의도 분류 (재시도 로직 포함) =====
            sendThinkingEvent(thinkingCallback, 1, 3, "in_progress", "질문 분석 중", null, null);
            log.info("[Agent Step 1/3] 의도 분류 시작");

            IntentClassificationDto intent = classifyUserIntentWithRetry(message, recentHistory, thinkingCallback);

            String intentDetail = getIntentTypeDisplayName(intent.getIntentType());
            sendThinkingEvent(thinkingCallback, 1, 3, "completed", "질문 분석 완료", intentDetail, null);
            log.info("[Agent Step 1/3] 의도 분류 완료 - type: {}, needsRAG: {}, confidence: {}, searchQuery: {}",
                intent.getIntentType(), intent.getNeedsRagSearch(), intent.getConfidence(), intent.getSearchQuery());

            // ===== Agent Step 2: RAG 검색 (조건부) =====
            List<VectorSearchResult> searchResults = new ArrayList<>();
            if (Boolean.TRUE.equals(intent.getNeedsRagSearch())) {
                // 검색 쿼리: searchQuery > summary > 원본 메시지 순서로 사용
                String searchQuery = getSearchQuery(intent, message);

                sendThinkingEvent(thinkingCallback, 2, 3, "in_progress", "관련 문서 검색 중", null, searchQuery);
                log.info("[Agent Step 2/3] RAG 검색 시작 - 쿼리: {}", searchQuery);

                int actualTopK = topK > 0 ? topK : chatbotProperties.getAgent().getRag().getTopK();
                float actualMinScore = minScore > 0 ? minScore : chatbotProperties.getAgent().getRag().getMinScore();

                searchResults = searchRelevantDocuments(searchQuery, actualTopK, actualMinScore);

                String searchDetail = searchResults.isEmpty()
                    ? "관련 문서 없음"
                    : String.format("%d개 문서", searchResults.size());
                sendThinkingEvent(thinkingCallback, 2, 3, "completed", "문서 검색 완료", searchDetail, searchQuery);
                log.info("[Agent Step 2/3] RAG 검색 완료 - 결과 수: {}", searchResults.size());
            } else {
                sendThinkingEvent(thinkingCallback, 2, 3, "skipped", "문서 검색 생략", "일반 대화", null);
                log.info("[Agent Step 2/3] RAG 검색 생략 - 의도: {} (일반 대화)", intent.getIntentType());
            }

            // ===== Agent Step 3: 응답 생성 (고품질 LLM, 스트리밍) =====
            sendThinkingEvent(thinkingCallback, 3, 3, "in_progress", "응답 생성 중", null, null);
            log.info("[Agent Step 3/3] 스트리밍 응답 생성 시작");
            String fullPrompt = buildFullPrompt(message, searchResults, recentHistory, intent);

            // 스트리밍 응답 생성
            StringBuilder fullResponse = new StringBuilder();
            final ChatSession finalSession = session;
            final int finalMessageIndex = messageIndex;
            final String finalFullPrompt = fullPrompt;
            final List<String> referencedDocIds = searchResults.stream()
                .map(r -> r.getMetadata().get("documentId"))
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

            String model = chatbotProperties.getModels().getResponseGenerator();
            suhAiderEngine.generateStreamAsync(model, fullPrompt, new StreamCallback() {
                @Override
                public void onNext(String chunk) {
                    fullResponse.append(chunk);
                    callback.onNext(chunk);
                }

                @Override
                public void onComplete() {
                    // AI 응답 저장 (별도 트랜잭션으로 처리, 토큰 계산 포함)
                    saveStreamingResponse(finalSession, fullResponse.toString(), finalMessageIndex + 1,
                        referencedDocIds, finalFullPrompt);

                    sendThinkingEvent(thinkingCallback, 3, 3, "completed", "응답 생성 완료", null, null);
                    log.info("[Agent Step 3/3] 스트리밍 응답 생성 완료");
                    log.info("[Agent-LLM Stream] 스트리밍 채팅 완료 - sessionId: {}, 응답 길이: {}",
                        finalSession.getChatSessionId(), fullResponse.length());
                    log.debug("응답 내용: {}", fullResponse.toString());

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
     * ThinkingEvent 전송 헬퍼
     */
    private void sendThinkingEvent(ThinkingCallback callback, int step, int totalSteps,
                                    String status, String title, String detail, String searchQuery) {
        if (callback != null) {
            callback.onThinking(ThinkingEventDto.builder()
                .step(step)
                .totalSteps(totalSteps)
                .status(status)
                .title(title)
                .detail(detail)
                .searchQuery(searchQuery)
                .build());
        }
    }

    /**
     * 의도 유형 표시명 반환
     */
    private String getIntentTypeDisplayName(String intentType) {
        if (intentType == null) return "알 수 없음";
        switch (intentType) {
            case "KNOWLEDGE_QUERY": return "지식 질문";
            case "GREETING": return "인사";
            case "CHITCHAT": return "잡담";
            case "CLARIFICATION": return "추가 질문";
            default: return intentType;
        }
    }

    /**
     * 검색 쿼리 결정: searchQuery > summary > 원본 메시지
     */
    private String getSearchQuery(IntentClassificationDto intent, String originalMessage) {
        if (intent.getSearchQuery() != null && !intent.getSearchQuery().isEmpty()) {
            return intent.getSearchQuery();
        }
        if (intent.getSummary() != null && !intent.getSummary().isEmpty()) {
            return intent.getSummary();
        }
        return originalMessage;
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
     * Agent Step 1: 사용자 의도 분류 (기존 호환성 유지)
     */
    private IntentClassificationDto classifyUserIntent(String userMessage, List<ChatMessage> recentHistory) {
        return classifyUserIntentWithRetry(userMessage, recentHistory, null);
    }

    /**
     * Agent Step 1: 사용자 의도 분류 (재시도 로직 포함)
     *
     * <p>신뢰도가 0.7 미만일 경우 강화된 프롬프트로 1회 재시도합니다.</p>
     */
    private IntentClassificationDto classifyUserIntentWithRetry(String userMessage, List<ChatMessage> recentHistory,
                                                                  ThinkingCallback thinkingCallback) {
        // 1차 시도
        IntentClassificationDto intent = classifyUserIntentInternal(userMessage, recentHistory, false);

        // 신뢰도 충분하면 바로 반환
        if (intent.getConfidence() >= CONFIDENCE_THRESHOLD) {
            return intent;
        }

        // 재시도 알림
        log.info("[Agent Step 1/3] 신뢰도 낮음 ({}%), 재분류 시도", Math.round(intent.getConfidence() * 100));
        sendThinkingEvent(thinkingCallback, 1, 3, "retrying", "재분석 중",
            String.format("신뢰도 낮음 (%d%%)", Math.round(intent.getConfidence() * 100)), null);

        // 2차 시도 (강화된 프롬프트)
        IntentClassificationDto retryIntent = classifyUserIntentInternal(userMessage, recentHistory, true);

        // 더 나은 결과 선택
        if (retryIntent.getConfidence() > intent.getConfidence()) {
            log.info("[Agent Step 1/3] 재분류 성공 - 신뢰도 향상: {}% → {}%",
                Math.round(intent.getConfidence() * 100), Math.round(retryIntent.getConfidence() * 100));
            return retryIntent;
        }

        log.info("[Agent Step 1/3] 재분류 결과 개선 없음, 원본 결과 사용");
        return intent;
    }

    /**
     * Agent Step 1: 사용자 의도 분류 내부 구현
     *
     * @param userMessage 사용자 질문
     * @param recentHistory 최근 대화 이력
     * @param isRetry 재시도 여부 (true면 강화된 프롬프트 사용)
     * @return 의도 분류 결과
     */
    private IntentClassificationDto classifyUserIntentInternal(String userMessage, List<ChatMessage> recentHistory,
                                                                 boolean isRetry) {
        StringBuilder prompt = new StringBuilder();

        // 의도 분류 시스템 프롬프트
        prompt.append("당신은 사용자 질문을 분석하여 의도를 분류하고, RAG 검색 쿼리를 생성하는 Agent AI입니다.\n\n");

        // 재시도 시 강화된 지시
        if (isRetry) {
            prompt.append("## ⚠️ 재분류 요청 (더 신중하게 분석해주세요)\n");
            prompt.append("이전 분류의 신뢰도가 낮았습니다. 다음을 더 신중하게 고려하세요:\n");
            prompt.append("1. 질문에 '이 사이트', '여기', '기능' 등 SUH Project Utility를 암시하는 표현이 있는지 확인\n");
            prompt.append("2. 사이트/앱/서비스 관련 질문은 무조건 KNOWLEDGE_QUERY로 분류\n");
            prompt.append("3. 애매하면 KNOWLEDGE_QUERY로 분류하고 needsRagSearch=true\n");
            prompt.append("4. searchQuery에 반드시 'SUH Project Utility' 키워드 포함\n\n");
        }

        prompt.append("## 분류 기준:\n\n");
        prompt.append("**1. KNOWLEDGE_QUERY (지식 질문)**\n");
        prompt.append("   - SUH Project Utility의 기능, 모듈, 사용법에 대한 질문\n");
        prompt.append("   - '이 사이트', '여기', '이 앱' 등을 언급하는 질문\n");
        prompt.append("   - 개발자(서새찬, suhsaechan) 정보에 대한 질문\n");
        prompt.append("   - 예시: \"이 사이트 기능 뭐 있어?\", \"Docker 로그는 어디서 봐?\", \"개발자 누구야?\"\n");
        prompt.append("   - RAG 검색 필요: **항상 true**\n\n");

        prompt.append("**2. GREETING (인사/감사)** - RAG 검색: false\n");
        prompt.append("   - 예시: \"안녕\", \"고마워\", \"잘 부탁해\"\n\n");

        prompt.append("**3. CHITCHAT (잡담/요청)** - RAG 검색: false\n");
        prompt.append("   - 예시: \"농담해줘\", \"심심해\", \"오늘 날씨 어때?\"\n\n");

        prompt.append("**4. CLARIFICATION (추가 질문)** - RAG 검색: 컨텍스트에 따라\n");
        prompt.append("   - 예시: \"그건 어떻게 해?\", \"더 자세히 알려줘\"\n\n");

        prompt.append("## 중요 판단 기준:\n");
        prompt.append("- '이 사이트', '여기', '기능', '모듈' 언급 → **무조건 KNOWLEDGE_QUERY**\n");
        prompt.append("- 애매하면 **KNOWLEDGE_QUERY 우선**\n\n");

        // searchQuery 생성 지시 (핵심)
        prompt.append("## searchQuery 생성 규칙 (매우 중요!):\n");
        prompt.append("searchQuery는 RAG 벡터 검색에 사용됩니다. 다음 규칙을 따르세요:\n");
        prompt.append("1. **반드시 'SUH Project Utility' 키워드 포함**\n");
        prompt.append("2. 관련 기능명/모듈명 추가: Docker, GitHub, 스터디, 번역, 공지사항 등\n");
        prompt.append("3. 핵심 동작 키워드 추가: 조회, 생성, 설정, 사용법, 기능, 소개 등\n");
        prompt.append("4. 예시:\n");
        prompt.append("   - '이 사이트 뭐하는 곳이야?' → 'SUH Project Utility 소개 목적 주요 기능 모듈'\n");
        prompt.append("   - '기능이 뭐가 있어?' → 'SUH Project Utility 주요 기능 Docker GitHub 스터디 번역 공지사항'\n");
        prompt.append("   - 'Docker 로그 어디서 봐?' → 'SUH Project Utility Docker 컨테이너 로그 조회 모니터링'\n");
        prompt.append("   - '개발자 누구야?' → 'SUH Project Utility 서새찬 suhsaechan 개발자 프로필 소개'\n\n");

        // responseFormat 생성 지시
        prompt.append("## responseFormat 선택 기준:\n");
        prompt.append("- LIST: 기능 나열, 항목별 설명 (예: '어떤 기능이 있어?')\n");
        prompt.append("- GUIDE: 사용법, 단계별 안내 (예: '어떻게 사용해?')\n");
        prompt.append("- INFO: 정보 제공, 설명 (예: '개발자가 누구야?')\n");
        prompt.append("- SIMPLE: 간단한 답변 (인사, 잡담)\n\n");

        // 대화 이력 포함
        if (!recentHistory.isEmpty()) {
            prompt.append("## 최근 대화 이력:\n");
            int historyLimit = Math.min(recentHistory.size(), 3);
            for (int i = recentHistory.size() - historyLimit; i < recentHistory.size(); i++) {
                ChatMessage msg = recentHistory.get(i);
                String role = msg.getRole() == MessageRole.USER ? "사용자" : "서니";
                prompt.append(role).append(": ").append(msg.getContent()).append("\n");
            }
            prompt.append("\n");
        }

        // 현재 질문
        prompt.append("## 현재 질문:\n");
        prompt.append(userMessage);
        prompt.append("\n\n");

        prompt.append("위 질문을 분석하고 JSON으로 반환하세요:\n");
        prompt.append("- intentType: KNOWLEDGE_QUERY | GREETING | CHITCHAT | CLARIFICATION\n");
        prompt.append("- needsRagSearch: true | false\n");
        prompt.append("- confidence: 0.0 ~ 1.0 (확실할수록 높게)\n");
        prompt.append("- reason: 분류 이유 (50자 이내)\n");
        prompt.append("- summary: 질문 요약 (30자 이내)\n");
        prompt.append("- searchQuery: RAG 검색 쿼리 ('SUH Project Utility' 포함 필수)\n");
        prompt.append("- responseFormat: LIST | GUIDE | INFO | SIMPLE");

        try {
            JsonSchema schema = JsonSchemaClassParser.parse(IntentClassificationDto.class);

            SuhAiderRequest request = SuhAiderRequest.builder()
                    .model(chatbotProperties.getModels().getIntentClassifier())
                    .prompt(prompt.toString())
                    .stream(false)
                    .responseSchema(schema)
                    .build();

            SuhAiderResponse response = suhAiderEngine.generate(request);

            IntentClassificationDto intent = objectMapper.readValue(
                response.getResponse(),
                IntentClassificationDto.class
            );

            log.debug("의도 분류 {} - type: {}, needsRAG: {}, confidence: {}, searchQuery: {}, responseFormat: {}",
                isRetry ? "(재시도)" : "(1차)",
                intent.getIntentType(), intent.getNeedsRagSearch(),
                intent.getConfidence(), intent.getSearchQuery(), intent.getResponseFormat());

            return intent;

        } catch (Exception e) {
            log.error("의도 분류 실패, 안전 모드로 전환: {}", e.getMessage(), e);
            return IntentClassificationDto.builder()
                .intentType("UNKNOWN")
                .needsRagSearch(true)
                .confidence(0.5f)
                .reason("분류 오류로 인한 기본값")
                .summary(userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage)
                .searchQuery("SUH Project Utility " + (userMessage.length() > 50 ? userMessage.substring(0, 50) : userMessage))
                .responseFormat("INFO")
                .build();
        }
    }

    /**
     * Agent Step 3: LLM 응답 생성 (고품질 모델)
     *
     * <p>Agent의 최종 단계로, 고품질 모델(rnj-1:8b)을 사용합니다.</p>
     * <p>의도 분류 결과와 RAG 검색 결과를 조합하여 최적의 응답을 생성합니다.</p>
     *
     * @param userMessage 사용자 질문
     * @param searchResults RAG 검색 결과 (비어있을 수 있음)
     * @param recentHistory 최근 대화 이력
     * @param intent 의도 분류 결과
     * @return AI 응답 텍스트
     */
    private String generateAiResponse(String userMessage, List<VectorSearchResult> searchResults,
                                       List<ChatMessage> recentHistory, IntentClassificationDto intent) {
        String fullPrompt = buildFullPrompt(userMessage, searchResults, recentHistory, intent);

        try {
            String model = chatbotProperties.getModels().getResponseGenerator();
            log.debug("LLM 응답 생성 시작 - 모델: {}, 프롬프트 길이: {}", model, fullPrompt.length());

            String response = suhAiderEngine.generate(model, fullPrompt);

            log.debug("LLM 응답 생성 완료 - 응답 길이: {}", response.length());
            log.info("[Agent Step 3/3] 응답 생성 완료");
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
     * 토큰 수 추정 (임시)
     * 한글은 약 2자당 1토큰, 영문은 약 4자당 1토큰으로 추정
     */
    private Integer estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int koreanCount = 0;
        int otherCount = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HANGUL) {
                koreanCount++;
            } else {
                otherCount++;
            }
        }
        // 한글: 2자당 1토큰, 영문/기타: 4자당 1토큰
        return (koreanCount / 2) + (otherCount / 4);
    }

    /**
     * 전체 프롬프트 구성 (Agent-LLM)
     *
     * <p>의도 분류 결과를 반영하여 프롬프트를 최적화합니다.</p>
     */
    private String buildFullPrompt(String userMessage, List<VectorSearchResult> searchResults,
                                   List<ChatMessage> recentHistory, IntentClassificationDto intent) {
        StringBuilder prompt = new StringBuilder();

        // 시스템 프롬프트
        prompt.append(buildSystemPrompt(searchResults, intent));

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
     * 시스템 프롬프트 구성 (Agent-LLM)
     *
     * <p>의도 분류 결과에 따라 프롬프트를 최적화합니다.</p>
     */
    private String buildSystemPrompt(List<VectorSearchResult> searchResults, IntentClassificationDto intent) {
        StringBuilder systemPrompt = new StringBuilder();

        // 역할 정의 (간결하게)
        systemPrompt.append("## 역할\n");
        systemPrompt.append("- 이름: SuhNi(서니)\n");
        systemPrompt.append("- 사이트: SUH Project Utility 도우미\n");
        systemPrompt.append("- 목적: SUH Project Utility의 기능, 모듈, 개발자 정보를 안내합니다\n");
        systemPrompt.append("- 개발자: 서새찬(suhsaechan)\n\n");

        // 응답 규칙 (명확하게)
        systemPrompt.append("## 응답 규칙\n");

        // 의도별 맞춤 응답 규칙
        if (intent != null && "GREETING".equals(intent.getIntentType())) {
            systemPrompt.append("1. 사용자가 인사를 건넸습니다. 간단히 인사를 받고 도움 여부를 물어보세요.\n");
        } else if (intent != null && "CHITCHAT".equals(intent.getIntentType())) {
            systemPrompt.append("**사용자가 잡담이나 행동 요청을 했습니다.**\n");
            systemPrompt.append("1. 농담/이야기 요청: 지금 바로 재밌는 농담이나 이야기를 해주세요\n");
            systemPrompt.append("2. 절대 \"어떤 종류를 좋아하세요?\" 같은 추가 질문 금지\n");
            systemPrompt.append("3. 즉시 수행하세요\n");
            systemPrompt.append("4. 일반 잡담: 친근하게 대화해주세요\n");
        } else {
            systemPrompt.append("1. 질문에 대해 즉시 본론으로 답변하세요. 자기소개나 \"질문이 무엇인가요?\" 같은 말은 생략하세요.\n");
        }

        if (intent == null || !"CHITCHAT".equals(intent.getIntentType())) {
            systemPrompt.append("2. 한국어 존댓말을 사용하되, 간결하고 정확하게 답변하세요.\n");
            systemPrompt.append("3. 확실하지 않은 내용은 추측하지 말고 솔직히 알려주세요.\n");
        }
        systemPrompt.append("\n");

        // 응답 형식 지시 (responseFormat 기반)
        if (intent != null && intent.getResponseFormat() != null) {
            systemPrompt.append("## 응답 형식\n");
            switch (intent.getResponseFormat()) {
                case "LIST":
                    systemPrompt.append("다음 형식으로 **목록** 형태로 답변하세요:\n");
                    systemPrompt.append("- 각 항목은 번호나 불릿으로 구분\n");
                    systemPrompt.append("- 항목마다 간단한 설명 포함\n");
                    systemPrompt.append("- 예: \"1. Docker 모니터링: 컨테이너 상태 확인 및 로그 조회\"\n");
                    break;
                case "GUIDE":
                    systemPrompt.append("다음 형식으로 **단계별 가이드** 형태로 답변하세요:\n");
                    systemPrompt.append("- 순서대로 번호를 매겨 안내\n");
                    systemPrompt.append("- 각 단계는 명확한 동작으로 설명\n");
                    systemPrompt.append("- 예: \"1. 메뉴에서 Docker를 클릭합니다.\"\n");
                    break;
                case "INFO":
                    systemPrompt.append("다음 형식으로 **정보 제공** 형태로 답변하세요:\n");
                    systemPrompt.append("- 핵심 정보를 먼저 제시\n");
                    systemPrompt.append("- 부가 설명은 간결하게 추가\n");
                    systemPrompt.append("- 관련 링크나 참고 자료가 있으면 포함\n");
                    break;
                case "SIMPLE":
                    systemPrompt.append("**간단하고 짧게** 답변하세요. 1-2문장이면 충분합니다.\n");
                    break;
                default:
                    break;
            }
            systemPrompt.append("\n");
        }

        // Agent 분석 결과 제공 (디버깅 및 컨텍스트 강화)
        if (intent != null) {
            systemPrompt.append("## Agent 분석 결과:\n");
            systemPrompt.append("- 질문 유형: ").append(intent.getIntentType()).append("\n");
            if (intent.getSummary() != null && !intent.getSummary().isEmpty()) {
                systemPrompt.append("- 질문 요약: ").append(intent.getSummary()).append("\n");
            }
            systemPrompt.append("\n");
        }

        // RAG 검색 결과 처리
        if (searchResults.isEmpty()) {
            // 의도에 따라 메시지 조정
            if (intent != null && "GREETING".equals(intent.getIntentType())) {
                systemPrompt.append("참고 문서 없이 친근하게 인사해주세요.\n");
            } else if (intent != null && "CHITCHAT".equals(intent.getIntentType())) {
                // CHITCHAT은 위의 응답 규칙에서 이미 명확히 지시했으므로 추가 메시지 불필요
            } else {
                systemPrompt.append("관련 참고 문서를 찾지 못했습니다. 일반적인 안내를 제공하되, 확실하지 않은 내용은 솔직하게 모른다고 말씀해주세요.\n");
            }
        } else {
            systemPrompt.append("### 참고 문서 (RAG 검색 결과):\n\n");
            for (int i = 0; i < searchResults.size(); i++) {
                VectorSearchResult result = searchResults.get(i);
                String title = result.getMetadata().getOrDefault("title", "제목 없음");
                String category = result.getMetadata().getOrDefault("category", "");
                systemPrompt.append(String.format("[%d] %s%s\n%s\n\n",
                    i + 1,
                    title,
                    category.isEmpty() ? "" : " (" + category + ")",
                    result.getContent()));
            }
            systemPrompt.append("위 참고 문서를 바탕으로 사용자 질문에 정확하게 답변해주세요.\n");
            systemPrompt.append("문서에 없는 내용은 추측하지 말고, 문서 기반으로만 답변해주세요.\n");
            systemPrompt.append("URL 관련 질문의 경우, 문서에서 찾은 URL을 명확하게 제공하고, 가능하면 클릭 가능한 링크 형식으로 제공해주세요.\n");
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
     * 스트리밍 초기 컨텍스트 생성 (트랜잭션)
     */
    @Transactional
    private StreamingContext initializeStreamingContext(String sessionToken, String message, String userIp, 
                                                         String userAgent, Consumer<String> sessionTokenCallback) {
        // 1. 세션 조회 또는 생성
        ChatSession session = getOrCreateSession(sessionToken, userIp, userAgent);

        // 세션 토큰 즉시 전달 (SSE 'connected' 이벤트용)
        if (sessionTokenCallback != null) {
            sessionTokenCallback.accept(session.getSessionToken());
            log.debug("세션 토큰 콜백 전달: {}", session.getSessionToken());
        }

        // 2. 사용자 메시지 저장
        int messageIndex = (int) messageRepository.countByChatSession(session);
        saveMessage(session, MessageRole.USER, message, messageIndex);

        // 3. 최근 대화 이력 조회 (의도 분류 전에 미리 조회)
        List<ChatMessage> recentHistory = getRecentHistory(session, chatbotProperties.getAgent().getHistory().getMaxMessages(), messageIndex);

        return new StreamingContext(session, messageIndex, recentHistory);
    }

    /**
     * 스트리밍 컨텍스트 (내부 클래스)
     */
    private static class StreamingContext {
        private final ChatSession session;
        private final int messageIndex;
        private final List<ChatMessage> recentHistory;

        public StreamingContext(ChatSession session, int messageIndex, List<ChatMessage> recentHistory) {
            this.session = session;
            this.messageIndex = messageIndex;
            this.recentHistory = recentHistory;
        }

        public ChatSession getSession() { return session; }
        public int getMessageIndex() { return messageIndex; }
        public List<ChatMessage> getRecentHistory() { return recentHistory; }
    }

    /**
     * 스트리밍 응답 저장 (별도 트랜잭션, 토큰 계산 포함)
     */
    @Transactional
    public void saveStreamingResponse(ChatSession session, String content, int messageIndex,
                                       List<String> referencedDocIds, String fullPrompt) {
        ChatMessage assistantMessage = saveMessage(session, MessageRole.ASSISTANT, content, messageIndex);
        assistantMessage.setReferencedDocumentIds(String.join(",", referencedDocIds));
        // 토큰 사용량 저장 (임시 계산)
        assistantMessage.setInputTokens(estimateTokenCount(fullPrompt));
        assistantMessage.setOutputTokens(estimateTokenCount(content));
        messageRepository.save(assistantMessage);
        updateSessionActivity(session);
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
