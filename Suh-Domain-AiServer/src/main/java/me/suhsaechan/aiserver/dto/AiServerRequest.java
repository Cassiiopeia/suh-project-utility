package me.suhsaechan.aiserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServerRequest {

    // API 호출시 사용할 모델명
    private String model;

    // AI에게 전달할 프롬프트 텍스트
    private String prompt;

    // 하위 호환성을 위한 입력 텍스트 필드
    private String input;

    // 스트림 모드 사용 여부 (기본값: false)
    private Boolean stream;

    // 모델 다운로드/삭제용 모델명
    private String modelName;

}
