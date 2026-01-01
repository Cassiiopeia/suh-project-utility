package me.suhsaechan.statistics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.statistics.dto.StatisticsResponse;
import me.suhsaechan.statistics.entity.FeatureUsageLog;
import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;
import me.suhsaechan.statistics.entity.PageVisitLog;
import me.suhsaechan.statistics.repository.FeatureUsageLogRepository;
import me.suhsaechan.statistics.repository.PageVisitLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통계 서비스
 * 페이지 방문 기록, 기능 사용 기록 및 통계 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final PageVisitLogRepository pageVisitLogRepository;
    private final FeatureUsageLogRepository featureUsageLogRepository;

    // 봇 탐지용 User-Agent 패턴
    private static final Pattern BOT_PATTERN = Pattern.compile(
        ".*(bot|crawl|spider|slurp|googlebot|bingbot|yandex|baidu|duckduck|facebot|ia_archiver).*",
        Pattern.CASE_INSENSITIVE
    );

    // 페이지 경로 상수
    private static final String PROFILE_PAGE_PATH = "/profile";

    /**
     * 페이지 방문 기록 저장 (비동기)
     */
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

    /**
     * 기능 사용 기록 저장 (비동기)
     */
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

    /**
     * 봇 여부 확인
     */
    private boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return false;
        }
        return BOT_PATTERN.matcher(userAgent).matches();
    }

    // === 통계 조회 메서드 ===

    /**
     * 총 페이지뷰 수 조회
     */
    @Transactional(readOnly = true)
    public long getTotalPageViews() {
        return pageVisitLogRepository.countByIsBotFalse();
    }

    /**
     * 오늘 페이지뷰 수 조회
     */
    @Transactional(readOnly = true)
    public long getTodayPageViews() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return pageVisitLogRepository.countByVisitedAtAfterAndIsBotFalse(todayStart);
    }

    /**
     * 총 고유 방문자 수 조회
     */
    @Transactional(readOnly = true)
    public long getTotalUniqueVisitors() {
        return pageVisitLogRepository.countDistinctClientHash();
    }

    /**
     * 오늘 고유 방문자 수 조회
     */
    @Transactional(readOnly = true)
    public long getTodayUniqueVisitors() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return pageVisitLogRepository.countDistinctClientHashAfter(todayStart);
    }

    /**
     * 프로필 페이지 총 조회수 조회
     */
    @Transactional(readOnly = true)
    public long getTotalProfileViews() {
        return pageVisitLogRepository.countByPagePathAndIsBotFalse(PROFILE_PAGE_PATH);
    }

    /**
     * 프로필 페이지 오늘 조회수 조회
     */
    @Transactional(readOnly = true)
    public long getTodayProfileViews() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return pageVisitLogRepository.countByPagePathAndVisitedAtAfterAndIsBotFalse(PROFILE_PAGE_PATH, todayStart);
    }

    /**
     * 세종대 인증 총 횟수 조회
     */
    @Transactional(readOnly = true)
    public long getTotalSejongAuth() {
        return featureUsageLogRepository.countByFeatureName(FeatureType.SEJONG_AUTH);
    }

    /**
     * 세종대 인증 오늘 횟수 조회
     */
    @Transactional(readOnly = true)
    public long getTodaySejongAuth() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return featureUsageLogRepository.countByFeatureNameAndUsedAtAfter(FeatureType.SEJONG_AUTH, todayStart);
    }

    /**
     * 기능별 사용 통계 조회 (GROUP BY 단일 쿼리)
     */
    @Transactional(readOnly = true)
    public Map<FeatureType, Long> getFeatureUsageCounts() {
        Map<FeatureType, Long> counts = new EnumMap<>(FeatureType.class);
        // 모든 타입 0으로 초기화
        for (FeatureType type : FeatureType.values()) {
            counts.put(type, 0L);
        }
        // GROUP BY 결과로 값 업데이트
        for (Object[] row : featureUsageLogRepository.countGroupByFeatureName()) {
            FeatureType type = (FeatureType) row[0];
            Long count = (Long) row[1];
            counts.put(type, count);
        }
        return counts;
    }

    /**
     * 오늘 기능별 사용 통계 조회 (GROUP BY 단일 쿼리)
     */
    @Transactional(readOnly = true)
    public Map<FeatureType, Long> getTodayFeatureUsageCounts() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<FeatureType, Long> counts = new EnumMap<>(FeatureType.class);
        // 모든 타입 0으로 초기화
        for (FeatureType type : FeatureType.values()) {
            counts.put(type, 0L);
        }
        // GROUP BY 결과로 값 업데이트
        for (Object[] row : featureUsageLogRepository.countGroupByFeatureNameAfter(todayStart)) {
            FeatureType type = (FeatureType) row[0];
            Long count = (Long) row[1];
            counts.put(type, count);
        }
        return counts;
    }

    /**
     * 방문자 통계 조회 (통합)
     */
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
}
