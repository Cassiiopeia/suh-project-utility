package me.suhsaechan.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

@Configuration
@RequiredArgsConstructor
public class UserDetailsConfig {
  // 일반 사용자 정보
  @Value("${login.id}")
  private String loginId;

  @Value("${login.password}")
  private String loginPassword;

  // 슈퍼 관리자 정보
  @Value("${admin.super.username}")
  private String adminUsername;

  @Value("${admin.super.password}")
  private String adminPassword;

  @Bean
  public UserDetailsManager userDetailsManager() {
    // 일반 사용자 계정
    UserDetails user = User.builder()
        .username(loginId)
        .password("{noop}" + loginPassword)
        .roles("USER")
        .build();
        
    // 슈퍼 관리자 계정
    UserDetails superAdmin = User.builder()
        .username(adminUsername)
        .password("{noop}" + adminPassword)
        .roles("USER", "ADMIN", "SUPER_ADMIN")
        .build();
        
    return new InMemoryUserDetailsManager(user, superAdmin);
  }
}
