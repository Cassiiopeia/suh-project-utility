package me.suhsaechan.grassplanter.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// extractContributionCountForDate 회귀 테스트 — GraphQL contributionsCollection 응답에서
// 특정 날짜의 contributionCount를 정확히 추출하는지 검증(#220 자동커밋 빈도 수정).
// 순수 파싱 로직만 검증하므로 Spring 컨텍스트 없이 직접 인스턴스화한다.
class GrassServiceParseTest {

  private final GrassService service =
      new GrassService(null, null, null, null, null, null);

  @Test
  void 오늘_기여_있음_파싱() {
    String json = "{\"data\":{\"user\":{\"contributionsCollection\":{\"contributionCalendar\":"
        + "{\"weeks\":[{\"contributionDays\":["
        + "{\"date\":\"2026-06-15\",\"contributionCount\":3},"
        + "{\"date\":\"2026-06-16\",\"contributionCount\":5}"
        + "]}]}}}}}";
    assertThat(service.extractContributionCountForDate(json, "2026-06-16")).isEqualTo(5);
  }

  @Test
  void 오늘_기여_없음_0_파싱() {
    String json = "{\"data\":{\"user\":{\"contributionsCollection\":{\"contributionCalendar\":"
        + "{\"weeks\":[{\"contributionDays\":["
        + "{\"date\":\"2026-06-16\",\"contributionCount\":0}"
        + "]}]}}}}}";
    assertThat(service.extractContributionCountForDate(json, "2026-06-16")).isZero();
  }

  @Test
  void 응답에_해당_날짜_없으면_0() {
    String json = "{\"data\":{\"user\":{\"contributionsCollection\":{\"contributionCalendar\":"
        + "{\"weeks\":[{\"contributionDays\":["
        + "{\"date\":\"2026-06-14\",\"contributionCount\":7}"
        + "]}]}}}}}";
    assertThat(service.extractContributionCountForDate(json, "2026-06-16")).isZero();
  }

  @Test
  void 여러_날짜중_정확한_날짜_매칭() {
    String json = "{\"contributionDays\":["
        + "{\"date\":\"2026-06-14\",\"contributionCount\":1},"
        + "{\"date\":\"2026-06-15\",\"contributionCount\":2},"
        + "{\"date\":\"2026-06-16\",\"contributionCount\":9},"
        + "{\"date\":\"2026-06-17\",\"contributionCount\":4}"
        + "]}";
    assertThat(service.extractContributionCountForDate(json, "2026-06-14")).isEqualTo(1);
    assertThat(service.extractContributionCountForDate(json, "2026-06-15")).isEqualTo(2);
    assertThat(service.extractContributionCountForDate(json, "2026-06-16")).isEqualTo(9);
    assertThat(service.extractContributionCountForDate(json, "2026-06-17")).isEqualTo(4);
  }

  @Test
  void 두자리수_기여_파싱() {
    String json = "{\"contributionDays\":[{\"date\":\"2026-06-16\",\"contributionCount\":42}]}";
    assertThat(service.extractContributionCountForDate(json, "2026-06-16")).isEqualTo(42);
  }

  @Test
  void 빈_응답은_0() {
    assertThat(service.extractContributionCountForDate("{}", "2026-06-16")).isZero();
  }
}
