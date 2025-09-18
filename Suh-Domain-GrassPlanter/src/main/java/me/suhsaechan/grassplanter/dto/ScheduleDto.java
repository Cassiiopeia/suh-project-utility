package me.suhsaechan.grassplanter.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDto {
    private UUID grassScheduleId;
    private UUID grassProfileId;
    private String githubUsername;
    private UUID repositoryId;
    private String repositoryName;
    private String scheduleName;
    private String cronExpression;
    private LocalTime executionTime;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String recurrenceType;
    private String weekDays;
    private Boolean isActive;
    private LocalDateTime lastExecutionTime;
    private LocalDateTime nextExecutionTime;
    private Integer executionCount;
    private Integer successCount;
    private Integer failureCount;
    private Double successRate;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
