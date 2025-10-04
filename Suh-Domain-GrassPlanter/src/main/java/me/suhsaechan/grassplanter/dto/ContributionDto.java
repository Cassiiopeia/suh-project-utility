package me.suhsaechan.grassplanter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionDto {
    private String githubUsername;
    private LocalDate date;
    private Integer contributionLevel;  // 0-4
    private Integer commitCount;
    private Boolean isAutoCommit;
    private String levelColor;  // GitHub 색상 (#ebedf0, #9be9a8, #40c463, #30a14e, #216e39)
    private String levelDescription;  // No contributions, Low, Medium, High, Very High
}
