package me.suhsaechan.suhprojectutility.object.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.suhsaechan.suhprojectutility.object.postgres.ModuleVersion;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModuleVersionResponse {
  private List<ModuleVersion> versions;
}