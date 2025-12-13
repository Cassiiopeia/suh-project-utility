package me.suhsaechan.chatbot.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QdrantConfig {

    private final QdrantProperties qdrantProperties;

    @Bean
    public QdrantClient qdrantClient() {
        log.info("Qdrant 클라이언트 초기화 - host: {}, grpcPort: {}",
            qdrantProperties.getHost(), qdrantProperties.getGrpcPort());

        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
            qdrantProperties.getHost(),
            qdrantProperties.getGrpcPort(),
            qdrantProperties.getUseTls()
        );

        // API Key가 있으면 설정
        if (qdrantProperties.getApiKey() != null && !qdrantProperties.getApiKey().isEmpty()) {
            builder.withApiKey(qdrantProperties.getApiKey());
        }

        return new QdrantClient(builder.build());
    }
}
