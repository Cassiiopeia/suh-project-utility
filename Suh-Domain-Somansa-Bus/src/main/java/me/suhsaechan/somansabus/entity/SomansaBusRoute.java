package me.suhsaechan.somansabus.entity;

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
import me.suhsaechan.common.entity.BasePostgresEntity;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class SomansaBusRoute extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID somansaBusRouteId;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false)
  private Integer disptid;

  @Column(nullable = false)
  private String caralias;

  @Column
  private String departureTime;

  @Column
  private String station;

  @Column
  private Integer busNumber;

  @Column(nullable = false)
  private Boolean isActive;
}
