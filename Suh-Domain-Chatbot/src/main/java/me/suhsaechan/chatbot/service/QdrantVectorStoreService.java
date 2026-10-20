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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.dto.VectorSearchResult;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

/**
 * Qdrant 벡터 저장소 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantVectorStoreService implements VectorStoreService {

    // Qdrant 비동기 호출 대기 최대 시간 (초)
    private static final long QDRANT_TIMEOUT_SECONDS = 30;

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
                ).get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                log.info("Qdrant 컬렉션 생성 완료 - {}", collectionName);
            } else {
                log.debug("Qdrant 컬렉션이 이미 존재함 - {}", collectionName);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw toVectorStoreException(e, "Qdrant 컬렉션 생성 실패 - {}", collectionName);
        }
    }

    @Override
    public boolean collectionExists(String collectionName) {
        try {
            // listCollections로 컬렉션 존재 여부 확인
            var collections = qdrantClient.listCollectionsAsync().get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return collections.stream()
                .anyMatch(name -> name.equals(collectionName));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw toVectorStoreException(e, "Qdrant 컬렉션 존재 확인 실패 - {}", collectionName);
        }
    }

    @Override
    public void deleteCollection(String collectionName) {
        try {
            log.info("Qdrant 컬렉션 삭제 - {}", collectionName);
            qdrantClient.deleteCollectionAsync(collectionName).get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw toVectorStoreException(e, "Qdrant 컬렉션 삭제 실패 - {}", collectionName);
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

            qdrantClient.upsertAsync(collectionName, List.of(point)).get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.debug("Qdrant 포인트 저장 완료 - pointId: {}", pointId);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw toVectorStoreException(e, "Qdrant 포인트 저장 실패 - pointId: {}", pointId);
        }
    }

    @Override
    public void upsertPoints(String collectionName, List<UUID> pointIds, List<List<Float>> vectors,
        List<String> contents, List<Map<String, String>> metadataList) {

        // 입력 검증
        if (pointIds == null || vectors == null || contents == null) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        if (pointIds.size() != vectors.size() || pointIds.size() != contents.size()) {
            log.error("upsertPoints 입력 크기 불일치: pointIds={}, vectors={}, contents={}",
                    pointIds.size(), vectors.size(), contents.size());
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        if (metadataList != null && metadataList.size() != pointIds.size()) {
            log.error("upsertPoints 입력 크기 불일치: metadataList={} (기대값: {})",
                    metadataList.size(), pointIds.size());
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
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

            qdrantClient.upsertAsync(collectionName, points).get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Qdrant 포인트 일괄 저장 완료 - 개수: {}", pointIds.size());

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw toVectorStoreException(e, "Qdrant 포인트 일괄 저장 실패");
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

            List<ScoredPoint> scoredPoints = qdrantClient.searchAsync(searchRequest).get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            logSearchDiagnostics(collectionName, scoredPoints, minScore, topK);

            return scoredPoints.stream()
                .filter(sp -> sp.getScore() >= minScore)
                .map(this::convertToSearchResult)
                .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw toVectorStoreException(e, "Qdrant 검색 실패");
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
            
            qdrantClient.deleteAsync(collectionName, qdrantPointIds).get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Qdrant 포인트 삭제 완료 - 개수: {}", pointIds.size());

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw toVectorStoreException(e, "Qdrant 포인트 삭제 실패");
        }
    }

    @Override
    public long countPoints(String collectionName) {
        try {
            var collectionInfo = qdrantClient.getCollectionInfoAsync(collectionName).get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return collectionInfo.getPointsCount();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw toVectorStoreException(e, "Qdrant 포인트 수 조회 실패 - {}", collectionName);
        }
    }

    /**
     * RAG 검색 진단 로그: 필터링 전 Qdrant 원본 결과를 노출한다.
     *
     * <p>"결과 0건"이 minScore 필터 미달 때문인지(원본 N건), Qdrant 원본부터 0건인지를
     * 로그만으로 판별할 수 있게 한다.</p>
     */
    private void logSearchDiagnostics(String collectionName, List<ScoredPoint> scoredPoints,
                                      float minScore, int topK) {
        long pointCount = countPoints(collectionName);
        log.info("[RAG-Search] collection={}, 컬렉션 포인트 수={}, minScore={}, topK={}",
            collectionName, pointCount, minScore, topK);

        if (scoredPoints.isEmpty()) {
            log.info("[RAG-Search] Qdrant 원본 0건 → 컬렉션이 비었거나 차원/컬렉션 불일치 의심");
            return;
        }

        log.info("[RAG-Search] Qdrant 원본 {}건:", scoredPoints.size());
        long passCount = 0;
        for (ScoredPoint sp : scoredPoints) {
            boolean pass = sp.getScore() >= minScore;
            if (pass) {
                passCount++;
            }
            String content = sp.getPayloadMap().getOrDefault("content",
                io.qdrant.client.grpc.JsonWithInt.Value.getDefaultInstance()).getStringValue();
            String preview = content.length() > 40 ? content.substring(0, 40) : content;
            log.info("  - score={} PASS={} payload=\"{}\"",
                String.format("%.3f", sp.getScore()), pass ? "Y" : "N", preview);
        }
        log.info("[RAG-Search] 필터 후 {}건 (원본 {}건 중 minScore={} 통과)",
            passCount, scoredPoints.size(), minScore);
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

    /**
     * Qdrant 호출 실패 > CHATBOT_VECTOR_STORE_ERROR (인터럽트일 때만 플래그 복구)
     */
    private CustomException toVectorStoreException(Exception e, String message, Object... messageArgs) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        Object[] logArgs = Arrays.copyOf(messageArgs, messageArgs.length + 1);
        logArgs[messageArgs.length] = e;
        log.error(message, logArgs);
        return new CustomException(ErrorCode.CHATBOT_VECTOR_STORE_ERROR);
    }
}
