package me.suhsaechan.web.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// 데이터베이스 설정
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {
    "me.suhsaechan.common.repository",
    "me.suhsaechan.docker.repository",
    "me.suhsaechan.github.repository", 
    "me.suhsaechan.translate.repository",
    "me.suhsaechan.notice.repository",
    "me.suhsaechan.module.repository",
    "me.suhsaechan.application.repository"
})
@EntityScan(basePackages = {
    "me.suhsaechan.common.entity",
    "me.suhsaechan.docker.entity",
    "me.suhsaechan.github.entity",
    "me.suhsaechan.translate.entity", 
    "me.suhsaechan.notice.entity",
    "me.suhsaechan.module.entity"
})
public class DatabaseConfig {
} 