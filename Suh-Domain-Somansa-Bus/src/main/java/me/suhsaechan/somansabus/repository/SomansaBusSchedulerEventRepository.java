package me.suhsaechan.somansabus.repository;

import java.util.List;
import java.util.UUID;
import me.suhsaechan.somansabus.entity.SomansaBusSchedulerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SomansaBusSchedulerEventRepository
    extends JpaRepository<SomansaBusSchedulerEvent, UUID> {

  List<SomansaBusSchedulerEvent> findTop30ByOrderByOccurredAtDesc();
}
