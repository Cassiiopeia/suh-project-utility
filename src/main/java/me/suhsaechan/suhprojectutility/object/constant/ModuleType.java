package me.suhsaechan.suhprojectutility.object.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ModuleType {
  GITHUB_ISSUE_HELPER("깃헙 이슈 도우미"),
  TRANSLATOR("번역기");

  private final String description;
}
