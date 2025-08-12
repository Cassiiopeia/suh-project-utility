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
public class IssueHelperResponse {
  private String branchName;
  private String commitMessage;
  
  // PR 댓글용 Markdown 형식 문자열
  private String commentMarkdown;
}

