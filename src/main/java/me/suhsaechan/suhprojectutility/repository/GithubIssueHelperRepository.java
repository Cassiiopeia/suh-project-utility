package me.suhsaechan.suhprojectutility.repository;

import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.suhprojectutility.object.postgres.GithubIssueHelper;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubIssueHelperRepository extends JpaRepository<GithubIssueHelper, UUID> {
  Optional<GithubIssueHelper> findByIssueUrl(String issueUrl);
}