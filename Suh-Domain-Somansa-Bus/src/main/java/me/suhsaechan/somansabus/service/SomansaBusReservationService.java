package me.suhsaechan.somansabus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.somansabus.dto.SomansaBusRequest;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.entity.SomansaBusReservationHistory;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import me.suhsaechan.somansabus.entity.SomansaBusSchedule;
import me.suhsaechan.somansabus.entity.SomansaBusUser;
import me.suhsaechan.somansabus.repository.SomansaBusReservationHistoryRepository;
import me.suhsaechan.somansabus.repository.SomansaBusRouteRepository;
import me.suhsaechan.somansabus.repository.SomansaBusScheduleRepository;
import me.suhsaechan.somansabus.repository.SomansaBusUserRepository;
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
  private final SomansaBusUserRepository userRepository;
  private final SomansaBusRouteRepository routeRepository;
  private final SomansaBusScheduleRepository scheduleRepository;
  private final SomansaBusReservationHistoryRepository historyRepository;

  @Transactional
  public SomansaBusResponse manualReserve(SomansaBusRequest request) {
    log.info("수동 예약 시작 - 사용자: {}, 노선: {}", request.getSomansaBusUserId(), request.getSomansaBusRouteId());

    SomansaBusUser user = userRepository.findById(request.getSomansaBusUserId())
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_USER_NOT_FOUND));

    SomansaBusRoute route = routeRepository.findById(request.getSomansaBusRouteId())
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_ROUTE_NOT_FOUND));

    LocalDate reservationDate = request.getReservationDate() != null
        ? request.getReservationDate()
        : LocalDate.now().plusDays(3);

    boolean success = executeReservation(user, route, reservationDate);

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
      SomansaBusUser user = schedule.getSomansaBusUser();
      SomansaBusRoute route = schedule.getSomansaBusRoute();

      if (!Boolean.TRUE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getIsVerified())) {
        log.info("비활성 또는 미인증 사용자 건너뜀: {}", user.getLoginId());
        continue;
      }

      int daysAhead = schedule.getDaysAhead() != null ? schedule.getDaysAhead() : 3;
      LocalDate reservationDate = LocalDate.now().plusDays(daysAhead);

      log.info("자동 예약 실행 - 사용자: {}, 노선: {}, 예약일: {}",
          user.getLoginId(), route.getDescription(), reservationDate);

      executeReservation(user, route, reservationDate);
    }

    log.info("자동 예약 스케줄러 실행 완료");
  }

  private boolean executeReservation(SomansaBusUser user, SomansaBusRoute route, LocalDate reservationDate) {
    log.info("예약 실행 - 사용자: {}, 노선: {}, 예약일: {}",
        user.getLoginId(), route.getDescription(), reservationDate);

    String errorMessage = null;
    boolean success = false;

    try {
      int passengerId = apiService.login(user.getLoginId());
      if (passengerId <= 0) {
        errorMessage = "로그인 실패";
        log.error("로그인 실패, 예약 중단: {}", user.getLoginId());
      } else {
        boolean sessionCreated = apiService.createSession(user.getLoginId(), passengerId);
        if (!sessionCreated) {
          errorMessage = "세션 생성 실패";
          log.error("세션 생성 실패, 예약 중단: {}", user.getLoginId());
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
        .somansaBusUser(user)
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
  public SomansaBusResponse getHistoryByUser(UUID userId) {
    log.info("사용자별 예약 이력 조회: {}", userId);
    List<SomansaBusReservationHistory> histories =
        historyRepository.findBySomansaBusUserSomansaBusUserIdOrderByExecutedAtDesc(userId);
    return SomansaBusResponse.builder()
        .histories(histories)
        .totalCount((long) histories.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getRecentHistoryByUser(UUID userId) {
    log.info("사용자별 최근 예약 이력 조회: {}", userId);
    List<SomansaBusReservationHistory> histories =
        historyRepository.findTop10BySomansaBusUserSomansaBusUserIdOrderByExecutedAtDesc(userId);
    return SomansaBusResponse.builder()
        .histories(histories)
        .totalCount((long) histories.size())
        .build();
  }
}
