package me.suhsaechan.statistics.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;

/**
 * 대시보드 Summary DTO
 * 대시보드 상단 통계 카드에 표시할 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {

    // === 방문자 통계 ===
    private Long totalUniqueVisitors;     // 총 고유 방문자 (clientHash 기준)
    private Long todayUniqueVisitors;     // 오늘 고유 방문자
    private Long totalPageViews;          // 총 페이지뷰
    private Long todayPageViews;          // 오늘 페이지뷰

    // === 챗봇 통계 ===
    private Long totalChatSessions;       // 총 챗봇 세션 수
    private Long todayChatSessions;       // 오늘 챗봇 세션 수
    private Long totalChatMessages;       // 총 메시지 수
    private Long todayChatMessages;       // 오늘 메시지 수

    // === 토큰 사용량 ===
    private Long totalInputTokens;        // 총 입력 토큰
    private Long totalOutputTokens;       // 총 출력 토큰
    private Long todayInputTokens;        // 오늘 입력 토큰
    private Long todayOutputTokens;       // 오늘 출력 토큰

    // === 콘텐츠 통계 ===
    private Long totalNoticeViews;        // 공지사항 총 조회수
    private Long totalStudyViews;         // 스터디 총 조회수

    // === 기능별 사용 통계 (확장용) ===
    private Map<FeatureType, Long> featureUsageCounts;
}
