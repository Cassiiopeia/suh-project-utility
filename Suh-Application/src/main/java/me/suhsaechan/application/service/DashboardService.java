package me.suhsaechan.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.repository.ChatMessageRepository;
import me.suhsaechan.chatbot.repository.ChatSessionRepository;
import me.suhsaechan.common.dto.DailyStatDto;
import me.suhsaechan.notice.repository.SuhProjectUtilityNoticeRepository;
import me.suhsaechan.statistics.dto.DashboardSummaryDto;
import me.suhsaechan.statistics.service.StatisticsService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StatisticsService statisticsService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SuhProjectUtilityNoticeRepository noticeRepository;

    private static final String DASHBOARD_SUMMARY_CACHE = "dashboardSummary";

    @Cacheable(value = DASHBOARD_SUMMARY_CACHE, key = "'summary'")
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        log.debug("대시보드 Summary 데이터 조회 시작");
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        try {
            long totalUniqueVisitors = statisticsService.getTotalUniqueVisitors();
            long todayUniqueVisitors = statisticsService.getTodayUniqueVisitors();
            long totalPageViews = statisticsService.getTotalPageViews();
            long todayPageViews = statisticsService.getTodayPageViews();

            long totalChatSessions = chatSessionRepository.count();
            long todayChatSessions = chatSessionRepository.countByCreatedDateAfter(todayStart);
            long totalChatMessages = chatMessageRepository.count();
            long todayChatMessages = chatMessageRepository.countByCreatedDateAfter(todayStart);

            Long totalInputTokens = chatMessageRepository.sumTotalInputTokens();
            Long totalOutputTokens = chatMessageRepository.sumTotalOutputTokens();
            Long todayInputTokens = chatMessageRepository.sumInputTokensAfter(todayStart);
            Long todayOutputTokens = chatMessageRepository.sumOutputTokensAfter(todayStart);

            Long totalNoticeViews = noticeRepository.sumTotalViewCount();

            Long totalProfileViews = statisticsService.getTotalProfileViews();
            Long todayProfileViews = statisticsService.getTodayProfileViews();

            Long totalSejongAuth = statisticsService.getTotalSejongAuth();
            Long todaySejongAuth = statisticsService.getTodaySejongAuth();

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
                .totalProfileViews(totalProfileViews)
                .todayProfileViews(todayProfileViews)
                .totalSejongAuth(totalSejongAuth)
                .todaySejongAuth(todaySejongAuth)
                .featureUsageCounts(statisticsService.getFeatureUsageCounts())
                .todayFeatureUsageCounts(statisticsService.getTodayFeatureUsageCounts())
                .build();

            log.debug("대시보드 Summary 데이터 조회 완료 - 방문자: {}, 챗봇세션: {}, 토큰: {}",
                totalUniqueVisitors, totalChatSessions, totalInputTokens + totalOutputTokens);

            return summary;

        } catch (Exception e) {
            log.error("대시보드 Summary 데이터 조회 실패: {}", e.getMessage(), e);
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
                .totalProfileViews(0L)
                .todayProfileViews(0L)
                .totalSejongAuth(0L)
                .todaySejongAuth(0L)
                .featureUsageCounts(java.util.Collections.emptyMap())
                .todayFeatureUsageCounts(java.util.Collections.emptyMap())
                .build();
        }
    }

    @CacheEvict(value = DASHBOARD_SUMMARY_CACHE, key = "'summary'")
    public void evictDashboardSummaryCache() {
        log.info("대시보드 Summary 캐시 무효화");
    }

    public DashboardSummaryDto refreshDashboardSummary() {
        evictDashboardSummaryCache();
        return getDashboardSummary();
    }

    @Transactional(readOnly = true)
    public List<DailyStatDto> getDailyChatMessages(int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        return mapToDailyStatDto(chatMessageRepository.countDailyMessagesSince(since));
    }

    @Transactional(readOnly = true)
    public List<DailyStatDto> getDailyChatTokens(int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        return mapToDailyStatDto(chatMessageRepository.countDailyTokensSince(since));
    }

    private List<DailyStatDto> mapToDailyStatDto(List<Object[]> results) {
        return results.stream()
            .map(row -> DailyStatDto.builder()
                .date(((java.sql.Date) row[0]).toLocalDate())
                .count(((Number) row[1]).longValue())
                .build())
            .collect(Collectors.toList());
    }
}
