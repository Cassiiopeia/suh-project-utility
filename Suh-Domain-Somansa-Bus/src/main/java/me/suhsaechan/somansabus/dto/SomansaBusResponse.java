package me.suhsaechan.somansabus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.somansabus.entity.SomansaBusMember;
import me.suhsaechan.somansabus.entity.SomansaBusReservationHistory;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import me.suhsaechan.somansabus.entity.SomansaBusSchedule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SomansaBusResponse {

    // 단일 객체
    private SomansaBusMember member;
    private SomansaBusRoute route;
    private SomansaBusSchedule schedule;
    private SomansaBusReservationHistory history;

    // 리스트
    @Builder.Default
    private List<SomansaBusMember> members = new ArrayList<>();

    @Builder.Default
    private List<SomansaBusRoute> routes = new ArrayList<>();

    @Builder.Default
    private List<SomansaBusSchedule> schedules = new ArrayList<>();

    @Builder.Default
    private List<SomansaBusReservationHistory> histories = new ArrayList<>();

    // 카운트
    private Long totalCount;

    // 예약 결과
    private Boolean isReservationSuccess;

    // 통계
    private Integer totalMembers;
    private Integer activeMembers;
    private Integer totalSchedules;
    private Integer activeSchedules;
    private Integer thisWeekReservations;
    private Integer thisWeekSuccessReservations;
    private Integer thisWeekFailedReservations;

    // 노선 동기화 결과
    private Integer syncCreatedCount;
    private Integer syncUpdatedCount;
    private Integer syncDeactivatedCount;
    private Integer syncTotalCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime syncedAt;

    private String syncTriggerLoginId;

    // 노선 list 조회 시 마지막 동기화 시각
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSyncedAt;
}
