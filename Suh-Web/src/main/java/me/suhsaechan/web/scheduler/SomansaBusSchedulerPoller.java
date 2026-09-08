package me.suhsaechan.web.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.somansabus.service.SomansaBusSchedulerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SomansaBusSchedulerPoller {

  // 발화 창(1시간)을 촘촘히 훑어 랜덤 발화 시각을 놓치지 않도록 2분 간격 폴링
  private static final long FIXED_DELAY_MS = 120_000L;

  private final SomansaBusSchedulerService schedulerService;

  @Scheduled(fixedDelay = FIXED_DELAY_MS)
  public void poll() {
    try {
      schedulerService.tick();
    } catch (Exception e) {
      log.error("스케줄러 폴링 중 예외 발생 — 다음 폴링까지 대기", e);
    }
  }
}
