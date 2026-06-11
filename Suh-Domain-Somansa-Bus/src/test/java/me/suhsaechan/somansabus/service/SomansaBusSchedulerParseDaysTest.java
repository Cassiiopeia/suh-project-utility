package me.suhsaechan.somansabus.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.util.Set;
import org.junit.jupiter.api.Test;

// parseDays 회귀 테스트 — 요일 설정값을 DayOfWeek 정식명(MONDAY,...)으로 통일한 뒤 정상 파싱되는지 검증(#218).
// 순수 파싱 로직만 검증하므로 Spring 컨텍스트 없이 직접 인스턴스화한다.
class SomansaBusSchedulerParseDaysTest {

  private final SomansaBusSchedulerService service =
      new SomansaBusSchedulerService(null, null, null);

  @Test
  void 정식명_월화수목금_파싱() {
    Set<DayOfWeek> days = service.parseDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
    assertThat(days).containsExactlyInAnyOrder(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
  }

  @Test
  void 주말_정식명_파싱() {
    Set<DayOfWeek> days = service.parseDays("SATURDAY,SUNDAY");
    assertThat(days).containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
  }

  @Test
  void 공백_소문자_허용() {
    Set<DayOfWeek> days = service.parseDays(" monday , TUESDAY , Wednesday ");
    assertThat(days).containsExactlyInAnyOrder(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY);
  }

  @Test
  void 빈값_또는_null은_빈_Set() {
    assertThat(service.parseDays(null)).isEmpty();
    assertThat(service.parseDays("")).isEmpty();
    assertThat(service.parseDays("   ")).isEmpty();
  }

  @Test
  void 약어나_잘못된_값은_무시하고_정식명만_파싱() {
    Set<DayOfWeek> days = service.parseDays("MONDAY,MON,XYZ,FRIDAY");
    assertThat(days).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
  }
}
