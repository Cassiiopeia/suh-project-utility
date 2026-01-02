package me.suhsaechan.somansabus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SomansaBusRequest {

    // 멤버 관련
    private UUID somansaBusMemberId;
    private String loginId;
    private String password;
    private Boolean isActive;

    // 노선 관련
    private UUID somansaBusRouteId;
    private String caralias;

    // 스케줄 관련
    private UUID somansaBusScheduleId;
    private Integer daysAhead;

    // 예약 관련
    private LocalDate reservationDate;

    // 검색/필터
    private String searchKeyword;
}
