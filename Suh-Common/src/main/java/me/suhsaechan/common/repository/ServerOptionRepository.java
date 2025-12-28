package me.suhsaechan.common.repository;

import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.entity.ServerOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 서버 설정 Repository
 */
@Repository
public interface ServerOptionRepository extends JpaRepository<ServerOption, UUID> {

  /**
   * 설정 키로 조회
   */
  Optional<ServerOption> findByOptionKey(ServerOptionKey optionKey);

  /**
   * 설정 키 존재 여부 확인
   */
  boolean existsByOptionKey(ServerOptionKey optionKey);
}

