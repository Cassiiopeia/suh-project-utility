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

  public String clientHash = "";

  public SystemType systemType = FileUtil.getCurrentSystem();
}
