package me.suhsaechan.web.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Value("${spring.data.redis.host}")
  private String redisHost;

  @Value("${spring.data.redis.connection-pool-size}")
  private int connectionPoolSize;

  @Value("${spring.data.redis.connection-minimum-size}")
  private int connectionMinimumSize;

  @Value("${spring.data.redis.port}")
  private String redisPort;

  @Value("${spring.data.redis.password}")
  private String redisPassword;

  private static final int CACHE_EXPIRED_TIME = 24 * 60; // 캐시 만료 시간 (24시간)

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(createRedisValueSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(createRedisValueSerializer());

    return template;
  }

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
    return RedisCacheManager.builder(factory)
        .cacheDefaults(createCacheConfiguration(Duration.ofMinutes(CACHE_EXPIRED_TIME)))
        // 대시보드 Summary 캐시: 5분 TTL
        .withCacheConfiguration("dashboardSummary", createCacheConfiguration(Duration.ofMinutes(5)))
        // 대시보드 기간 통계 캐시: 5분 TTL
        .withCacheConfiguration("dashboardStatistics", createCacheConfiguration(Duration.ofMinutes(5)))
        // ServerOption 캐시: 1시간 TTL (설정 변경 빈도 낮음)
        .withCacheConfiguration("serverOption", createCacheConfiguration(Duration.ofHours(1)))
        .build();
  }

  /**
   * 캐시 설정 (TTL만 다름): 문자열 키 + JSON 값, null 미캐시
   */
  private RedisCacheConfiguration createCacheConfiguration(Duration timeToLive) {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(timeToLive)
        .disableCachingNullValues()
        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createRedisValueSerializer()));
  }

  /**
   * Redis 값 JSON 직렬화기: 기본 타입 정보 형식 유지(기존 캐시 호환) + LocalDateTime 등 java.time 지원
   */
  private GenericJackson2JsonRedisSerializer createRedisValueSerializer() {
    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
    serializer.configure(objectMapper -> objectMapper
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    return serializer;
  }

  @Bean
  public RedissonClient redissonClient() {
    // Redisson의 Config 객체 생성
    Config config = new Config();
    String address = "redis://" + redisHost + ":" + redisPort;

    // Redis 단일 서버 설정
    config.useSingleServer()
        .setAddress(address) // Redis 서버 주소
        .setConnectionPoolSize(connectionPoolSize) // 연결 풀 크기
        .setConnectionMinimumIdleSize(connectionMinimumSize) // 최소 연결 수
        .setPassword(redisPassword); // 비밀번호 설정

    // RedissonClient 객체를 반환
    return Redisson.create(config);
  }
}