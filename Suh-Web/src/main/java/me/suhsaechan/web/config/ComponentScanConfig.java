package me.suhsaechan.web.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// 컴포넌트 스캔 설정
@Configuration
@ComponentScan(basePackages = {
    "me.suhsaechan.common",
    "me.suhsaechan.docker",
    "me.suhsaechan.github",
    "me.suhsaechan.translate",
    "me.suhsaechan.notice",
    "me.suhsaechan.module",
    "me.suhsaechan.application",
    "me.suhsaechan.study",
    "me.suhsaechan.grassplanter",
    "me.suhsaechan.web"
})
public class ComponentScanConfig {
} 