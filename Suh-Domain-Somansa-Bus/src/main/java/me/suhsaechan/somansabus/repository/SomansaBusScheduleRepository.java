package me.suhsaechan.somansabus.repository;

import me.suhsaechan.somansabus.entity.SomansaBusMember;
import me.suhsaechan.somansabus.entity.SomansaBusSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SomansaBusScheduleRepository extends JpaRepository<SomansaBusSchedule, UUID> {

  List<SomansaBusSchedule> findBySomansaBusMember(SomansaBusMember member);

  List<SomansaBusSchedule> findBySomansaBusMemberSomansaBusMemberId(UUID memberId);

  @Query("SELECT s FROM SomansaBusSchedule s " +
      "JOIN FETCH s.somansaBusMember " +
      "JOIN FETCH s.somansaBusRoute " +
      "WHERE s.somansaBusMember.somansaBusMemberId = :memberId")
  List<SomansaBusSchedule> findByMemberIdWithDetails(@Param("memberId") UUID memberId);

  List<SomansaBusSchedule> findByIsActiveTrue();

  List<SomansaBusSchedule> findBySomansaBusMemberAndIsActiveTrue(SomansaBusMember member);

  List<SomansaBusSchedule> findBySomansaBusMemberSomansaBusMemberIdAndIsActiveTrue(UUID memberId);
}
