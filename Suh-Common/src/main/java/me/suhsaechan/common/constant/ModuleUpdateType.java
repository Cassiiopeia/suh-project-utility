package me.suhsaechan.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ModuleUpdateType {
  PERFORMANCE("성능 개선", "lightning", "green"),
  FEATURE("기능 추가", "plus circle", "blue"),
  BUGFIX("버그 수정", "bug", "red"),
  RELEASE("출시", "rocket", "orange"),
  OPTIMIZATION("최적화", "microchip", "blue"),
  SECURITY("보안 강화", "shield", "teal"),
  UI("UI 개선", "paint brush", "violet"),
  OTHER("기타", "info circle", "grey");

  private final String description;
  private final String semanticUiIcon;
  private final String color;
}
