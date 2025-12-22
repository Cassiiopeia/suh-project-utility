package me.suhsaechan.somansabus.repository;

import me.suhsaechan.somansabus.entity.SomansaBusUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SomansaBusUserRepository extends JpaRepository<SomansaBusUser, UUID> {

  Optional<SomansaBusUser> findByLoginId(String loginId);

  List<SomansaBusUser> findByIsActiveTrue();

  List<SomansaBusUser> findByIsVerifiedTrue();

  List<SomansaBusUser> findByIsActiveTrueAndIsVerifiedTrue();

  boolean existsByLoginId(String loginId);
}
