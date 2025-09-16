package me.suhsaechan.grassplanter.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import me.suhsaechan.common.entity.BasePostgresEntity;
import me.suhsaechan.github.entity.GithubRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class GrassSchedule extends BasePostgresEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID grassScheduleId;

    // 연관된 프로필
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grass_profile_id", nullable = false)
    private GrassProfile grassProfile;

    // 대상 저장소
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    private GithubRepository repository;

    // 스케줄 이름
    @Column(nullable = false)
    private String scheduleName;

    // Cron 표현식
    @Column
    private String cronExpression;

    // 실행 시간 (매일 실행시)
    @Column
    private LocalTime executionTime;

    // 시작 날짜
    @Column
    private LocalDateTime startDate;

    // 종료 날짜
    @Column
    private LocalDateTime endDate;

    // 반복 유형 (DAILY, WEEKLY, MONTHLY, CUSTOM)
    @Column(nullable = false)
    private String recurrenceType;

    // 요일 설정 (WEEKLY 타입시 사용, 예: "MON,WED,FRI")
    @Column
    private String weekDays;

    // 활성화 여부
    @Column(nullable = false)
    private Boolean isActive;

    // 마지막 실행 시간
    @Column
    private LocalDateTime lastExecutionTime;

    // 다음 실행 예정 시간
    @Column
    private LocalDateTime nextExecutionTime;

    // 실행 횟수
    @Column
    private Integer executionCount;

    // 성공 횟수
    @Column
    private Integer successCount;

    // 실패 횟수
    @Column
    private Integer failureCount;
}
