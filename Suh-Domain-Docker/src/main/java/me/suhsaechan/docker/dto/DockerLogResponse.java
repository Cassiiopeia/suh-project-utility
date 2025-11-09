package me.suhsaechan.docker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Docker 로그 폴링 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockerLogResponse {
    private String logs;
    private Integer totalLines;
    private String error;
}

