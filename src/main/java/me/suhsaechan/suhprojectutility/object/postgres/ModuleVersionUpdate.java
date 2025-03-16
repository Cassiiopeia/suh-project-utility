package me.suhsaechan.suhprojectutility.object.postgres;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.suhprojectutility.object.constant.ModuleUpdateType;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ModuleVersionUpdate extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID moduleVersionUpdateId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  private ModuleVersion moduleVersion; // 연관된 모듈 버전

  private String updateTitle; // 업데이트 제목

  @Column
  private String updateDescription; // 업데이트 설명

  @Enumerated(EnumType.STRING)
  private ModuleUpdateType moduleUpdateType; // 업데이트 타입 (성능, 기능, 출시 등)

  @Transient
  private String moduleUpdateTypeIcon; // ModuleUpdateType.getSemanticUiIcon()


}
