package me.suhsaechan.somansabus.job;

import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.somansabus.service.SomansaBusReservationService;
import org.quartz.*;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@DisallowConcurrentExecution
public class SomansaBusAutoReservationJob extends QuartzJobBean {

  @Override
  protected void executeInternal(JobExecutionContext context) {
    ApplicationContext appContext = (ApplicationContext) context.getJobDetail()
        .getJobDataMap().get("applicationContext");

    ServerOptionService serverOptionService = appContext.getBean(ServerOptionService.class);
    SomansaBusReservationService reservationService = appContext.getBean(SomansaBusReservationService.class);
    Scheduler scheduler = context.getScheduler();

    try {
      String enabled = serverOptionService.getOption(ServerOptionKey.SOMANSA_BUS_SCHEDULER_ENABLED).getOptionValue();
      if (!"true".equalsIgnoreCase(enabled)) {
        log.info("소만사 버스 자동 예약 스케줄러 비활성화 상태 — 건너뜀");
        scheduleNext(scheduler, serverOptionService);
        return;
      }

      String daysStr = serverOptionService.getOption(ServerOptionKey.SOMANSA_BUS_SCHEDULER_DAYS).getOptionValue();
      Set<DayOfWeek> allowedDays = parseDays(daysStr);
      DayOfWeek tomorrow = LocalDate.now().plusDays(1).getDayOfWeek();

      if (!allowedDays.contains(tomorrow)) {
        log.info("내일({})은 예약 대상 요일이 아님 — 건너뜀", tomorrow);
        scheduleNext(scheduler, serverOptionService);
        return;
      }

      reservationService.scheduledAutoReservation();
    } catch (Exception e) {
      log.error("자동 예약 Job 실행 중 오류 발생", e);
    } finally {
      try {
        scheduleNext(scheduler, serverOptionService);
      } catch (Exception e) {
        log.error("다음 실행 스케줄 등록 실패", e);
      }
    }
  }

  private void scheduleNext(Scheduler scheduler, ServerOptionService serverOptionService) throws SchedulerException {
    int fromHour = Integer.parseInt(serverOptionService.getOption(ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_FROM).getOptionValue());
    int toHour = Integer.parseInt(serverOptionService.getOption(ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_TO).getOptionValue());

    if (fromHour > toHour) toHour = fromHour;
    int rangeMinutes = (toHour - fromHour) * 60 + 59;
    int randomMinutes = new Random().nextInt(rangeMinutes + 1);

    LocalTime nextTime = LocalTime.of(fromHour, 0).plusMinutes(randomMinutes);
    LocalDate tomorrow = LocalDate.now().plusDays(1);

    Date nextFireTime = Date.from(
        tomorrow.atTime(nextTime).atZone(ZoneId.of("Asia/Seoul")).toInstant()
    );

    TriggerKey triggerKey = TriggerKey.triggerKey("somansaBusAutoReservationTrigger", "somansaBus");
    Trigger newTrigger = TriggerBuilder.newTrigger()
        .withIdentity(triggerKey)
        .startAt(nextFireTime)
        .build();

    if (scheduler.checkExists(triggerKey)) {
      scheduler.rescheduleJob(triggerKey, newTrigger);
    } else {
      JobKey jobKey = JobKey.jobKey("somansaBusAutoReservationJob", "somansaBus");
      scheduler.scheduleJob(scheduler.getJobDetail(jobKey), newTrigger);
    }

    log.info("다음 자동 예약 실행 예약: {}", nextFireTime);
  }

  private Set<DayOfWeek> parseDays(String daysStr) {
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
