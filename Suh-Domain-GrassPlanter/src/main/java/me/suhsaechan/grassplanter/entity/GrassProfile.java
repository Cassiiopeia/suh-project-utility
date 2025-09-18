package me.suhsaechan.grassplanter.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.entity.BasePostgresEntity;
import me.suhsaechan.github.entity.GithubRepository;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class GrassProfile extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID grassProfileId;

    // GitHub 사용자명
    @Column(nullable = false, unique = true)
    private String githubUsername;

    // 암호화된 Personal Access Token
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedPat;

    // 프로필 활성화 여부
    @Column(nullable = false)
    private Boolean isActive;

    // 자동 커밋 활성화 여부
    @Column(nullable = false)
    private Boolean isAutoCommitEnabled;

    // 기본 저장소 관계 (GithubRepository 엔티티 참조)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_repository_id")
    private GithubRepository defaultRepository;

    // 커밋 메시지 템플릿
    @Column(columnDefinition = "TEXT")
    private String commitMessageTemplate;

    // 일일 커밋 목표
    @Column
    private Integer dailyCommitGoal;

    // 목표 커밋 레벨 (0-4)
    @Column
    private Integer targetCommitLevel;

    // 연속 커밋 일수
    @Column
    private Integer streakDays;

    // 소유자 ID (Member 대신 UUID로 관리)
    @Column(nullable = false)
    private UUID ownerId;

    // 소유자 닉네임 (캐싱용)
    @Column
    private String ownerNickname;

    // ⚠️ 절대 금지: @OneToMany는 사용하지 않음
    // 커밋 로그와 스케줄은 Repository에서 직접 조회
}