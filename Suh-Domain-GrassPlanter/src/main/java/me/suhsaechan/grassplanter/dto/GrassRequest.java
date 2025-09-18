package me.suhsaechan.grassplanter.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrassRequest {

    // === 프로필 관련 ===
    private UUID profileId;
    private String githubUsername;
    private String personalAccessToken;  // PAT (생성시만 사용)
    private Boolean isActive;
    private Boolean isAutoCommitEnabled;
    private UUID defaultRepositoryId;
    private String commitMessageTemplate;
    private Integer dailyCommitGoal;
    private Integer targetCommitLevel;
    private UUID ownerId;
    private String ownerNickname;

    // === 스케줄 관련 ===
    private UUID scheduleId;
    private String scheduleName;
    private UUID repositoryId;
    private String cronExpression;
    private LocalTime executionTime;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String recurrenceType;  // DAILY, WEEKLY, MONTHLY, CUSTOM
    private String weekDays;  // MON,WED,FRI 형식
    
    // === 커밋 로그 조회 관련 ===
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private Boolean onlySuccess;
    private Boolean onlyAutoCommit;
    
    // === 페이징 관련 ===
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
    
    // === 실행 관련 ===
    private String repositoryFullName;  // owner/repo 형식
    private String commitMessage;
    private Boolean forceCommit;  // 강제 커밋 여부
}
