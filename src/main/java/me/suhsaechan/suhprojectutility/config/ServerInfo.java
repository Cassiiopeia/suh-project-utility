package me.suhsaechan.suhprojectutility.config;

import lombok.Getter;
import lombok.Setter;
import me.suhsaechan.suhprojectutility.object.constant.SystemType;
import me.suhsaechan.suhprojectutility.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class ServerInfo {
  @Value("${aes.secret-key}")
  public String secretKey;

  @Value("${aes.iv}")
  public String iv;

  public SystemType systemType = FileUtil.getCurrentSystem();

  // 세션에서 사용할 clientHash 키 상수
  public static final String CLIENT_HASH_SESSION_KEY = "clientHash";

  // 사용자 인증 관련 정보를 위한 필드
  private boolean authenticationRequired = true;
}
