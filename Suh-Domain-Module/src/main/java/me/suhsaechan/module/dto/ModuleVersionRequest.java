package me.suhsaechan.module.object.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.suhsaechan.common.constant.ModuleType;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModuleVersionRequest {
  private ModuleType moduleType;
}