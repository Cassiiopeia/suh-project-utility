package me.suhsaechan.common.repository;

import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.common.entity.GithubIssueHelper;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubIssueHelperRepository extends JpaRepository<GithubIssueHelper, UUID> {
  Optional<GithubIssueHelper> findByIssueUrl(String issueUrl);
}