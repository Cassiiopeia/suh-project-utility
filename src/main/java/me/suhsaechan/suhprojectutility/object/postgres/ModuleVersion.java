package me.suhsaechan.suhprojectutility.object.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.suhprojectutility.object.constant.ModuleType;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ModuleVersion extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID moduleVersionId;

  @Enumerated(EnumType.STRING)
  private ModuleType moduleType; // 모듈 이름

  private String versionNumber; // 버전 번호 (1.1.0)

  private LocalDate releaseDate; // 출시일

  @Column
  private Boolean isLatest; // 최신 버전 여부

  @Transient
  private List<ModuleVersionUpdate> updates = new ArrayList<>();
}

