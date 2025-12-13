package me.suhsaechan.chatbot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.service.DocumentService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 챗봇 초기화 컴포넌트
 * 애플리케이션 시작 시 Qdrant 컬렉션 자동 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatbotInitializer implements ApplicationRunner {

    private final DocumentService documentService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("챗봇 초기화 시작...");
            documentService.initializeCollection();
            log.info("챗봇 초기화 완료");
        } catch (Exception e) {
            log.warn("챗봇 초기화 실패 - Qdrant 연결 확인 필요: {}", e.getMessage());
            // 초기화 실패해도 앱은 계속 실행
        }
    }
}
