package me.suhsaechan.somansabus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.somansabus.dto.SomansaBusRequest;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import me.suhsaechan.somansabus.entity.SomansaBusSchedule;
import me.suhsaechan.somansabus.entity.SomansaBusUser;
import me.suhsaechan.somansabus.repository.SomansaBusRouteRepository;
import me.suhsaechan.somansabus.repository.SomansaBusScheduleRepository;
import me.suhsaechan.somansabus.repository.SomansaBusUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusScheduleService {

  private final SomansaBusScheduleRepository scheduleRepository;
  private final SomansaBusUserRepository userRepository;
  private final SomansaBusRouteRepository routeRepository;

  @Transactional
  public SomansaBusResponse createSchedule(SomansaBusRequest request) {
    log.info("예약 스케줄 생성 - 사용자: {}, 노선: {}",
        request.getSomansaBusUserId(), request.getSomansaBusRouteId());

    SomansaBusUser user = userRepository.findById(request.getSomansaBusUserId())
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_USER_NOT_FOUND));

    SomansaBusRoute route = routeRepository.findById(request.getSomansaBusRouteId())
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_ROUTE_NOT_FOUND));

    Integer daysAhead = request.getDaysAhead() != null ? request.getDaysAhead() : 3;

    SomansaBusSchedule schedule = SomansaBusSchedule.builder()
        .somansaBusUser(user)
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
  public SomansaBusResponse getSchedulesByUser(UUID userId) {
    log.info("사용자별 스케줄 조회: {}", userId);
    List<SomansaBusSchedule> schedules = scheduleRepository.findBySomansaBusUserSomansaBusUserId(userId);
    return SomansaBusResponse.builder()
        .schedules(schedules)
        .totalCount((long) schedules.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getActiveSchedulesByUser(UUID userId) {
    log.info("사용자별 활성 스케줄 조회: {}", userId);
    List<SomansaBusSchedule> schedules = scheduleRepository.findBySomansaBusUserSomansaBusUserIdAndIsActiveTrue(userId);
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
}
