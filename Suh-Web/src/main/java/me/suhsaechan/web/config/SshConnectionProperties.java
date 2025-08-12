package me.suhsaechan.web.config;

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
}
