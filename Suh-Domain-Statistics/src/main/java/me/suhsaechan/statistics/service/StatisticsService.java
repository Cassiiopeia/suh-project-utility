package me.suhsaechan.statistics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.statistics.dto.DailyStatisticsDto;
import me.suhsaechan.statistics.dto.DashboardStatisticsDto;
import me.suhsaechan.statistics.dto.FeatureUsageStatisticsDto;
import me.suhsaechan.statistics.dto.PageViewStatisticsDto;
import me.suhsaechan.statistics.dto.ReferrerSource;
import me.suhsaechan.statistics.dto.ReferrerSourceStatisticsDto;
import me.suhsaechan.statistics.dto.StatisticsPeriodTotalDto;
import me.suhsaechan.statistics.dto.VisitHeatmapCellDto;
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
    private final ServerOptionService serverOptionService;

    // 봇 탐지용 User-Agent 패턴
    private static final Pattern BOT_PATTERN = Pattern.compile(
        ".*(bot|crawl|spider|slurp|googlebot|bingbot|yandex|baidu|duckduck|facebot|ia_archiver).*",
        Pattern.CASE_INSENSITIVE
    );

    // 페이지 경로 상수
    private static final String PROFILE_PAGE_PATH = "/profile";

    // 기능 통계에서 제외하는 기능 (로그인·프로필·공지 조회는 사용 통계 의미 없음)
    private static final Set<FeatureType> EXCLUDED_FEATURE_TYPES =
        EnumSet.of(FeatureType.LOGIN, FeatureType.PROFILE_VIEW, FeatureType.NOTICE_VIEW);

    // 홈 화면으로 합산하는 경로
    private static final String HOME_PAGE_PATH = "/";
    private static final String DASHBOARD_PAGE_PATH = "/dashboard";

    private static final int TOP_FEATURE_LIMIT = 6;
    private static final int TOP_PAGE_LIMIT = 5;
    private static final int HEATMAP_DAY_COUNT = 7;
    // 히트맵 시간 구간: 3시간 단위 8칸 (0시, 3시, ... 21시)
    private static final int HEATMAP_HOUR_BUCKET_COUNT = 8;

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
    public Map<FeatureType, Number> getFeatureUsageCounts() {
        Map<FeatureType, Number> counts = new EnumMap<>(FeatureType.class);
        // 모든 타입 0으로 초기화
        for (FeatureType type : FeatureType.values()) {
            counts.put(type, 0L);
        }
        // GROUP BY 결과로 값 업데이트
        for (Object[] row : featureUsageLogRepository.countGroupByFeatureName()) {
            FeatureType type = (FeatureType) row[0];
            Number count = (Number) row[1];
            counts.put(type, count);
        }
        return counts;
    }

    /**
     * 오늘 기능별 사용 통계 조회 (GROUP BY 단일 쿼리)
     */
    @Transactional(readOnly = true)
    public Map<FeatureType, Number> getTodayFeatureUsageCounts() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<FeatureType, Number> counts = new EnumMap<>(FeatureType.class);
        // 모든 타입 0으로 초기화
        for (FeatureType type : FeatureType.values()) {
            counts.put(type, 0L);
        }
        // GROUP BY 결과로 값 업데이트
        for (Object[] row : featureUsageLogRepository.countGroupByFeatureNameAfter(todayStart)) {
            FeatureType type = (FeatureType) row[0];
            Number count = (Number) row[1];
            counts.put(type, count);
        }
        return counts;
    }

    // === 기간 통계 ===

    /**
     * 기간 통계 조립: 일별 > 기능 > 유입 경로 > 인기 페이지 > 히트맵 (periodDays·previousTotal 은 호출부에서 채움)
     */
    @Transactional(readOnly = true)
    public DashboardStatisticsDto buildPeriodStatistics(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return DashboardStatisticsDto.builder()
            .currentTotal(getPeriodTotal(startInclusive, endExclusive))
            .dailyStatistics(buildDailyStatistics(startInclusive, endExclusive))
            .featureUsages(buildFeatureUsages(startInclusive, endExclusive))
            .referrerSources(buildReferrerSources(startInclusive, endExclusive))
            .topPages(buildTopPages(startInclusive, endExclusive))
            .heatmapCells(buildHeatmapCells(startInclusive, endExclusive))
            .build();
    }

    /**
     * 기간 합계: 고유 방문자, 페이지뷰, 기능 사용 (chatTokens 는 호출부에서 채움)
     */
    @Transactional(readOnly = true)
    public StatisticsPeriodTotalDto getPeriodTotal(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return StatisticsPeriodTotalDto.builder()
            .uniqueVisitors(pageVisitLogRepository.countDistinctClientHashBetween(startInclusive, endExclusive))
            .pageViews(pageVisitLogRepository.countPageViewsBetween(startInclusive, endExclusive))
            .featureUsages(featureUsageLogRepository.countUsageBetween(
                startInclusive, endExclusive, EXCLUDED_FEATURE_TYPES))
            .chatTokens(0L)
            .build();
    }

    // 일별 통계 (시작일부터 종료일 전날까지 빈 날짜 0 채움)
    private List<DailyStatisticsDto> buildDailyStatistics(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        Map<LocalDate, DailyStatisticsDto> dailyStatisticsByDate = new LinkedHashMap<>();
        for (LocalDate date = startInclusive.toLocalDate(); date.isBefore(endExclusive.toLocalDate()); date = date.plusDays(1)) {
            dailyStatisticsByDate.put(date, DailyStatisticsDto.builder()
                .date(date).uniqueVisitors(0L).pageViews(0L).featureUsages(0L).chatTokens(0L).build());
        }

        // 방문 행 [날짜, 고유 방문자, 페이지뷰]
        for (Object[] row : pageVisitLogRepository.countDailyVisitsBetween(startInclusive, endExclusive)) {
            Optional.ofNullable(dailyStatisticsByDate.get(toLocalDate(row[0]))).ifPresent(day -> {
                day.setUniqueVisitors(((Number) row[1]).longValue());
                day.setPageViews(((Number) row[2]).longValue());
            });
        }

        // 기능 행 [날짜, 건수]
        List<String> excludedFeatureNames = EXCLUDED_FEATURE_TYPES.stream().map(Enum::name).toList();
        for (Object[] row : featureUsageLogRepository.countDailyUsageBetween(
            startInclusive, endExclusive, excludedFeatureNames)) {
            Optional.ofNullable(dailyStatisticsByDate.get(toLocalDate(row[0])))
                .ifPresent(day -> day.setFeatureUsages(((Number) row[1]).longValue()));
        }
        return new ArrayList<>(dailyStatisticsByDate.values());
    }

    // 기능 사용 상위 N개 (0건 제외, 내림차순)
    private List<FeatureUsageStatisticsDto> buildFeatureUsages(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return featureUsageLogRepository
            .countGroupByFeatureNameBetween(startInclusive, endExclusive, EXCLUDED_FEATURE_TYPES).stream()
            .map(row -> FeatureUsageStatisticsDto.builder()
                .featureName((FeatureType) row[0]).usageCount(((Number) row[1]).longValue()).build())
            .filter(usage -> usage.getUsageCount() > 0)
            .sorted(Comparator.comparing(FeatureUsageStatisticsDto::getUsageCount).reversed())
            .limit(TOP_FEATURE_LIMIT)
            .toList();
    }

    // 유입 경로 (분류별 합산, 내부 이동·0건 제외, 내림차순)
    private List<ReferrerSourceStatisticsDto> buildReferrerSources(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        List<String> internalHosts = ReferrerSourceClassifier.parseInternalHosts(
            serverOptionService.getOptionValue(ServerOptionKey.STATISTICS_INTERNAL_REFERRER_HOSTS));

        Map<ReferrerSource, Long> viewCountBySource = new EnumMap<>(ReferrerSource.class);
        for (Object[] row : pageVisitLogRepository.countGroupByReferrerBetween(startInclusive, endExclusive)) {
            ReferrerSource source = ReferrerSourceClassifier.classify((String) row[0], internalHosts);
            viewCountBySource.merge(source, ((Number) row[1]).longValue(), Long::sum);
        }

        return viewCountBySource.entrySet().stream()
            .filter(entry -> entry.getKey() != ReferrerSource.INTERNAL && entry.getValue() > 0)
            .map(entry -> ReferrerSourceStatisticsDto.builder()
                .source(entry.getKey()).viewCount(entry.getValue()).build())
            .sorted(Comparator.comparing(ReferrerSourceStatisticsDto::getViewCount).reversed())
            .toList();
    }

    // 인기 페이지 (첫 경로 기준 합산, 상위 N개)
    private List<PageViewStatisticsDto> buildTopPages(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        Map<String, Long> viewCountByPagePath = new LinkedHashMap<>();
        for (Object[] row : pageVisitLogRepository.countGroupByPagePathBetween(startInclusive, endExclusive)) {
            viewCountByPagePath.merge(normalizePagePath((String) row[0]), ((Number) row[1]).longValue(), Long::sum);
        }
        return viewCountByPagePath.entrySet().stream()
            .map(entry -> PageViewStatisticsDto.builder().pagePath(entry.getKey()).viewCount(entry.getValue()).build())
            .sorted(Comparator.comparing(PageViewStatisticsDto::getViewCount).reversed())
            .limit(TOP_PAGE_LIMIT)
            .toList();
    }

    // 히트맵 7 x 8 전체 칸 (없는 칸 0, 요일 > 구간 오름차순)
    private List<VisitHeatmapCellDto> buildHeatmapCells(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        long[][] pageViewsByCell = new long[HEATMAP_DAY_COUNT + 1][HEATMAP_HOUR_BUCKET_COUNT];
        // 행 [ISO 요일, 시간 구간, 페이지뷰]
        for (Object[] row : pageVisitLogRepository.countHeatmapBetween(startInclusive, endExclusive)) {
            int dayOfWeek = ((Number) row[0]).intValue();
            int hourBucket = ((Number) row[1]).intValue();
            if (dayOfWeek >= 1 && dayOfWeek <= HEATMAP_DAY_COUNT && hourBucket >= 0 && hourBucket < HEATMAP_HOUR_BUCKET_COUNT) {
                pageViewsByCell[dayOfWeek][hourBucket] += ((Number) row[2]).longValue();
            }
        }

        List<VisitHeatmapCellDto> heatmapCells = new ArrayList<>();
        for (int dayOfWeek = 1; dayOfWeek <= HEATMAP_DAY_COUNT; dayOfWeek++) {
            for (int hourBucket = 0; hourBucket < HEATMAP_HOUR_BUCKET_COUNT; hourBucket++) {
                heatmapCells.add(VisitHeatmapCellDto.builder()
                    .dayOfWeek(dayOfWeek).hourBucket(hourBucket).pageViews(pageViewsByCell[dayOfWeek][hourBucket]).build());
            }
        }
        return heatmapCells;
    }

    // 경로 > 첫 세그먼트 (/dashboard > /, /notice/12?x=1 > /notice)
    private String normalizePagePath(String pagePath) {
        String pathWithoutQuery = pagePath.split("\\?", 2)[0];
        String[] segments = pathWithoutQuery.split("/");
        String firstPath = segments.length > 1 ? "/" + segments[1] : HOME_PAGE_PATH;
        return DASHBOARD_PAGE_PATH.equals(firstPath) ? HOME_PAGE_PATH : firstPath;
    }

    // native 쿼리 날짜 컬럼 (java.sql.Date 또는 LocalDate)
    private LocalDate toLocalDate(Object dateValue) {
        if (dateValue instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return (LocalDate) dateValue;
    }
}
