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
public class TunnelInfoDto {
    
    private String status;
    private String url;
    private String timestamp;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("local_endpoint")
    private String localEndpoint;
    
    @JsonProperty("api_key")
    private String apiKey;
    
    private String message;
    
    @JsonProperty("service_info")
    private ServiceInfoDto serviceInfo;
    
    private EndpointsDto endpoints;
    
}
