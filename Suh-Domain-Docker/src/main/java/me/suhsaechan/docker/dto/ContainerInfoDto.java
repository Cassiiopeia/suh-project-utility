package me.suhsaechan.docker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Docker 컨테이너 정보 DTO 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInfoDto {

    private String id;

    private String name;

    private String image;

    private String status;

    // 컨테이너 실행 상태 (running/stopped 등)
    private Boolean isRunning;
}