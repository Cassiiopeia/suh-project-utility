package me.suhsaechan.web.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpConfig {

  /**
   * OkHttpClient.Builder 빈 등록 : CookieJar 함께 사용
   */
  @Bean
  public OkHttpClient.Builder okHttpClientBuilder() {
    return new OkHttpClient.Builder()
        .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true);
  }

  /**
   * 기본 OkHttpClient 빈 등록
   */
  @Bean
  public OkHttpClient okHttpClient(OkHttpClient.Builder builder) {
    return builder.build();
  }
}