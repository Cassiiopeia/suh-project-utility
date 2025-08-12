package me.suhsaechan.github.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.entity.BasePostgresEntity;

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

  // count 증가 +1 메소드
  public void incrementCount() {
    if (this.count == null) {
      this.count = 1L;
    } else {
      this.count++;
    }
  }
}