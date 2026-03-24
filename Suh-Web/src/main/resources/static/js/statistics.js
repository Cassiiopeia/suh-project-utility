// src/main/resources/static/js/statistics.js

var visitorsChart = null;
var featuresChart = null;
var chatChart = null;

var FEATURE_DISPLAY_NAMES = {
  'CHATBOT': '챗봇',
  'GITHUB_ISSUE_HELPER': '깃헙 이슈 도우미',
  'TRANSLATOR': '번역기',
  'DOCKER_LOG': '컨테이너 로그',
  'NOTICE': '공지사항',
  'GRASS_PLANTER': 'Grass Planter',
  'SUH_RANDOM': 'SUH 랜덤',
  'SEJONG_AUTH': '세종대 인증',
  'DEVOPS_TEMPLATE': 'DevOps 템플릿',
  'TASK_TRACKER': 'Task 트래커',
  'AI_SERVER': 'AI 서버 관리',
  'SOMANSA_BUS': '버스 예약',
  'CHATBOT_MANAGEMENT': '챗봇 문서 관리',
  'PROFILE': '프로필',
  'DASHBOARD': '대시보드',
  'STATISTICS': '통계'
};

document.addEventListener('DOMContentLoaded', function() {
  loadSummary();
  loadDailyVisitors();
  loadDailyFeatures();
  loadDailyChat();
  observeThemeChange();
});

function getThemeMode() {
  return document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
}

function getChartBaseOptions() {
  var isDark = getThemeMode() === 'dark';
  return {
    theme: { mode: isDark ? 'dark' : 'light' },
    chart: {
      background: 'transparent',
      toolbar: { show: true, tools: { download: true, zoom: true, zoomin: true, zoomout: true, pan: true, reset: true } },
      fontFamily: 'inherit'
    },
    tooltip: { theme: isDark ? 'dark' : 'light' },
    grid: { borderColor: isDark ? '#374151' : '#e5e7eb' }
  };
}

function formatNumber(num) {
  if (num === null || num === undefined) return '0';
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
  return num.toString();
}

function getFeatureDisplayName(key) {
  return FEATURE_DISPLAY_NAMES[key] || key;
}

// Summary 로드
function loadSummary() {
  sendFormRequest('/api/dashboard/summary', null,
    function(response) {
      if (response) {
        $('#stat-total-visitors').text(formatNumber(response.totalUniqueVisitors || 0));
        $('#stat-today-visitors').text('+' + formatNumber(response.todayUniqueVisitors || 0));
        $('#stat-total-pageviews').text(formatNumber(response.totalPageViews || 0));
        $('#stat-today-pageviews').text('+' + formatNumber(response.todayPageViews || 0));
        $('#stat-total-chats').text(formatNumber(response.totalChatMessages || 0));
        $('#stat-today-chats').text('+' + formatNumber(response.todayChatMessages || 0));

        var totalTokens = (response.totalInputTokens || 0) + (response.totalOutputTokens || 0);
        var todayTokens = (response.todayInputTokens || 0) + (response.todayOutputTokens || 0);
        $('#stat-total-tokens').text(formatNumber(totalTokens));
        $('#stat-today-tokens').text('+' + formatNumber(todayTokens));

        renderFeatureTable(response.featureUsageCounts, response.todayFeatureUsageCounts);
      }
      $('#summary-skeleton').addClass('hidden');
      $('#summary-content').removeClass('hidden');
    },
    function() {
      $('#summary-skeleton').addClass('hidden');
      $('#summary-content').removeClass('hidden');
    }
  );
}

