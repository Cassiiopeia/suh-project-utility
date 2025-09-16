package me.suhsaechan.grassplanter.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrassResponse {

    // === 단일 객체 응답 ===
    private ProfileDto profile;
    private ScheduleDto schedule;
    private CommitLogDto commitLog;
    
    // === 목록 응답 ===
    private List<ProfileDto> profiles;
    private List<ScheduleDto> schedules;
    private List<CommitLogDto> commitLogs;
    
    // === 통계 정보 ===
    private Integer totalCommits;
    private Integer successfulCommits;
    private Integer failedCommits;
    private Integer currentStreak;
    private Integer longestStreak;
    private Integer todayCommitLevel;
    private Double successRate;
    
    // === 페이징 정보 ===
    private Integer currentPage;
    private Integer totalPages;
    private Long totalElements;
    private Integer pageSize;
    
    // === 실행 결과 ===
    private String commitSha;
    private String errorDetails;
    
    // === 기여도 정보 ===
    private ContributionDto contribution;
    private List<ContributionDto> contributions;
    private Integer contributionLevel;
}
