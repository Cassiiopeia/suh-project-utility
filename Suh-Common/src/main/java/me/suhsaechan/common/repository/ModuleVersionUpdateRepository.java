package me.suhsaechan.common.repository;

import java.util.List;
import java.util.UUID;
import me.suhsaechan.common.entity.ModuleVersion;
import me.suhsaechan.common.entity.ModuleVersionUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleVersionUpdateRepository extends JpaRepository<ModuleVersionUpdate, UUID> {
  List<ModuleVersionUpdate> findByModuleVersionOrderByCreatedDateDesc(ModuleVersion moduleVersion);
}