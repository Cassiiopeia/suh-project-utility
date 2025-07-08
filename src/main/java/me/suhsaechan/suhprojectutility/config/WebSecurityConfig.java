package me.suhsaechan.suhprojectutility.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.util.security.AESUtil;
import me.suhsaechan.suhprojectutility.util.security.CustomAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * 웹 보안 설정 클래스
 */
@Configuration
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

    // 공개 API , 리소스 경로
    List<String> publicApiEndpoints = publicEndpointConfig.getPublicApiEndpoints();
    List<String> publicResourceEndpoints = publicEndpointConfig.getPublicResourceEndpoints();

    List<String> allPublicEndpoints = new ArrayList<>();
    allPublicEndpoints.addAll(publicResourceEndpoints);
    allPublicEndpoints.addAll(publicApiEndpoints);

    // API 경로 > AntPathRequestMatcher 목록 생성
    List<AntPathRequestMatcher> apiMatchers = publicApiEndpoints.stream()
        .map(AntPathRequestMatcher::new)
        .collect(Collectors.toList());

    // CSRF 설정
    // CSRF 토큰을 사용할 경우, form에서 반드시 _csrf 필드를 사용해야 합니다.
    http
        .csrf(csrf -> csrf
            // API 경로에 대해 CSRF 토큰 검사 비활성화
            .ignoringRequestMatchers(apiMatchers.toArray(new AntPathRequestMatcher[0]))
//            //FIXME: DEV 에서만 추가
//                .ignoringRequestMatchers("/api/**")

            // XSRF-TOKEN 쿠키 처리
//            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())

        ); // 다른 경로는 기본적으로 _csrf 필드 필요

    http
        .authorizeHttpRequests(auth -> auth
            // 인증 없이 접근 가능한 경로 설정
            .requestMatchers(allPublicEndpoints.toArray(new String[0])).permitAll()
            .requestMatchers(HttpMethod.POST, "/api/docker/get/container-info").hasAnyRole("USER", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/module/get/versions").hasAnyRole("USER", "ADMIN")
            .anyRequest().authenticated() // 나머지 경로는 인증 필요
        );

    // Form Login 설정
    http
        .formLogin(formLogin -> formLogin
            // 커스텀 로그인 페이지 경로
            .loginPage("/login")
            // 실제 로그인을 처리할 URL (POST)
            .loginProcessingUrl("/login")
            // 로그인 성공시 이동할 페이지
//            .defaultSuccessUrl("/dashboard", true)
            .successHandler(new CustomAuthenticationSuccessHandler(aesUtil, serverInfo, userAuthority))
            // 로그인 실패시 이동할 페이지
            .failureUrl("/login?error=true")
            .permitAll()
        );

    // Logout 설정
    http
        .logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // GET/POST 둘 다 대응
            .logoutSuccessUrl("/login?logout=true") // 로그아웃 성공 시
            .invalidateHttpSession(true)    // 세션 무효화
            .deleteCookies("JSESSIONID")    // JSESSIONID 쿠키 삭제
            .permitAll()
        );

    // 세션 정책 설정
    http
        .sessionManagement(session -> session
            .invalidSessionUrl("/login?sessionExpired=true")
            .maximumSessions(20)  // 중복 세션 방지: 한 계정당 세션 20개
            .expiredUrl("/login?sessionExpired=true")
        );

    // CORS 설정
    http
        .cors(Customizer.withDefaults());

    return http.build();
  }
}