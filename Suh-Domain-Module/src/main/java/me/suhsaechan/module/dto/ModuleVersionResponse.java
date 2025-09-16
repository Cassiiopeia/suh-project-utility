package me.suhsaechan.module.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.suhsaechan.common.entity.ModuleVersion;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModuleVersionResponse {
  private List<ModuleVersion> versions;
}