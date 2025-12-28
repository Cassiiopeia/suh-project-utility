package me.suhsaechan.web.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.somansabus.dto.SomansaBusRequest;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.service.SomansaBusMemberService;
import me.suhsaechan.somansabus.service.SomansaBusReservationService;
import me.suhsaechan.somansabus.service.SomansaBusRouteService;
import me.suhsaechan.somansabus.service.SomansaBusScheduleService;
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

  private final SomansaBusMemberService memberService;
  private final SomansaBusRouteService routeService;
  private final SomansaBusScheduleService scheduleService;
  private final SomansaBusReservationService reservationService;

  @PostMapping(value = "/member/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> registerMember(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(memberService.registerMember(request));
  }

  @PostMapping(value = "/member/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getAllMembers() {
    return ResponseEntity.ok(memberService.getAllMembers());
  }

  @PostMapping(value = "/member/list/active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getActiveMembers() {
    return ResponseEntity.ok(memberService.getActiveMembers());
  }

  @PostMapping(value = "/member/detail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getMemberDetail(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(memberService.getMemberById(request.getSomansaBusMemberId()));
  }

  @PostMapping(value = "/member/toggle-active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> toggleMemberActive(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(memberService.toggleMemberActive(request.getSomansaBusMemberId()));
  }

  @PostMapping(value = "/member/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<Void> deleteMember(@ModelAttribute SomansaBusRequest request) {
    memberService.deleteMember(request.getSomansaBusMemberId());
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

  @PostMapping(value = "/schedule/list/by-member", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getSchedulesByMember(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(scheduleService.getSchedulesByMember(request.getSomansaBusMemberId()));
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

  @PostMapping(value = "/history/list/by-member", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getHistoryByMember(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(reservationService.getHistoryByMember(request.getSomansaBusMemberId()));
  }

  @PostMapping(value = "/history/recent/by-member", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getRecentHistoryByMember(@ModelAttribute SomansaBusRequest request) {
    return ResponseEntity.ok(reservationService.getRecentHistoryByMember(request.getSomansaBusMemberId()));
  }

  @PostMapping(value = "/history/recent", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getRecentHistory() {
    return ResponseEntity.ok(reservationService.getRecentHistory());
  }

  @PostMapping(value = "/stats", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @LogMonitor
  public ResponseEntity<SomansaBusResponse> getStats() {
    return ResponseEntity.ok(scheduleService.getStats());
  }
}
