package me.suhsaechan.web.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 필터 설정 클래스
 * SSE 스트리밍 엔드포인트에서 응답 로깅을 제외하여 ArrayIndexOutOfBoundsException 방지
 */
@Slf4j
@Configuration
public class FilterConfig {

    /**
     * SSE 스트리밍 엔드포인트 제외 필터
     * SuhLoggingFilter가 SSE 응답을 버퍼링하려고 시도하여 발생하는 오류 방지
     */
    @Bean
    public FilterRegistrationBean<Filter> sseSkipFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();

        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                          FilterChain filterChain) throws ServletException, IOException {
                String requestURI = request.getRequestURI();

                // SSE 엔드포인트 체크 (Docker 로그 스트리밍, AI 서버 모델 다운로드 등)
                if (requestURI.contains("/api/docker-log/stream") ||
                    requestURI.contains("/api/ai-server/models/pull/stream") ||
                    response.getContentType() != null && response.getContentType().contains("text/event-stream")) {

                    log.debug("SSE 엔드포인트 로깅 제외: {}", requestURI);

                    // SSE 응답임을 표시하는 속성 설정
                    request.setAttribute("SKIP_RESPONSE_LOGGING", true);
                }

                filterChain.doFilter(request, response);
            }
        });

        // SuhLoggingFilter보다 먼저 실행되도록 우선순위 설정
        registration.setOrder(Integer.MIN_VALUE);
        registration.addUrlPatterns("/*");

        log.info("SSE 로깅 제외 필터 등록 완료");
        return registration;
    }
}
