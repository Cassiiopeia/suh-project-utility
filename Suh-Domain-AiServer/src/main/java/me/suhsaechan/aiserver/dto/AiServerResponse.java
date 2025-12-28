package me.suhsaechan.aiserver.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServerResponse {

    // 하위 호환성을 위한 터널 정보 (deprecated)
    private TunnelInfoDto tunnelInfo;

    // 서버 활성 상태
    private Boolean isActive;

    // 현재 AI 서버 URL
    private String currentUrl;

    // Health check 결과
    private Boolean isHealthy;
    private String healthMessage;

    // 모델 목록 API 응답 JSON (원본 그대로) - 하위 호환성용
    private String modelsJson;

    // 모델 목록 (파싱된 배열)
    private List<ModelDto> models;

    // 임베딩 API 응답 JSON (원본 그대로) - 하위 호환성용
    private String embeddingsJson;

    // generate API 응답 JSON (원본 그대로)
    private String generatedJson;

    // 모델 다운로드 진행률 JSON
    private String pullProgressJson;

    // API 요청시 사용된 모델명
    private String model;

    // API 요청시 사용된 입력 텍스트 - 하위 호환성용
    private String input;

    // API 요청시 사용된 프롬프트
    private String prompt;

    // 스트림 모드 사용 여부
    private Boolean stream;

    // SuhAider 임베딩 결과
    private List<Double> embeddingVector;

    // SuhAider 배치 임베딩 결과
    private List<List<Double>> embeddingVectors;

    // 벡터 차원 수
    private Integer vectorDimension;

    // generate 응답 텍스트 (파싱된)
    private String generatedText;

    // 처리 시간 (ms)
    private Long processingTime;

    // 다운로드 진행 상황 (단일)
    private DownloadProgressDto downloadProgress;

    // 다운로드 진행 상황 맵 (전체)
    private java.util.Map<String, DownloadProgressDto> downloadProgressMap;

}
