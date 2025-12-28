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

  private static final SimpleCookieJar cookieJar = new SimpleCookieJar();
  private static final Dispatcher dispatcher = new Dispatcher();

  static {
    dispatcher.setMaxRequests(100);
    dispatcher.setMaxRequestsPerHost(10);
    log.debug("OkHttpClient Dispatcher 설정: MaxRequests=100, MaxRequestsPerHost=10");
  }

  private static final OkHttpClient client = new OkHttpClient.Builder()
      .cookieJar(cookieJar)
      .dispatcher(dispatcher)
      .build();

  public static OkHttpClient getClient() {
    return client;
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
