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
  CHATBOT_RAG_MIN_SCORE("RAG 벡터 검색 최소 유사도 점수 (0.0~1.0)", "0.5"),
  CHATBOT_RAG_TOP_K("RAG 벡터 검색 시 반환할 최대 문서 수", "3"),
  CHATBOT_EMBEDDING_MODEL("챗봇 임베딩 모델 (변경 시 전체 문서 재처리 필요, Qdrant 벡터 차원과 일치해야 함)", "embeddinggemma:latest"),
  CHATBOT_MAX_MESSAGE_LENGTH("챗봇 메시지 최대 길이 (글자 수, 비로그인 공개 API 보호)", "2000"),
  TRANSLATE_MODEL("번역 모델", "gemma3:1b"),
  TRANSLATE_MAX_TEXT_LENGTH("번역 입력 최대 길이 (글자 수, 비로그인 공개 API 보호)", "5000"),
  SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID("소만사 버스 노선 동기화 트리거 회원 loginId", "chan4760@somansa.com"),
  SOMANSA_BUS_SCHEDULER_ENABLED("소만사 버스 자동 예약 스케줄러 활성화 여부 (true/false)", "true"),
  SOMANSA_BUS_SCHEDULER_TIME_FROM("자동 예약 실행 시작 시간 (0~23)", "22"),
  SOMANSA_BUS_SCHEDULER_TIME_TO("자동 예약 실행 종료 시간 (0~23)", "23"),
  SOMANSA_BUS_SCHEDULER_DAYS("자동 예약 대상일 요일 (쉼표 구분, MONDAY/TUESDAY/WEDNESDAY/THURSDAY/FRIDAY/SATURDAY/SUNDAY). 예: MONDAY 지정 시 일요일 밤에 발화해 월요일 좌석을 예약", "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY"),
  STATISTICS_INTERNAL_REFERRER_HOSTS("통계 유입 경로에서 제외할 자기 사이트 호스트 (쉼표 구분, 서브도메인 포함)", "lab.suhsaechan.kr,suh-project.synology.me,localhost");

  private final String description;
  private final String defaultValue;
}

