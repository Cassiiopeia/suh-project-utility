package me.suhsaechan.common.util.security;

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

  public String encrypt(String data) {
    try {
      SecretKeySpec keySpec = new SecretKeySpec(
          secretKey.getBytes(StandardCharsets.UTF_8), "AES");
      IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
      byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encryptedBytes);
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
      IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
      SecretKeySpec keySpec = new SecretKeySpec(
          secretKey.getBytes(StandardCharsets.UTF_8), "AES");

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

      byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
      return new String(decrypted);
    } catch (Exception e) {
      log.error("복호화 실패 - 상세 오류: {}", e.toString(), e);
      if (e.getCause() != null) {
        log.error("원인: {}", e.getCause().toString());
      }
      return "decryption_failed_" + encrypted;
    }
  }
}