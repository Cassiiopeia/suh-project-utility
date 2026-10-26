package me.suhsaechan.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import me.suhsaechan.chatbot.repository.ChatMessageRepository;
import me.suhsaechan.chatbot.repository.ChatSessionRepository;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.notice.repository.SuhProjectUtilityNoticeRepository;
import me.suhsaechan.statistics.dto.DailyStatisticsDto;
import me.suhsaechan.statistics.dto.DashboardStatisticsDto;
import me.suhsaechan.statistics.dto.StatisticsPeriodTotalDto;
import me.suhsaechan.statistics.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceStatisticsTest {

  @Mock private StatisticsService statisticsService;
  @Mock private ChatSessionRepository chatSessionRepository;
  @Mock private ChatMessageRepository chatMessageRepository;
  @Mock private SuhProjectUtilityNoticeRepository noticeRepository;

  private DashboardService dashboardService;

  @BeforeEach
  void setUp() {
    dashboardService = new DashboardService(statisticsService, chatSessionRepository,
        chatMessageRepository, noticeRepository);
  }

  private StatisticsPeriodTotalDto emptyTotal() {
    return StatisticsPeriodTotalDto.builder()
        .uniqueVisitors(0L).pageViews(0L).featureUsages(0L).chatTokens(0L).build();
  }

  private DashboardStatisticsDto statisticsWithDays(int dayCount) {
    LocalDate firstDate = LocalDate.now().minusDays(dayCount - 1L);
    List<DailyStatisticsDto> dailyStatistics = new ArrayList<>();
    for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
      dailyStatistics.add(DailyStatisticsDto.builder().date(firstDate.plusDays(dayIndex))
          .uniqueVisitors(0L).pageViews(0L).featureUsages(0L).chatTokens(0L).build());
    }
    return DashboardStatisticsDto.builder()
        .currentTotal(emptyTotal()).dailyStatistics(dailyStatistics).build();
  }

  @Test
  void 허용되지_않는_기간이면_INVALID_PARAMETER() {
    assertThatThrownBy(() -> dashboardService.validatePeriodDays(14))
        .isInstanceOf(CustomException.class)
        .extracting(thrown -> ((CustomException) thrown).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_PARAMETER);
    assertThatThrownBy(() -> dashboardService.validatePeriodDays(null))
        .isInstanceOf(CustomException.class)
        .extracting(thrown -> ((CustomException) thrown).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_PARAMETER);
    dashboardService.validatePeriodDays(30);
  }

  @Test
  void 직전_기간은_같은_길이로_바로_앞() {
    when(statisticsService.buildPeriodStatistics(any(), any())).thenReturn(statisticsWithDays(7));
    when(statisticsService.getPeriodTotal(any(), any())).thenReturn(emptyTotal());
    when(chatMessageRepository.sumDailyTokensBetween(any(), any())).thenReturn(Collections.emptyList());
    when(chatMessageRepository.sumTokensBetween(any(), any())).thenReturn(0L);

    DashboardStatisticsDto result = dashboardService.getDashboardStatistics(7);

    ArgumentCaptor<LocalDateTime> currentStart = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<LocalDateTime> currentEnd = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<LocalDateTime> previousStart = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<LocalDateTime> previousEnd = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(statisticsService).buildPeriodStatistics(currentStart.capture(), currentEnd.capture());
    verify(statisticsService).getPeriodTotal(previousStart.capture(), previousEnd.capture());

    assertThat(previousEnd.getValue()).isEqualTo(currentStart.getValue());
    assertThat(Duration.between(currentStart.getValue(), currentEnd.getValue()).toDays()).isEqualTo(7);
    assertThat(Duration.between(previousStart.getValue(), previousEnd.getValue()).toDays()).isEqualTo(7);
    assertThat(currentEnd.getValue()).isEqualTo(LocalDate.now().plusDays(1).atStartOfDay());
    assertThat(result.getPeriodDays()).isEqualTo(7);
    assertThat(result.getDailyStatistics()).hasSize(7);
  }

  @Test
  void 챗봇_토큰을_날짜별로_합친다() {
    when(statisticsService.buildPeriodStatistics(any(), any())).thenReturn(statisticsWithDays(7));
    when(statisticsService.getPeriodTotal(any(), any())).thenReturn(emptyTotal());
    List<Object[]> dailyTokenRows = new ArrayList<>();
    dailyTokenRows.add(new Object[] {Date.valueOf(LocalDate.now()), 120L});
    when(chatMessageRepository.sumDailyTokensBetween(any(), any())).thenReturn(dailyTokenRows);
    when(chatMessageRepository.sumTokensBetween(any(), any())).thenReturn(120L, 50L);

    DashboardStatisticsDto result = dashboardService.getDashboardStatistics(7);

    List<DailyStatisticsDto> days = result.getDailyStatistics();
    assertThat(days.get(days.size() - 1).getChatTokens()).isEqualTo(120L);
    assertThat(days.subList(0, days.size() - 1)).allMatch(day -> day.getChatTokens() == 0L);
    assertThat(result.getCurrentTotal().getChatTokens()).isEqualTo(120L);
    assertThat(result.getPreviousTotal().getChatTokens()).isEqualTo(50L);
  }
}
