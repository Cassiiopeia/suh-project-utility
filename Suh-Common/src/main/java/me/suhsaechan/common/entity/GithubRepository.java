package me.suhsaechan.common.entity;

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

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class GithubRepository extends BasePostgresEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID githubRepositoryId;

  // 저장소 전체 이름 (예: owner/repository)
  @Column(nullable = false, unique = true)
  private String fullName;

  // 저장소의 인기 지표: 별 개수
  @Column
  private Long starCount;

  // 저장소의 인기 지표: 포크 개수
  @Column
  private Long forkCount;

  // 저장소의 인기 지표: 워쳐 개수
  @Column
  private Long watcherCount;

  // 설명
  @Column(columnDefinition = "TEXT")
  private String description;
}