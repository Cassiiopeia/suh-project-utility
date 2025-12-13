package me.suhsaechan.chatbot.service;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.dto.VectorSearchResult;
import org.springframework.stereotype.Service;

/**
 * Qdrant 벡터 저장소 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantVectorStoreService implements VectorStoreService {

    private final QdrantClient qdrantClient;

    @Override
    public void createCollectionIfNotExists(String collectionName, int vectorSize) {
        try {
            if (!collectionExists(collectionName)) {
                log.info("Qdrant 컬렉션 생성 - name: {}, vectorSize: {}", collectionName, vectorSize);

                qdrantClient.createCollectionAsync(
                    collectionName,
                    VectorParams.newBuilder()
                        .setSize(vectorSize)
                        .setDistance(Distance.Cosine)
                        .build()
                ).get();

                log.info("Qdrant 컬렉션 생성 완료 - {}", collectionName);
            } else {
                log.debug("Qdrant 컬렉션이 이미 존재함 - {}", collectionName);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant 컬렉션 생성 실패 - {}", collectionName, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant 컬렉션 생성 실패", e);
        }
    }

    @Override
    public boolean collectionExists(String collectionName) {
        try {
            // listCollections로 컬렉션 존재 여부 확인
            var collections = qdrantClient.listCollectionsAsync().get();
            return collections.stream()
                .anyMatch(name -> name.equals(collectionName));
        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant 컬렉션 존재 확인 실패 - {}", collectionName, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void deleteCollection(String collectionName) {
        try {
            log.info("Qdrant 컬렉션 삭제 - {}", collectionName);
            qdrantClient.deleteCollectionAsync(collectionName).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant 컬렉션 삭제 실패 - {}", collectionName, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant 컬렉션 삭제 실패", e);
        }
    }

    @Override
    public void upsertPoint(String collectionName, UUID pointId, List<Float> vector,
        String content, Map<String, String> metadata) {

        try {
            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new HashMap<>();
            payload.put("content", value(content));

            if (metadata != null) {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    payload.put(entry.getKey(), value(entry.getValue()));
                }
            }

            PointStruct point = PointStruct.newBuilder()
                .setId(id(pointId))
                .setVectors(vectors(vector))
                .putAllPayload(payload)
                .build();

            qdrantClient.upsertAsync(collectionName, List.of(point)).get();
            log.debug("Qdrant 포인트 저장 완료 - pointId: {}", pointId);

        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant 포인트 저장 실패 - pointId: {}", pointId, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant 포인트 저장 실패", e);
        }
    }

    @Override
    public void upsertPoints(String collectionName, List<UUID> pointIds, List<List<Float>> vectors,
        List<String> contents, List<Map<String, String>> metadataList) {

        // 입력 검증
        if (pointIds == null || vectors == null || contents == null) {
            throw new IllegalArgumentException("upsertPoints 필수 파라미터는 null일 수 없습니다");
        }

        if (pointIds.size() != vectors.size() || pointIds.size() != contents.size()) {
            throw new IllegalArgumentException(
                String.format("upsertPoints 입력 크기 불일치: pointIds=%d, vectors=%d, contents=%d",
                    pointIds.size(), vectors.size(), contents.size()));
        }

        if (metadataList != null && metadataList.size() != pointIds.size()) {
            throw new IllegalArgumentException(
                String.format("upsertPoints 입력 크기 불일치: metadataList=%d (기대값: %d)",
                    metadataList.size(), pointIds.size()));
        }

        try {
            List<PointStruct> points = new ArrayList<>();

            for (int i = 0; i < pointIds.size(); i++) {
                Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new HashMap<>();
                payload.put("content", value(contents.get(i)));

                if (metadataList != null && metadataList.get(i) != null) {
                    for (Map.Entry<String, String> entry : metadataList.get(i).entrySet()) {
                        payload.put(entry.getKey(), value(entry.getValue()));
                    }
                }

                PointStruct point = PointStruct.newBuilder()
                    .setId(id(pointIds.get(i)))
                    .setVectors(vectors(vectors.get(i)))
                    .putAllPayload(payload)
                    .build();

                points.add(point);
            }

            qdrantClient.upsertAsync(collectionName, points).get();
            log.info("Qdrant 포인트 일괄 저장 완료 - 개수: {}", pointIds.size());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant 포인트 일괄 저장 실패", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant 포인트 일괄 저장 실패", e);
        }
    }

    @Override
    public List<VectorSearchResult> search(String collectionName, List<Float> queryVector, int topK) {
        return search(collectionName, queryVector, topK, 0.0f);
    }

    @Override
    public List<VectorSearchResult> search(String collectionName, List<Float> queryVector, int topK, float minScore) {
        try {
            // SearchPoints 객체로 검색 요청 생성
            SearchPoints searchRequest = SearchPoints.newBuilder()
                .setCollectionName(collectionName)
                .addAllVector(queryVector)
                .setLimit(topK)
                .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                .build();

            List<ScoredPoint> scoredPoints = qdrantClient.searchAsync(searchRequest).get();

            return scoredPoints.stream()
                .filter(sp -> sp.getScore() >= minScore)
                .map(this::convertToSearchResult)
                .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant 검색 실패", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant 검색 실패", e);
        }
    }

    @Override
    public void deletePoint(String collectionName, UUID pointId) {
        deletePoints(collectionName, List.of(pointId));
    }

    @Override
    public void deletePoints(String collectionName, List<UUID> pointIds) {
        try {
            var qdrantPointIds = pointIds.stream()
                .map(uuid -> id(uuid))
                .toList();
            
            qdrantClient.deleteAsync(collectionName, qdrantPointIds).get();
            log.info("Qdrant 포인트 삭제 완료 - 개수: {}", pointIds.size());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant 포인트 삭제 실패", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant 포인트 삭제 실패", e);
        }
    }

    @Override
    public long countPoints(String collectionName) {
        try {
            var collectionInfo = qdrantClient.getCollectionInfoAsync(collectionName).get();
            return collectionInfo.getPointsCount();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant 포인트 수 조회 실패 - {}", collectionName, e);
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    /**
     * ScoredPoint를 VectorSearchResult로 변환
     */
    private VectorSearchResult convertToSearchResult(ScoredPoint scoredPoint) {
        Map<String, String> metadata = new HashMap<>();
        String content = "";

        for (Map.Entry<String, io.qdrant.client.grpc.JsonWithInt.Value> entry :
            scoredPoint.getPayloadMap().entrySet()) {

            String key = entry.getKey();
            String value = entry.getValue().getStringValue();

            if ("content".equals(key)) {
                content = value;
            } else {
                metadata.put(key, value);
            }
        }

        UUID pointId = UUID.fromString(scoredPoint.getId().getUuid());

        return VectorSearchResult.builder()
            .pointId(pointId)
            .score(scoredPoint.getScore())
            .content(content)
            .metadata(metadata)
            .build();
    }
}
