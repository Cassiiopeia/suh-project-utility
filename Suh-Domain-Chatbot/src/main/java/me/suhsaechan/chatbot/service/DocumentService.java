package me.suhsaechan.chatbot.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.config.QdrantProperties;
import me.suhsaechan.chatbot.entity.ChatDocument;
import me.suhsaechan.chatbot.entity.ChatDocumentChunk;
import me.suhsaechan.chatbot.repository.ChatDocumentChunkRepository;
import me.suhsaechan.chatbot.repository.ChatDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문서 관리 서비스
 * 문서 CRUD, 청크 분할, 벡터 저장 등 핵심 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final ChatDocumentRepository documentRepository;
    private final ChatDocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final QdrantProperties qdrantProperties;

    // 청크 분할 설정 (토큰 수 기준, 근사치)
    private static final int DEFAULT_CHUNK_SIZE_TOKENS = 3000;       // 기본 청크 크기 (토큰 수)
    private static final int DEFAULT_CHUNK_OVERLAP_TOKENS = 300;     // 청크 중첩 크기 (토큰 수, 10%)

    /**
     * 벡터 컬렉션 초기화
     */
    public void initializeCollection() {
        String collectionName = qdrantProperties.getCollectionName();
        int vectorSize = qdrantProperties.getVectorSize();

        vectorStoreService.createCollectionIfNotExists(collectionName, vectorSize);
        log.info("벡터 컬렉션 초기화 완료 - name: {}, vectorSize: {}", collectionName, vectorSize);
    }

    /**
     * 문서 생성 및 벡터화
     */
    @Transactional
    public ChatDocument createDocument(String title, String category, String content, String description) {
        log.info("문서 생성 시작 - title: {}, category: {}", title, category);

        // 1. 문서 엔티티 생성
        ChatDocument document = ChatDocument.builder()
            .title(title)
            .category(category)
            .content(content)
            .description(description)
            .isActive(true)
            .isProcessed(false)
            .chunkCount(0)
            .build();

        document = documentRepository.save(document);
        log.info("문서 저장 완료 - documentId: {}", document.getChatDocumentId());

        // 2. 청크 분할 및 벡터화
        processDocument(document);

        return document;
    }

    /**
     * 문서 처리 (청크 분할 + 벡터화)
     */
    @Transactional
    public void processDocument(ChatDocument document) {
        log.info("문서 처리 시작 - documentId: {}", document.getChatDocumentId());

        // 1. 기존 청크 삭제 (재처리 시)
        deleteDocumentChunks(document);

        // 2. 청크 분할 (토큰 수 기준)
        List<String> chunks = splitIntoChunksByTokens(document.getContent(), DEFAULT_CHUNK_SIZE_TOKENS, DEFAULT_CHUNK_OVERLAP_TOKENS);
        log.info("청크 분할 완료 - 총 {} 개", chunks.size());

        // 3. 임베딩 생성
        List<List<Float>> embeddings = embeddingService.embedBatch(chunks);

        // 4. Qdrant에 저장 및 청크 엔티티 생성
        List<UUID> pointIds = new ArrayList<>();
        List<Map<String, String>> metadataList = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            UUID pointId = UUID.randomUUID();
            pointIds.add(pointId);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("documentId", document.getChatDocumentId().toString());
            metadata.put("title", document.getTitle());
            metadata.put("category", document.getCategory());
            metadata.put("chunkIndex", String.valueOf(i));
            metadataList.add(metadata);
        }

        // 5. Qdrant에 일괄 저장
        String collectionName = qdrantProperties.getCollectionName();
        vectorStoreService.upsertPoints(collectionName, pointIds, embeddings, chunks, metadataList);

        // 6. 청크 엔티티 저장
        List<ChatDocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ChatDocumentChunk chunk = ChatDocumentChunk.builder()
                .chatDocument(document)
                .pointId(pointIds.get(i))
                .chunkIndex(i)
                .content(chunks.get(i))
                .tokenCount(estimateTokenCount(chunks.get(i)))
                .build();
            chunkEntities.add(chunk);
        }
        chunkRepository.saveAll(chunkEntities);

        // 7. 문서 상태 업데이트
        document.setIsProcessed(true);
        document.setChunkCount(chunks.size());
        documentRepository.save(document);

        log.info("문서 처리 완료 - documentId: {}, chunkCount: {}",
            document.getChatDocumentId(), chunks.size());
    }

    /**
     * 문서 수정
     */
    @Transactional
    public ChatDocument updateDocument(UUID documentId, String title, String category,
        String content, String description) {

        ChatDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다: " + documentId));

        document.setTitle(title);
        document.setCategory(category);
        document.setContent(content);
        document.setDescription(description);
        document.setIsProcessed(false);  // 재처리 필요

        document = documentRepository.save(document);

        // 재처리
        processDocument(document);

        return document;
    }

    /**
     * 문서 삭제
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        ChatDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다: " + documentId));

        // 1. Qdrant에서 벡터 삭제
        deleteDocumentChunks(document);

        // 2. 문서 삭제
        documentRepository.delete(document);
        log.info("문서 삭제 완료 - documentId: {}", documentId);
    }

    /**
     * 문서의 청크 삭제 (Qdrant + PostgreSQL)
     */
    private void deleteDocumentChunks(ChatDocument document) {
        List<ChatDocumentChunk> chunks = chunkRepository
            .findByChatDocumentOrderByChunkIndexAsc(document);

        if (!chunks.isEmpty()) {
            // Qdrant에서 삭제
            List<UUID> pointIds = chunks.stream()
                .map(ChatDocumentChunk::getPointId)
                .collect(Collectors.toList());

            String collectionName = qdrantProperties.getCollectionName();
            vectorStoreService.deletePoints(collectionName, pointIds);

            // PostgreSQL에서 삭제
            chunkRepository.deleteByChatDocument(document);

            log.info("기존 청크 삭제 완료 - documentId: {}, 삭제 개수: {}",
                document.getChatDocumentId(), chunks.size());
        }
    }

    /**
     * 문서 활성화/비활성화
     */
    @Transactional
    public void setDocumentActive(UUID documentId, boolean isActive) {
        ChatDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다: " + documentId));

        document.setIsActive(isActive);
        documentRepository.save(document);

        log.info("문서 활성화 상태 변경 - documentId: {}, isActive: {}", documentId, isActive);
    }

    /**
     * 모든 활성 문서 조회
     */
    @Transactional(readOnly = true)
    public List<ChatDocument> getAllActiveDocuments() {
        return documentRepository.findByIsActiveTrueOrderByOrderIndexAsc();
    }

    /**
     * 카테고리별 문서 조회
     */
    @Transactional(readOnly = true)
    public List<ChatDocument> getDocumentsByCategory(String category) {
        return documentRepository.findByCategoryAndIsActiveTrueOrderByOrderIndexAsc(category);
    }

    /**
     * 문서 ID로 조회
     */
    @Transactional(readOnly = true)
    public ChatDocument getDocument(UUID documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다: " + documentId));
    }

    /**
     * 문서의 청크 조회
     */
    @Transactional(readOnly = true)
    public List<ChatDocumentChunk> getDocumentChunks(UUID documentId) {
        return chunkRepository.findByChatDocumentChatDocumentIdOrderByChunkIndexAsc(documentId);
    }

    /**
     * 텍스트를 토큰 수 기준으로 청크로 분할
     * 문장 경계를 고려하여 분할 (토큰 수는 근사치로 계산)
     */
    private List<String> splitIntoChunksByTokens(String text, int chunkSizeTokens, int overlapTokens) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // 문단 단위로 먼저 분할
        String[] paragraphs = text.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();
        int currentTokenCount = 0;

        for (String paragraph : paragraphs) {
            int paragraphTokens = estimateTokenCount(paragraph);

            // 문단이 청크 크기를 초과하면 문장 단위로 분할
            if (paragraphTokens > chunkSizeTokens) {
                // 현재 청크가 있으면 저장
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    // 오버랩 처리 (토큰 수 기준)
                    String overlapText = getOverlapTextByTokens(currentChunk.toString(), overlapTokens);
                    currentChunk = new StringBuilder(overlapText);
                    currentTokenCount = estimateTokenCount(overlapText);
                }

                // 긴 문단을 문장 단위로 분할
                String[] sentences = paragraph.split("(?<=[.!?。！？])+\\s*");
                for (String sentence : sentences) {
                    int sentenceTokens = estimateTokenCount(sentence);

                    if (currentTokenCount + sentenceTokens > chunkSizeTokens) {
                        if (currentChunk.length() > 0) {
                            chunks.add(currentChunk.toString().trim());
                            // 오버랩 처리 (토큰 수 기준)
                            String overlapText = getOverlapTextByTokens(currentChunk.toString(), overlapTokens);
                            currentChunk = new StringBuilder(overlapText);
                            currentTokenCount = estimateTokenCount(overlapText);
                        }
                    }
                    currentChunk.append(sentence).append(" ");
                    currentTokenCount += sentenceTokens;
                }
            } else {
                // 현재 청크에 문단 추가
                if (currentTokenCount + paragraphTokens > chunkSizeTokens) {
                    chunks.add(currentChunk.toString().trim());
                    // 오버랩 처리 (토큰 수 기준)
                    String overlapText = getOverlapTextByTokens(currentChunk.toString(), overlapTokens);
                    currentChunk = new StringBuilder(overlapText);
                    currentTokenCount = estimateTokenCount(overlapText);
                }
                currentChunk.append(paragraph).append("\n\n");
                currentTokenCount += paragraphTokens;
            }
        }

        // 마지막 청크 저장
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 오버랩 텍스트 추출 (문자 수 기준, 하위 호환성 유지)
     */
    private String getOverlapText(String text, int overlap) {
        if (text.length() <= overlap) {
            return text;
        }
        return text.substring(text.length() - overlap);
    }

    /**
     * 오버랩 텍스트 추출 (토큰 수 기준, 근사치)
     * 토큰 비율로 시작 위치를 추정하여 성능 최적화
     */
    private String getOverlapTextByTokens(String text, int overlapTokens) {
        if (text == null || text.isEmpty() || overlapTokens <= 0) {
            return "";
        }

        // 전체 텍스트의 토큰 수가 목표보다 작으면 전체 반환
        int totalTokens = estimateTokenCount(text);
        if (totalTokens <= overlapTokens) {
            return text;
        }

        // 토큰 비율로 시작 위치 추정 (간단한 근사 방식)
        double tokenRatio = (double) overlapTokens / totalTokens;
        int estimatedStartIndex = (int) (text.length() * (1 - tokenRatio));
        
        // 안전하게 시작 위치 보정
        int startIndex = Math.max(0, estimatedStartIndex);
        String candidate = text.substring(startIndex);
        
        // 한 번만 토큰 수 확인하고 필요시 조정
        int candidateTokens = estimateTokenCount(candidate);
        if (candidateTokens < overlapTokens && startIndex > 0) {
            // 토큰이 부족하면 앞으로 조금 이동
            int adjustChars = (int) ((overlapTokens - candidateTokens) * 2); // 한글 기준
            startIndex = Math.max(0, startIndex - adjustChars);
            candidate = text.substring(startIndex);
        } else if (candidateTokens > overlapTokens * 1.2) {
            // 토큰이 너무 많으면 뒤로 조금 이동
            int adjustChars = (int) ((candidateTokens - overlapTokens) * 2);
            startIndex = Math.min(text.length(), startIndex + adjustChars);
            candidate = text.substring(startIndex);
        }

        return candidate;
    }

    /**
     * 토큰 수 추정 (간단한 휴리스틱)
     * 한글: 글자 수 × 0.5 ≈ 토큰 수
     * 영어: 단어 수 ≈ 토큰 수
     * 혼합: 한글/영어 비율에 따라 가중 평균
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int totalLength = text.length();
        if (totalLength == 0) {
            return 0;
        }

        // 한글 문자 수 계산
        long koreanCount = text.chars()
            .filter(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HANGUL)
            .count();

        double koreanRatio = (double) koreanCount / totalLength;

        if (koreanRatio > 0.5) {
            // 한글 위주: 문자 수 × 0.5
            return (int) (totalLength * 0.5);
        } else if (koreanRatio < 0.1) {
            // 영어 위주: 단어 수
            String[] words = text.split("\\s+");
            return words.length > 0 ? words.length : 1;
        } else {
            // 혼합 텍스트: 한글과 영어를 분리하여 계산
            int koreanTokens = (int) (koreanCount * 0.5);
            // 비한글 문자 길이 기반으로 영어 토큰 추정 (평균 영단어 길이 ~4)
            int nonKoreanLength = totalLength - (int) koreanCount;
            int englishTokens = Math.max(1, (int) (nonKoreanLength / 4.0));
            return koreanTokens + englishTokens;
        }
    }
}
