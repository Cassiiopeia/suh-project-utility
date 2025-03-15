package me.suhsaechan.suhprojectutility.object.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.suhsaechan.suhprojectutility.object.constant.ModuleType;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModuleVersionRequest {
  private ModuleType moduleType;
}