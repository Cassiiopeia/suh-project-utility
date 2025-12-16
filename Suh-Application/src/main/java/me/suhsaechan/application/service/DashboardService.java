package me.suhsaechan.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.repository.ChatMessageRepository;
import me.suhsaechan.chatbot.repository.ChatSessionRepository;
import me.suhsaechan.notice.repository.SuhProjectUtilityNoticeRepository;
import me.suhsaechan.statistics.dto.DashboardSummaryDto;
import me.suhsaechan.statistics.service.StatisticsService;
import me.suhsaechan.study.repository.StudyPostRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 통합 서비스
 * 여러 도메인 모듈의 통계 데이터를 집계하여 대시보드 Summary 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StatisticsService statisticsService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SuhProjectUtilityNoticeRepository noticeRepository;
    private final StudyPostRepository studyPostRepository;

    private static final String DASHBOARD_SUMMARY_CACHE = "dashboardSummary";

    /**
     * 대시보드 Summary 데이터 조회
     * 모든 통계 데이터를 집계하여 반환 (5분 캐싱)
     */
    @Cacheable(value = DASHBOARD_SUMMARY_CACHE, key = "'summary'")
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        log.debug("대시보드 Summary 데이터 조회 시작");
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        try {
            // 방문자 통계
            long totalUniqueVisitors = statisticsService.getTotalUniqueVisitors();
            long todayUniqueVisitors = statisticsService.getTodayUniqueVisitors();
            long totalPageViews = statisticsService.getTotalPageViews();
            long todayPageViews = statisticsService.getTodayPageViews();

            // 챗봇 통계
            long totalChatSessions = chatSessionRepository.count();
            long todayChatSessions = chatSessionRepository.countByCreatedDateAfter(todayStart);
            long totalChatMessages = chatMessageRepository.count();
            long todayChatMessages = chatMessageRepository.countByCreatedDateAfter(todayStart);

            // 토큰 사용량
            Long totalInputTokens = chatMessageRepository.sumTotalInputTokens();
            Long totalOutputTokens = chatMessageRepository.sumTotalOutputTokens();
            Long todayInputTokens = chatMessageRepository.sumInputTokensAfter(todayStart);
            Long todayOutputTokens = chatMessageRepository.sumOutputTokensAfter(todayStart);

            // 콘텐츠 조회수
            Long totalNoticeViews = noticeRepository.sumTotalViewCount();
            Long totalStudyViews = studyPostRepository.sumTotalViewCount();

            DashboardSummaryDto summary = DashboardSummaryDto.builder()
                .totalUniqueVisitors(totalUniqueVisitors)
                .todayUniqueVisitors(todayUniqueVisitors)
                .totalPageViews(totalPageViews)
                .todayPageViews(todayPageViews)
                .totalChatSessions(totalChatSessions)
                .todayChatSessions(todayChatSessions)
                .totalChatMessages(totalChatMessages)
                .todayChatMessages(todayChatMessages)
                .totalInputTokens(totalInputTokens)
                .totalOutputTokens(totalOutputTokens)
                .todayInputTokens(todayInputTokens)
                .todayOutputTokens(todayOutputTokens)
                .totalNoticeViews(totalNoticeViews)
                .totalStudyViews(totalStudyViews)
                .featureUsageCounts(statisticsService.getFeatureUsageCounts())
                .build();

            log.debug("대시보드 Summary 데이터 조회 완료 - 방문자: {}, 챗봇세션: {}, 토큰: {}",
                totalUniqueVisitors, totalChatSessions, totalInputTokens + totalOutputTokens);

            return summary;

        } catch (Exception e) {
            log.error("대시보드 Summary 데이터 조회 실패: {}", e.getMessage(), e);
            // 에러 발생 시 빈 데이터 반환
            return DashboardSummaryDto.builder()
                .totalUniqueVisitors(0L)
                .todayUniqueVisitors(0L)
                .totalPageViews(0L)
                .todayPageViews(0L)
                .totalChatSessions(0L)
                .todayChatSessions(0L)
                .totalChatMessages(0L)
                .todayChatMessages(0L)
                .totalInputTokens(0L)
                .totalOutputTokens(0L)
                .todayInputTokens(0L)
                .todayOutputTokens(0L)
                .totalNoticeViews(0L)
                .totalStudyViews(0L)
                .build();
        }
    }

    /**
     * 대시보드 Summary 캐시 무효화 후 새로 조회
     * 새로고침 버튼 클릭 시 호출
     */
    @CacheEvict(value = DASHBOARD_SUMMARY_CACHE, key = "'summary'")
    public void evictDashboardSummaryCache() {
        log.info("대시보드 Summary 캐시 무효화");
    }

    /**
     * 캐시 무효화 후 새 데이터 조회
     */
    public DashboardSummaryDto refreshDashboardSummary() {
        evictDashboardSummaryCache();
        return getDashboardSummary();
    }
}
