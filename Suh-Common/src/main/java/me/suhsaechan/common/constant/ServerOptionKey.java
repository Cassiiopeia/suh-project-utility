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
  CHATBOT_RESPONSE_GENERATOR_MODEL("챗봇 응답 생성 모델", "rnj-1:8b");

  private final String description;
  private final String defaultValue;
}

