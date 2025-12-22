package me.suhsaechan.somansabus.repository;

import me.suhsaechan.somansabus.entity.SomansaBusSchedule;
import me.suhsaechan.somansabus.entity.SomansaBusUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SomansaBusScheduleRepository extends JpaRepository<SomansaBusSchedule, UUID> {

  List<SomansaBusSchedule> findBySomansaBusUser(SomansaBusUser user);

  List<SomansaBusSchedule> findBySomansaBusUserSomansaBusUserId(UUID userId);

  List<SomansaBusSchedule> findByIsActiveTrue();

  List<SomansaBusSchedule> findBySomansaBusUserAndIsActiveTrue(SomansaBusUser user);

  List<SomansaBusSchedule> findBySomansaBusUserSomansaBusUserIdAndIsActiveTrue(UUID userId);
}
