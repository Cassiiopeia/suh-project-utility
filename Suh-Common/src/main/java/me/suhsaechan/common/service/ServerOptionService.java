package me.suhsaechan.common.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.entity.ServerOption;
import me.suhsaechan.common.repository.ServerOptionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서버 설정 서비스
 * DB 기반 동적 설정 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServerOptionService {

  private final ServerOptionRepository serverOptionRepository;

  /**
   * 설정 값 조회 (없으면 기본값 반환)
   * Redis 캐시 사용 (1시간 TTL)
   */
  @Cacheable(value = "serverOption", key = "#key.name()")
  public String getOptionValue(ServerOptionKey key) {
    return serverOptionRepository.findByOptionKey(key)
        .map(ServerOption::getOptionValue)
        .orElseGet(() -> {
          log.debug("설정 값이 없어 기본값 사용 - key: {}, defaultValue: {}",
              key, key.getDefaultValue());
          return key.getDefaultValue();
        });
  }

  /**
   * 설정 값을 Integer로 조회
   */
  public int getOptionValueAsInt(ServerOptionKey key) {
    String value = getOptionValue(key);
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      log.warn("설정 값을 Integer로 변환 실패 - key: {}, value: {}, 기본값 사용",
          key, value);
      return Integer.parseInt(key.getDefaultValue());
    }
  }

  /**
   * 설정 값 저장 또는 업데이트
   * 저장 시 해당 키의 캐시 무효화
   */
  @CacheEvict(value = "serverOption", key = "#key.name()")
  @Transactional
  public ServerOption setOptionValue(ServerOptionKey key, String value) {
    ServerOption option = serverOptionRepository.findByOptionKey(key)
        .orElse(ServerOption.builder()
            .optionKey(key)
            .build());

    option.setOptionValue(value);
    ServerOption saved = serverOptionRepository.save(option);
    log.info("서버 설정 저장 - key: {}, value: {}", key, value);
    return saved;
  }

  /**
   * 모든 설정 조회
   * Redis 캐시 사용 (1시간 TTL)
   */
  @Cacheable(value = "serverOption", key = "'all'")
  public List<ServerOption> getAllOptions() {
    return serverOptionRepository.findAll();
  }

  /**
   * 특정 설정 조회 (엔티티 반환)
   */
  public ServerOption getOption(ServerOptionKey key) {
    return serverOptionRepository.findByOptionKey(key)
        .orElse(null);
  }

  /**
   * 기본 설정 초기화
   * 설정이 없으면 기본값으로 생성
   */
  @Transactional
  public void initializeDefaultOptions() {
    for (ServerOptionKey key : ServerOptionKey.values()) {
      if (!serverOptionRepository.existsByOptionKey(key)) {
        ServerOption option = ServerOption.builder()
            .optionKey(key)
            .optionValue(key.getDefaultValue())
            .build();
        serverOptionRepository.save(option);
        log.info("기본 설정 생성 - key: {}, value: {}", key, key.getDefaultValue());
      }
    }
  }
}

