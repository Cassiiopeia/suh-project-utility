package me.suhsaechan.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.constant.ServerOptionKey;

/**
 * 서버 설정 엔티티
 * 애플리케이션 전역 설정을 DB에 저장
 */
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ServerOption extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID serverOptionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, unique = true)
  private ServerOptionKey optionKey;

  @Column(nullable = false, length = 1000)
  private String optionValue;
}

