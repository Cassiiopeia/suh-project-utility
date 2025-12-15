package me.suhsaechan.chatbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 챗봇 설정 Properties
 * application.yml의 chatbot.* 설정을 바인딩
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chatbot")
public class ChatbotProperties {

    private Models models = new Models();
    private Agent agent = new Agent();

    /**
     * AI 모델 설정
     */
    @Getter
    @Setter
    public static class Models {
        /**
         * 의도 분류 모델 (경량 모델)
         * 예: gemma3:1b
         */
        private String intentClassifier = "gemma3:1b";

        /**
         * 최종 응답 생성 모델 (고품질 모델)
         * 예: rnj-1:8b
         */
        private String responseGenerator = "rnj-1:8b";

        /**
         * 임베딩 모델
         * 예: embeddinggemma:latest
         */
        private String embedding = "embeddinggemma:latest";
    }

    /**
     * Agent 동작 설정
     */
    @Getter
    @Setter
    public static class Agent {
        private Rag rag = new Rag();
        private History history = new History();

        /**
         * RAG 검색 설정
         */
        @Getter
        @Setter
        public static class Rag {
            /**
             * 검색 결과 최대 개수
             */
            private Integer topK = 3;

            /**
             * 최소 유사도 점수
             */
            private Float minScore = 0.5f;
        }

        /**
         * 대화 이력 설정
         */
        @Getter
        @Setter
        public static class History {
            /**
             * 최대 대화 이력 메시지 수
             */
            private Integer maxMessages = 30;
        }
    }
}
