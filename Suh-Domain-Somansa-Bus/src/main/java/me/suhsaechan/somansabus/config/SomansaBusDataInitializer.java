package me.suhsaechan.somansabus.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import me.suhsaechan.somansabus.repository.SomansaBusRouteRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SomansaBusDataInitializer implements ApplicationRunner {

  private final SomansaBusRouteRepository routeRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (routeRepository.count() > 0) {
      log.info("버스 노선 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
      return;
    }

    log.info("버스 노선 초기 데이터 생성 시작");
    initializeRoutes();
    log.info("버스 노선 초기 데이터 생성 완료: {}개 노선", routeRepository.count());
  }

  private void initializeRoutes() {
    Object[][] routeData = {
        {"06:55 당산역 1호 - 출근", 46563, "출근", "06:55", "당산역", 1},
        {"06:55 당산역 2호 - 출근", 46564, "출근", "06:55", "당산역", 2},
        {"07:10 당산역 1호 - 출근", 46565, "출근", "07:10", "당산역", 1},
        {"07:10 당산역 2호 - 출근", 46566, "출근", "07:10", "당산역", 2},
        {"07:30 당산역 1호 - 출근", 46567, "출근", "07:30", "당산역", 1},
        {"07:30 당산역 2호 - 출근", 46568, "출근", "07:30", "당산역", 2},
        {"07:30 당산역 3호 - 출근", 46569, "출근", "07:30", "당산역", 3},
        {"07:50 당산역 1호 - 출근", 46570, "출근", "07:50", "당산역", 1},
        {"07:50 당산역 2호 - 출근", 46571, "출근", "07:50", "당산역", 2},
        {"08:15 당산역 1호 - 출근", 46572, "출근", "08:15", "당산역", 1},
        {"17:40 당산역 1호 - 퇴근", 46573, "퇴근", "17:40", "당산역", 1},
        {"18:10 당산역 1호 - 퇴근", 46574, "퇴근", "18:10", "당산역", 1},
        {"18:10 당산역 2호 - 퇴근", 46575, "퇴근", "18:10", "당산역", 2},
        {"18:40 당산역 1호 - 퇴근", 46576, "퇴근", "18:40", "당산역", 1},
        {"19:00 당산역 1호 - 퇴근", 46577, "퇴근", "19:00", "당산역", 1}
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
  }
}
