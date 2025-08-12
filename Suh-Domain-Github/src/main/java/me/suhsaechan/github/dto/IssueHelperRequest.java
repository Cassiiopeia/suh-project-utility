package me.suhsaechan.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IssueHelperRequest {
  // GitHub 이슈 URL
  private String issueUrl;

  private String clientHash;

  // GIHub 저장소 이름
  private String githubRepositoryName;
}
