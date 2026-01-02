package me.suhsaechan.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.common.constant.ServerOptionKey;

/**
 * 서버 설정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerOptionRequest {

  private ServerOptionKey optionKey;
  private String optionValue;
}

