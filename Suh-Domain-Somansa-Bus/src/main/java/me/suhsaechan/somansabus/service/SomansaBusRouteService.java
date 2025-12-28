package me.suhsaechan.somansabus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.somansabus.dto.SomansaBusRequest;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import me.suhsaechan.somansabus.repository.SomansaBusRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusRouteService {

  private final SomansaBusRouteRepository routeRepository;

  @Transactional(readOnly = true)
  public SomansaBusResponse getAllRoutes() {
    log.info("전체 버스 노선 조회");
    List<SomansaBusRoute> routes = routeRepository.findByIsActiveTrueOrderByDepartureTimeAsc();
    return SomansaBusResponse.builder()
        .routes(routes)
        .totalCount((long) routes.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getRoutesByType(SomansaBusRequest request) {
    log.info("버스 노선 타입별 조회: {}", request.getCaralias());
    List<SomansaBusRoute> routes = routeRepository.findByCaraliasAndIsActiveTrue(request.getCaralias());
    return SomansaBusResponse.builder()
        .routes(routes)
        .totalCount((long) routes.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getRouteById(UUID routeId) {
    log.info("버스 노선 상세 조회: {}", routeId);
    SomansaBusRoute route = routeRepository.findById(routeId)
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_ROUTE_NOT_FOUND));
    return SomansaBusResponse.builder()
        .route(route)
        .build();
  }

  @Transactional
  public void initializeRoutes() {
    log.info("버스 노선 초기 데이터 생성");

    if (routeRepository.count() > 0) {
      log.info("이미 노선 데이터가 존재합니다. 초기화 건너뜀.");
      return;
    }

    Object[][] routeData = {
        {"06:55 당산역 1호 - 출근", 46563, "출근", "06:55", "당산역", 1},
        {"07:05 군자 1호 - 출근", 46569, "출근", "07:05", "군자", 1},
        {"07:10 당산역 2호 - 출근", 46565, "출근", "07:10", "당산역", 2},
        {"07:20 당산역 3호 - 출근", 46567, "출근", "07:20", "당산역", 3},
        {"07:20 서울역 1호 - 출근", 46552, "출근", "07:20", "서울역", 1},
        {"07:35 군자 2호 - 출근", 46571, "출근", "07:35", "군자", 2},
        {"07:50 서울역 2호 - 출근", 46561, "출근", "07:50", "서울역", 2},
        {"17:45 군자 1호 - 퇴근", 46570, "퇴근", "17:45", "군자", 1},
        {"17:45 서울역 1호 - 퇴근", 46553, "퇴근", "17:45", "서울역", 1},
        {"17:45 당산역 1호 - 퇴근", 46564, "퇴근", "17:45", "당산역", 1},
        {"18:15 군자 2호 - 퇴근", 46572, "퇴근", "18:15", "군자", 2},
        {"18:15 당산역 2호 - 퇴근", 46566, "퇴근", "18:15", "당산역", 2},
        {"18:15 서울역 2호 - 퇴근", 46562, "퇴근", "18:15", "서울역", 2},
        {"18:30 당산역 3호 - 퇴근", 46568, "퇴근", "18:30", "당산역", 3}
    };

    for (Object[] data : routeData) {
      SomansaBusRoute route = SomansaBusRoute.builder()
          .description((String) data[0])
          .disptid((Integer) data[1])
          .caralias((String) data[2])
          .departureTime((String) data[3])
          .station((String) data[4])
          .busNumber((Integer) data[5])
          .isActive(true)
          .build();
      routeRepository.save(route);
    }

    log.info("버스 노선 초기 데이터 생성 완료: {}개", routeData.length);
  }
}
