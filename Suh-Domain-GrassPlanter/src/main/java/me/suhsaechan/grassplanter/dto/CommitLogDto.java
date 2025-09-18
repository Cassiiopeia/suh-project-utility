package me.suhsaechan.grassplanter.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitLogDto {
    private UUID grassCommitLogId;
    private UUID grassProfileId;
    private String githubUsername;
    private UUID repositoryId;
    private String repositoryName;
    private LocalDateTime commitTime;
    private String commitMessage;
    private String commitSha;
    private Boolean isSuccess;
    private String errorMessage;
    private Integer commitLevel;
    private Boolean isAutoCommit;
    private LocalDateTime createdDate;
}
