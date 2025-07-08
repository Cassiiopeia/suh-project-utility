package me.suhsaechan.suhprojectutility.object.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Docker 로그 스트리밍 요청 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockerLogRequest {
    
    /**
     * Docker 컨테이너 이름
     */
    private String containerName;
    
    /**
     * 로그 라인 제한 수 (0 = 무제한)
     */
    private Integer lineLimit;
    
    /**
     * 로그 스트리밍 간격 (밀리초)
     */
    private Integer streamInterval;
    
    /**
     * 로그 명령어 옵션 (예: --tail=100)
     */
    private String logOptions;
} 