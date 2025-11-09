package me.suhsaechan.web.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 웹 MVC 설정 클래스
 * 정적 리소스 핸들링, 파일 업로드 경로 설정
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.domain}")
    private String fileDomain;

    @Value("${file.dir}")
    private String fileDir;

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
}
