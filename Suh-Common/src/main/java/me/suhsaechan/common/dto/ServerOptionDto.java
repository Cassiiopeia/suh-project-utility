package me.suhsaechan.common.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.entity.ServerOption;

/**
 * 서버 설정 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerOptionDto {

  private UUID serverOptionId;
  private ServerOptionKey optionKey;
  private String optionValue;
  private String description;

  /**
   * Entity -> DTO 변환
   */
  public static ServerOptionDto from(ServerOption option) {
    return ServerOptionDto.builder()
        .serverOptionId(option.getServerOptionId())
        .optionKey(option.getOptionKey())
        .optionValue(option.getOptionValue())
        .description(option.getOptionKey().getDescription())
        .build();
  }

  /**
   * 기본값으로 DTO 생성
   */
  public static ServerOptionDto fromDefault(ServerOptionKey key) {
    return ServerOptionDto.builder()
        .optionKey(key)
        .optionValue(key.getDefaultValue())
        .description(key.getDescription())
        .build();
  }
}

