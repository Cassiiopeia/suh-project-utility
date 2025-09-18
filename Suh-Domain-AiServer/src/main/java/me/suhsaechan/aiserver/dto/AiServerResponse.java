package me.suhsaechan.aiserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServerResponse {
    
    private TunnelInfoDto tunnelInfo;
    private Boolean isActive;
    private String currentUrl;
    
}
