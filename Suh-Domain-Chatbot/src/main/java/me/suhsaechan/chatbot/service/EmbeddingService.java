package me.suhsaechan.chatbot.service;

import java.util.List;

/**
 * 임베딩 서비스 인터페이스
 * 텍스트를 벡터로 변환하는 기능을 추상화
 * - Ollama embedding-gemma (768차원)
 * - OpenAI text-embedding-ada-002
 * - Vertex AI text-embedding-005
 * 등 다양한 임베딩 모델로 교체 가능
 */
public interface EmbeddingService {

    /**
     * 단일 텍스트를 벡터로 변환
     * @param text 변환할 텍스트
     * @return 벡터 (float 배열)
     */
    List<Float> embed(String text);

    /**
     * 여러 텍스트를 벡터로 일괄 변환
     * @param texts 변환할 텍스트 목록
     * @return 벡터 목록
     */
    List<List<Float>> embedBatch(List<String> texts);

    /**
     * 임베딩 모델의 벡터 차원 수 반환
     * @return 벡터 차원 수 (예: 768, 1536)
     */
    int getDimension();

    /**
     * 임베딩 모델명 반환
     * @return 모델명 (예: "embedding-gemma", "text-embedding-ada-002")
     */
    String getModelName();
}
