package me.suhsaechan.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import me.suhsaechan.statistics.dto.ReferrerSource;
import org.junit.jupiter.api.Test;

class ReferrerSourceClassifierTest {

  private static final List<String> INTERNAL_HOSTS = List.of("lab.suhsaechan.kr", "localhost");

  private ReferrerSource classify(String referrer) {
    return ReferrerSourceClassifier.classify(referrer, INTERNAL_HOSTS);
  }

  @Test
  void 빈_값은_DIRECT() {
    assertThat(classify(null)).isEqualTo(ReferrerSource.DIRECT);
    assertThat(classify("")).isEqualTo(ReferrerSource.DIRECT);
    assertThat(classify("   ")).isEqualTo(ReferrerSource.DIRECT);
  }

  @Test
  void 구글_도메인은_GOOGLE() {
    assertThat(classify("https://www.google.com/")).isEqualTo(ReferrerSource.GOOGLE);
    assertThat(classify("https://www.google.co.kr/search?q=x")).isEqualTo(ReferrerSource.GOOGLE);
  }

  @Test
  void 네이버_도메인은_NAVER() {
    assertThat(classify("https://search.naver.com/search.naver?query=x")).isEqualTo(ReferrerSource.NAVER);
    assertThat(classify("https://m.naver.com")).isEqualTo(ReferrerSource.NAVER);
  }

  @Test
  void 깃허브_도메인은_GITHUB() {
    assertThat(classify("https://github.com/Cassiiopeia")).isEqualTo(ReferrerSource.GITHUB);
    assertThat(classify("https://cassiiopeia.github.io/x")).isEqualTo(ReferrerSource.GITHUB);
  }

  @Test
  void 자기_사이트는_INTERNAL() {
    assertThat(classify("https://LAB.SUHSAECHAN.KR:443/notice")).isEqualTo(ReferrerSource.INTERNAL);
    assertThat(classify("http://localhost:8080/")).isEqualTo(ReferrerSource.INTERNAL);
  }

  @Test
  void 접미사_위장_호스트는_OTHER() {
    assertThat(classify("https://evil-lab.suhsaechan.kr.attacker.com/")).isEqualTo(ReferrerSource.OTHER);
  }

  @Test
  void 호스트가_없거나_URL이_아니면_OTHER() {
    assertThat(classify("android-app://com.google.android.gm/")).isEqualTo(ReferrerSource.OTHER);
    assertThat(classify("not a url")).isEqualTo(ReferrerSource.OTHER);
  }

  @Test
  void 내부_호스트_문자열을_정리해_파싱() {
    assertThat(ReferrerSourceClassifier.parseInternalHosts(" A.com, ,b.com "))
        .containsExactly("a.com", "b.com");
  }
}
