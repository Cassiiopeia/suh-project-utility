package me.suhsaechan.somansabus.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.somansabus.dto.RouteData;
import me.suhsaechan.somansabus.dto.SomansaBusRequest;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import me.suhsaechan.somansabus.repository.SomansaBusRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusRouteService {

  private final SomansaBusRouteRepository routeRepository;
  private final SomansaBusApiService apiService;
  private final ServerOptionService serverOptionService;

  @Transactional(readOnly = true)
  public SomansaBusResponse getAllRoutes() {
    log.info("전체 버스 노선 조회");
    List<SomansaBusRoute> routes = routeRepository.findByIsActiveTrueOrderByDepartureTimeAsc();
    LocalDateTime lastSyncedAt = routeRepository.findMaxUpdatedDate().orElse(null);
    return SomansaBusResponse.builder()
        .routes(routes)
        .totalCount((long) routes.size())
        .lastSyncedAt(lastSyncedAt)
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

  /**
   * 외부 buseezy 노선 데이터를 가져와 DB 와 동기화한다.
   * HTTP 호출은 트랜잭션 밖에서 실행하여 DB connection 점유 시간을 최소화한다.
   * upsert + soft delete 단계만 트랜잭션으로 묶는다.
   */
  public SomansaBusResponse syncRoutes() {
    String triggerLoginId = resolveTriggerLoginId();
    log.info("버스 노선 동기화 시작 - 트리거 회원: {}", triggerLoginId);

    List<RouteData> remoteRoutes = fetchRemoteRoutes(triggerLoginId);
    log.info("외부 노선 응답 수신: {}개", remoteRoutes.size());

    return applyRemoteRoutes(remoteRoutes, triggerLoginId);
  }

  private List<RouteData> fetchRemoteRoutes(String triggerLoginId) {
    int passengerId = apiService.login(triggerLoginId);
    if (passengerId <= 0) {
      log.error("동기화 로그인 실패: {}", triggerLoginId);
      throw new CustomException(ErrorCode.SOMANSA_BUS_LOGIN_FAILED);
    }

    boolean sessionCreated = apiService.createSession(triggerLoginId, passengerId);
    if (!sessionCreated) {
      log.error("동기화 세션 생성 실패: {}", triggerLoginId);
      throw new CustomException(ErrorCode.SOMANSA_BUS_SESSION_FAILED);
    }

    return apiService.fetchRouteList();
  }

  @Transactional
  public SomansaBusResponse applyRemoteRoutes(List<RouteData> remoteRoutes, String triggerLoginId) {
    Set<Integer> remoteDisptids = new HashSet<>();
    int created = 0;
    int updated = 0;
    int deactivated = 0;

    for (RouteData remote : remoteRoutes) {
      remoteDisptids.add(remote.getDisptid());

      SomansaBusRoute existing = routeRepository.findByDisptid(remote.getDisptid()).orElse(null);
      if (existing != null) {
        boolean changed = applyRemoteToEntity(existing, remote);
        boolean reactivated = false;
        if (Boolean.FALSE.equals(existing.getIsActive())) {
          existing.setIsActive(true);
          reactivated = true;
        }
        if (changed || reactivated) {
          routeRepository.save(existing);
          updated++;
        }
      } else {
        SomansaBusRoute newRoute = SomansaBusRoute.builder()
            .disptid(remote.getDisptid())
            .description(remote.getDescription())
            .caralias(remote.getCaralias())
            .departureTime(remote.getDepartureTime())
            .station(remote.getStation())
            .busNumber(remote.getBusNumber())
            .isShuttle(Boolean.TRUE.equals(remote.getIsShuttle()))
            .isActive(true)
            .build();
        routeRepository.save(newRoute);
        created++;
      }
    }

    List<SomansaBusRoute> activeRoutes = routeRepository.findByIsActiveTrue();
    for (SomansaBusRoute existing : activeRoutes) {
      if (!remoteDisptids.contains(existing.getDisptid())) {
        existing.setIsActive(false);
        routeRepository.save(existing);
        deactivated++;
      }
    }

    long totalCount = routeRepository.count();
    log.info("버스 노선 동기화 완료 - 신규: {}, 변경: {}, 비활성: {}, 총: {}",
        created, updated, deactivated, totalCount);

    return SomansaBusResponse.builder()
        .syncCreatedCount(created)
        .syncUpdatedCount(updated)
        .syncDeactivatedCount(deactivated)
        .syncTotalCount((int) totalCount)
        .syncedAt(LocalDateTime.now())
        .syncTriggerLoginId(triggerLoginId)
        .build();
  }

  private String resolveTriggerLoginId() {
    String value = serverOptionService.getOptionValue(ServerOptionKey.SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID);
    if (value == null || value.isBlank()) {
      return ServerOptionKey.SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID.getDefaultValue();
    }
    return value;
  }

  private boolean applyRemoteToEntity(SomansaBusRoute entity, RouteData remote) {
    boolean changed = false;
    if (!Objects.equals(entity.getDescription(), remote.getDescription())) {
      entity.setDescription(remote.getDescription());
      changed = true;
    }
    if (!Objects.equals(entity.getCaralias(), remote.getCaralias())) {
      entity.setCaralias(remote.getCaralias());
      changed = true;
    }
    if (!Objects.equals(entity.getDepartureTime(), remote.getDepartureTime())) {
      entity.setDepartureTime(remote.getDepartureTime());
      changed = true;
    }
    if (!Objects.equals(entity.getStation(), remote.getStation())) {
      entity.setStation(remote.getStation());
      changed = true;
    }
    if (!Objects.equals(entity.getBusNumber(), remote.getBusNumber())) {
      entity.setBusNumber(remote.getBusNumber());
      changed = true;
    }
    boolean remoteIsShuttle = Boolean.TRUE.equals(remote.getIsShuttle());
    boolean entityIsShuttle = Boolean.TRUE.equals(entity.getIsShuttle());
    if (entityIsShuttle != remoteIsShuttle) {
      entity.setIsShuttle(remoteIsShuttle);
      changed = true;
    }
    return changed;
  }
}
