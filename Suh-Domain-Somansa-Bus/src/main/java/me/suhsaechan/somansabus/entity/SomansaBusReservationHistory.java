package me.suhsaechan.somansabus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.entity.BasePostgresEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class SomansaBusReservationHistory extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID somansaBusReservationHistoryId;

  @ManyToOne(fetch = FetchType.LAZY)
  private SomansaBusMember somansaBusMember;

  @ManyToOne(fetch = FetchType.LAZY)
  private SomansaBusRoute somansaBusRoute;

  @Column
  private LocalDate reservationDate;

  @Column(nullable = false)
  private Boolean isSuccess;

  @Column(columnDefinition = "TEXT")
  private String errorMessage;

  @Column
  private LocalDateTime executedAt;
}
