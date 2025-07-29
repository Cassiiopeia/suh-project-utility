package me.suhsaechan.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Profile("dev")
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "📚 SUH-PROJECT-UTILITY 📚",
        description = """
            ### 🌐 새찬 서버 유틸리티 🌐 : http://suh-project.synology.me:8090
            """,
        version = "1.0v"
    ),
    servers = {
        @Server(url = "https://lab.suhsaechan.me", description = "메인 서버"),
        @Server(url = "http://localhost:8080", description = "로컬 서버") // 포트 수정
    }
)
public class SwaggerConfig implements WebMvcConfigurer {

  @Bean
  public OpenAPI openAPI() {
    SecurityScheme cookieAuthScheme = new SecurityScheme()
        .type(SecurityScheme.Type.APIKEY)
        .in(SecurityScheme.In.COOKIE)
        .name("JSESSIONID")
        .description("로그인 후 브라우저에서 발급된 JSESSIONID를 입력하세요.");

    SecurityRequirement securityRequirement = new SecurityRequirement()
        .addList("sessionCookie");

    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("sessionCookie", cookieAuthScheme))
        .addSecurityItem(securityRequirement)
        .servers(List.of(
            new io.swagger.v3.oas.models.servers.Server()
                .url("http://localhost:8080")
                .description("로컬 서버"),
            new io.swagger.v3.oas.models.servers.Server()
                .url("https://lab.suhsaechan.me")
                .description("메인 서버")
        ));
  }

  // Swagger Static 리소스
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/docs/swagger**")
        .addResourceLocations("classpath:/META-INF/resources/swagger-ui.html");
    registry.addResourceHandler("/docs/webjars/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/");
  }
}