package me.suhsaechan.chatbot.service;

import me.suhsaechan.chatbot.dto.ThinkingEventDto;

/**
 * 챗봇 생각 과정 콜백 인터페이스
 * Agent-LLM의 각 단계 진행 상황을 실시간으로 전달합니다.
 *
 * <p>사용 예제:</p>
 * <pre>
 * chatbotService.chatStream(request, new ThinkingCallback() {
 *     &#64;Override
 *     public void onThinking(ThinkingEventDto event) {
 *         // SSE로 thinking 이벤트 전송
 *         emitter.send(SseEmitter.event()
 *             .name("thinking")
 *             .data(event));
 *     }
 * }, streamCallback);
 * </pre>
 */
public interface ThinkingCallback {

    /**
     * 생각 과정 이벤트 발생 시 호출됩니다.
     * 각 Agent 단계의 시작, 완료, 재시도 등의 상태를 전달합니다.
     *
     * @param event 생각 과정 이벤트 정보
     */
    void onThinking(ThinkingEventDto event);
}
