package me.suhsaechan.clipboard.entity;

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
import me.suhsaechan.common.constant.ClipboardItemType;
import me.suhsaechan.common.entity.BasePostgresEntity;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ClipboardItem extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID clipboardItemId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ClipboardItemType itemType;

  @Column(columnDefinition = "TEXT")
  private String textContent;

  private String originalFileName;

  // 디스크 저장 파일명: UUID (확장자 없음, 사용자 파일명은 경로에 쓰지 않음)
  private String storedFileName;

  private String contentType;

  private Long fileSizeBytes;
}
