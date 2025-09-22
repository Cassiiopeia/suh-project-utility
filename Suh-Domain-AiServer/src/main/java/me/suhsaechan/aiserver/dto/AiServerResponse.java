package me.suhsaechan.aiserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServerResponse {

    private TunnelInfoDto tunnelInfo;
    private Boolean isActive;
    private String currentUrl;

    // 모델 목록 API 응답 JSON (원본 그대로)
    private String modelsJson;

    // 임베딩 API 응답 JSON (원본 그대로) - 하위 호환성용
    private String embeddingsJson;

    // generate API 응답 JSON (원본 그대로)
    private String generatedJson;

    // API 요청시 사용된 모델명
    private String model;

    // API 요청시 사용된 입력 텍스트 - 하위 호환성용
    private String input;

    // API 요청시 사용된 프롬프트
    private String prompt;

    // 스트림 모드 사용 여부
    private Boolean stream;

}
