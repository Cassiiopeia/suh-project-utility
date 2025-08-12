package me.suhsaechan.github.repository;

import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.github.entity.GithubIssueHelper;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubIssueHelperRepository extends JpaRepository<GithubIssueHelper, UUID> {
  Optional<GithubIssueHelper> findByIssueUrl(String issueUrl);
}