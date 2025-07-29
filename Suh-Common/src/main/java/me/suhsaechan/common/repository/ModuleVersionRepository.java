package me.suhsaechan.common.repository;

import java.util.List;
import java.util.UUID;
import me.suhsaechan.common.constant.ModuleType;
import me.suhsaechan.common.entity.ModuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleVersionRepository extends JpaRepository<ModuleVersion, UUID> {
  List<ModuleVersion> findByModuleTypeOrderByReleaseDateDesc(ModuleType moduleType);
}