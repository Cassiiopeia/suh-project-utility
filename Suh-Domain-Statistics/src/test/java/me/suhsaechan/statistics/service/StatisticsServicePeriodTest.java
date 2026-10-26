package me.suhsaechan.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.statistics.dto.DailyStatisticsDto;
import me.suhsaechan.statistics.dto.DashboardStatisticsDto;
import me.suhsaechan.statistics.dto.ReferrerSource;
import me.suhsaechan.statistics.entity.FeatureUsageLog.FeatureType;
import me.suhsaechan.statistics.repository.FeatureUsageLogRepository;
import me.suhsaechan.statistics.repository.PageVisitLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatisticsServicePeriodTest {

  private static final LocalDateTime START = LocalDateTime.parse("2026-10-24T00:00:00");
  private static final LocalDateTime END = LocalDateTime.parse("2026-10-27T00:00:00");

  private PageVisitLogRepository pageVisitLogRepository;
  private FeatureUsageLogRepository featureUsageLogRepository;
  private ServerOptionService serverOptionService;
  private StatisticsService statisticsService;

  @BeforeEach
  void setUp() {
    pageVisitLogRepository = mock(PageVisitLogRepository.class);
    featureUsageLogRepository = mock(FeatureUsageLogRepository.class);
    serverOptionService = mock(ServerOptionService.class);
    statisticsService = new StatisticsService(
        pageVisitLogRepository, featureUsageLogRepository, serverOptionService);

    when(serverOptionService.getOptionValue(ServerOptionKey.STATISTICS_INTERNAL_REFERRER_HOSTS))
        .thenReturn("lab.suhsaechan.kr");

    when(pageVisitLogRepository.countDailyVisitsBetween(any(), any())).thenReturn(List.of());
    when(pageVisitLogRepository.countGroupByPagePathBetween(any(), any())).thenReturn(List.of());
    when(pageVisitLogRepository.countGroupByReferrerBetween(any(), any())).thenReturn(List.of());
    when(pageVisitLogRepository.countHeatmapBetween(any(), any())).thenReturn(List.of());
    when(featureUsageLogRepository.countDailyUsageBetween(any(), any(), any())).thenReturn(List.of());
    when(featureUsageLogRepository.countGroupByFeatureNameBetween(any(), any(), anyCollection()))
        .thenReturn(List.of());
  }

  private static List<Object[]> rows(Object[]... rows) {
    return new ArrayList<>(List.of(rows));
  }

  @Test
  void 기록이_없으면_모든_날짜를_0으로_채운다() {
    DashboardStatisticsDto result = statisticsService.buildPeriodStatistics(START, END);

    assertThat(result.getDailyStatistics()).extracting(DailyStatisticsDto::getDate)
        .containsExactly(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 26));
    assertThat(result.getDailyStatistics()).allSatisfy(day -> {
      assertThat(day.getUniqueVisitors()).isZero();
      assertThat(day.getPageViews()).isZero();
      assertThat(day.getFeatureUsages()).isZero();
      assertThat(day.getChatTokens()).isZero();
    });
    assertThat(result.getHeatmapCells()).hasSize(56);
    assertThat(result.getTopPages()).isEmpty();
    assertThat(result.getFeatureUsages()).isEmpty();
    assertThat(result.getReferrerSources()).isEmpty();
  }

  @Test
  void 일별_방문을_날짜에_맞춰_채운다() {
    when(pageVisitLogRepository.countDailyVisitsBetween(any(), any()))
        .thenReturn(rows(new Object[] {Date.valueOf("2026-10-25"), 3L, 9L}));

    DashboardStatisticsDto result = statisticsService.buildPeriodStatistics(START, END);

    DailyStatisticsDto day = result.getDailyStatistics().get(1);
    assertThat(day.getDate()).isEqualTo(LocalDate.of(2026, 10, 25));
    assertThat(day.getUniqueVisitors()).isEqualTo(3L);
    assertThat(day.getPageViews()).isEqualTo(9L);
    assertThat(result.getDailyStatistics().get(0).getPageViews()).isZero();
    assertThat(result.getDailyStatistics().get(2).getPageViews()).isZero();
  }

  @Test
  void 인기_페이지는_첫_경로로_합산해_상위_5개만() {
    when(pageVisitLogRepository.countGroupByPagePathBetween(any(), any())).thenReturn(rows(
        new Object[] {"/", 5L}, new Object[] {"/dashboard", 4L},
        new Object[] {"/notice/1", 2L}, new Object[] {"/notice/2", 2L},
        new Object[] {"/login", 1L}, new Object[] {"/profile", 1L},
        new Object[] {"/chatbot", 1L}, new Object[] {"/docker", 1L}));

    var topPages = statisticsService.buildPeriodStatistics(START, END).getTopPages();

    assertThat(topPages).hasSize(5);
    assertThat(topPages.get(0).getPagePath()).isEqualTo("/");
    assertThat(topPages.get(0).getViewCount()).isEqualTo(9L);
    assertThat(topPages.get(1).getPagePath()).isEqualTo("/notice");
    assertThat(topPages.get(1).getViewCount()).isEqualTo(4L);
  }

  @Test
  void 유입_경로는_내부_이동을_빼고_분류별로_합산() {
    when(pageVisitLogRepository.countGroupByReferrerBetween(any(), any())).thenReturn(rows(
        new Object[] {null, 5L},
        new Object[] {"https://www.google.com/", 3L},
        new Object[] {"https://www.google.co.kr/", 2L},
        new Object[] {"https://lab.suhsaechan.kr/x", 7L}));

    var sources = statisticsService.buildPeriodStatistics(START, END).getReferrerSources();

    assertThat(sources).hasSize(2);
    assertThat(sources.get(0).getSource()).isEqualTo(ReferrerSource.GOOGLE);
    assertThat(sources.get(0).getViewCount()).isEqualTo(5L);
    assertThat(sources.get(1).getSource()).isEqualTo(ReferrerSource.DIRECT);
    assertThat(sources.get(1).getViewCount()).isEqualTo(5L);
  }

  @Test
  void 기능은_상위_6개_내림차순() {
    when(featureUsageLogRepository.countGroupByFeatureNameBetween(any(), any(), anyCollection()))
        .thenReturn(rows(
            new Object[] {FeatureType.CHATBOT, 1L},
            new Object[] {FeatureType.GITHUB_ISSUE_HELPER, 7L},
            new Object[] {FeatureType.AI_TRANSLATOR, 3L},
            new Object[] {FeatureType.CONTAINER_LOG, 6L},
            new Object[] {FeatureType.GRASS_PLANTER, 2L},
            new Object[] {FeatureType.SEJONG_AUTH, 5L},
            new Object[] {FeatureType.SUH_RANDOM, 4L}));

    var usages = statisticsService.buildPeriodStatistics(START, END).getFeatureUsages();

    assertThat(usages).hasSize(6);
    assertThat(usages).extracting(u -> u.getUsageCount()).containsExactly(7L, 6L, 5L, 4L, 3L, 2L);
  }

  @Test
  void 히트맵_칸에_값을_넣는다() {
    when(pageVisitLogRepository.countHeatmapBetween(any(), any()))
        .thenReturn(rows(new Object[] {4, 5, 11L}));

    var cells = statisticsService.buildPeriodStatistics(START, END).getHeatmapCells();

    assertThat(cells).hasSize(56);
    assertThat(cells).filteredOn(cell -> cell.getPageViews() > 0).hasSize(1)
        .allSatisfy(cell -> {
          assertThat(cell.getDayOfWeek()).isEqualTo(4);
          assertThat(cell.getHourBucket()).isEqualTo(5);
          assertThat(cell.getPageViews()).isEqualTo(11L);
        });
  }
}
