package me.suhsaechan.chatbot.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.suhsaechan.chatbot.dto.VectorSearchResult;

/**
 * 벡터 저장소 서비스 인터페이스
 * Qdrant, Milvus, Pinecone 등 다양한 벡터DB로 교체 가능
 */
public interface VectorStoreService {

    /**
     * 컬렉션 생성 (없으면 생성, 있으면 스킵)
     * @param collectionName 컬렉션명
     * @param vectorSize 벡터 차원 수
     */
    void createCollectionIfNotExists(String collectionName, int vectorSize);

    /**
     * 컬렉션 존재 여부 확인
     * @param collectionName 컬렉션명
     * @return 존재 여부
     */
    boolean collectionExists(String collectionName);

    /**
     * 컬렉션 삭제
     * @param collectionName 컬렉션명
     */
    void deleteCollection(String collectionName);

    /**
     * 벡터 포인트 저장 (Upsert)
     * @param collectionName 컬렉션명
     * @param pointId 포인트 ID
     * @param vector 벡터 데이터
     * @param content 원본 텍스트 내용
     * @param metadata 메타데이터
     */
    void upsertPoint(String collectionName, UUID pointId, List<Float> vector,
        String content, Map<String, String> metadata);

    /**
     * 여러 벡터 포인트 일괄 저장
     * @param collectionName 컬렉션명
     * @param pointIds 포인트 ID 목록
     * @param vectors 벡터 목록
     * @param contents 원본 텍스트 목록
     * @param metadataList 메타데이터 목록
     */
    void upsertPoints(String collectionName, List<UUID> pointIds, List<List<Float>> vectors,
        List<String> contents, List<Map<String, String>> metadataList);

    /**
     * 유사도 검색
     * @param collectionName 컬렉션명
     * @param queryVector 검색 쿼리 벡터
     * @param topK 반환할 최대 결과 수
     * @return 검색 결과 목록
     */
    List<VectorSearchResult> search(String collectionName, List<Float> queryVector, int topK);

    /**
     * 유사도 검색 (점수 필터링)
     * @param collectionName 컬렉션명
     * @param queryVector 검색 쿼리 벡터
     * @param topK 반환할 최대 결과 수
     * @param minScore 최소 유사도 점수 (0.0 ~ 1.0)
     * @return 검색 결과 목록
     */
    List<VectorSearchResult> search(String collectionName, List<Float> queryVector, int topK, float minScore);

    /**
     * 포인트 삭제
     * @param collectionName 컬렉션명
     * @param pointId 삭제할 포인트 ID
     */
    void deletePoint(String collectionName, UUID pointId);

    /**
     * 여러 포인트 삭제
     * @param collectionName 컬렉션명
     * @param pointIds 삭제할 포인트 ID 목록
     */
    void deletePoints(String collectionName, List<UUID> pointIds);

    /**
     * 컬렉션 내 포인트 수 조회
     * @param collectionName 컬렉션명
     * @return 포인트 수
     */
    long countPoints(String collectionName);
}
