package me.suhsaechan.somansabus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.somansabus.entity.SomansaBusReservationHistory;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import me.suhsaechan.somansabus.entity.SomansaBusSchedule;
import me.suhsaechan.somansabus.entity.SomansaBusUser;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SomansaBusResponse {

    // 단일 객체
    private SomansaBusUser user;
    private SomansaBusRoute route;
    private SomansaBusSchedule schedule;
    private SomansaBusReservationHistory history;

    // 리스트
    @Builder.Default
    private List<SomansaBusUser> users = new ArrayList<>();

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
}
