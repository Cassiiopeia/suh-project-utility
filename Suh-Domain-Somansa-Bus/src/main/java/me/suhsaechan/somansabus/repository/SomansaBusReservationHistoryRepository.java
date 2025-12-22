package me.suhsaechan.somansabus.repository;

import me.suhsaechan.somansabus.entity.SomansaBusReservationHistory;
import me.suhsaechan.somansabus.entity.SomansaBusUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SomansaBusReservationHistoryRepository extends JpaRepository<SomansaBusReservationHistory, UUID> {

  List<SomansaBusReservationHistory> findBySomansaBusUser(SomansaBusUser user);

  List<SomansaBusReservationHistory> findBySomansaBusUserSomansaBusUserId(UUID userId);

  List<SomansaBusReservationHistory> findBySomansaBusUserSomansaBusUserIdOrderByExecutedAtDesc(UUID userId);

  List<SomansaBusReservationHistory> findByReservationDate(LocalDate reservationDate);

  List<SomansaBusReservationHistory> findByIsSuccessTrue();

  List<SomansaBusReservationHistory> findByIsSuccessFalse();

  List<SomansaBusReservationHistory> findTop10BySomansaBusUserSomansaBusUserIdOrderByExecutedAtDesc(UUID userId);
}
