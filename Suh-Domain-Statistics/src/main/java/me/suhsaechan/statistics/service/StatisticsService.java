package me.suhsaechan.statistics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.dto.DailyStatDto;
import me.suhsaechan.statistics.dto.StatisticsResponse;
import me.suhsaechan.statistics.entity.FeatureUsageLog;
import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;
import me.suhsaechan.statistics.entity.PageVisitLog;
import me.suhsaechan.statistics.repository.FeatureUsageLogRepository;
import me.suhsaechan.statistics.repository.PageVisitLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final PageVisitLogRepository pageVisitLogRepository;
    private final FeatureUsageLogRepository featureUsageLogRepository;

    private static final Pattern BOT_PATTERN = Pattern.compile(
        ".*(bot|crawl|spider|slurp|googlebot|bingbot|yandex|baidu|duckduck|facebot|ia_archiver).*",
        Pattern.CASE_INSENSITIVE
    );

    private static final String PROFILE_PAGE_PATH = "/profile";

    @Async
    @Transactional
    public void logPageVisitAsync(String pagePath, String clientHash, String userIp,
                                   String userAgent, String referrer) {
        try {
            boolean isBot = isBot(userAgent);

            PageVisitLog visitLog = PageVisitLog.builder()
                .pagePath(pagePath)
                .clientHash(clientHash)
                .userIp(userIp)
                .userAgent(userAgent)
                .referrer(referrer)
                .visitedAt(LocalDateTime.now())
                .isBot(isBot)
                .build();

            pageVisitLogRepository.save(visitLog);
            log.debug("페이지 방문 기록 저장 - path: {}, isBot: {}", pagePath, isBot);
        } catch (Exception e) {
            log.error("페이지 방문 기록 저장 실패: {}", e.getMessage());
        }
    }

    @Async
    @Transactional
    public void logFeatureUsageAsync(FeatureType featureName, String clientHash, String additionalInfo) {
        try {
            FeatureUsageLog usageLog = FeatureUsageLog.builder()
                .featureName(featureName)
                .clientHash(clientHash)
                .usedAt(LocalDateTime.now())
                .additionalInfo(additionalInfo)
                .build();

            featureUsageLogRepository.save(usageLog);
            log.debug("기능 사용 기록 저장 - feature: {}", featureName);
        } catch (Exception e) {
            log.error("기능 사용 기록 저장 실패: {}", e.getMessage());
        }
    }

    private boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return false;
        }
        return BOT_PATTERN.matcher(userAgent).matches();
    }

    // === 통계 조회 메서드 ===

    @Transactional(readOnly = true)
    public long getTotalPageViews() {
        return pageVisitLogRepository.countByIsBotFalse();
    }

    @Transactional(readOnly = true)
    public long getTodayPageViews() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return pageVisitLogRepository.countByVisitedAtAfterAndIsBotFalse(todayStart);
    }

    @Transactional(readOnly = true)
    public long getTotalUniqueVisitors() {
        return pageVisitLogRepository.countDistinctClientHash();
    }

    @Transactional(readOnly = true)
    public long getTodayUniqueVisitors() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return pageVisitLogRepository.countDistinctClientHashAfter(todayStart);
    }

    @Transactional(readOnly = true)
    public long getTotalProfileViews() {
        return pageVisitLogRepository.countByPagePathAndIsBotFalse(PROFILE_PAGE_PATH);
    }

    @Transactional(readOnly = true)
    public long getTodayProfileViews() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return pageVisitLogRepository.countByPagePathAndVisitedAtAfterAndIsBotFalse(PROFILE_PAGE_PATH, todayStart);
    }

    @Transactional(readOnly = true)
    public long getTotalSejongAuth() {
        return featureUsageLogRepository.countByFeatureName(FeatureType.SEJONG_AUTH);
    }

    @Transactional(readOnly = true)
    public long getTodaySejongAuth() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return featureUsageLogRepository.countByFeatureNameAndUsedAtAfter(FeatureType.SEJONG_AUTH, todayStart);
    }

    @Transactional(readOnly = true)
    public Map<FeatureType, Long> getFeatureUsageCounts() {
        Map<FeatureType, Long> counts = new EnumMap<>(FeatureType.class);
        for (FeatureType type : FeatureType.values()) {
            counts.put(type, 0L);
        }
        for (Object[] row : featureUsageLogRepository.countGroupByFeatureName()) {
            FeatureType type = (FeatureType) row[0];
            Long count = (Long) row[1];
            counts.put(type, count);
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public Map<FeatureType, Long> getTodayFeatureUsageCounts() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<FeatureType, Long> counts = new EnumMap<>(FeatureType.class);
        for (FeatureType type : FeatureType.values()) {
            counts.put(type, 0L);
        }
        for (Object[] row : featureUsageLogRepository.countGroupByFeatureNameAfter(todayStart)) {
            FeatureType type = (FeatureType) row[0];
            Long count = (Long) row[1];
            counts.put(type, count);
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public List<DailyStatDto> getDailyVisitors(int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        return mapToDailyStatDto(pageVisitLogRepository.countDailyUniqueVisitorsSince(since));
    }

    @Transactional(readOnly = true)
    public List<DailyStatDto> getDailyPageViews(int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        return mapToDailyStatDto(pageVisitLogRepository.countDailyPageViewsSince(since));
    }

    @Transactional(readOnly = true)
    public List<DailyStatDto> getDailyFeatureUsage(int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        return mapToDailyStatDto(featureUsageLogRepository.countDailyUsageSince(since));
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getVisitorStatistics() {
        return StatisticsResponse.builder()
            .totalUniqueVisitors(getTotalUniqueVisitors())
            .todayUniqueVisitors(getTodayUniqueVisitors())
            .totalPageViews(getTotalPageViews())
            .todayPageViews(getTodayPageViews())
            .featureUsageCounts(getFeatureUsageCounts())
            .todayFeatureUsageCounts(getTodayFeatureUsageCounts())
            .build();
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
