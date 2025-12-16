package me.suhsaechan.statistics.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;

/**
 * 통계 요청 DTO
 * 통계 조회 및 기록 관련 모든 요청 통합
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsRequest {

    // 페이지 방문 기록용
    private String pagePath;
    private String clientHash;
    private String userIp;
    private String userAgent;
    private String referrer;

    // 기능 사용 기록용
    private FeatureType featureName;
    private String additionalInfo;

    // 조회 기간 필터용
    private LocalDate startDate;
    private LocalDate endDate;
}
