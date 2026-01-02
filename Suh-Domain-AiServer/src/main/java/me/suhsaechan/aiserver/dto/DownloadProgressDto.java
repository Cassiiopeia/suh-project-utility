package me.suhsaechan.aiserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 모델 다운로드 진행 상황 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadProgressDto {

    /**
     * 모델 이름
     */
    private String modelName;

    /**
     * 다운로드 상태 (downloading, completed, failed)
     */
    private String status;

    /**
     * 다운로드된 바이트 수
     */
    private Long completed;

    /**
     * 전체 바이트 수
     */
    private Long total;

    /**
     * 진행률 (0-100)
     */
    private Integer percentage;

    /**
     * 상태 메시지
     */
    private String message;

    /**
     * 다운로드 시작 시간
     */
    private Long startTime;

    /**
     * 다운로드 종료 시간
     */
    private Long endTime;

    /**
     * 마지막 진행상황 업데이트 시간 (stale 감지용)
     */
    private Long lastUpdateTime;
}
