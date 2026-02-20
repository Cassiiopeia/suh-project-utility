package me.suhsaechan.web.config;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.service.UserAuthority;
import me.suhsaechan.common.util.AESUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * 웹 보안 설정 클래스
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class WebSecurityConfig {

  private final PublicEndpointConfig publicEndpointConfig;
  private final ServerInfo serverInfo;
  private final AESUtil aesUtil;
  private final UserAuthority userAuthority;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    log.info("설정 - SecurityFilterChain 초기화");

    // PublicEndpointConfig에서 중앙 관리되는 공개 경로 가져오기
    List<String> allPublicEndpoints = publicEndpointConfig.getAllPublicEndpoints();

    // CSRF 설정 : _csrf 필드 필요
    http
        .csrf(csrf -> csrf
            // API 경로에 대해 CSRF 토큰 검사 비활성화
            .ignoringRequestMatchers("/api/issue-helper/create/commit-branch/github-workflow")
            .ignoringRequestMatchers("/api/grass/**")  // GrassPlanter API CSRF 비활성화
        );

    http
        .authorizeHttpRequests(auth -> auth
            // 인증 없이 접근 가능한 경로 설정
            .requestMatchers(allPublicEndpoints.toArray(new String[0])).permitAll()
            .requestMatchers(HttpMethod.POST, "/api/docker/get/container-info").hasAnyRole("USER", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/module/get/versions").hasAnyRole("USER", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/chatbot/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        );

    // Form Login 설정
    http
        .formLogin(formLogin -> formLogin
            // 커스텀 로그인 페이지 경로
            .loginPage("/login")
            // 실제 로그인을 처리할 URL (POST)
            .loginProcessingUrl("/login")
            .successHandler(new CustomAuthenticationSuccessHandler(aesUtil, userAuthority))
            // 로그인 실패시 이동할 페이지
            .failureUrl("/login?error=true")
            .permitAll()
        );

    // Logout 설정
    http
        .logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
            .logoutSuccessUrl("/login?logout=true")             // 로그아웃 성공 시
            .invalidateHttpSession(true)                        // 세션 무효화
            .deleteCookies("JSESSIONID")  // JSESSIONID 쿠키 삭제
            .permitAll()
        );

    // 세션 정책 설정
    http
        .sessionManagement(session -> session
            .invalidSessionUrl("/login?sessionExpired=true")
            .maximumSessions(20)  // 중복 세션 방지: 한 계정당 세션 20개
            .expiredUrl("/login?sessionExpired=true")
        );

    // 접근 거부(403) 처리
    http
        .exceptionHandling(exception -> exception
            .accessDeniedHandler((request, response, e) -> {
              response.sendError(HttpServletResponse.SC_FORBIDDEN);
            })
        );

    // CORS 설정
    http
        .cors(Customizer.withDefaults());
    return http.build();
  }
}