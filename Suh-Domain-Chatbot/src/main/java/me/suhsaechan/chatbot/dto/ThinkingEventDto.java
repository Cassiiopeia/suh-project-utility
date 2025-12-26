package me.suhsaechan.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇 생각 과정 이벤트 DTO
 * SSE를 통해 사용자에게 AI의 처리 과정을 실시간으로 전달합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThinkingEventDto {

    /**
     * 현재 단계 번호 (1, 2, 3)
     */
    private Integer step;

    /**
     * 전체 단계 수 (기본 3)
     */
    private Integer totalSteps;

    /**
     * 단계 상태
     * - in_progress: 진행 중
     * - completed: 완료
     * - skipped: 생략 (예: RAG 검색 불필요 시)
     * - retrying: 재시도 중
     */
    private String status;

    /**
     * 단계 제목 (UI에 표시)
     * 예: "질문 분석 중...", "문서 검색 중...", "응답 생성 중..."
     */
    private String title;

    /**
     * 상세 정보 (UI에 표시)
     * 예: "지식 질문 (95%)", "3개 문서 검색됨"
     */
    private String detail;

    /**
     * 검색 쿼리 (Step 2에서 사용, UI 표시용)
     */
    private String searchQuery;
}
