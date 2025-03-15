package me.suhsaechan.suhprojectutility.repository;

import java.util.List;
import java.util.UUID;
import me.suhsaechan.suhprojectutility.object.constant.ModuleType;
import me.suhsaechan.suhprojectutility.object.postgres.ModuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleVersionRepository extends JpaRepository<ModuleVersion, UUID> {
  List<ModuleVersion> findByModuleTypeOrderByReleaseDateDesc(ModuleType moduleType);
}