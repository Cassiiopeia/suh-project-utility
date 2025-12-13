package me.suhsaechan.chatbot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.config.QdrantProperties;
import org.springframework.stereotype.Service;

/**
 * Mock 임베딩 서비스 (개발/테스트용)
 * 실제 LLM 연동 전까지 사용하는 더미 임베딩 생성기
 * 나중에 OllamaEmbeddingService로 교체 예정
 */
@Slf4j
@Service
public class MockEmbeddingService implements EmbeddingService {

    private final int dimension;
    private final Random random = new Random();

    public MockEmbeddingService(QdrantProperties qdrantProperties) {
        this.dimension = qdrantProperties.getVectorSize();
        log.info("MockEmbeddingService 초기화 - 벡터 차원: {}", dimension);
    }

    @Override
    public List<Float> embed(String text) {
        log.debug("Mock 임베딩 생성 - 텍스트 길이: {}", text.length());

        // 텍스트 해시 기반으로 일관된 랜덤 시드 생성 (같은 텍스트 = 같은 벡터)
        Random seededRandom = new Random(text.hashCode());

        List<Float> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            // -1.0 ~ 1.0 범위의 정규화된 랜덤 값
            vector.add((seededRandom.nextFloat() * 2) - 1);
        }

        return normalizeVector(vector);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        log.debug("Mock 일괄 임베딩 생성 - 텍스트 수: {}", texts.size());

        List<List<Float>> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(embed(text));
        }
        return embeddings;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelName() {
        return "mock-embedding";
    }

    /**
     * 벡터 정규화 (L2 norm = 1)
     */
    private List<Float> normalizeVector(List<Float> vector) {
        double norm = 0.0;
        for (Float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        List<Float> normalized = new ArrayList<>(vector.size());
        for (Float v : vector) {
            normalized.add((float) (v / norm));
        }
        return normalized;
    }
}
