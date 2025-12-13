package me.suhsaechan.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    // 세션 토큰 (없으면 새 세션 생성)
    private String sessionToken;

    // 사용자 메시지
    private String message;

    // 검색할 문서 수 (기본값: 3)
    private Integer topK;

    // 최소 유사도 점수 (기본값: 0.5)
    private Float minScore;
}
