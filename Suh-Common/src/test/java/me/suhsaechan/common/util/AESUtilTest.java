package me.suhsaechan.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// clientHash 용 고정 IV 암호화: 결정성 · 기존 decrypt 왕복
class AESUtilTest {

  private AESUtil aesUtil;

  @BeforeEach
  void setUp() {
    aesUtil = new AESUtil();
    ReflectionTestUtils.setField(aesUtil, "secretKey", "0123456789abcdef");
    ReflectionTestUtils.setField(aesUtil, "legacyIv", "fedcba9876543210");
  }

  @Test
  void 같은_IP는_항상_같은_해시() {
    assertThat(aesUtil.encryptWithFixedIv("203.0.113.7"))
        .isEqualTo(aesUtil.encryptWithFixedIv("203.0.113.7"));
  }

  @Test
  void 다른_IP는_다른_해시() {
    assertThat(aesUtil.encryptWithFixedIv("203.0.113.7"))
        .isNotEqualTo(aesUtil.encryptWithFixedIv("203.0.113.8"));
  }

  @Test
  void 고정_IV_해시는_decrypt로_원래_IP_복원() {
    String clientHash = aesUtil.encryptWithFixedIv("203.0.113.7");
    assertThat(aesUtil.decrypt(clientHash)).isEqualTo("203.0.113.7");
  }

  @Test
  void IV_미설정이면_랜덤_IV_암호화로_대체() {
    ReflectionTestUtils.setField(aesUtil, "legacyIv", "");
    String clientHash = aesUtil.encryptWithFixedIv("203.0.113.7");
    assertThat(aesUtil.decrypt(clientHash)).isEqualTo("203.0.113.7");
  }
}
