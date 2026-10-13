package me.suhsaechan.web.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * 웹 MVC 설정 클래스
 * 정적 리소스 핸들링, 파일 업로드 경로 설정, 인터셉터 등록
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PageVisitInterceptor pageVisitInterceptor;
    private final PublicEndpointConfig publicEndpointConfig;
    private final LocaleChangeInterceptor localeChangeInterceptor;

    /**
     * 정적 리소스 핸들러 설정
     * 파일 업로드 경로 매핑
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 로컬 파일 시스템 업로드 경로 매핑
        String uploadDir = System.getProperty("user.dir") + "/upload";
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadResourcePath = uploadPath.toUri().toString();

        registry.addResourceHandler("/upload/**", "/uploads/**", "/suh-project-utility/dev-uploads/**", "/suh-project-utility/upload/**")
                .addResourceLocations(uploadResourcePath);

        // CSS/JS 는 파일 내용 해시가 URL 에 붙으므로 길게 캐시해도 배포 시 자동으로 갱신된다
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .resourceChain(true)
                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .resourceChain(true)
                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic());
    }

    /**
     * 템플릿의 정적 리소스 링크를 해시가 포함된 URL 로 바꿔 준다.
     * 배포 환경의 application.yml 은 외부 시크릿으로 덮어써지므로 yml 설정 대신 코드로 등록한다.
     */
    @Bean
    public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
        return new ResourceUrlEncodingFilter();
    }

    /**
     * 인터셉터 등록
     * 페이지 방문 기록 인터셉터 추가
     *
     * PublicEndpointConfig에서 중앙 관리되는 화이트리스트 경로를 자동으로 제외합니다.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(pageVisitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(publicEndpointConfig.getInterceptorExcludedPaths());

        registry.addInterceptor(localeChangeInterceptor);
    }
}
