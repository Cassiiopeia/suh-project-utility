package me.suhsaechan.somansabus.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SomansaBusHttpClient {

  private static final Dispatcher dispatcher = new Dispatcher();

  static {
    dispatcher.setMaxRequests(100);
    dispatcher.setMaxRequestsPerHost(10);
    log.debug("OkHttpClient Dispatcher 설정: MaxRequests=100, MaxRequestsPerHost=10");
  }

  // 요청마다 독립 쿠키 저장소를 가진 클라이언트 생성 — 멤버 간 세션 혼용 방지
  public static OkHttpClient newClient() {
    return new OkHttpClient.Builder()
        .cookieJar(new SimpleCookieJar())
        .dispatcher(dispatcher)
        .build();
  }

  @Slf4j
  public static class SimpleCookieJar implements CookieJar {
    private final List<Cookie> cookieStore = new ArrayList<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
      cookieStore.addAll(cookies);
      for (Cookie cookie : cookies) {
        log.debug("쿠키 저장: {}={}, Domain: {}", cookie.name(), cookie.value(), cookie.domain());
      }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
      List<Cookie> validCookies = new ArrayList<>();
      for (Cookie cookie : cookieStore) {
        if (cookie.matches(url)) {
          validCookies.add(cookie);
        }
      }
      return validCookies;
    }
  }
}
