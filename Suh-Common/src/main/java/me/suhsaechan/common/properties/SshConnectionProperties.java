package me.suhsaechan.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ssh")
public class SshConnectionProperties {
  private String host;
  private int port;
  private String username;
  private String password;
  // known_hosts 파일 경로 (설정 시 호스트키 검증, 미설정 시 검증 생략 + 경고)
  private String knownHostsPath;
}
