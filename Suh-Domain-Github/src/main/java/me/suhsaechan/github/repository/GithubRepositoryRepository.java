package me.suhsaechan.github.repository;

import java.util.UUID;
import me.suhsaechan.github.entity.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubRepositoryRepository extends JpaRepository<GithubRepository, UUID> {
  GithubRepository findByFullName(String fullName);
}
