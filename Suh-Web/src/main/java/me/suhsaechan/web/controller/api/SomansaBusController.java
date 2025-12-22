package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.somansabus.dto.SomansaBusRequest;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.service.SomansaBusReservationService;
import me.suhsaechan.somansabus.service.SomansaBusRouteService;
import me.suhsaechan.somansabus.service.SomansaBusScheduleService;
import me.suhsaechan.somansabus.service.SomansaBusUserService;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/somansa-bus")
public class SomansaBusController {

  private final SomansaBusUserService userService;
  private final SomansaBusRouteService routeService;
  private final SomansaBusScheduleService scheduleService;
  private final SomansaBusReservationService reservationService;

  @PostMapping(value = "/user/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> registerUser(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(request));
  }

  @PostMapping(value = "/user/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getAllUsers() {
    return ResponseEntity.ok(userService.getAllUsers());
  }

  @PostMapping(value = "/user/list/active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getActiveUsers() {
    return ResponseEntity.ok(userService.getActiveUsers());
  }

  @PostMapping(value = "/user/detail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getUserDetail(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(userService.getUserById(request.getSomansaBusUserId()));
  }

  @PostMapping(value = "/user/toggle-active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> toggleUserActive(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(userService.toggleUserActive(request.getSomansaBusUserId()));
  }

  @PostMapping(value = "/user/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> deleteUser(@ModelAttribute SomansaBusRequest request) {
    userService.deleteUser(request.getSomansaBusUserId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/route/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getAllRoutes() {
    return ResponseEntity.ok(routeService.getAllRoutes());
  }

  @PostMapping(value = "/route/list/by-type", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getRoutesByType(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(routeService.getRoutesByType(request));
  }

  @PostMapping(value = "/route/detail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getRouteDetail(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(routeService.getRouteById(request.getSomansaBusRouteId()));
  }

  @PostMapping(value = "/schedule/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> createSchedule(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(request));
  }

  @PostMapping(value = "/schedule/list/by-user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getSchedulesByUser(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(scheduleService.getSchedulesByUser(request.getSomansaBusUserId()));
  }

  @PostMapping(value = "/schedule/list/active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getAllActiveSchedules() {
    return ResponseEntity.ok(scheduleService.getAllActiveSchedules());
  }

  @PostMapping(value = "/schedule/toggle-active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> toggleScheduleActive(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(scheduleService.toggleScheduleActive(request.getSomansaBusScheduleId()));
  }

  @PostMapping(value = "/schedule/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> deleteSchedule(@ModelAttribute SomansaBusRequest request) {
    scheduleService.deleteSchedule(request.getSomansaBusScheduleId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/reserve/manual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> manualReserve(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(reservationService.manualReserve(request));
  }

  @PostMapping(value = "/history/list/by-user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getHistoryByUser(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(reservationService.getHistoryByUser(request.getSomansaBusUserId()));
  }

  @PostMapping(value = "/history/recent/by-user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getRecentHistoryByUser(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(reservationService.getRecentHistoryByUser(request.getSomansaBusUserId()));
  }
}
