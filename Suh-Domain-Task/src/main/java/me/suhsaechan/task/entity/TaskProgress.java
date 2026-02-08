package me.suhsaechan.task.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@ToString(callSuper = true, exclude = "taskGoal")
public class TaskProgress extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID taskProgressId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  @JsonIgnore
  private TaskGoal taskGoal;

  @JsonFormat(pattern = "yyyy-MM-dd")
  @Column(nullable = false)
  private LocalDate progressDate;

  @Column(columnDefinition = "TEXT")
  private String content;

  private Integer startAmount;

  private Integer endAmount;

  @Column(columnDefinition = "TEXT")
  private String memo;

  @Column(nullable = false)
  @Builder.Default
  private Boolean isCompleted = false;
}
