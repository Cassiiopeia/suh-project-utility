package me.suhsaechan.web.config;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * 인증 없이 접근 가능한 공개 엔드포인트 관리
 *
 * Spring Security와 Interceptor의 화이트리스트를 중앙에서 관리합니다.
 * 새로운 공개 경로를 추가할 때는 이 클래스만 수정하면 자동으로 적용됩니다.
 */
@Component
public class PublicEndpointConfig {

  /**
   * 인증 없이 접근 가능한 API 경로 목록 반환
   * Spring Security의 permitAll에 사용됩니다.
   */
  public List<String> getPublicApiEndpoints() {
    return Arrays.asList(
        "/api/translate",   // 번역
        "/api/status",      // 상태 확인
        "/api/issue-helper/create/commit-branch/github-workflow",  // GitHub 이슈 자동 코멘트 엔드포인트
        "/api/chatbot/chat",        // 챗봇 채팅 (비스트리밍)
        "/api/chatbot/chat/stream", // 챗봇 채팅 (스트리밍 SSE)
        "/api/chatbot/feedback",    // 챗봇 피드백
        "/api/chatbot/history",     // 챗봇 대화 이력
        "/api/sejong-auth/**"       // 세종대 인증 테스트
    );
  }

  /**
   * 인증 없이 접근 가능한 정적 리소스 경로 목록 반환
   * Spring Security의 permitAll에 사용됩니다.
   */
  public List<String> getPublicResourceEndpoints() {
    return Arrays.asList(
        "/login",
        "/logout",
        "/profile",
        "/issue-helper",  // GitHub 이슈 도우미 페이지
        "/translator",    // AI 번역기 페이지
        "/sejong-auth",   // 세종대 인증 테스트 페이지
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

  /**
   * Interceptor에서 제외할 경로 목록 반환
   * WebConfig의 Interceptor 등록 시 excludePathPatterns에 사용됩니다.
   *
   * @return Interceptor 제외 경로 배열
   */
  public String[] getInterceptorExcludedPaths() {
    List<String> excludedPaths = new ArrayList<>();

    // API 경로 추가
    excludedPaths.add("/api/**");  // 모든 API는 인터셉터 제외

    // 정적 리소스 경로 추가
    excludedPaths.add("/css/**");
    excludedPaths.add("/js/**");
    excludedPaths.add("/images/**");
    excludedPaths.add("/static/**");
    excludedPaths.add("/upload/**");
    excludedPaths.add("/uploads/**");
    excludedPaths.add("/suh-project-utility/**");

    // Swagger 경로 추가
    excludedPaths.add("/swagger-ui/**");
    excludedPaths.add("/v3/api-docs/**");

    return excludedPaths.toArray(new String[0]);
  }

  /**
   * Spring Security와 Interceptor 모두에서 제외할 전체 경로 목록 반환
   *
   * @return 모든 공개 경로 리스트
   */
  public List<String> getAllPublicEndpoints() {
    List<String> allPublicEndpoints = new ArrayList<>();
    allPublicEndpoints.addAll(getPublicResourceEndpoints());
    allPublicEndpoints.addAll(getPublicApiEndpoints());
    return allPublicEndpoints;
  }
}