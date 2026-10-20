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
  // known_hosts 파일 경로 (미설정 시 classpath ssh/known_hosts 사용)
  private String knownHostsPath;
}
