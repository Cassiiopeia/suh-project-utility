package me.suhsaechan.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@Component
public class GrassEncryptionUtil {

    @Value("${grass.encryption.secret-key}")
    private String secretKey;

    @Value("${grass.encryption.iv:}")
    private String legacyIv;

    private static final String ALGORITHM = "AES/CBC/PKCS5PADDING";

    public String encrypt(String value) {
        try {
            // 랜덤 IV 생성 (16바이트)
            byte[] ivBytes = new byte[16];
            new SecureRandom().nextBytes(ivBytes);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            SecretKeySpec skeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            
            // IV + 암호문을 결합하여 Base64 인코딩
            byte[] result = new byte[ivBytes.length + encrypted.length];
            System.arraycopy(ivBytes, 0, result, 0, ivBytes.length);
            System.arraycopy(encrypted, 0, result, ivBytes.length, encrypted.length);
            
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception ex) {
            log.error("암호화 중 오류 발생: {}", ex.getMessage(), ex);
            throw new RuntimeException("암호화 실패", ex);
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
                    
                    IvParameterSpec iv = new IvParameterSpec(ivBytes);
                    SecretKeySpec skeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

                    Cipher cipher = Cipher.getInstance(ALGORITHM);
                    cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

                    byte[] original = cipher.doFinal(cipherBytes);
                    return new String(original, StandardCharsets.UTF_8);
                } catch (Exception newFormatException) {
                    // 새로운 형식 복호화 실패시 레거시 형식으로 시도
                    log.debug("새로운 형식 복호화 실패, 레거시 형식으로 시도: {}", newFormatException.getMessage());
                }
            }
            
            // 레거시 형식 복호화 시도 (고정 IV 사용)
            return decryptLegacy(encrypted);
            
        } catch (Exception ex) {
            log.error("복호화 중 오류 발생: {}", ex.getMessage(), ex);
            throw new RuntimeException("복호화 실패", ex);
        }
    }

    /**
     * 레거시 복호화 메서드 (고정 IV 사용)
     * @param encrypted 암호화된 문자열
     * @return 복호화된 문자열
     */
    private String decryptLegacy(String encrypted) {
        if (legacyIv == null || legacyIv.isEmpty()) {
            throw new IllegalStateException("레거시 IV가 설정되지 않았습니다. grass.encryption.iv 속성을 확인하세요.");
        }
        
        try {
            IvParameterSpec iv = new IvParameterSpec(legacyIv.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec skeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            log.info("레거시 형식으로 복호화 성공");
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException("레거시 복호화 실패", ex);
        }
    }
}