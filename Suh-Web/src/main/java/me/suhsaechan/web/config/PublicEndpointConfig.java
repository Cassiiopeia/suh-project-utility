package me.suhsaechan.web.config;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Arrays;

/**
 * 인증 없이 접근 가능한 공개 엔드포인트를 관리
 */
@Component
public class PublicEndpointConfig {

  /**
   * 인증 없이 접근 가능한 API 경로 목록 반환
   */
  public List<String> getPublicApiEndpoints() {
    return Arrays.asList(
        "/api/translate",   // 번역
        "/api/status"       // 상태 확인
    );
  }

  /**
   * 인증 없이 접근 가능한 정적 리소스 경로 목록 반환
   */
  public List<String> getPublicResourceEndpoints() {
    return Arrays.asList(
        "/login",
        "/logout",
        "/css/**",
        "/js/**",
        "/images/**",
        "/static/**",
        "/swagger-ui/**",      // Swagger UI
        "/v3/api-docs/**"      // OpenAPI 문서
    );
  }
}