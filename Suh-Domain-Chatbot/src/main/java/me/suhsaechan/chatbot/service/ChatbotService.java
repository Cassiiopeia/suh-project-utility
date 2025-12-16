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
        String responseContent = generateAiResponse(request.getMessage(), searchResults, recentHistory, intent);

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
     * 스트리밍 채팅 메시지 처리 (Agent-LLM)
     *
     * <p>Agent 흐름:</p>
     * <pre>
     * Step 1: 의도 분류 (경량 LLM)
     * Step 2: RAG 검색 (조건부)
     * Step 3: 응답 생성 (고품질 LLM, 스트리밍)
     * </pre>
     *
     * @param sessionTokenCallback 세션 토큰을 받을 콜백 (null 가능)
     */
    public void chatStream(String sessionToken, String message, int topK, float minScore,
                           String userIp, String userAgent, StreamCallback callback,
                           Consumer<String> sessionTokenCallback) {
        log.info("[Agent-LLM Stream] 스트리밍 채팅 요청 처리 시작 - message: {}", message);

        try {
            // 초기 DB 작업 (트랜잭션으로 처리)
            StreamingContext context = initializeStreamingContext(sessionToken, message, userIp, userAgent, sessionTokenCallback);
            ChatSession session = context.getSession();
            int messageIndex = context.getMessageIndex();
            List<ChatMessage> recentHistory = context.getRecentHistory();

            // ===== Agent Step 1: 의도 분류 =====
            log.info("[Agent Step 1/3] 의도 분류 시작");
            IntentClassificationDto intent = classifyUserIntent(message, recentHistory);
            log.info("[Agent Step 1/3] 의도 분류 완료 - type: {}, needsRAG: {}, confidence: {}, summary: {}",
                intent.getIntentType(), intent.getNeedsRagSearch(), intent.getConfidence(), intent.getSummary());

            // ===== Agent Step 2: RAG 검색 (조건부) =====
            List<VectorSearchResult> searchResults = new ArrayList<>();
            if (Boolean.TRUE.equals(intent.getNeedsRagSearch())) {
                log.info("[Agent Step 2/3] RAG 검색 시작");
                int actualTopK = topK > 0 ? topK : chatbotProperties.getAgent().getRag().getTopK();
                float actualMinScore = minScore > 0 ? minScore : chatbotProperties.getAgent().getRag().getMinScore();

                // 검색 쿼리는 원본 메시지 또는 요약 사용
                String searchQuery = intent.getSummary() != null && !intent.getSummary().isEmpty()
                    ? intent.getSummary()
                    : message;

                searchResults = searchRelevantDocuments(searchQuery, actualTopK, actualMinScore);
                log.info("[Agent Step 2/3] RAG 검색 완료 - 결과 수: {}, 쿼리: {}", searchResults.size(), searchQuery);
            } else {
                log.info("[Agent Step 2/3] RAG 검색 생략 - 의도: {} (일반 대화)", intent.getIntentType());
            }

            // ===== Agent Step 3: 응답 생성 (고품질 LLM, 스트리밍) =====
            log.info("[Agent Step 3/3] 스트리밍 응답 생성 시작");
            String fullPrompt = buildFullPrompt(message, searchResults, recentHistory, intent);

            // 스트리밍 응답 생성
            StringBuilder fullResponse = new StringBuilder();
            final ChatSession finalSession = session;
            final int finalMessageIndex = messageIndex;
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
                    // AI 응답 저장 (별도 트랜잭션으로 처리)
                    saveStreamingResponse(finalSession, fullResponse.toString(), finalMessageIndex + 1, referencedDocIds);

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
     * Agent Step 1: 사용자 의도 분류 (경량 LLM + Structured Output)
     *
     * <p>경량 모델(gemma3:1b)을 사용하여 빠르게 의도를 분류합니다.</p>
     * <p>SUH-AIDER의 Structured Output 기능으로 JSON 파싱 오류를 방지합니다.</p>
     *
     * @param userMessage 사용자 질문
     * @param recentHistory 최근 대화 이력 (컨텍스트)
     * @return 의도 분류 결과
     */
    private IntentClassificationDto classifyUserIntent(String userMessage, List<ChatMessage> recentHistory) {
        StringBuilder prompt = new StringBuilder();

        // 의도 분류 시스템 프롬프트
        prompt.append("당신은 사용자 질문을 분석하여 의도를 분류하는 Agent AI입니다.\n\n");
        prompt.append("## 분류 기준:\n\n");
        prompt.append("**1. KNOWLEDGE_QUERY (지식 질문)**\n");
        prompt.append("   - SUH Project Utility의 기능, 모듈, 사용법에 대한 질문\n");
        prompt.append("   - 개발자(서새찬, suhsaechan) 정보에 대한 질문\n");
        prompt.append("   - 예시:\n");
        prompt.append("     * 기능: \"Docker 로그는 어디서 볼 수 있나요?\", \"스터디 노트 작성 방법\", \"GitHub 이슈 헬퍼 사용법\"\n");
        prompt.append("     * 개발자: \"서새찬은 누구?\", \"suhsaechan 알려줘\", \"개발자 소개\", \"만든 사람은?\"\n");
        prompt.append("   - RAG 검색 필요: **항상 true**\n\n");

        prompt.append("**2. GREETING (인사/감사)**\n");
        prompt.append("   - 인사말, 감사 표현, 작별 인사\n");
        prompt.append("   - RAG 검색 필요: false\n");
        prompt.append("   - 예시: \"안녕하세요\", \"고마워요\", \"잘 부탁드립니다\", \"안녕히 가세요\"\n\n");

        prompt.append("**3. CHITCHAT (잡담/요청)**\n");
        prompt.append("   - 농담, 이야기 등의 행동 요청 OR 일반적인 잡담\n");
        prompt.append("   - RAG 검색 필요: false\n");
        prompt.append("   - 예시: \"농담해줘\", \"재밌는 이야기 해줘\", \"오늘 날씨 어때?\", \"심심해\"\n\n");

        prompt.append("**4. CLARIFICATION (추가 질문)**\n");
        prompt.append("   - 이전 답변에 대한 추가 설명 요청\n");
        prompt.append("   - RAG 검색 필요: 컨텍스트에 따라 판단 (대부분 true)\n");
        prompt.append("   - 예시: \"그럼 그건 어떻게 해?\", \"좀 더 자세히 알려줘\", \"다른 방법은 없어?\"\n\n");

        prompt.append("## 중요 판단 기준:\n");
        prompt.append("- SUH Project Utility / 기능 / 모듈 관련 → **무조건 KNOWLEDGE_QUERY**\n");
        prompt.append("- 개발자(서새찬, suhsaechan) 관련 → **무조건 KNOWLEDGE_QUERY**\n");
        prompt.append("- 농담/이야기 요청 → **CHITCHAT**\n");
        prompt.append("- 애매하면 **KNOWLEDGE_QUERY 우선** (RAG 검색 활성화)\n\n");

        // 대화 이력 포함 (컨텍스트 제공)
        if (!recentHistory.isEmpty()) {
            prompt.append("## 최근 대화 이력:\n\n");
            int historyLimit = Math.min(recentHistory.size(), 3);
            for (int i = recentHistory.size() - historyLimit; i < recentHistory.size(); i++) {
                ChatMessage msg = recentHistory.get(i);
                String role = msg.getRole() == MessageRole.USER ? "사용자" : "서니";
                prompt.append(role).append(": ").append(msg.getContent()).append("\n");
            }
            prompt.append("\n");
        }

        // 현재 질문
        prompt.append("## 현재 질문:\n\n");
        prompt.append(userMessage);
        prompt.append("\n\n");
        prompt.append("위 질문을 분석하고, 다음 정보를 JSON 형식으로 반환하세요:\n");
        prompt.append("- intentType: 질문 유형 (KNOWLEDGE_QUERY | GREETING | CHITCHAT | CLARIFICATION)\n");
        prompt.append("- needsRagSearch: RAG 검색 필요 여부 (true | false)\n");
        prompt.append("- confidence: 분류 신뢰도 (0.0 ~ 1.0)\n");
        prompt.append("- reason: 이렇게 분류한 구체적인 이유 (50자 이내)\n");
        prompt.append("- summary: 질문의 핵심 내용 요약 (30자 이내)");

        try {
            // Structured Output 스키마 생성
            JsonSchema schema = JsonSchemaClassParser.parse(IntentClassificationDto.class);

            // 의도 분류용 경량 모델 사용
            SuhAiderRequest request = SuhAiderRequest.builder()
                    .model(chatbotProperties.getModels().getIntentClassifier())
                    .prompt(prompt.toString())
                    .stream(false)
                    .responseSchema(schema)  // Structured Output 활성화
                    .build();

            SuhAiderResponse response = suhAiderEngine.generate(request);

            // JSON 파싱 (SUH-AIDER가 자동으로 정제한 JSON)
            IntentClassificationDto intent = objectMapper.readValue(
                response.getResponse(),
                IntentClassificationDto.class
            );

            log.debug("의도 분류 성공 - type: {}, needsRAG: {}, confidence: {}, reason: {}, summary: {}",
                intent.getIntentType(), intent.getNeedsRagSearch(),
                intent.getConfidence(), intent.getReason(), intent.getSummary());

            return intent;

        } catch (Exception e) {
            log.error("의도 분류 실패, 안전 모드로 전환 (RAG 검색 수행): {}", e.getMessage(), e);
            // 실패 시 안전하게 RAG 검색 수행
            return IntentClassificationDto.builder()
                .intentType("UNKNOWN")
                .needsRagSearch(true)
                .confidence(0.5f)
                .reason("분류 오류로 인한 기본값")
                .summary(userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage)
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
     * @return 생성된 응답 텍스트
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
     * 스트리밍 응답 저장 (별도 트랜잭션)
     */
    @Transactional
    public void saveStreamingResponse(ChatSession session, String content, int messageIndex, List<String> referencedDocIds) {
        ChatMessage assistantMessage = saveMessage(session, MessageRole.ASSISTANT, content, messageIndex);
        assistantMessage.setReferencedDocumentIds(String.join(",", referencedDocIds));
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
