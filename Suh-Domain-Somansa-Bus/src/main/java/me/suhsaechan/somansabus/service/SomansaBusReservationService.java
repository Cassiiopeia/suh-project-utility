package me.suhsaechan.somansabus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.somansabus.dto.SomansaBusRequest;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.entity.SomansaBusMember;
import me.suhsaechan.somansabus.entity.SomansaBusReservationHistory;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import me.suhsaechan.somansabus.entity.SomansaBusSchedule;
import me.suhsaechan.somansabus.repository.SomansaBusMemberRepository;
import me.suhsaechan.somansabus.repository.SomansaBusReservationHistoryRepository;
import me.suhsaechan.somansabus.repository.SomansaBusRouteRepository;
import me.suhsaechan.somansabus.repository.SomansaBusScheduleRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusReservationService {

  private final SomansaBusApiService apiService;
  private final SomansaBusMemberRepository memberRepository;
  private final SomansaBusRouteRepository routeRepository;
  private final SomansaBusScheduleRepository scheduleRepository;
  private final SomansaBusReservationHistoryRepository historyRepository;

  @Transactional
  public SomansaBusResponse manualReserve(SomansaBusRequest request) {
    log.info("수동 예약 시작 - 멤버: {}, 노선: {}", request.getSomansaBusMemberId(), request.getSomansaBusRouteId());

    SomansaBusMember member = memberRepository.findById(request.getSomansaBusMemberId())
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_MEMBER_NOT_FOUND));

    SomansaBusRoute route = routeRepository.findById(request.getSomansaBusRouteId())
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_ROUTE_NOT_FOUND));

    LocalDate reservationDate = request.getReservationDate() != null
        ? request.getReservationDate()
        : LocalDate.now().plusDays(3);

    boolean success = executeReservation(member, route, reservationDate);

    return SomansaBusResponse.builder()
        .isReservationSuccess(success)
        .build();
  }

  @Scheduled(cron = "0 13 0 * * ?")
  @Transactional
  public void scheduledAutoReservation() {
    log.info("자동 예약 스케줄러 실행 시작");

    List<SomansaBusSchedule> activeSchedules = scheduleRepository.findByIsActiveTrue();
    log.info("활성 스케줄 수: {}", activeSchedules.size());

    for (SomansaBusSchedule schedule : activeSchedules) {
      SomansaBusMember member = schedule.getSomansaBusMember();
      SomansaBusRoute route = schedule.getSomansaBusRoute();

      if (!Boolean.TRUE.equals(member.getIsActive()) || !Boolean.TRUE.equals(member.getIsVerified())) {
        log.info("비활성 또는 미인증 멤버 건너뜀: {}", member.getLoginId());
        continue;
      }

      int daysAhead = schedule.getDaysAhead() != null ? schedule.getDaysAhead() : 3;
      LocalDate reservationDate = LocalDate.now().plusDays(daysAhead);

      log.info("자동 예약 실행 - 멤버: {}, 노선: {}, 예약일: {}",
          member.getLoginId(), route.getDescription(), reservationDate);

      executeReservation(member, route, reservationDate);
    }

    log.info("자동 예약 스케줄러 실행 완료");
  }

  private boolean executeReservation(SomansaBusMember member, SomansaBusRoute route, LocalDate reservationDate) {
    log.info("예약 실행 - 멤버: {}, 노선: {}, 예약일: {}",
        member.getLoginId(), route.getDescription(), reservationDate);

    String errorMessage = null;
    boolean success = false;

    try {
      int passengerId = apiService.login(member.getLoginId());
      if (passengerId <= 0) {
        errorMessage = "로그인 실패";
        log.error("로그인 실패, 예약 중단: {}", member.getLoginId());
      } else {
        boolean sessionCreated = apiService.createSession(member.getLoginId(), passengerId);
        if (!sessionCreated) {
          errorMessage = "세션 생성 실패";
          log.error("세션 생성 실패, 예약 중단: {}", member.getLoginId());
        } else {
          success = apiService.makeReservation(passengerId, route, reservationDate);
          if (!success) {
            errorMessage = "예약 API 호출 실패";
          }
        }
      }
    } catch (Exception e) {
      errorMessage = e.getMessage();
      log.error("예약 중 예외 발생: {}", e.getMessage(), e);
    }

    SomansaBusReservationHistory history = SomansaBusReservationHistory.builder()
        .somansaBusMember(member)
        .somansaBusRoute(route)
        .reservationDate(reservationDate)
        .isSuccess(success)
        .errorMessage(errorMessage)
        .executedAt(LocalDateTime.now())
        .build();

    historyRepository.save(history);
    log.info("예약 이력 저장 완료 - 성공여부: {}", success);

    return success;
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getHistoryByMember(UUID memberId) {
    log.info("멤버별 예약 이력 조회: {}", memberId);
    List<SomansaBusReservationHistory> histories =
        historyRepository.findByMemberIdWithDetails(memberId);
    return SomansaBusResponse.builder()
        .histories(histories)
        .totalCount((long) histories.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getRecentHistoryByMember(UUID memberId) {
    log.info("멤버별 최근 예약 이력 조회: {}", memberId);
    List<SomansaBusReservationHistory> histories =
        historyRepository.findTop10ByMemberIdWithDetails(memberId);
    return SomansaBusResponse.builder()
        .histories(histories)
        .totalCount((long) histories.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getRecentHistory() {
    log.info("전체 최근 예약 이력 조회");
    List<SomansaBusReservationHistory> histories =
        historyRepository.findTop20ByOrderByExecutedAtDesc();
    return SomansaBusResponse.builder()
        .histories(histories)
        .totalCount((long) histories.size())
        .build();
  }
}
