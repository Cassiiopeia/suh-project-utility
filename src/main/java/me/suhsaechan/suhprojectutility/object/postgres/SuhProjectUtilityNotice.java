package me.suhsaechan.suhprojectutility.object.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 공지사항 Entity
 * 대시보드에 표시되는 공지사항 정보를 관리합니다.
 */
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class SuhProjectUtilityNotice extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID noticeId;

  // 공지사항 제목
  @Column(nullable = false)
  private String title;

  // 공지사항 내용
  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  // 중요 공지 여부
  @Column
  private Boolean isImportant;

  // 게시 시작일
  @Column
  private LocalDateTime startDate;

  // 게시 종료일 (null인 경우 무기한)
  @Column
  private LocalDateTime endDate;

  // 활성화 여부
  @Column(nullable = false)
  private Boolean isActive;

  // 작성자
  @Column
  private String author;

  // 조회수
  @Column
  private Long viewCount;

  // 조회수 증가 메소드
  public void incrementViewCount() {
    if (this.viewCount == null) {
      this.viewCount = 1L;
    } else {
      this.viewCount++;
    }
  }
} 