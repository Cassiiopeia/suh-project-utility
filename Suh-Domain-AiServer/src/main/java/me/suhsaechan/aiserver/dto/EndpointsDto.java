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
public class EndpointsDto {
    
    private String health;
    private String info;
    
    @JsonProperty("tunnel_info")
    private String tunnelInfo;
    
    private String test;
    
}
