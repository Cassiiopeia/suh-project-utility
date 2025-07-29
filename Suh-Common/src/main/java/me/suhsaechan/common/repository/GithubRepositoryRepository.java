package me.suhsaechan.common.repository;

import java.util.UUID;
import me.suhsaechan.common.entity.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubRepositoryRepository extends JpaRepository<GithubRepository, UUID> {
  GithubRepository findByFullName(String fullName);
}