// 기능별 상세 테이블
function renderFeatureTable(totalCounts, todayCounts) {
  var tbody = $('#feature-table-body');
  tbody.empty();

  if (!totalCounts || Object.keys(totalCounts).length === 0) {
    tbody.append('<tr><td colspan="3" class="text-center text-gray-400 py-8">데이터가 없습니다</td></tr>');
    $('#feature-table-skeleton').addClass('hidden');
    $('#feature-table-content').removeClass('hidden');
    return;
  }

  var sortedKeys = Object.keys(totalCounts).sort(function(a, b) {
    return (totalCounts[b] || 0) - (totalCounts[a] || 0);
  });

  sortedKeys.forEach(function(key) {
    var total = totalCounts[key] || 0;
    var today = (todayCounts && todayCounts[key]) ? todayCounts[key] : 0;
    var displayName = escapeHtml(getFeatureDisplayName(key));
    var row = '<tr>' +
      '<td class="font-medium">' + displayName + '</td>' +
      '<td class="text-right">' + formatNumber(total) + '</td>' +
      '<td class="text-right"><span class="text-green-600 font-medium">+' + formatNumber(today) + '</span></td>' +
      '</tr>';
    tbody.append(row);
  });

  $('#feature-table-skeleton').addClass('hidden');
  $('#feature-table-content').removeClass('hidden');
}

// 일별 방문자 차트
function loadDailyVisitors() {
  sendFormRequest('/api/statistics/daily-visitors', null,
    function(response) {
      if (response) {
        renderVisitorsChart(response.dailyVisitors || [], response.dailyPageViews || []);
      }
      $('#visitors-chart-skeleton').addClass('hidden');
      $('#visitors-chart').removeClass('hidden');
    },
    function() {
      $('#visitors-chart-skeleton').addClass('hidden');
      $('#visitors-chart').removeClass('hidden');
    }
  );
}

function renderVisitorsChart(visitors, pageViews) {
  var baseOpts = getChartBaseOptions();
  var visitorsData = visitors.map(function(d) { return { x: new Date(d.date + 'T00:00:00').getTime(), y: d.count }; });
  var pageViewsData = pageViews.map(function(d) { return { x: new Date(d.date + 'T00:00:00').getTime(), y: d.count }; });

  var options = {
    series: [
      { name: '방문자', data: visitorsData },
      { name: '페이지뷰', data: pageViewsData }
    ],
    chart: Object.assign({}, baseOpts.chart, {
      type: 'area',
      height: 300,
      zoom: { enabled: true, type: 'x', autoScaleYaxis: true }
    }),
    theme: baseOpts.theme,
    tooltip: baseOpts.tooltip,
    grid: baseOpts.grid,
    dataLabels: { enabled: false },
    stroke: { curve: 'smooth', width: 2 },
    fill: {
      type: 'gradient',
      gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05, stops: [0, 90, 100] }
    },
    xaxis: {
      type: 'datetime',
      labels: {
        datetimeFormatter: { year: 'yyyy', month: "yy'MM", day: 'MM/dd', hour: 'HH:mm' }
      }
    },
    yaxis: { min: 0, labels: { formatter: function(v) { return Math.round(v); } } },
    colors: ['#3b82f6', '#6366f1'],
    legend: { position: 'top', horizontalAlign: 'right' }
  };

  if (visitorsChart) {
    visitorsChart.destroy();
  }
  visitorsChart = new ApexCharts(document.querySelector('#visitors-chart'), options);
  visitorsChart.render();
}

// 기능별 사용 비율 차트
function loadDailyFeatures() {
  sendFormRequest('/api/statistics/daily-features', null,
    function(response) {
      if (response && response.featureUsageCounts) {
        renderFeaturesChart(response.featureUsageCounts);
      }
      $('#features-chart-skeleton').addClass('hidden');
      $('#features-chart').removeClass('hidden');
    },
    function() {
      $('#features-chart-skeleton').addClass('hidden');
      $('#features-chart').removeClass('hidden');
    }
  );
}

