package me.suhsaechan.somansabus.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.SomansaBusSchedulerEventType;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.entity.SomansaBusSchedulerEvent;
import me.suhsaechan.somansabus.repository.SomansaBusSchedulerEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusSchedulerEventService {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  private final SomansaBusSchedulerEventRepository eventRepository;

  // 이벤트 기록 실패가 예약 자체를 막으면 안 되므로 예외를 삼킨다
  @Transactional
  public void record(SomansaBusSchedulerEventType eventType, LocalDate targetDate, String message) {
    try {
      eventRepository.save(SomansaBusSchedulerEvent.builder()
          .eventType(eventType)
          .targetDate(targetDate)
          .message(message)
          .occurredAt(LocalDateTime.now(SEOUL))
          .build());
      log.info("스케줄러 이벤트 기록 - 유형: {}, 대상일: {}, 내용: {}", eventType, targetDate, message);
    } catch (Exception e) {
      log.error("스케줄러 이벤트 기록 실패 - 유형: {}", eventType, e);
    }
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getRecentEvents() {
    List<SomansaBusSchedulerEvent> events = eventRepository.findTop30ByOrderByOccurredAtDesc();
    return SomansaBusResponse.builder()
        .schedulerEvents(events)
        .totalCount((long) events.size())
        .build();
  }
}
