package me.suhsaechan.somansabus.repository;

import java.util.UUID;
import me.suhsaechan.somansabus.entity.SomansaBusSchedulerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SomansaBusSchedulerStateRepository
    extends JpaRepository<SomansaBusSchedulerState, UUID> {
}