function renderFeaturesChart(featureCounts) {
  var labels = [];
  var series = [];

  var sortedKeys = Object.keys(featureCounts).sort(function(a, b) {
    return (featureCounts[b] || 0) - (featureCounts[a] || 0);
  });

  sortedKeys.forEach(function(key) {
    if (featureCounts[key] > 0) {
      labels.push(getFeatureDisplayName(key));
      series.push(featureCounts[key]);
    }
  });

  if (labels.length === 0) {
    $('#features-chart').html('<div class="flex items-center justify-center h-72 text-gray-400">데이터가 없습니다</div>');
    return;
  }

  var baseOpts = getChartBaseOptions();

  var options = {
    series: series,
    labels: labels,
    chart: Object.assign({}, baseOpts.chart, {
      type: 'donut',
      height: 300
    }),
    theme: baseOpts.theme,
    tooltip: baseOpts.tooltip,
    plotOptions: {
      pie: {
        donut: {
          size: '55%',
          labels: {
            show: true,
            total: {
              show: true,
              label: '전체',
              formatter: function(w) {
                return w.globals.seriesTotals.reduce(function(a, b) { return a + b; }, 0);
              }
            }
          }
        }
      }
    },
    legend: { position: 'bottom', fontSize: '12px' },
    dataLabels: { enabled: true, formatter: function(val) { return val.toFixed(1) + '%'; } },
    colors: ['#3b82f6', '#6366f1', '#8b5cf6', '#a855f7', '#ec4899', '#f43f5e', '#f97316', '#eab308', '#22c55e', '#14b8a6', '#06b6d4', '#0ea5e9']
  };

  if (featuresChart) {
    featuresChart.destroy();
  }
  featuresChart = new ApexCharts(document.querySelector('#features-chart'), options);
  featuresChart.render();
}

// 일별 챗봇 차트
function loadDailyChat() {
  sendFormRequest('/api/statistics/daily-chat', null,
    function(response) {
      if (response) {
        renderChatChart(response.dailyChatMessages || [], response.dailyChatTokens || []);
      }
      $('#chat-chart-skeleton').addClass('hidden');
      $('#chat-chart').removeClass('hidden');
    },
    function() {
      $('#chat-chart-skeleton').addClass('hidden');
      $('#chat-chart').removeClass('hidden');
    }
  );
}

function renderChatChart(messages, tokens) {
  var baseOpts = getChartBaseOptions();
  var messagesData = messages.map(function(d) { return { x: new Date(d.date + 'T00:00:00').getTime(), y: d.count }; });
  var tokensData = tokens.map(function(d) { return { x: new Date(d.date + 'T00:00:00').getTime(), y: d.count }; });

  var options = {
    series: [
      { name: '메시지', type: 'line', data: messagesData },
      { name: '토큰', type: 'line', data: tokensData }
    ],
    chart: Object.assign({}, baseOpts.chart, {
      type: 'line',
      height: 300,
      zoom: { enabled: true }
    }),
    theme: baseOpts.theme,
    tooltip: baseOpts.tooltip,
    grid: baseOpts.grid,
    stroke: { curve: 'smooth', width: [2, 2] },
    xaxis: {
      type: 'datetime',
      labels: {
        datetimeFormatter: { year: 'yyyy', month: "yy'MM", day: 'MM/dd', hour: 'HH:mm' }
      }
    },
    yaxis: [
      {
        title: { text: '메시지 수' },
        min: 0,
        labels: { formatter: function(v) { return Math.round(v); } }
      },
      {
        opposite: true,
        title: { text: '토큰 수' },
        min: 0,
        labels: { formatter: function(v) { return formatNumber(Math.round(v)); } }
      }
    ],
    colors: ['#8b5cf6', '#f59e0b'],
    legend: { position: 'top', horizontalAlign: 'right' },
    dataLabels: { enabled: false }
  };

  if (chatChart) {
    chatChart.destroy();
  }
  chatChart = new ApexCharts(document.querySelector('#chat-chart'), options);
  chatChart.render();
}

// 테마 변경 감지 및 차트 재렌더링
function observeThemeChange() {
  var observer = new MutationObserver(function(mutations) {
    mutations.forEach(function(mutation) {
      if (mutation.type === 'attributes' && mutation.attributeName === 'data-theme') {
        reRenderAllCharts();
      }
    });
  });

  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });

  window.addEventListener('beforeunload', function() { observer.disconnect(); });
}

function reRenderAllCharts() {
  if (visitorsChart) {
    visitorsChart.updateOptions(getChartBaseOptions());
  }
  if (featuresChart) {
    featuresChart.updateOptions({ theme: getChartBaseOptions().theme, tooltip: getChartBaseOptions().tooltip });
  }
  if (chatChart) {
    chatChart.updateOptions(getChartBaseOptions());
  }
}
