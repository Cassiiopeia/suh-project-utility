package me.suhsaechan.aiserver.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class AiServerRequest {
    
    // 현재는 특별한 요청 파라미터가 없지만, 확장성을 위해 빈 클래스로 유지
    // 향후 필터링, 페이징 등의 파라미터가 추가될 수 있음
    
}
