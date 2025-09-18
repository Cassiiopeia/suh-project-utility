package me.suhsaechan.aiserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfoDto {
    
    private String name;
    private String version;
    
    @JsonProperty("proxy_port")
    private Integer proxyPort;
    
    @JsonProperty("target_port")
    private Integer targetPort;
    
}
