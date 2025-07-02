package me.suhsaechan.suhprojectutility.object.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.suhsaechan.suhprojectutility.object.postgres.SuhProjectUtilityNotice;

/**
 * 공지사항 응답 객체
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class NoticeResponse {
  
  // 공지사항 목록
  private List<SuhProjectUtilityNotice> notices;
  
  // 단일 공지사항
  private SuhProjectUtilityNotice notice;
  
  // 처리 결과 메시지
  private String message;
  
  // 처리 결과 성공 여부
  private Boolean success;
  
  // 총 공지사항 수
  private Long totalCount;
  
  /**
   * 성공 응답 생성 (공지사항 목록)
   */
  public static NoticeResponse ofList(List<SuhProjectUtilityNotice> notices, Long totalCount) {
    return NoticeResponse.builder()
        .notices(notices)
        .totalCount(totalCount)
        .success(true)
        .message("공지사항 목록 조회 성공")
        .build();
  }
  
  /**
   * 성공 응답 생성 (단일 공지사항)
   */
  public static NoticeResponse ofSingle(SuhProjectUtilityNotice notice) {
    return NoticeResponse.builder()
        .notice(notice)
        .success(true)
        .message("공지사항 조회 성공")
        .build();
  }
  
  /**
   * 성공 응답 생성 (처리 결과)
   */
  public static NoticeResponse ofSuccess(String message) {
    return NoticeResponse.builder()
        .success(true)
        .message(message)
        .build();
  }
  
  /**
   * 실패 응답 생성
   */
  public static NoticeResponse ofError(String message) {
    return NoticeResponse.builder()
        .success(false)
        .message(message)
        .build();
  }
} 