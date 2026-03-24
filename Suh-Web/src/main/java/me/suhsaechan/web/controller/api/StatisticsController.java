package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.application.service.DashboardService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.statistics.dto.StatisticsResponse;
import me.suhsaechan.statistics.service.StatisticsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final DashboardService dashboardService;

    private static final int DEFAULT_LOOKBACK_DAYS = 30;

    @PostMapping(value = "/daily-visitors", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogMonitor
    public ResponseEntity<StatisticsResponse> getDailyVisitors() {
        return ResponseEntity.ok(StatisticsResponse.builder()
            .dailyVisitors(statisticsService.getDailyVisitors(DEFAULT_LOOKBACK_DAYS))
            .dailyPageViews(statisticsService.getDailyPageViews(DEFAULT_LOOKBACK_DAYS))
            .build());
    }

    @PostMapping(value = "/daily-features", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogMonitor
    public ResponseEntity<StatisticsResponse> getDailyFeatures() {
        return ResponseEntity.ok(StatisticsResponse.builder()
            .dailyFeatureUsage(statisticsService.getDailyFeatureUsage(DEFAULT_LOOKBACK_DAYS))
            .featureUsageCounts(statisticsService.getFeatureUsageCounts())
            .build());
    }

    @PostMapping(value = "/daily-chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogMonitor
    public ResponseEntity<StatisticsResponse> getDailyChat() {
        return ResponseEntity.ok(StatisticsResponse.builder()
            .dailyChatMessages(dashboardService.getDailyChatMessages(DEFAULT_LOOKBACK_DAYS))
            .dailyChatTokens(dashboardService.getDailyChatTokens(DEFAULT_LOOKBACK_DAYS))
            .build());
    }
}
