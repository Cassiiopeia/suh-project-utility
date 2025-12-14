package me.suhsaechan.chatbot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import kr.suhsaechan.ai.service.SuhAiderEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.config.QdrantProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * SUH-AIDER 기반 임베딩 서비스
 * SuhAiderEngine.embed() API를 사용하여 텍스트를 벡터로 변환
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class SuhAiderEmbeddingService implements EmbeddingService {

    private static final String EMBEDDING_MODEL = "embeddinggemma:latest";

    private final SuhAiderEngine suhAiderEngine;
    private final QdrantProperties qdrantProperties;

    @Override
    public List<Float> embed(String text) {
        log.debug("임베딩 생성 요청 - 텍스트 길이: {}", text.length());

        List<Double> vector = suhAiderEngine.embed(EMBEDDING_MODEL, text);

        // Double -> Float 변환
        List<Float> floatVector = vector.stream()
            .map(Double::floatValue)
            .collect(Collectors.toList());

        log.debug("임베딩 생성 완료 - 벡터 차원: {}", floatVector.size());
        return floatVector;
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        log.debug("일괄 임베딩 생성 요청 - 텍스트 수: {}", texts.size());

        List<List<Double>> vectors = suhAiderEngine.embed(EMBEDDING_MODEL, texts);

        // Double -> Float 변환
        List<List<Float>> floatVectors = new ArrayList<>();
        for (List<Double> vector : vectors) {
            List<Float> floatVector = vector.stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());
            floatVectors.add(floatVector);
        }

        log.debug("일괄 임베딩 생성 완료 - 생성된 벡터 수: {}", floatVectors.size());
        return floatVectors;
    }

    @Override
    public int getDimension() {
        return qdrantProperties.getVectorSize();
    }

    @Override
    public String getModelName() {
        return EMBEDDING_MODEL;
    }
}
