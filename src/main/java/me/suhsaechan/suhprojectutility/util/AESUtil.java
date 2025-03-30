package me.suhsaechan.suhprojectutility.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AESUtil {

  @Value("${aes.secret-key}")
  private String secretKey;

  @Value("${aes.iv}")
  private String iv;

  private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

  public String encrypt(String value) {
    try {
      IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
      SecretKeySpec keySpec = new SecretKeySpec(
          secretKey.getBytes(StandardCharsets.UTF_8), "AES");

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

      byte[] encrypted = cipher.doFinal(value.getBytes());
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
      throw new RuntimeException("암호화 실패", e);
    }
  }

  public String decrypt(String encrypted) {
    try {
      IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
      SecretKeySpec keySpec = new SecretKeySpec(
          secretKey.getBytes(StandardCharsets.UTF_8), "AES");

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

      byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
      return new String(decrypted);
    } catch (Exception e) {
      log.warn("복호화 실패: {}. 원본 값을 반환합니다.", e.getMessage());
      return encrypted;
    }
  }
}