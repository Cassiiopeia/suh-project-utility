package me.suhsaechan.chatbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {
    private String host = "localhost";
    private Integer port = 6333;
    private Integer grpcPort = 6334;
    private String apiKey = "";
    private Boolean useTls = false;
    private String collectionName = "chatbot-docs";
    private Integer vectorSize = 768;
}
