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

    private Agent agent = new Agent();

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
