package me.suhsaechan.somansabus.repository;

import me.suhsaechan.somansabus.entity.SomansaBusMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SomansaBusMemberRepository extends JpaRepository<SomansaBusMember, UUID> {

  Optional<SomansaBusMember> findByLoginId(String loginId);

  List<SomansaBusMember> findByIsActiveTrue();

  List<SomansaBusMember> findByIsVerifiedTrue();

  List<SomansaBusMember> findByIsActiveTrueAndIsVerifiedTrue();

  boolean existsByLoginId(String loginId);
}
