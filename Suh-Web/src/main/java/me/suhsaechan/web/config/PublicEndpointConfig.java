package me.suhsaechan.web.config;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Arrays;

/**
 * 인증 없이 접근 가능한 공개 엔드포인트 관리
 */
@Component
public class PublicEndpointConfig {

  /**
   * 인증 없이 접근 가능한 API 경로 목록 반환
   */
  public List<String> getPublicApiEndpoints() {
    return Arrays.asList(
        "/api/translate",   // 번역
        "/api/status",      // 상태 확인
        "/api/issue-helper/create/commit-branch/github-workflow"  // GitHub 이슈 자동 코멘트 엔드포인트
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
        "/upload/**",         // 업로드된 파일 접근
        "/uploads/**",        // 업로드된 파일 접근 (legacy)
        "/suh-project-utility/dev-uploads/**", // 시놀로지 연결용 URL
        "/suh-project-utility/upload/**",     // 시놀로지 연결용 URL
        "/swagger-ui/**",      // Swagger UI
        "/v3/api-docs/**"      // OpenAPI 문서
    );
  }
}