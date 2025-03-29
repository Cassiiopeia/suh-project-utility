package me.suhsaechan.suhprojectutility.object.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class GithubIssueHelper extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID issueHelperId;

  // GitHub Issue URL
  @Column(nullable = false, unique = true)
  private String issueUrl;

  // 생성된 브랜치명
  @Column(nullable = false)
  private String branchName;

  // 생성된 커밋 메시지
  @Column(nullable = false, columnDefinition = "TEXT")
  private String commitMessage;

  @ManyToOne(fetch = FetchType.LAZY)
  private GithubRepository githubRepository;

  @Column
  private String clientIp;

  private Long count;
}