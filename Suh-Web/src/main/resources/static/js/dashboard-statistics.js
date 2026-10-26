/**
 * 대시보드 서버 통계: 기간 통계·누적 요약 조회 > KPI·차트·히트맵 렌더 > 테마 변경 시 다시 그림
 * 전역 공개 함수 2개: initDashboardStatistics(), refreshDashboardStatistics()
 */
(function() {
  const STATISTICS_URL = '/api/dashboard/statistics';
  const STATISTICS_REFRESH_URL = '/api/dashboard/statistics/refresh';
  const SUMMARY_URL = '/api/dashboard/summary';
  const SUMMARY_REFRESH_URL = '/api/dashboard/summary/refresh';
  const DEFAULT_PERIOD_DAYS = 30;

  // 기능 누적 합계에서 빼는 3종 (서버 기간 통계와 같은 기준)
  const EXCLUDED_FEATURE_TYPES = ['LOGIN', 'PROFILE_VIEW', 'NOTICE_VIEW'];

  // 도넛·유입 목록 색 순서 (차트 색 <-> 목록 점 클래스 동일 순서)
  const SOURCE_COLOR_TOKENS = ['primary', 'secondary', 'accent', 'info', 'warning'];
  const SOURCE_DOT_CLASSES = ['bg-primary', 'bg-secondary', 'bg-accent', 'bg-info', 'bg-warning'];

  // 히트맵 4단계: 0 / 최댓값 1/3 이하 / 2/3 이하 / 그 이상 (Tailwind 스캔용 리터럴)
  const HEATMAP_LEVEL_CLASSES = ['bg-base-200', 'bg-primary/25', 'bg-primary/55', 'bg-primary'];
  const HEATMAP_HOURS_PER_BUCKET = 3;

  // KPI 4종: 응답 필드명 > 누적값 계산
  const KPI_DEFINITIONS = [
    { field: 'uniqueVisitors', toCumulative: function(summary) { return summary.totalUniqueVisitors || 0; } },
    { field: 'pageViews', toCumulative: function(summary) { return summary.totalPageViews || 0; } },
    { field: 'featureUsages', toCumulative: sumIncludedFeatureUsages },
    { field: 'chatTokens', toCumulative: function(summary) { return (summary.totalInputTokens || 0) + (summary.totalOutputTokens || 0); } }
  ];

  const chartById = {};
  let selectedPeriodDays = DEFAULT_PERIOD_DAYS;
  let latestStatisticsRequestSequence = 0;
  let lastStatistics = null;
  let lastSummary = null;
  let palette = null;

  /**
   * 초기화: 기간 버튼·테마 감지 연결 > 요약·통계 첫 조회
   */
  function initDashboardStatistics() {
    if (!document.getElementById('statistics-section') || typeof Chart === 'undefined') {
      return;
    }
    $('#statistics-section [data-period-days]').on('click', function() {
      onPeriodButtonPressed(Number($(this).data('period-days')));
    });
    observeThemeChange();
    loadSummary(SUMMARY_URL);
    loadStatistics(STATISTICS_URL, selectedPeriodDays, null);
  }

  /**
   * 새로고침 버튼: 캐시 무효화 조회 2건(현재 기간 통계, 누적 요약) > 둘 다 끝나면 버튼 복구
   */
  function refreshDashboardStatistics() {
    if (typeof Chart === 'undefined') {
      return;
    }
    const refreshButton = $('#statistics-refresh-button');
    const refreshIcon = refreshButton.find('i');
    let pendingRequestCount = 2;
    const onRequestFinished = function() {
      pendingRequestCount--;
      if (pendingRequestCount === 0) {
        refreshButton.prop('disabled', false);
        refreshIcon.removeClass('animate-spin');
      }
    };
    refreshButton.prop('disabled', true);
    refreshIcon.addClass('animate-spin');
    loadSummary(SUMMARY_REFRESH_URL, onRequestFinished);
    loadStatistics(STATISTICS_REFRESH_URL, selectedPeriodDays, onRequestFinished);
  }

  function onPeriodButtonPressed(periodDays) {
    if (periodDays === selectedPeriodDays) {
      return;
    }
    selectedPeriodDays = periodDays;
    $('#statistics-section [data-period-days]').each(function() {
      const isSelected = Number($(this).data('period-days')) === periodDays;
      $(this).toggleClass('btn-primary', isSelected).attr('aria-pressed', String(isSelected));
    });
    loadStatistics(STATISTICS_URL, periodDays, null);
  }

  // ===== 조회 =====

  function loadSummary(url, onFinished) {
    sendFormRequest(url, null, function(summary) {
      lastSummary = summary || null;
      renderCumulativeTotals();
      if (onFinished) onFinished();
    }, function() {
      if (onFinished) onFinished();
    });
  }

  /**
   * 기간 통계 조회: 스켈레톤 > 응답 (마지막 요청만 반영) > 렌더
   */
  function loadStatistics(url, periodDays, onFinished) {
    const requestSequence = ++latestStatisticsRequestSequence;
    setAllCardStates('loading');
    sendFormRequest(url, { periodDays: periodDays }, function(statistics) {
      if (requestSequence === latestStatisticsRequestSequence) {
        lastStatistics = statistics;
        renderAll();
      }
      if (onFinished) onFinished();
    }, function() {
      // 오류 토스트는 전역 ajaxError 핸들러 담당 > 여기서는 빈 상태만 표시
      if (requestSequence === latestStatisticsRequestSequence) {
        lastStatistics = null;
        setAllCardStates('empty');
        renderKpiCardsUnavailable();
      }
      if (onFinished) onFinished();
    });
  }

  // ===== 렌더 =====

  function renderAll() {
    if (!lastStatistics) {
      return;
    }
    palette = resolvePalette();
    applyChartDefaults();
    renderKpiCards();
    renderTrendChart();
    renderFeatureChart();
    renderSourceChart();
    renderPageChart();
    renderHeatmap();
  }

  /**
   * KPI 4개: 기간 합계 > 증감 배지 > 스파크라인 > 누적
   */
  function renderKpiCards() {
    const currentTotal = lastStatistics.currentTotal || {};
    const previousTotal = lastStatistics.previousTotal || {};
    const dailyStatistics = lastStatistics.dailyStatistics || [];

    KPI_DEFINITIONS.forEach(function(definition) {
      const card = $('#statistics-section [data-kpi="' + definition.field + '"]');
      const currentValue = currentTotal[definition.field] || 0;
      setCardState(card, 'content');
      card.find('[data-kpi-value]').text(currentValue.toLocaleString());
      renderChangeBadge(card.find('[data-kpi-change]'), currentValue, previousTotal[definition.field] || 0);

      const sparklineCanvas = card.find('[data-kpi-sparkline]')[0];
      drawChart('sparkline-' + definition.field, sparklineCanvas, {
        type: 'line',
        data: {
          labels: dailyStatistics.map(function(daily) { return daily.date; }),
          datasets: [{
            data: dailyStatistics.map(function(daily) { return daily[definition.field] || 0; }),
            borderColor: palette.primary,
            borderWidth: 2,
            pointRadius: 0,
            tension: 0.3,
            fill: false
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          animation: false,
          events: [],
          plugins: { legend: { display: false }, tooltip: { enabled: false } },
          scales: { x: { display: false }, y: { display: false, beginAtZero: true } }
        }
      });
    });
    renderCumulativeTotals();
  }

  // 조회 실패 시 KPI: 값 "-", 배지·스파크라인 제거 (누적은 요약 응답 기준 유지)
  function renderKpiCardsUnavailable() {
    KPI_DEFINITIONS.forEach(function(definition) {
      const card = $('#statistics-section [data-kpi="' + definition.field + '"]');
      destroyChart('sparkline-' + definition.field);
      card.find('[data-kpi-value]').text('-');
      card.find('[data-kpi-change]').addClass('hidden').text('');
      setCardState(card, 'content');
    });
  }

  // 증감 배지: 증가 success, 감소 error, 변화 없음 ghost, 직전 0 이면 숨김
  function renderChangeBadge(badge, currentValue, previousValue) {
    const changeText = formatChangePercent(currentValue, previousValue);
    badge.removeClass('badge-success badge-error badge-ghost');
    if (changeText === null) {
      badge.addClass('hidden').text('');
      return;
    }
    if (currentValue > previousValue) {
      badge.addClass('badge-success');
    } else if (currentValue < previousValue) {
      badge.addClass('badge-error');
    } else {
      badge.addClass('badge-ghost');
    }
    badge.text(changeText).removeClass('hidden');
  }

  function renderCumulativeTotals() {
    if (!lastSummary) {
      return;
    }
    KPI_DEFINITIONS.forEach(function(definition) {
      const cumulativeValue = definition.toCumulative(lastSummary);
      $('#statistics-section [data-kpi="' + definition.field + '"] [data-kpi-cumulative]').text(cumulativeValue.toLocaleString());
    });
  }

  /**
   * 일별 추이: 방문자(선) + 페이지뷰(채움 면), 날짜 M/D
   */
  function renderTrendChart() {
    const card = $('#statistics-trend-card');
    const dailyStatistics = lastStatistics.dailyStatistics || [];
    const hasAnyVisit = dailyStatistics.some(function(daily) {
      return (daily.uniqueVisitors || 0) > 0 || (daily.pageViews || 0) > 0;
    });
    if (!hasAnyVisit) {
      destroyChart('trend');
      setCardState(card, 'empty');
      return;
    }
    setCardState(card, 'content');
    drawChart('trend', document.getElementById('statistics-trend-chart'), {
      type: 'line',
      data: {
        labels: dailyStatistics.map(function(daily) { return toMonthDayLabel(daily.date); }),
        datasets: [
          {
            label: window.MSG['dashboard.stats.kpi.visitors'],
            data: dailyStatistics.map(function(daily) { return daily.uniqueVisitors || 0; }),
            borderColor: palette.primary,
            backgroundColor: palette.primary,
            borderWidth: 2.5,
            pointRadius: 0,
            pointHoverRadius: 4,
            tension: 0.3,
            fill: false,
            order: 1
          },
          {
            label: window.MSG['dashboard.stats.kpi.pageViews'],
            data: dailyStatistics.map(function(daily) { return daily.pageViews || 0; }),
            borderColor: palette.primarySoft,
            backgroundColor: palette.primaryFaint,
            borderWidth: 1,
            pointRadius: 0,
            pointHoverRadius: 4,
            tension: 0.3,
            fill: true,
            order: 2
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { position: 'top', align: 'end', labels: { usePointStyle: true, boxWidth: 8 } },
          tooltip: {
            callbacks: {
              label: function(context) { return context.dataset.label + ' ' + context.parsed.y.toLocaleString(); }
            }
          }
        },
        scales: {
          x: { grid: { display: false }, ticks: { autoSkip: true, maxTicksLimit: 8, maxRotation: 0 } },
          y: { beginAtZero: true, ticks: { precision: 0 } }
        }
      }
    });
  }

  /**
   * 많이 쓴 기능: 가로 막대, 툴팁 횟수·비중%
   */
  function renderFeatureChart() {
    const card = $('#statistics-features-card');
    const featureUsages = (lastStatistics.featureUsages || []).filter(function(feature) { return (feature.usageCount || 0) > 0; });
    if (featureUsages.length === 0) {
      destroyChart('features');
      setCardState(card, 'empty');
      return;
    }
    const totalUsageCount = (lastStatistics.currentTotal || {}).featureUsages || 0;
    setCardState(card, 'content');
    drawChart('features', document.getElementById('statistics-features-chart'), buildHorizontalBarConfig(
      featureUsages.map(function(feature) { return window.MSG['dashboard.feature.' + feature.featureName] || feature.featureName; }),
      featureUsages.map(function(feature) { return feature.usageCount; }),
      palette.primary,
      function(count) { return formatCount(count) + ' · ' + (totalUsageCount > 0 ? Math.round(count / totalUsageCount * 100) : 0) + '%'; }
    ));
  }

  /**
   * 유입 경로: 도넛 + 옆 목록(색 점, 이름, 건수, 비중%)
   */
  function renderSourceChart() {
    const card = $('#statistics-sources-card');
    const referrerSources = lastStatistics.referrerSources || [];
    const totalViewCount = sumBy(referrerSources, 'viewCount');
    if (totalViewCount === 0) {
      destroyChart('sources');
      setCardState(card, 'empty');
      return;
    }
    const sourceLabels = referrerSources.map(function(source) { return window.MSG['dashboard.stats.source.' + source.source] || source.source; });
    const sourceColors = referrerSources.map(function(source, index) { return palette[SOURCE_COLOR_TOKENS[index % SOURCE_COLOR_TOKENS.length]]; });
    setCardState(card, 'content');
    renderSourceList(referrerSources, sourceLabels, totalViewCount);
    drawChart('sources', document.getElementById('statistics-sources-chart'), {
      type: 'doughnut',
      data: {
        labels: sourceLabels,
        datasets: [{
          data: referrerSources.map(function(source) { return source.viewCount || 0; }),
          backgroundColor: sourceColors,
          hoverBackgroundColor: sourceColors,
          borderColor: palette.base100,
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '62%',
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: function(context) {
                return context.label + ' ' + formatCount(context.parsed) + ' · ' + Math.round(context.parsed / totalViewCount * 100) + '%';
              }
            }
          }
        }
      }
    });
  }

  function renderSourceList(referrerSources, sourceLabels, totalViewCount) {
    const sourceList = $('#statistics-sources-list').empty();
    referrerSources.forEach(function(source, index) {
      const viewCount = source.viewCount || 0;
      const dotClass = SOURCE_DOT_CLASSES[index % SOURCE_DOT_CLASSES.length];
      const listItem = $('<li class="flex items-center gap-3 py-2 text-sm"></li>');
      listItem.append($('<span class="w-2.5 h-2.5 rounded-sm shrink-0"></span>').addClass(dotClass));
      listItem.append($('<span class="flex-1 font-medium text-base-content"></span>').text(sourceLabels[index]));
      listItem.append($('<span class="text-base-content/60"></span>').text(viewCount.toLocaleString()));
      listItem.append($('<span class="w-12 text-right font-bold text-base-content"></span>').text(Math.round(viewCount / totalViewCount * 100) + '%'));
      sourceList.append(listItem);
    });
  }

  /**
   * 인기 페이지: 가로 막대, 라벨 = 경로
   */
  function renderPageChart() {
    const card = $('#statistics-pages-card');
    const topPages = (lastStatistics.topPages || []).filter(function(page) { return (page.viewCount || 0) > 0; });
    if (topPages.length === 0) {
      destroyChart('pages');
      setCardState(card, 'empty');
      return;
    }
    setCardState(card, 'content');
    drawChart('pages', document.getElementById('statistics-pages-chart'), buildHorizontalBarConfig(
      topPages.map(function(page) { return page.pagePath; }),
      topPages.map(function(page) { return page.viewCount; }),
      palette.primarySoft,
      formatCount
    ));
  }

  /**
   * 요일×시간대 히트맵: 칸마다 단계 클래스 + title("목 15시~18시 · 11회")
   */
  function renderHeatmap() {
    const card = $('#statistics-heatmap-card');
    const heatmapCells = lastStatistics.heatmapCells || [];
    const maximumPageViews = heatmapCells.reduce(function(maximum, cell) { return Math.max(maximum, cell.pageViews || 0); }, 0);
    if (maximumPageViews === 0) {
      setCardState(card, 'empty');
      return;
    }
    setCardState(card, 'content');
    card.find('[data-heatmap-day]').removeClass(HEATMAP_LEVEL_CLASSES.join(' ')).addClass(HEATMAP_LEVEL_CLASSES[0]).attr('title', '');
    heatmapCells.forEach(function(cell) {
      const pageViews = cell.pageViews || 0;
      const startHour = cell.hourBucket * HEATMAP_HOURS_PER_BUCKET;
      const hourRange = window.MSG['dashboard.stats.heatmap.hourRange']
        .replace('{0}', startHour)
        .replace('{1}', startHour + HEATMAP_HOURS_PER_BUCKET);
      card.find('[data-heatmap-day="' + cell.dayOfWeek + '"][data-heatmap-hour="' + cell.hourBucket + '"]')
        .removeClass(HEATMAP_LEVEL_CLASSES[0])
        .addClass(HEATMAP_LEVEL_CLASSES[toHeatmapLevel(pageViews, maximumPageViews)])
        .attr('title', window.MSG['dashboard.stats.day.' + cell.dayOfWeek] + ' ' + hourRange + ' · ' + formatCount(pageViews));
    });
  }

  // 단계: 0 > 0, 최댓값 1/3 이하 > 1, 2/3 이하 > 2, 그 이상 > 3
  function toHeatmapLevel(pageViews, maximumPageViews) {
    if (pageViews <= 0) return 0;
    const ratio = pageViews / maximumPageViews;
    if (ratio <= 1 / 3) return 1;
    if (ratio <= 2 / 3) return 2;
    return 3;
  }

  // ===== Chart.js 공통 =====

  function buildHorizontalBarConfig(labels, values, barColor, formatTooltipValue) {
    return {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          data: values,
          backgroundColor: barColor,
          hoverBackgroundColor: palette.primary,
          borderRadius: 6,
          maxBarThickness: 22
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: function(context) { return formatTooltipValue(context.parsed.x); } } }
        },
        scales: {
          x: { beginAtZero: true, ticks: { precision: 0 } },
          y: { grid: { display: false } }
        }
      }
    };
  }

  // 차트 다시 그리기 (기존 인스턴스 destroy 후 생성)
  function drawChart(chartKey, canvas, config) {
    destroyChart(chartKey);
    chartById[chartKey] = new Chart(canvas, config);
  }

  function destroyChart(chartKey) {
    if (chartById[chartKey]) {
      chartById[chartKey].destroy();
      delete chartById[chartKey];
    }
  }

  function applyChartDefaults() {
    Chart.defaults.color = palette.baseContentMuted;
    Chart.defaults.borderColor = palette.base300;
    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
  }

  /**
   * DaisyUI 테마 변수 > rgba 문자열 (probe 계산색 > 1px 캔버스로 sRGB 변환, oklch 미지원 파서 대비)
   */
  function resolvePalette() {
    const probe = document.getElementById('statistics-color-probe');
    const canvasContext = document.createElement('canvas').getContext('2d', { willReadFrequently: true });
    const resolveColor = function(cssColorExpression) {
      probe.style.color = cssColorExpression;
      canvasContext.clearRect(0, 0, 1, 1);
      canvasContext.fillStyle = getComputedStyle(probe).color;
      canvasContext.fillRect(0, 0, 1, 1);
      const pixel = canvasContext.getImageData(0, 0, 1, 1).data;
      return 'rgba(' + pixel[0] + ', ' + pixel[1] + ', ' + pixel[2] + ', ' + (pixel[3] / 255).toFixed(3) + ')';
    };
    return {
      primary: resolveColor('var(--color-primary)'),
      primarySoft: resolveColor('color-mix(in oklab, var(--color-primary) 30%, transparent)'),
      primaryFaint: resolveColor('color-mix(in oklab, var(--color-primary) 15%, transparent)'),
      secondary: resolveColor('var(--color-secondary)'),
      accent: resolveColor('var(--color-accent)'),
      info: resolveColor('var(--color-info)'),
      warning: resolveColor('var(--color-warning)'),
      baseContentMuted: resolveColor('color-mix(in oklab, var(--color-base-content) 70%, transparent)'),
      base300: resolveColor('var(--color-base-300)'),
      base100: resolveColor('var(--color-base-100)')
    };
  }

  // data-theme 변경 감지 > 마지막 응답으로 전부 다시 그림
  function observeThemeChange() {
    const themeObserver = new MutationObserver(function() {
      renderAll();
    });
    themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
  }

  // ===== 표시 상태·포맷 =====

  // 카드 상태 전환: loading(스켈레톤) / content / empty
  function setCardState(card, state) {
    card.find('[data-card-state]').addClass('hidden');
    card.find('[data-card-state="' + state + '"]').removeClass('hidden');
  }

  function setAllCardStates(state) {
    $('#statistics-section [data-statistics-card]').each(function() {
      const card = $(this);
      // KPI 카드는 빈 상태 없음 > 해당 상태가 있는 카드만 전환
      if (card.find('[data-card-state="' + state + '"]').length > 0) {
        setCardState(card, state);
      }
    });
  }

  /**
   * 증감률 표시: 직전 0 이하 > null(배지 숨김), 그 외 반올림 정수 "+12%" / "-5%"
   */
  function formatChangePercent(current, previous) {
    if (!(previous > 0)) {
      return null;
    }
    const changePercent = Math.round((current - previous) / previous * 100);
    return (changePercent >= 0 ? '+' : '') + changePercent + '%';
  }

  // "yyyy-MM-dd" > "M/D"
  function toMonthDayLabel(isoDate) {
    const dateParts = String(isoDate).split('-');
    return Number(dateParts[1]) + '/' + Number(dateParts[2]);
  }

  function formatCount(count) {
    return (count || 0).toLocaleString() + window.MSG['dashboard.stats.unit.times'];
  }

  function sumBy(items, fieldName) {
    return items.reduce(function(total, item) { return total + (item[fieldName] || 0); }, 0);
  }

  // 기능 누적: featureUsageCounts 에서 제외 3종 뺀 합
  function sumIncludedFeatureUsages(summary) {
    const featureUsageCounts = summary.featureUsageCounts || {};
    return Object.keys(featureUsageCounts).reduce(function(total, featureType) {
      return EXCLUDED_FEATURE_TYPES.indexOf(featureType) >= 0 ? total : total + Number(featureUsageCounts[featureType] || 0);
    }, 0);
  }

  window.initDashboardStatistics = initDashboardStatistics;
  window.refreshDashboardStatistics = refreshDashboardStatistics;
})();
