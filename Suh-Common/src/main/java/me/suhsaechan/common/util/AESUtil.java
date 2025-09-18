package me.suhsaechan.common.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
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

  @Value("${aes.iv:}")
  private String legacyIv;

  private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

  public String encrypt(String data) {
    try {
      // 랜덤 IV 생성 (16바이트)
      byte[] ivBytes = new byte[16];
      new SecureRandom().nextBytes(ivBytes);
      IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
      
      SecretKeySpec keySpec = new SecretKeySpec(
          secretKey.getBytes(StandardCharsets.UTF_8), "AES");

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
      byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
      
      // IV + 암호문을 결합하여 Base64 인코딩
      byte[] result = new byte[ivBytes.length + encryptedBytes.length];
      System.arraycopy(ivBytes, 0, result, 0, ivBytes.length);
      System.arraycopy(encryptedBytes, 0, result, ivBytes.length, encryptedBytes.length);
      
      return Base64.getEncoder().encodeToString(result);
    } catch (Exception e) {
      log.error("암호화 실패 - 상세 오류: {}", e.toString(), e);
      if (e.getCause() != null) {
        log.error("원인: {}", e.getCause().toString());
      }
      return "encryption_failed";
    }
  }

  public String decrypt(String encrypted) {
    try {
      byte[] allBytes = Base64.getDecoder().decode(encrypted);
      
      // 새로운 형식 (IV + 암호문)인지 확인 (최소 17바이트: 16바이트 IV + 최소 1바이트 암호문)
      if (allBytes.length >= 17) {
        try {
          // IV와 암호문 분리
          byte[] ivBytes = Arrays.copyOfRange(allBytes, 0, 16);
          byte[] cipherBytes = Arrays.copyOfRange(allBytes, 16, allBytes.length);
          
          IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
          SecretKeySpec keySpec = new SecretKeySpec(
              secretKey.getBytes(StandardCharsets.UTF_8), "AES");

          Cipher cipher = Cipher.getInstance(ALGORITHM);
          cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

          byte[] decrypted = cipher.doFinal(cipherBytes);
          return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception newFormatException) {
          // 새로운 형식 복호화 실패시 레거시 형식으로 시도
          log.debug("새로운 형식 복호화 실패, 레거시 형식으로 시도: {}", newFormatException.getMessage());
        }
      }
      
      // 레거시 형식 복호화 시도 (고정 IV 사용)
      return decryptLegacy(encrypted);
      
    } catch (Exception e) {
      log.error("복호화 실패 - 상세 오류: {}", e.toString(), e);
      if (e.getCause() != null) {
        log.error("원인: {}", e.getCause().toString());
      }
      return "decryption_failed_" + encrypted;
    }
  }

  /**
   * 레거시 복호화 메서드 (고정 IV 사용)
   * @param encrypted 암호화된 문자열
   * @return 복호화된 문자열
   */
  private String decryptLegacy(String encrypted) {
    if (legacyIv == null || legacyIv.isEmpty()) {
      throw new IllegalStateException("레거시 IV가 설정되지 않았습니다. aes.iv 속성을 확인하세요.");
    }
    
    try {
      IvParameterSpec ivSpec = new IvParameterSpec(legacyIv.getBytes(StandardCharsets.UTF_8));
      SecretKeySpec keySpec = new SecretKeySpec(
          secretKey.getBytes(StandardCharsets.UTF_8), "AES");

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

      byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
      log.info("레거시 형식으로 복호화 성공");
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("레거시 복호화 실패", e);
    }
  }
}