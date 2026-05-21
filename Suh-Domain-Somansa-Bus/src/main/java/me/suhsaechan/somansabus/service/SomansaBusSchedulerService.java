package me.suhsaechan.somansabus.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.somansabus.entity.SomansaBusSchedulerState;
import me.suhsaechan.somansabus.repository.SomansaBusSchedulerStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusSchedulerService {

  static final UUID SINGLETON_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  static final int DEFAULT_FROM_HOUR = 22;
  static final int DEFAULT_TO_HOUR = 23;

  private final SomansaBusSchedulerStateRepository repository;
  private final ServerOptionService serverOptionService;
  private final SomansaBusReservationService reservationService;

  public void tick() {
    SomansaBusSchedulerState state;
    try {
      state = loadOrInitState();
    } catch (Exception e) {
      log.error("스케줄러 state 로드/생성 실패", e);
      return;
    }

    String enabled = serverOptionService.getOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_ENABLED);
    if (!"true".equalsIgnoreCase(enabled)) {
      log.info("스케줄러 비활성화 상태 — tick skip");
      return;
    }

    LocalDateTime now = LocalDateTime.now(SEOUL);
    if (now.isBefore(state.getNextFireAt())) {
      log.debug("아직 발화 시각 전 - nextFireAt: {}, now: {}", state.getNextFireAt(), now);
      return;
    }

    LocalDateTime cutoff = state.getNextFireAt().toLocalDate().atTime(23, 59, 59);
    if (now.isAfter(cutoff)) {
      log.warn("발화 시각 컷오프 초과 — skip, nextFireAt 만 갱신 (nextFireAt: {}, now: {})",
          state.getNextFireAt(), now);
      updateNextFireAtOnly(now);
      return;
    }

    LocalDate tomorrow = LocalDate.now(SEOUL).plusDays(1);
    Set<DayOfWeek> allowedDays = parseDays(serverOptionService.getOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS));
    if (!allowedDays.contains(tomorrow.getDayOfWeek())) {
      log.info("내일({}) 비허용 요일 — 예약 skip, nextFireAt 만 갱신", tomorrow.getDayOfWeek());
      updateNextFireAtOnly(now);
      return;
    }

    boolean fired = invokeReservationSafely();
    markStateAfterFire(now, fired);
  }

  private boolean invokeReservationSafely() {
    try {
      reservationService.scheduledAutoReservation();
      log.info("자동 예약 호출 완료");
      return true;
    } catch (Exception e) {
      log.error("자동 예약 호출 중 예외 발생 — nextFireAt 만 갱신", e);
      return false;
    }
  }

  @Transactional
  SomansaBusSchedulerState loadOrInitState() {
    return repository.findById(SINGLETON_ID).orElseGet(() -> {
      LocalDateTime nextFireAt = computeNextFireAt(LocalDateTime.now(SEOUL));
      SomansaBusSchedulerState fresh = SomansaBusSchedulerState.builder()
          .somansaBusSchedulerStateId(SINGLETON_ID)
          .nextFireAt(nextFireAt)
          .lastFiredAt(null)
          .build();
      SomansaBusSchedulerState saved = repository.save(fresh);
      log.info("스케줄러 state row 신규 생성 - nextFireAt: {}", nextFireAt);
      return saved;
    });
  }

  @Transactional
  void updateNextFireAtOnly(LocalDateTime now) {
    SomansaBusSchedulerState state = repository.findById(SINGLETON_ID).orElseThrow();
    state.setNextFireAt(computeNextFireAt(now));
    repository.save(state);
  }

  @Transactional
  void markStateAfterFire(LocalDateTime now, boolean fired) {
    SomansaBusSchedulerState state = repository.findById(SINGLETON_ID).orElseThrow();
    if (fired) {
      state.setLastFiredAt(now);
    }
    state.setNextFireAt(computeNextFireAt(now));
    repository.save(state);
    log.info("state 갱신 완료 - fired: {}, nextFireAt: {}", fired, state.getNextFireAt());
  }

  LocalDateTime computeNextFireAt(LocalDateTime referenceTime) {
    int fromHour = readHour(ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_FROM, DEFAULT_FROM_HOUR);
    int toHour = readHour(ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_TO, DEFAULT_TO_HOUR);
    if (fromHour > toHour) toHour = fromHour;
    int rangeMinutes = (toHour - fromHour) * 60 + 59;

    Set<DayOfWeek> allowedDays = parseDays(
        serverOptionService.getOptionValue(ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS));

    LocalDate candidateDate = referenceTime.toLocalDate();
    LocalDateTime windowEnd = candidateDate.atTime(toHour, 59);
    if (referenceTime.isAfter(windowEnd)) {
      candidateDate = candidateDate.plusDays(1);
    }

    int guard = 8;
    while (!allowedDays.contains(candidateDate.getDayOfWeek()) && guard-- > 0) {
      candidateDate = candidateDate.plusDays(1);
    }
    if (guard <= 0) {
      log.error("허용 요일 설정이 비어있거나 잘못됨 — fallback: 다음날 사용");
      candidateDate = referenceTime.toLocalDate().plusDays(1);
    }

    int randomMinutes = ThreadLocalRandom.current().nextInt(rangeMinutes + 1);
    LocalTime nextTime = LocalTime.of(fromHour, 0).plusMinutes(randomMinutes);
    return candidateDate.atTime(nextTime);
  }

  private int readHour(ServerOptionKey key, int fallback) {
    try {
      return serverOptionService.getOptionValueAsInt(key);
    } catch (Exception e) {
      log.warn("ServerOption {} 읽기 실패, 기본값 {} 사용", key, fallback, e);
      return fallback;
    }
  }

  Set<DayOfWeek> parseDays(String daysStr) {
    Set<DayOfWeek> days = new HashSet<>();
    if (daysStr == null || daysStr.isBlank()) return days;
    for (String d : daysStr.split(",")) {
      try {
        days.add(DayOfWeek.valueOf(d.trim().toUpperCase()));
      } catch (IllegalArgumentException e) {
        log.warn("알 수 없는 요일 설정값 무시: {}", d.trim());
      }
    }
    return days;
  }
}
