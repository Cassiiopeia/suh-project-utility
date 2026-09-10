package me.suhsaechan.somansabus.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.constant.SomansaBusSchedulerEventType;
import me.suhsaechan.common.entity.BasePostgresEntity;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class SomansaBusSchedulerEvent extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID somansaBusSchedulerEventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private SomansaBusSchedulerEventType eventType;

  // 예약하려던 날짜. 스케줄러 상태 오류처럼 대상일을 특정할 수 없으면 비어 있다
  @Column
  private LocalDate targetDate;

  @Column(columnDefinition = "TEXT")
  private String message;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Column(nullable = false)
  private LocalDateTime occurredAt;
}
