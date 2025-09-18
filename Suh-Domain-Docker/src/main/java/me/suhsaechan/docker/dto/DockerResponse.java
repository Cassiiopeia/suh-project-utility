package me.suhsaechan.docker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Docker 관련 응답을 처리하는 통합 응답 DTO 클래스
 * 모듈의 모든 API 엔드포인트는 이 단일 응답 클래스를 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockerResponse {
    
    // Docker 컨테이너 관련 응답
    private String container;
    private String error;
    private String started;
    private String stopped;
    private String restarted;
    private String status;
    
    // 컨테이너 목록 응답
    private List<ContainerInfoDto> containers;
    
    // 개별 컨테이너 정보 응답
    private ContainerInfoDto containerInfo;
}