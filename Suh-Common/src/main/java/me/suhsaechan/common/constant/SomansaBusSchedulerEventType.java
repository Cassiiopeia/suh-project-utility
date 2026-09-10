package me.suhsaechan.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 자동 예약 스케줄러가 발화 시각에 도달한 뒤 어떤 판단을 했는지 남기는 이벤트 종류.
// 예약 시도까지 가지 못한 경우도 기록해 "왜 오늘 예약이 없는지"를 화면에서 알 수 있게 한다.
@Getter
@RequiredArgsConstructor
public enum SomansaBusSchedulerEventType {

  FIRED("발화", false),
  SKIPPED_CUTOFF("발화 누락", true),
  SKIPPED_NOT_ALLOWED_DAY("비허용 요일", false),
  SKIPPED_MEMBER_INACTIVE("멤버 건너뜀", false),
  SKIPPED_ALREADY_RESERVED("중복 방지", false),
  FIRE_ERROR("실행 오류", true),
  STATE_ERROR("상태 오류", true);

  private final String description;
  // 사용자가 조치를 검토해야 하는 이벤트. 화면에서 경고로 강조한다
  private final boolean isProblem;
}
