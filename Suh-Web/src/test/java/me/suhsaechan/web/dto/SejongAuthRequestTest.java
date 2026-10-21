package me.suhsaechan.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 세종 인증 요청 로그 노출: toString 에 비밀번호 미포함 (#263)
 */
class SejongAuthRequestTest {

  @Test
  void toString에_비밀번호가_포함되지_않는다() {
    SejongAuthRequest request = SejongAuthRequest.builder()
        .studentId("21011234")
        .password("secret-password-value")
        .authType("INTEGRATED")
        .build();

    assertThat(request.toString())
        .contains("21011234")
        .doesNotContain("secret-password-value");
  }
}
