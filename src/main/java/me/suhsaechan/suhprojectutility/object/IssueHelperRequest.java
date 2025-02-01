package me.suhsaechan.suhprojectutility.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IssueHelperRequest {
  // GitHub 이슈 URL
  private String issueUrl;
}
