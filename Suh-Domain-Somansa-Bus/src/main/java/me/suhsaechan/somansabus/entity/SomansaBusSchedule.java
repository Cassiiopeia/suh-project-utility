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

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class SomansaBusSchedule extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID somansaBusScheduleId;

  @ManyToOne(fetch = FetchType.LAZY)
  private SomansaBusUser somansaBusUser;

  @ManyToOne(fetch = FetchType.LAZY)
  private SomansaBusRoute somansaBusRoute;

  @Column(nullable = false)
  private Boolean isActive;

  @Column
  private Integer daysAhead;
}
