package me.suhsaechan.somansabus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.SomansaBusSchedulerEventType;
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
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusReservationService {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final int MAX_ATTEMPTS = 3;
  private static final long RETRY_DELAY_MS = 3_000L;

  private final SomansaBusApiService apiService;
  private final SomansaBusMemberRepository memberRepository;
  private final SomansaBusRouteRepository routeRepository;
  private final SomansaBusScheduleRepository scheduleRepository;
  private final SomansaBusReservationHistoryRepository historyRepository;
  private final SomansaBusSchedulerEventService eventService;

  @Transactional
  public SomansaBusResponse manualReserve(SomansaBusRequest request) {
    log.info("수동 예약 시작 - 멤버: {}, 노선: {}", request.getSomansaBusMemberId(), request.getSomansaBusRouteId());

    SomansaBusMember member = memberRepository.findById(request.getSomansaBusMemberId())
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_MEMBER_NOT_FOUND));

    SomansaBusRoute route = routeRepository.findById(request.getSomansaBusRouteId())
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_ROUTE_NOT_FOUND));

    LocalDate reservationDate = request.getReservationDate() != null
        ? request.getReservationDate()
        : LocalDate.now(SEOUL).plusDays(3);

    boolean success = executeReservation(member, route, reservationDate);

    return SomansaBusResponse.builder()
        .isReservationSuccess(success)
        .build();
  }

  public void scheduledAutoReservation() {
    log.info("자동 예약 스케줄러 실행 시작");

    List<SomansaBusSchedule> activeSchedules = scheduleRepository.findActiveWithDetails();
    log.info("활성 스케줄 수: {}", activeSchedules.size());

    for (SomansaBusSchedule schedule : activeSchedules) {
      SomansaBusMember member = schedule.getSomansaBusMember();
      SomansaBusRoute route = schedule.getSomansaBusRoute();

      LocalDate reservationDate = LocalDate.now(SEOUL).plusDays(1);

      if (!Boolean.TRUE.equals(member.getIsActive()) || !Boolean.TRUE.equals(member.getIsVerified())) {
        log.info("비활성 또는 미인증 멤버 건너뜀: {}", member.getLoginId());
        eventService.record(SomansaBusSchedulerEventType.SKIPPED_MEMBER_INACTIVE, reservationDate,
            member.getLoginId() + " 는 비활성이거나 미인증 상태라 " + route.getDescription()
                + " 예약을 건너뛰었습니다.");
        continue;
      }

      // 스케줄러가 같은 날 여러 번 발화하더라도 외부 예약 API를 중복 호출하지 않는다
      if (historyRepository
          .existsBySomansaBusMemberSomansaBusMemberIdAndSomansaBusRouteSomansaBusRouteIdAndReservationDateAndIsSuccessTrue(
              member.getSomansaBusMemberId(), route.getSomansaBusRouteId(), reservationDate)) {
        log.info("이미 성공한 예약 존재 — 건너뜀 (멤버: {}, 노선: {}, 예약일: {})",
            member.getLoginId(), route.getDescription(), reservationDate);
        eventService.record(SomansaBusSchedulerEventType.SKIPPED_ALREADY_RESERVED, reservationDate,
            member.getLoginId() + " 의 " + route.getDescription()
                + " 예약이 이미 성공 상태라 중복 호출을 막았습니다.");
        continue;
      }

      log.info("자동 예약 실행 - 멤버: {}, 노선: {}, 예약일: {}",
          member.getLoginId(), route.getDescription(), reservationDate);

      executeReservationWithRetry(member, route, reservationDate);
    }

    log.info("자동 예약 스케줄러 실행 완료");
  }

  private boolean executeReservationWithRetry(SomansaBusMember member, SomansaBusRoute route,
      LocalDate reservationDate) {
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      if (executeReservation(member, route, reservationDate)) {
        return true;
      }
      if (attempt < MAX_ATTEMPTS) {
        log.warn("예약 실패 — 재시도 {}/{} (멤버: {}, 노선: {})",
            attempt + 1, MAX_ATTEMPTS, member.getLoginId(), route.getDescription());
        sleepQuietly();
      }
    }
    log.error("예약 최종 실패 - 멤버: {}, 노선: {}, 예약일: {}",
        member.getLoginId(), route.getDescription(), reservationDate);
    return false;
  }

  private void sleepQuietly() {
    try {
      Thread.sleep(RETRY_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private boolean executeReservation(SomansaBusMember member, SomansaBusRoute route, LocalDate reservationDate) {
    log.info("예약 실행 - 멤버: {}, 노선: {}, 예약일: {}",
        member.getLoginId(), route.getDescription(), reservationDate);

    String errorMessage = null;
    boolean success = false;

    try {
      OkHttpClient session = apiService.newSession();
      int passengerId = apiService.login(member.getLoginId(), session);
      if (passengerId <= 0) {
        errorMessage = "로그인 실패";
        log.error("로그인 실패, 예약 중단: {}", member.getLoginId());
      } else {
        boolean sessionCreated = apiService.createSession(member.getLoginId(), passengerId, session);
        if (!sessionCreated) {
          errorMessage = "세션 생성 실패";
          log.error("세션 생성 실패, 예약 중단: {}", member.getLoginId());
        } else {
          success = apiService.makeReservation(passengerId, route, reservationDate, session);
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
        .executedAt(LocalDateTime.now(SEOUL))
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
