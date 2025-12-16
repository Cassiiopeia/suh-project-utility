package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.application.service.DashboardService;
import me.suhsaechan.statistics.dto.DashboardSummaryDto;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대시보드 API 컨트롤러
 * 대시보드 Summary 통계 데이터 제공
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 대시보드 Summary 통계 조회
     * 방문자, 챗봇, 토큰, 콘텐츠 조회수 등 통합 통계 반환 (5분 캐싱)
     */
    @PostMapping(value = "/summary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogMonitor
    public ResponseEntity<DashboardSummaryDto> getSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }

    /**
     * 대시보드 Summary 통계 새로고침
     * 캐시 무효화 후 최신 데이터로 갱신
     */
    @PostMapping(value = "/summary/refresh", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogMonitor
    public ResponseEntity<DashboardSummaryDto> refreshSummary() {
        return ResponseEntity.ok(dashboardService.refreshDashboardSummary());
    }
}
