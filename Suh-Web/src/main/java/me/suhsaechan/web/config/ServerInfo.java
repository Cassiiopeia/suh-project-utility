package me.suhsaechan.web.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.suhsaechan.common.constant.SystemType;
import me.suhsaechan.common.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ToString
public class ServerInfo {
  @Value("${aes.secret-key}")
  public String secretKey;

  @Value("${aes.iv}")
  public String iv;

  public SystemType systemType = FileUtil.getCurrentSystem();

  // 세션 사용 clientHash 키 상수
  public static final String CLIENT_HASH_SESSION_KEY = "clientHash";

  // 사용자 인증 관련 정보를 위한 필드
  private boolean authenticationRequired = true;
}
