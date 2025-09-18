package me.suhsaechan.github.repository;

import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.github.entity.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GithubRepositoryRepository extends JpaRepository<GithubRepository, UUID> {
  Optional<GithubRepository> findByFullName(String fullName);
}
