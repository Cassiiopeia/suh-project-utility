package me.suhsaechan.common.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@ToString
@SuperBuilder
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class BasePostgresEntity {

  // 생성일
  @CreatedDate
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdDate;

  // 수정일
  @LastModifiedDate
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Column(nullable = false)
  private LocalDateTime updatedDate;
}
