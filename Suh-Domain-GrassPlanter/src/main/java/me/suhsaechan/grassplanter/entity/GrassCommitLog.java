package me.suhsaechan.grassplanter.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.entity.BasePostgresEntity;
import me.suhsaechan.github.entity.GithubRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class GrassCommitLog extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID grassCommitLogId;

    // 연관된 프로필
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grass_profile_id", nullable = false)
    private GrassProfile grassProfile;

    // 커밋한 저장소
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    private GithubRepository repository;

    // 저장소 이름 (캐싱용)
    @Column
    private String repositoryName;

    // 커밋 시간
    @Column(nullable = false)
    private LocalDateTime commitTime;

    // 커밋 메시지
    @Column(columnDefinition = "TEXT")
    private String commitMessage;

    // 커밋 SHA
    @Column
    private String commitSha;

    // 커밋 성공 여부
    @Column(nullable = false)
    private Boolean isSuccess;

    // 에러 메시지 (실패시)
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // 커밋 레벨 (0-4, GitHub 기여도 레벨)
    @Column
    private Integer commitLevel;

    // 자동 커밋 여부
    @Column(nullable = false)
    private Boolean isAutoCommit;
}
