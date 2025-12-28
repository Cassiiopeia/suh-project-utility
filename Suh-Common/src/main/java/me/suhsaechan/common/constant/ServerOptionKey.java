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
  CHATBOT_CHUNK_SIZE("챗봇 청크 크기 (토큰 수)", "3000"),
  CHATBOT_CHUNK_OVERLAP("챗봇 청크 중첩 크기 (토큰 수)", "300");

  private final String description;
  private final String defaultValue;
}

