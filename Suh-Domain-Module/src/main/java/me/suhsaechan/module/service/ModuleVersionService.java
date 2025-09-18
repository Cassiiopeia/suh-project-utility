package me.suhsaechan.module.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.entity.ModuleVersion;
import me.suhsaechan.common.entity.ModuleVersionUpdate;
import me.suhsaechan.common.repository.ModuleVersionRepository;
import me.suhsaechan.common.repository.ModuleVersionUpdateRepository;
import me.suhsaechan.module.dto.ModuleVersionRequest;
import me.suhsaechan.module.dto.ModuleVersionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleVersionService {
  private final ModuleVersionRepository moduleVersionRepository;
  private final ModuleVersionUpdateRepository moduleVersionUpdateRepository;

  @Transactional(readOnly = true)
  public ModuleVersionResponse getModuleVersions(ModuleVersionRequest request) {
    log.info("Getting module versions for module type: {}", request.getModuleType());

    List<ModuleVersion> moduleVersions = moduleVersionRepository.findByModuleTypeOrderByReleaseDateDesc(request.getModuleType());

    for (ModuleVersion version : moduleVersions) {
      List<ModuleVersionUpdate> updates = moduleVersionUpdateRepository.findByModuleVersionOrderByCreatedDateDesc(version);
      for(ModuleVersionUpdate moduleVersionUpdate : updates) {
        moduleVersionUpdate.setModuleUpdateTypeIcon(moduleVersionUpdate.getModuleUpdateType().getSemanticUiIcon());
      }
      version.setUpdates(updates);
    }

    return ModuleVersionResponse.builder()
        .versions(moduleVersions)
        .build();
  }
}