package me.suhsaechan.suhprojectutility.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

@Configuration
public class UserDetailsConfig {
  @Value("${login.id}")
  private String id;

  @Value("${login.password}")
  private String password;

  @Bean
  public UserDetailsManager userDetailsManager() {
    UserDetails user = User.withDefaultPasswordEncoder()
        .username(id)
        .password(password)
        .roles("ADMIN")
        .build();
    return new InMemoryUserDetailsManager(user);
  }
}
