package me.suhsaechan.suhprojectutility.repository;

import java.util.List;
import java.util.UUID;
import me.suhsaechan.suhprojectutility.object.postgres.ModuleVersion;
import me.suhsaechan.suhprojectutility.object.postgres.ModuleVersionUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleVersionUpdateRepository extends JpaRepository<ModuleVersionUpdate, UUID> {
  List<ModuleVersionUpdate> findByModuleVersionOrderByCreatedDateDesc(ModuleVersion moduleVersion);
}