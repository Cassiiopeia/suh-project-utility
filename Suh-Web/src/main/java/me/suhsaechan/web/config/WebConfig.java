package me.suhsaechan.web.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 MVC 설정 클래스
 * 정적 리소스 핸들링, 파일 업로드 경로 설정, 인터셉터 등록
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PageVisitInterceptor pageVisitInterceptor;

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
    }

    /**
     * 인터셉터 등록
     * 페이지 방문 기록 인터셉터 추가
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(pageVisitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/**", "/static/**", "/css/**", "/js/**", "/images/**");
    }
}
