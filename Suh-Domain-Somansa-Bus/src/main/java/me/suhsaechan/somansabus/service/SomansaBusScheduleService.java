package me.suhsaechan.somansabus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.somansabus.dto.SomansaBusRequest;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.entity.SomansaBusMember;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import me.suhsaechan.somansabus.entity.SomansaBusSchedule;
import me.suhsaechan.somansabus.repository.SomansaBusMemberRepository;
import me.suhsaechan.somansabus.repository.SomansaBusReservationHistoryRepository;
import me.suhsaechan.somansabus.repository.SomansaBusRouteRepository;
import me.suhsaechan.somansabus.repository.SomansaBusScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusScheduleService {

  private final SomansaBusScheduleRepository scheduleRepository;
  private final SomansaBusMemberRepository memberRepository;
  private final SomansaBusRouteRepository routeRepository;
  private final SomansaBusReservationHistoryRepository historyRepository;

  @Transactional
  public SomansaBusResponse createSchedule(SomansaBusRequest request) {
    log.info("예약 스케줄 생성 - 멤버: {}, 노선: {}",
        request.getSomansaBusMemberId(), request.getSomansaBusRouteId());

    SomansaBusMember member = memberRepository.findById(request.getSomansaBusMemberId())
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_MEMBER_NOT_FOUND));

    SomansaBusRoute route = routeRepository.findById(request.getSomansaBusRouteId())
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_ROUTE_NOT_FOUND));

    Integer daysAhead = request.getDaysAhead() != null ? request.getDaysAhead() : 3;

    SomansaBusSchedule schedule = SomansaBusSchedule.builder()
        .somansaBusMember(member)
        .somansaBusRoute(route)
        .isActive(true)
        .daysAhead(daysAhead)
        .build();

    SomansaBusSchedule savedSchedule = scheduleRepository.save(schedule);
    log.info("예약 스케줄 생성 완료: {}", savedSchedule.getSomansaBusScheduleId());

    return SomansaBusResponse.builder()
        .schedule(savedSchedule)
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getSchedulesByMember(UUID memberId) {
    log.info("멤버별 스케줄 조회: {}", memberId);
    List<SomansaBusSchedule> schedules = scheduleRepository.findByMemberIdWithDetails(memberId);
    return SomansaBusResponse.builder()
        .schedules(schedules)
        .totalCount((long) schedules.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getActiveSchedulesByMember(UUID memberId) {
    log.info("멤버별 활성 스케줄 조회: {}", memberId);
    List<SomansaBusSchedule> schedules = scheduleRepository.findBySomansaBusMemberSomansaBusMemberIdAndIsActiveTrue(memberId);
    return SomansaBusResponse.builder()
        .schedules(schedules)
        .totalCount((long) schedules.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getAllActiveSchedules() {
    log.info("전체 활성 스케줄 조회");
    List<SomansaBusSchedule> schedules = scheduleRepository.findByIsActiveTrue();
    return SomansaBusResponse.builder()
        .schedules(schedules)
        .totalCount((long) schedules.size())
        .build();
  }

  @Transactional
  public SomansaBusResponse toggleScheduleActive(UUID scheduleId) {
    log.info("스케줄 활성화 상태 토글: {}", scheduleId);
    SomansaBusSchedule schedule = scheduleRepository.findById(scheduleId)
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_SCHEDULE_NOT_FOUND));

    schedule.setIsActive(!schedule.getIsActive());
    SomansaBusSchedule savedSchedule = scheduleRepository.save(schedule);

    log.info("스케줄 활성화 상태 변경 완료: {} -> {}", scheduleId, savedSchedule.getIsActive());
    return SomansaBusResponse.builder()
        .schedule(savedSchedule)
        .build();
  }

  @Transactional
  public SomansaBusResponse deleteSchedule(UUID scheduleId) {
    log.info("스케줄 삭제: {}", scheduleId);
    SomansaBusSchedule schedule = scheduleRepository.findById(scheduleId)
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_SCHEDULE_NOT_FOUND));

    scheduleRepository.delete(schedule);
    log.info("스케줄 삭제 완료: {}", scheduleId);

    return SomansaBusResponse.builder()
        .schedule(schedule)
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getStats() {
    log.info("버스 예약 시스템 통계 조회");

    Integer totalMembers = (int) memberRepository.count();
    Integer activeMembers = memberRepository.findByIsActiveTrueAndIsVerifiedTrue().size();
    Integer totalSchedules = (int) scheduleRepository.count();
    Integer activeSchedules = scheduleRepository.findByIsActiveTrue().size();

    LocalDate today = LocalDate.now();
    LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

    Integer thisWeekReservations = historyRepository.countByReservationDateBetween(weekStart, weekEnd);
    Integer thisWeekSuccess = historyRepository.countByReservationDateBetweenAndIsSuccessTrue(weekStart, weekEnd);
    Integer thisWeekFailed = historyRepository.countByReservationDateBetweenAndIsSuccessFalse(weekStart, weekEnd);

    return SomansaBusResponse.builder()
        .totalMembers(totalMembers)
        .activeMembers(activeMembers)
        .totalSchedules(totalSchedules)
        .activeSchedules(activeSchedules)
        .thisWeekReservations(thisWeekReservations != null ? thisWeekReservations : 0)
        .thisWeekSuccessReservations(thisWeekSuccess != null ? thisWeekSuccess : 0)
        .thisWeekFailedReservations(thisWeekFailed != null ? thisWeekFailed : 0)
        .build();
  }
}
