package me.suhsaechan.somansabus.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.entity.BasePostgresEntity;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class SomansaBusSchedulerState extends BasePostgresEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID somansaBusSchedulerStateId;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Column(nullable = false)
  private LocalDateTime nextFireAt;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Column
  private LocalDateTime lastFiredAt;
}
