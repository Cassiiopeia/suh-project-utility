package me.suhsaechan.docker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Docker 관련 요청을 처리하는 통합 요청 DTO 클래스
 * 모듈의 모든 API 엔드포인트는 이 단일 요청 클래스를 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockerRequest {
    
    // Docker 컨테이너 관련 필드
    private String containerName;
    
    // 로그 스트리밍 관련 필드 (기존 DockerLogRequest 통합)
    private Integer lineLimit;
    private Integer streamInterval;
    private String logOptions;
}
