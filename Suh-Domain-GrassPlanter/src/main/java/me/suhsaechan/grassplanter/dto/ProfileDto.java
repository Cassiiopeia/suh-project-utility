package me.suhsaechan.grassplanter.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {
    private UUID grassProfileId;
    private String githubUsername;
    private Boolean isActive;
    private Boolean isAutoCommitEnabled;
    private UUID defaultRepositoryId;
    private String defaultRepositoryName;
    private String commitMessageTemplate;
    private Integer dailyCommitGoal;
    private Integer targetCommitLevel;
    private Integer streakDays;
    private UUID ownerId;
    private String ownerNickname;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // 통계 정보
    private Integer totalCommits;
    private Integer successfulCommits;
    private Double successRate;
    private LocalDateTime lastCommitTime;
}
