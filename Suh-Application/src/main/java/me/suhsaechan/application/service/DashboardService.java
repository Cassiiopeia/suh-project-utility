package me.suhsaechan.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.statistics.dto.DailyStatisticsDto;
import me.suhsaechan.statistics.dto.DashboardStatisticsDto;
import me.suhsaechan.statistics.dto.StatisticsPeriodTotalDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.chatbot.repository.ChatMessageRepository;
import me.suhsaechan.chatbot.repository.ChatSessionRepository;
import me.suhsaechan.notice.repository.SuhProjectUtilityNoticeRepository;
import me.suhsaechan.statistics.dto.DashboardSummaryDto;
import me.suhsaechan.statistics.service.StatisticsService;
import org.springframework.cache.annotation.CachePut;
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
    private static final String DASHBOARD_SUMMARY_CACHE = "dashboardSummary";
    private static final String DASHBOARD_STATISTICS_CACHE = "dashboardStatistics";
    // 조회 가능한 기간 일수
    private static final Set<Integer> ALLOWED_PERIOD_DAYS = Set.of(7, 30, 90);

    /**
     * 기간 일수 검증: 7, 30, 90 외·누락은 INVALID_PARAMETER
     */
    public void validatePeriodDays(Integer periodDays) {
        if (Optional.ofNullable(periodDays).filter(ALLOWED_PERIOD_DAYS::contains).isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }
    }

    /**
     * 기간 통계 조회 (5분 캐싱, 기간별 키)
     */
    @Cacheable(value = DASHBOARD_STATISTICS_CACHE, key = "#periodDays")
    @Transactional(readOnly = true)
    public DashboardStatisticsDto getDashboardStatistics(Integer periodDays) {
        return aggregateDashboardStatistics(periodDays);
    }

    /**
     * 기간 통계 캐시 갱신 후 반환
     */
    @CachePut(value = DASHBOARD_STATISTICS_CACHE, key = "#periodDays")
    @Transactional(readOnly = true)
    public DashboardStatisticsDto refreshDashboardStatistics(Integer periodDays) {
        log.info("[dashboard statistics] cache refresh : periodDays={}", periodDays);
        return aggregateDashboardStatistics(periodDays);
    }

    private DashboardStatisticsDto aggregateDashboardStatistics(Integer periodDays) {
        // 기간 경계 (현재: [오늘-(N-1)일, 내일), 직전: 같은 길이로 바로 앞)
        LocalDate today = LocalDate.now();
        LocalDateTime currentStart = today.minusDays(periodDays - 1L).atStartOfDay();
        LocalDateTime currentEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime previousStart = currentStart.minusDays(periodDays);

        // 방문·기능 통계 + 직전 기간 합계
        DashboardStatisticsDto statistics = statisticsService.buildPeriodStatistics(currentStart, currentEnd);
        StatisticsPeriodTotalDto previousTotal = statisticsService.getPeriodTotal(previousStart, currentStart);

        // 챗봇 토큰 (일별, 기간 합계)
        Map<LocalDate, Long> chatTokensByDate = chatMessageRepository
            .sumDailyTokensBetween(currentStart, currentEnd).stream()
            .collect(Collectors.toMap(row -> toLocalDate(row[0]), row -> ((Number) row[1]).longValue(), Long::sum));
        for (DailyStatisticsDto day : statistics.getDailyStatistics()) {
            day.setChatTokens(chatTokensByDate.getOrDefault(day.getDate(), 0L));
        }
        statistics.getCurrentTotal().setChatTokens(chatMessageRepository.sumTokensBetween(currentStart, currentEnd));
        previousTotal.setChatTokens(chatMessageRepository.sumTokensBetween(previousStart, currentStart));

        statistics.setPeriodDays(periodDays);
        statistics.setPreviousTotal(previousTotal);
        return statistics;
    }

    private LocalDate toLocalDate(Object dateValue) {
        return LocalDate.parse(dateValue.toString().substring(0, 10));
    }

    /**
     * 대시보드 Summary 데이터 조회
     * 모든 통계 데이터를 집계하여 반환 (5분 캐싱)
     */
    @Cacheable(value = DASHBOARD_SUMMARY_CACHE, key = "'summary'")
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        return aggregateDashboardSummary();
    }

    /**
     * 캐시 갱신 후 새 데이터 반환 (새로고침 버튼)
     */
    @CachePut(value = DASHBOARD_SUMMARY_CACHE, key = "'summary'")
    @Transactional(readOnly = true)
    public DashboardSummaryDto refreshDashboardSummary() {
        log.info("대시보드 Summary 캐시 갱신");
        return aggregateDashboardSummary();
    }

    private DashboardSummaryDto aggregateDashboardSummary() {
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

            // 페이지 조회수
            Long totalProfileViews = statisticsService.getTotalProfileViews();
            Long todayProfileViews = statisticsService.getTodayProfileViews();

            // 세종대 인증 통계
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
                .totalProfileViews(0L)
                .todayProfileViews(0L)
                .totalSejongAuth(0L)
                .todaySejongAuth(0L)
                .featureUsageCounts(java.util.Collections.emptyMap())
                .todayFeatureUsageCounts(java.util.Collections.emptyMap())
                .build();
        }
    }
}
