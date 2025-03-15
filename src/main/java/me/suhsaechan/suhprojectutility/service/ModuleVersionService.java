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

    // Get all versions for the module type, ordered by release date
    List<ModuleVersion> moduleVersions = moduleVersionRepository.findByModuleTypeOrderByReleaseDateDesc(request.getModuleType());

    // For each version, fetch its updates
    for (ModuleVersion version : moduleVersions) {
      List<ModuleVersionUpdate> updates = moduleVersionUpdateRepository.findByModuleVersionOrderByCreatedDateDesc(version);
      version.setUpdates(updates); // Make sure ModuleVersion has a List<ModuleVersionUpdate> updates field
    }

    return ModuleVersionResponse.builder()
        .versions(moduleVersions) // Changed from moduleVersions to versions to match frontend expectations
        .build();
  }
}