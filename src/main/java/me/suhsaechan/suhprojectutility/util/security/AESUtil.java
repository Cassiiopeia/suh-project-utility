package me.suhsaechan.suhprojectutility.util.security;

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

  public static String encrypt(String data, String key) {
    try {
      SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      byte[] encryptedBytes = cipher.doFinal(data.getBytes("UTF-8"));
      return Base64.getEncoder().encodeToString(encryptedBytes);
    } catch (Exception e) {
      log.error("암호화 실패 - 상세 오류: {}", e.toString(), e);
      // 암호화 실패 시 로그만 남기고 진행 (예외를 던지지 않음)
      if (e.getCause() != null) {
        log.error("원인: {}", e.getCause().toString());
      }
      // 암호화 실패 시 기본값 반환 (빈 문자열 또는 특정 표시)
      return "encryption_failed";
    }}

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