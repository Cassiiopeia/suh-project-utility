package me.suhsaechan.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서버 설정 키 Enum
 * 각 설정 항목의 키, 설명, 기본값을 정의
 */
@Getter
@RequiredArgsConstructor
public enum ServerOptionKey {
  CHATBOT_CHUNK_SIZE("챗봇 청크 크기 (토큰 수)", "500"),
  CHATBOT_CHUNK_OVERLAP("챗봇 청크 중첩 크기 (토큰 수)", "100"),
  CHATBOT_INTENT_CLASSIFIER_MODEL("챗봇 의도 분류 모델", "gemma3:1b"),
  CHATBOT_RESPONSE_GENERATOR_MODEL("챗봇 응답 생성 모델", "rnj-1:8b"),
  SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID("소만사 버스 노선 동기화 트리거 회원 loginId", "chan4760@somansa.com"),
  SOMANSA_BUS_SCHEDULER_ENABLED("소만사 버스 자동 예약 스케줄러 활성화 여부 (true/false)", "true"),
  SOMANSA_BUS_SCHEDULER_TIME_FROM("자동 예약 실행 시작 시간 (0~23)", "22"),
  SOMANSA_BUS_SCHEDULER_TIME_TO("자동 예약 실행 종료 시간 (0~23)", "23"),
  SOMANSA_BUS_SCHEDULER_DAYS("자동 예약 실행 요일 (쉼표 구분, MON/TUE/WED/THU/FRI/SAT/SUN)", "MON,TUE,WED,THU,FRI");

  private final String description;
  private final String defaultValue;
}

