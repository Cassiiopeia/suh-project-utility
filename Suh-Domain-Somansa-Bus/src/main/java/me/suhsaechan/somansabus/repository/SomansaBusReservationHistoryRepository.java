package me.suhsaechan.somansabus.repository;

import me.suhsaechan.somansabus.entity.SomansaBusMember;
import me.suhsaechan.somansabus.entity.SomansaBusReservationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SomansaBusReservationHistoryRepository extends JpaRepository<SomansaBusReservationHistory, UUID> {

  List<SomansaBusReservationHistory> findBySomansaBusMember(SomansaBusMember member);

  List<SomansaBusReservationHistory> findBySomansaBusMemberSomansaBusMemberId(UUID memberId);

  List<SomansaBusReservationHistory> findBySomansaBusMemberSomansaBusMemberIdOrderByExecutedAtDesc(UUID memberId);

  List<SomansaBusReservationHistory> findByReservationDate(LocalDate reservationDate);

  List<SomansaBusReservationHistory> findByIsSuccessTrue();

  List<SomansaBusReservationHistory> findByIsSuccessFalse();

  List<SomansaBusReservationHistory> findTop10BySomansaBusMemberSomansaBusMemberIdOrderByExecutedAtDesc(UUID memberId);

  List<SomansaBusReservationHistory> findByReservationDateBetween(LocalDate startDate, LocalDate endDate);

  @Query("SELECT COUNT(h) FROM SomansaBusReservationHistory h WHERE h.reservationDate BETWEEN :startDate AND :endDate")
  Integer countByReservationDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  @Query("SELECT COUNT(h) FROM SomansaBusReservationHistory h WHERE h.reservationDate BETWEEN :startDate AND :endDate AND h.isSuccess = true")
  Integer countByReservationDateBetweenAndIsSuccessTrue(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  @Query("SELECT COUNT(h) FROM SomansaBusReservationHistory h WHERE h.reservationDate BETWEEN :startDate AND :endDate AND h.isSuccess = false")
  Integer countByReservationDateBetweenAndIsSuccessFalse(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  List<SomansaBusReservationHistory> findTop20ByOrderByExecutedAtDesc();
}
