package me.suhsaechan.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 전역 설정
 * LocalDateTime, LocalDate, LocalTime 직렬화 포맷 설정
 * Hibernate 프록시 객체 직렬화 지원
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "HH:mm:ss";

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Hibernate 프록시 처리 모듈 등록
        Hibernate6Module hibernate6Module = new Hibernate6Module();
        hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        objectMapper.registerModule(hibernate6Module);

        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime 직렬화 포맷
        javaTimeModule.addSerializer(
            java.time.LocalDateTime.class,
            new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))
        );

        // LocalDate 직렬화 포맷
        javaTimeModule.addSerializer(
            java.time.LocalDate.class,
            new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT))
        );

        // LocalTime 직렬화 포맷
        javaTimeModule.addSerializer(
            java.time.LocalTime.class,
            new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_FORMAT))
        );

        objectMapper.registerModule(javaTimeModule);

        // 타임스탬프 형식 비활성화 (ISO-8601 문자열 형식 사용)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}
