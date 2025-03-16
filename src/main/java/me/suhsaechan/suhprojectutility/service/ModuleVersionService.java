package me.suhsaechan.suhprojectutility.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.object.postgres.ModuleVersion;
import me.suhsaechan.suhprojectutility.object.postgres.ModuleVersionUpdate;
import me.suhsaechan.suhprojectutility.object.request.ModuleVersionRequest;
import me.suhsaechan.suhprojectutility.object.response.ModuleVersionResponse;
import me.suhsaechan.suhprojectutility.repository.ModuleVersionRepository;
import me.suhsaechan.suhprojectutility.repository.ModuleVersionUpdateRepository;
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