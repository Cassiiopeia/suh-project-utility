package me.suhsaechan.statistics.service;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import me.suhsaechan.statistics.dto.ReferrerSource;
import org.springframework.util.StringUtils;

/**
 * 유입 경로 분류: referrer URL > 유입 출처(구글, 네이버, 깃허브, 직접, 기타, 내부)
 * 상태 없는 유틸, 내부 호스트는 호출자가 전달
 */
public final class ReferrerSourceClassifier {

  private static final Pattern GOOGLE_HOST_PATTERN = Pattern.compile("^(www\\.)?google\\.[a-z.]+$");
  private static final String HOST_DELIMITER = ",";

  private ReferrerSourceClassifier() {
  }

  public static ReferrerSource classify(String referrer, List<String> internalHosts) {
    if (!StringUtils.hasText(referrer)) {
      return ReferrerSource.DIRECT;
    }

    // URI 파싱 실패 또는 호스트 없음 (android-app:// 등)
    String host;
    try {
      host = URI.create(referrer.trim()).getHost();
    } catch (IllegalArgumentException e) {
      return ReferrerSource.OTHER;
    }
    if (!StringUtils.hasText(host)) {
      return ReferrerSource.OTHER;
    }
    host = host.toLowerCase(Locale.ROOT);

    if (isHostOrSubdomainOfAny(host, internalHosts)) {
      return ReferrerSource.INTERNAL;
    }
    if (GOOGLE_HOST_PATTERN.matcher(host).matches()) {
      return ReferrerSource.GOOGLE;
    }
    if (isHostOrSubdomainOf(host, "naver.com")) {
      return ReferrerSource.NAVER;
    }
    if (isHostOrSubdomainOf(host, "github.com") || host.endsWith(".github.io")) {
      return ReferrerSource.GITHUB;
    }
    return ReferrerSource.OTHER;
  }

  /**
   * 쉼표 구분 호스트 문자열 > 호스트 목록 (trim, 소문자, 빈 항목 제거)
   */
  public static List<String> parseInternalHosts(String commaSeparatedHosts) {
    if (!StringUtils.hasText(commaSeparatedHosts)) {
      return List.of();
    }
    return Arrays.stream(commaSeparatedHosts.split(HOST_DELIMITER))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .map(host -> host.toLowerCase(Locale.ROOT))
        .toList();
  }

  private static boolean isHostOrSubdomainOfAny(String host, List<String> baseHosts) {
    return baseHosts.stream().anyMatch(baseHost -> isHostOrSubdomainOf(host, baseHost));
  }

  // 접미사 위장 방지: 점 경계까지 포함해 비교
  private static boolean isHostOrSubdomainOf(String host, String baseHost) {
    return host.equals(baseHost) || host.endsWith("." + baseHost);
  }
}
