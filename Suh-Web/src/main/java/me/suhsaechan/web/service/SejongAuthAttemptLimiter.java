package me.suhsaechan.web.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 세종 인증 학번별 연속 실패 제한: 데모 API 로 남의 포털 계정을 잠그지 못하게 함 (#266)
 * 포털은 약 5회 실패 시 계정 잠금 > 연속 3회 실패하면 30분 동안 포털 호출 차단, 성공 시 초기화
 */
@Service
@RequiredArgsConstructor
public class SejongAuthAttemptLimiter {

	// Redis 키: sejong-auth:failed-attempts:{학번 SHA-256} (학번 평문 저장 안 함)
	private static final String FAILED_ATTEMPTS_KEY_PREFIX = "sejong-auth:failed-attempts:";
	private static final int MAX_CONSECUTIVE_FAILURES = 3;
	private static final Duration BLOCK_DURATION = Duration.ofMinutes(30);

	private final RedissonClient redissonClient;

	public boolean isBlocked(String studentId) {
		if (isBlank(studentId)) {
			return false;
		}
		return counterOf(studentId).get() >= MAX_CONSECUTIVE_FAILURES;
	}

	/**
	 * 실패 1회 기록 (마지막 실패 기준 30분 뒤 만료)
	 */
	public void recordFailure(String studentId) {
		if (isBlank(studentId)) {
			return;
		}
		RAtomicLong counter = counterOf(studentId);
		counter.incrementAndGet();
		counter.expire(BLOCK_DURATION);
	}

	public void recordSuccess(String studentId) {
		if (isBlank(studentId)) {
			return;
		}
		counterOf(studentId).delete();
	}

	private RAtomicLong counterOf(String studentId) {
		return redissonClient.getAtomicLong(FAILED_ATTEMPTS_KEY_PREFIX + hash(studentId.trim()));
	}

	private static String hash(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e);
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
