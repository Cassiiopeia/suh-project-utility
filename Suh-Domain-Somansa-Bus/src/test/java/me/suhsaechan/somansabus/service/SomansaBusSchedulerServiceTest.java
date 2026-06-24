package me.suhsaechan.somansabus.service;

import static me.suhsaechan.suhlogger.util.SuhLogger.lineLog;
import static me.suhsaechan.suhlogger.util.SuhLogger.timeLog;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.somansabus.entity.SomansaBusSchedulerState;
import me.suhsaechan.somansabus.repository.SomansaBusSchedulerStateRepository;
import me.suhsaechan.web.SuhProjectUtilityApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SuhProjectUtilityApplication.class)
@ActiveProfiles("dev")
@Slf4j
class SomansaBusSchedulerServiceTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  @Autowired
  private SomansaBusSchedulerService schedulerService;

  @Autowired
  private SomansaBusSchedulerStateRepository stateRepository;

  @Autowired
  private ServerOptionService serverOptionService;

  @Test
  public void mainTest() {
    lineLog("테스트시작");

    lineLog(null);
    timeLog(this::tick_초기_row_생성_테스트);
    lineLog(null);
    timeLog(this::tick_미래시각_무동작_테스트);
    lineLog(null);
    timeLog(this::tick_컷오프초과_skip_테스트);
    lineLog(null);
    timeLog(this::tick_비허용요일_skip_테스트);
    lineLog(null);
    timeLog(this::computeNextFireAt_허용요일_전진_테스트);

    lineLog("테스트종료");
  }

  public void tick_초기_row_생성_테스트() {
    lineLog("초기 row 생성 테스트 실행중");

    serverOptionService.setOptionValue(ServerOptionKey.SOMANSA_BUS_SCHEDULER_ENABLED, "true");
    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
    serverOptionService.setOptionValue(ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_FROM, "22");
    serverOptionService.setOptionValue(ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_TO, "23");

    schedulerService.tick();

    SomansaBusSchedulerState state = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    assertThat(state.getNextFireAt()).isAfter(LocalDateTime.now(SEOUL));
    log.info("생성된 state - nextFireAt: {}", state.getNextFireAt());
  }

  public void tick_미래시각_무동작_테스트() {
    lineLog("미래 시각 무동작 테스트 실행중");

    LocalDateTime futureFireAt = LocalDateTime.now(SEOUL).plusHours(1);
    SomansaBusSchedulerState state = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    state.setNextFireAt(futureFireAt);
    state.setLastFiredAt(null);
    stateRepository.saveAndFlush(state);

    schedulerService.tick();

    SomansaBusSchedulerState after = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    assertThat(after.getNextFireAt()).isEqualTo(futureFireAt);
    assertThat(after.getLastFiredAt()).isNull();
  }

  public void tick_컷오프초과_skip_테스트() {
    lineLog("컷오프 초과 skip 테스트 실행중");

    LocalDateTime yesterdayFireAt = LocalDateTime.now(SEOUL).minusDays(1)
        .withHour(22).withMinute(0).withSecond(0).withNano(0);
    SomansaBusSchedulerState state = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    state.setNextFireAt(yesterdayFireAt);
    state.setLastFiredAt(null);
    stateRepository.saveAndFlush(state);

    schedulerService.tick();

    SomansaBusSchedulerState after = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    assertThat(after.getLastFiredAt()).isNull();
    assertThat(after.getNextFireAt()).isAfter(LocalDateTime.now(SEOUL));
  }

  public void tick_비허용요일_skip_테스트() {
    lineLog("비허용 요일 skip 테스트 실행중");

    DayOfWeek tomorrow = LocalDateTime.now(SEOUL).toLocalDate().plusDays(1).getDayOfWeek();
    DayOfWeek disallowed = oppositeDay(tomorrow);
    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS, disallowed.name());

    LocalDateTime pastFireAt = LocalDateTime.now(SEOUL).minusMinutes(1);
    SomansaBusSchedulerState state = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    state.setNextFireAt(pastFireAt);
    state.setLastFiredAt(null);
    stateRepository.saveAndFlush(state);

    schedulerService.tick();

    SomansaBusSchedulerState after = stateRepository
        .findById(SomansaBusSchedulerService.SINGLETON_ID)
        .orElseThrow();
    assertThat(after.getLastFiredAt()).isNull();
    assertThat(after.getNextFireAt()).isAfter(LocalDateTime.now(SEOUL));

    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
  }

  public void computeNextFireAt_허용요일_전진_테스트() {
    lineLog("허용 요일 전진 계산 테스트 실행중");

    // DAYS 는 예약 대상일 요일이다. MONDAY 좌석을 예약하려면 일요일 밤에 발화해야 한다.
    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS, "MONDAY");
    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_FROM, "22");
    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_TO, "23");

    LocalDateTime reference = LocalDateTime.now(SEOUL);
    LocalDateTime next = schedulerService.computeNextFireAt(reference);

    assertThat(next.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    assertThat(next.plusDays(1).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(next.getHour()).isBetween(22, 23);
    log.info("계산된 nextFireAt: {} ({})", next, next.getDayOfWeek());

    serverOptionService.setOptionValue(
        ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
  }

  private DayOfWeek oppositeDay(DayOfWeek day) {
    return day == DayOfWeek.MONDAY ? DayOfWeek.TUESDAY : DayOfWeek.MONDAY;
  }
}
