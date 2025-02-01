package me.suhsaechan.suhprojectutility.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/docs/swagger**")
        .addResourceLocations("classpath:/META-INF/resources/swagger-ui.html");
    registry.addResourceHandler("/docs/webjars/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/");
  }
}

