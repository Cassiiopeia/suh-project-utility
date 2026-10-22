package me.suhsaechan.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;

/**
 * 세종 인증 학번별 연속 실패 제한: 3회 연속 실패 시 차단, 성공 시 초기화, 학번은 해시 키로만 저장 (#266)
 */
class SejongAuthAttemptLimiterTest {

  private final Map<String, Long> countByKey = new HashMap<>();
  private final SejongAuthAttemptLimiter limiter = new SejongAuthAttemptLimiter(inMemoryRedisson());

  @Test
  void 연속_3번_실패하면_차단한다() {
    limiter.recordFailure("21011234");
    limiter.recordFailure("21011234");
    assertThat(limiter.isBlocked("21011234")).isFalse();

    limiter.recordFailure("21011234");

    assertThat(limiter.isBlocked("21011234")).isTrue();
  }

  @Test
  void 성공하면_실패_횟수를_초기화한다() {
    limiter.recordFailure("21011234");
    limiter.recordFailure("21011234");
    limiter.recordSuccess("21011234");
    limiter.recordFailure("21011234");

    assertThat(limiter.isBlocked("21011234")).isFalse();
  }

  @Test
  void 다른_학번은_서로_영향이_없다() {
    limiter.recordFailure("21011234");
    limiter.recordFailure("21011234");
    limiter.recordFailure("21011234");

    assertThat(limiter.isBlocked("21015678")).isFalse();
  }

  @Test
  void 학번을_평문으로_저장하지_않는다() {
    limiter.recordFailure("21011234");

    assertThat(countByKey.keySet()).allSatisfy(key -> assertThat(key).doesNotContain("21011234"));
  }

  @Test
  void 빈_학번은_제한하지_않는다() {
    limiter.recordFailure(" ");
    limiter.recordFailure(null);

    assertThat(limiter.isBlocked(null)).isFalse();
    assertThat(countByKey).isEmpty();
  }

  // Redis 대신 메모리 Map (RAtomicLong 의 get·incrementAndGet·delete 만 사용)
  private RedissonClient inMemoryRedisson() {
    RedissonClient redissonClient = mock(RedissonClient.class);
    when(redissonClient.getAtomicLong(anyString())).thenAnswer(invocation -> {
      String key = invocation.getArgument(0);
      RAtomicLong counter = mock(RAtomicLong.class);
      when(counter.get()).thenAnswer(ignored -> countByKey.getOrDefault(key, 0L));
      when(counter.incrementAndGet()).thenAnswer(ignored -> countByKey.merge(key, 1L, Long::sum));
      when(counter.delete()).thenAnswer(ignored -> countByKey.remove(key) != null);
      return counter;
    });
    return redissonClient;
  }
}
