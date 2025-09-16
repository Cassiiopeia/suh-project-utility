package me.suhsaechan.docker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("ID")
    private String id;
    
    @JsonProperty("Name")
    private String name;
    
    @JsonProperty("Image")
    private String image;
    
    @JsonProperty("Status")
    private String status;
    
    // 컨테이너 실행 상태 (running/stopped 등)
    private Boolean isRunning;
}