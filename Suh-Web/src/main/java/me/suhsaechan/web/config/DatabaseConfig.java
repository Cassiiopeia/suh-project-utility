package me.suhsaechan.web.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// 데이터베이스 설정
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "me.suhsaechan.*.repository")
@EntityScan(basePackages = "me.suhsaechan.*.entity")
public class DatabaseConfig {
} 