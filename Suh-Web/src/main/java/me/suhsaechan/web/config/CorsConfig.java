package me.suhsaechan.web.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  // 세종 인증 API 경로 (WebSecurityConfig CSRF 제외 경로와 공유)
  public static final String SEJONG_AUTH_API_PATTERN = "/api/sejong-auth/**";

  // SUH-sejong-univ-auth GitHub Pages 데모 출처 (#263)
  private static final String SEJONG_AUTH_LIBRARY_PAGES_ORIGIN = "https://cassiiopeia.github.io";

  // 허용할 Origin 목록
  private static final List<String> LAB_ALLOWED_ORIGINS = List.of(
      "http://localhost:8089",
      "http://localhost:8080",
      "http://localhost:3000",
      "https://lab.suhsaechan.kr",
      "http://suh-project.synology.me:8090"
  );

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowedOrigins(LAB_ALLOWED_ORIGINS);

    // 모든 HTTP Method 허용 (GET, POST, PUT, DELETE 등)
    config.setAllowedMethods(List.of("*"));

    // 모든 HTTP Header 허용
    config.setAllowedHeaders(List.of("*"));

    // 자격 증명 허용 (쿠키, Authorization 헤더 포함)
    config.setAllowCredentials(true);

    // Pre-flight 요청 캐싱 시간(1시간)
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // 먼저 등록한 패턴 우선 매칭 (세종 인증 > 전체)
    source.registerCorsConfiguration(SEJONG_AUTH_API_PATTERN, createSejongAuthCorsConfiguration());
    source.registerCorsConfiguration("/**", config);

    return source;
  }

  /**
   * 세종 인증 API 전용 CORS: lab 출처 + GitHub Pages 데모 출처, POST만, 자격 증명 없음
   */
  private CorsConfiguration createSejongAuthCorsConfiguration() {
    List<String> sejongAuthAllowedOrigins = new ArrayList<>(LAB_ALLOWED_ORIGINS);
    sejongAuthAllowedOrigins.add(SEJONG_AUTH_LIBRARY_PAGES_ORIGIN);

    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(sejongAuthAllowedOrigins);
    config.setAllowedMethods(List.of("POST", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(false);
    config.setMaxAge(3600L);
    return config;
  }
}
