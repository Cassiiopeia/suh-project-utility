package me.suhsaechan.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * CORS 경로별 규칙: 세종 인증 API만 GitHub Pages 출처 허용 (#263)
 */
class CorsConfigTest {

  private static final String GITHUB_PAGES_ORIGIN = "https://cassiiopeia.github.io";

  private final CorsConfigurationSource corsConfigurationSource = new CorsConfig().corsConfigurationSource();

  @Test
  void 세종인증_API는_GitHubPages_출처를_자격증명없이_허용한다() {
    CorsConfiguration config = findCorsConfiguration("/api/sejong-auth/authenticate");

    assertThat(config.checkOrigin(GITHUB_PAGES_ORIGIN)).isEqualTo(GITHUB_PAGES_ORIGIN);
    assertThat(config.checkOrigin("https://lab.suhsaechan.kr")).isEqualTo("https://lab.suhsaechan.kr");
    assertThat(config.getAllowCredentials()).isFalse();
  }

  @Test
  void 그외_API는_GitHubPages_출처를_허용하지_않는다() {
    CorsConfiguration config = findCorsConfiguration("/api/notice/get/active");

    assertThat(config.checkOrigin(GITHUB_PAGES_ORIGIN)).isNull();
    assertThat(config.checkOrigin("https://lab.suhsaechan.kr")).isEqualTo("https://lab.suhsaechan.kr");
    assertThat(config.getAllowCredentials()).isTrue();
  }

  private CorsConfiguration findCorsConfiguration(String requestPath) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", requestPath);
    request.setRequestURI(requestPath);
    return corsConfigurationSource.getCorsConfiguration(request);
  }
}
