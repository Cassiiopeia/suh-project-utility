package me.suhsaechan.suhprojectutility.repository;

import java.util.UUID;
import me.suhsaechan.suhprojectutility.object.postgres.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubRepositoryRepository extends JpaRepository<GithubRepository, UUID> {
  GithubRepository findByFullName(String fullName);
}
