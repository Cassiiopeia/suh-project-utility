package me.suhsaechan.suhprojectutility.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class WebSecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // CSRF 설정
    // CSRF 토큰을 사용할 경우, form에서 반드시 _csrf 필드를 사용해야 합니다.
    http
        .csrf(csrf -> csrf
            // API 용으로 CSRF를 끔
            .ignoringRequestMatchers(
                new AntPathRequestMatcher("/api/**")
            )
        ); // 기본: _csrf 필드 필요

    http
        .authorizeHttpRequests(auth -> auth
            // 로그인 없이 접근 가능
            .requestMatchers(
                "/login",         // 로그인 페이지
                "/logout",        // 로그아웃 처리
                "/css/**",
                "/js/**",
                "/images/**",
                "/static/**"
            ).permitAll()
            .anyRequest().authenticated() // 기본: 인증 필요
        );

    // Form Login
    http
        .formLogin(formLogin -> formLogin
            // 커스텀 로그인 페이지 경로
            .loginPage("/login")
            // 실제 로그인을 처리할 URL (POST)
            .loginProcessingUrl("/login")
            // 로그인 성공시 이동할 페이지
            .defaultSuccessUrl("/dashboard", true)
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
    // sessionCreationPolicy를 IF_REQUIRED 로 두고, 만료 시 행동 -> TODO: 세션연장 팝업창 표시 필요
    http
        .sessionManagement(session -> session
            .invalidSessionUrl("/login?sessionExpired=true")
            .maximumSessions(10)  // 중복 세션 방지: 한 계정당 세션 10개
            .expiredUrl("/login?sessionExpired=true")
        );

    // CORS 설정
    http
        .cors(Customizer.withDefaults());

    return http.build();
  }
}
