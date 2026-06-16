/**
 * Task Tracker JavaScript
 * D-Day 관리 및 일일 진행 기록 기능
 */

let goals = [];

$(document).ready(function() {
  loadGoals();
  loadProgress();
  setDefaultDate();

  $('#progressGoalId').on('change', function() {
    const goalId = $(this).val();
    if (goalId) {
      const goal = goals.find(g => g.taskGoalId === goalId);
      if (goal && goal.totalAmount > 0) {
        const lastEnd = goal.currentAmount || 0;
        initRangeSlider(goal.totalAmount, lastEnd, lastEnd);
      } else {
        initRangeSlider(100, 0, 0);
      }
    } else {
      initRangeSlider(100, 0, 0);
    }
  });
});

/**
 * 기본 날짜 설정 (오늘)
 */
function setDefaultDate() {
  const today = new Date().toISOString().split('T')[0];
  $('#progressDate').val(today);

  const nextMonth = new Date();
  nextMonth.setMonth(nextMonth.getMonth() + 1);
  $('#goalTargetDate').val(nextMonth.toISOString().split('T')[0]);
}

/**
 * Goal 목록 로드
 */
function loadGoals() {
  const includeCompleted = $('#showCompleted').is(':checked');
  const params = {
    isActiveOnly: !includeCompleted
  };

  sendFormRequest('/api/task/goal/list', params,
    function(response) {
      goals = response.goals || [];
      renderGoals(goals);
      updateGoalSelects(goals);
    },
    function(xhr, status, error) {
      console.error('Goal 목록 로드 실패:', error);
      $('#taskGoalsGrid').html('<div class="col-span-full text-center py-8 text-gray-500">' + window.MSG['taskTracker.toast.loadFailed'] + '</div>');
    }
  );
}

/**
 * Goal 카드 렌더링
 */
function renderGoals(goals) {
  const container = $('#taskGoalsGrid');
  const emptyMessage = $('#emptyGoalsMessage');

  if (!goals || goals.length === 0) {
    container.addClass('hidden');
    emptyMessage.removeClass('hidden');
    return;
  }

  container.removeClass('hidden');
  emptyMessage.addClass('hidden');
  container.empty();

  goals.forEach(function(goal) {
    container.append(renderGoalCard(goal));
  });
}

/**
 * 단일 Goal 카드 렌더링
 */
function renderGoalCard(goal) {
  const daysRemaining = goal.daysRemaining || 0;
  const progressPercentage = goal.progressPercentage || 0;

  let ddayText = '';
  let ddayClass = '';

  if (daysRemaining > 0) {
    ddayText = 'D-' + daysRemaining;
    if (daysRemaining <= 7) {
      ddayClass = 'text-red-500';
    } else if (daysRemaining <= 30) {
      ddayClass = 'text-yellow-500';
    } else {
      ddayClass = 'text-green-500';
    }
  } else if (daysRemaining === 0) {
    ddayText = 'D-Day';
    ddayClass = 'text-red-600 font-bold';
  } else {
    ddayText = 'D+' + Math.abs(daysRemaining);
    ddayClass = 'text-gray-400';
  }

  const colorClass = getColorClass(goal.color || 'blue');
  const icon = goal.icon || 'fa-solid fa-bullseye';

  const card = $('<div class="card bg-base-100 border border-gray-200 hover:shadow-md transition-shadow cursor-pointer">');

  const cardBody = $('<div class="card-body p-4">');

  const header = $('<div class="flex items-center justify-between mb-3">');
  const iconTitle = $('<div class="flex items-center gap-2">');
  iconTitle.append($('<i>').addClass(icon + ' text-xl ' + colorClass));
  iconTitle.append($('<span class="font-semibold text-gray-800 truncate max-w-32">').text(goal.title));
  header.append(iconTitle);

  const ddayBadge = $('<div class="text-lg font-bold">').addClass(ddayClass).text(ddayText);
  header.append(ddayBadge);
  cardBody.append(header);

  if (goal.description) {
    const desc = $('<p class="text-sm text-gray-500 mb-3 line-clamp-2">').text(goal.description);
    cardBody.append(desc);
  }

  if (goal.totalAmount > 0) {
    const progressDiv = $('<div class="mb-3">');
    const progressInfo = $('<div class="flex justify-between text-xs text-gray-500 mb-1">');
    progressInfo.append($('<span>').text(window.MSG['taskTracker.label.progressRate']));
    progressInfo.append($('<span>').text(progressPercentage + '%'));
    progressDiv.append(progressInfo);

    const progressBar = $('<progress class="progress w-full h-2">');
    progressBar.addClass(progressPercentage >= 100 ? 'progress-success' : 'progress-primary');
    progressBar.attr('value', progressPercentage);
    progressBar.attr('max', '100');
    progressDiv.append(progressBar);

    const amountInfo = $('<div class="text-xs text-gray-400 mt-1">');
    const currentAmount = goal.currentAmount || 0;
    amountInfo.text(currentAmount + ' / ' + goal.totalAmount + ' ' + (goal.unit || ''));
    progressDiv.append(amountInfo);

    cardBody.append(progressDiv);
  }

  if (goal.targetDate) {
    const targetDate = new Date(goal.targetDate).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
    const dateDiv = $('<div class="flex items-center gap-2 text-sm text-gray-500 mb-3">');
    dateDiv.append($('<i class="fa-regular fa-calendar text-blue-500">'));
    dateDiv.append($('<span>').text(targetDate));
    cardBody.append(dateDiv);
  }

  const buttonDiv = $('<div class="flex gap-1 justify-end flex-wrap">');

  const progressBtn = $('<button class="btn btn-circle btn-sm btn-success">').attr('title', window.MSG['taskTracker.progress.addTitle']);
  progressBtn.append($('<i class="fa-solid fa-plus text-xs">'));
  progressBtn.attr('onclick', 'showAddProgressModal("' + goal.taskGoalId + '")');
  buttonDiv.append(progressBtn);

  const editBtn = $('<button class="btn btn-circle btn-sm btn-info">').attr('title', window.MSG['taskTracker.button.edit']);
  editBtn.append($('<i class="fa-solid fa-pen text-xs">'));
  editBtn.attr('onclick', 'editGoal("' + goal.taskGoalId + '")');
  buttonDiv.append(editBtn);

  if (!goal.isCompleted) {
    const completeBtn = $('<button class="btn btn-circle btn-sm btn-primary">').attr('title', window.MSG['taskTracker.button.complete']);
    completeBtn.append($('<i class="fa-solid fa-check text-xs">'));
    completeBtn.attr('onclick', 'completeGoal("' + goal.taskGoalId + '")');
    buttonDiv.append(completeBtn);

    const cancelBtn = $('<button class="btn btn-circle btn-sm btn-warning">').attr('title', window.MSG['taskTracker.button.cancel']);
    cancelBtn.append($('<i class="fa-solid fa-ban text-xs">'));
    cancelBtn.attr('onclick', 'cancelGoal("' + goal.taskGoalId + '")');
    buttonDiv.append(cancelBtn);
  }

  const deleteBtn = $('<button class="btn btn-circle btn-sm btn-error">').attr('title', window.MSG['taskTracker.button.delete']);
  deleteBtn.append($('<i class="fa-solid fa-trash text-xs">'));
  deleteBtn.attr('onclick', 'deleteGoal("' + goal.taskGoalId + '")');
  buttonDiv.append(deleteBtn);

  cardBody.append(buttonDiv);
  card.append(cardBody);

  card.on('click', function(e) {
    if (!$(e.target).closest('button').length) {
      showGoalDetail(goal.taskGoalId);
    }
  });

  return card;
}

/**
 * 색상 클래스 반환
 */
function getColorClass(color) {
  const colorMap = {
    'blue': 'text-blue-500',
    'green': 'text-green-500',
    'red': 'text-red-500',
    'yellow': 'text-yellow-500',
    'purple': 'text-purple-500',
    'pink': 'text-pink-500',
    'orange': 'text-orange-500',
    'gray': 'text-gray-500'
  };
  return colorMap[color] || 'text-blue-500';
}

/**
 * Goal 선택 드롭다운 업데이트
 */
function updateGoalSelects(goals) {
  const filterSelect = $('#progressGoalFilter');
  const progressSelect = $('#progressGoalId');

  filterSelect.find('option:not(:first)').remove();
  progressSelect.find('option:not(:first)').remove();

  goals.forEach(function(goal) {
    if (goal.isActive && !goal.isCompleted) {
      filterSelect.append($('<option>').val(goal.taskGoalId).text(goal.title));
      progressSelect.append($('<option>').val(goal.taskGoalId).text(goal.title));
    }
  });
}

/**
 * Goal 추가 모달 표시
 */
function showAddGoalModal() {
  $('#goalForm')[0].reset();
  $('#goalId').val('');
  $('#goalModalTitle').html('<i class="fa-solid fa-bullseye text-blue-500"></i> ' + window.MSG['taskTracker.button.addTask']);
  setDefaultDate();
  document.getElementById('goalModal').showModal();
}

/**
 * Goal 수정
 */
function editGoal(goalId) {
  const goal = goals.find(g => g.taskGoalId === goalId);
  if (!goal) {
    showToast(window.MSG['taskTracker.toast.taskNotFound'], 'negative');
    return;
  }

  $('#goalId').val(goal.taskGoalId);
  $('#goalTitle').val(goal.title);
  $('#goalDescription').val(goal.description || '');

  if (goal.targetDate) {
    const targetDate = new Date(goal.targetDate).toISOString().split('T')[0];
    $('#goalTargetDate').val(targetDate);
  }

  $('#goalTotalAmount').val(goal.totalAmount || '');
  $('#goalUnit').val(goal.unit || '페이지');
  $('#goalIcon').val(goal.icon || 'fa-solid fa-bullseye');
  $('#goalColor').val(goal.color || 'blue');

  $('#goalModalTitle').html('<i class="fa-solid fa-pen text-blue-500"></i> ' + window.MSG['taskTracker.goal.editTitle']);
  document.getElementById('goalModal').showModal();
}

/**
 * Goal 저장
 */
function saveGoal() {
  const formData = new FormData($('#goalForm')[0]);

  const goalId = $('#goalId').val();
  const url = goalId ? '/api/task/goal/update' : '/api/task/goal/create';

  sendFormRequest(url, Object.fromEntries(formData),
    function(response) {
      showToast(goalId ? window.MSG['taskTracker.toast.taskUpdated'] : window.MSG['taskTracker.toast.taskCreated'], 'positive');
      document.getElementById('goalModal').close();
      loadGoals();
    },
    function(xhr, status, error) {
      let errorMessage = window.MSG['taskTracker.toast.taskSaveFailed'];
      if (xhr.responseJSON && xhr.responseJSON.message) {
        errorMessage = xhr.responseJSON.message;
      }
      showToast(errorMessage, 'negative');
    }
  );
}

/**
 * Goal 완료 처리
 */
function completeGoal(goalId) {
  if (!confirm(window.MSG['taskTracker.confirm.complete'])) {
    return;
  }

  sendFormRequest('/api/task/goal/complete', { taskGoalId: goalId },
    function(response) {
      showToast(window.MSG['taskTracker.toast.taskCompleted'], 'positive');
      loadGoals();
    },
    function(xhr, status, error) {
      showToast(window.MSG['taskTracker.toast.completeFailed'], 'negative');
    }
  );
}

/**
 * Goal 취소 처리
 */
function cancelGoal(goalId) {
  if (!confirm(window.MSG['taskTracker.confirm.cancel'])) {
    return;
  }

  sendFormRequest('/api/task/goal/cancel', { taskGoalId: goalId },
    function(response) {
      showToast(window.MSG['taskTracker.toast.taskCancelled'], 'positive');
      loadGoals();
    },
    function(xhr, status, error) {
      showToast(window.MSG['taskTracker.toast.cancelFailed'], 'negative');
    }
  );
}

/**
 * Goal 삭제
 */
function deleteGoal(goalId) {
  if (!confirm(window.MSG['taskTracker.confirm.deleteTask'])) {
    return;
  }

  sendFormRequest('/api/task/goal/delete', { taskGoalId: goalId },
    function(response) {
      showToast(window.MSG['taskTracker.toast.taskDeleted'], 'positive');
      loadGoals();
      loadProgress();
    },
    function(xhr, status, error) {
      showToast(window.MSG['taskTracker.toast.deleteFailed'], 'negative');
    }
  );
}

/**
 * Goal 상세 보기
 */
function showGoalDetail(goalId) {
  const detailContent = $('#goalDetailContent');
  detailContent.html('<div class="flex justify-center py-8"><span class="loading loading-spinner loading-lg text-primary"></span></div>');
  document.getElementById('goalDetailModal').showModal();

  sendFormRequest('/api/task/goal/detail', { taskGoalId: goalId },
    function(response) {
      const goal = response.goal;
      if (!goal) {
        detailContent.html('<p class="text-center text-gray-500">' + window.MSG['taskTracker.toast.taskNotFound'] + '</p>');
        return;
      }

      renderGoalDetail(goal, response.progressList || []);
    },
    function(xhr, status, error) {
      detailContent.html('<p class="text-center text-red-500">' + window.MSG['taskTracker.toast.loadFailed'] + '</p>');
    }
  );
}

/**
 * Goal 상세 렌더링
 */
function renderGoalDetail(goal, progressList) {
  const detailContent = $('#goalDetailContent');
  const colorClass = getColorClass(goal.color || 'blue');
  const icon = goal.icon || 'fa-solid fa-bullseye';

  const daysRemaining = goal.daysRemaining || 0;
  let ddayText = daysRemaining > 0 ? 'D-' + daysRemaining : (daysRemaining === 0 ? 'D-Day' : 'D+' + Math.abs(daysRemaining));

  let html = `
    <div class="flex items-start gap-4 mb-6">
      <div class="text-4xl ${colorClass}"><i class="${icon}"></i></div>
      <div class="flex-1">
        <h3 class="text-2xl font-bold text-gray-800">${escapeHtml(goal.title)}</h3>
        ${goal.description ? `<p class="text-gray-500 mt-1">${escapeHtml(goal.description)}</p>` : ''}
      </div>
      <div class="text-3xl font-bold ${daysRemaining <= 7 ? 'text-red-500' : 'text-blue-500'}">${ddayText}</div>
    </div>
  `;

  html += `
    <div class="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
      <div class="stat bg-base-200 rounded-lg p-4">
        <div class="stat-title text-xs">${window.MSG['taskTracker.label.targetDate']}</div>
        <div class="stat-value text-lg">${goal.targetDate ? new Date(goal.targetDate).toLocaleDateString('ko-KR') : '-'}</div>
      </div>
      <div class="stat bg-base-200 rounded-lg p-4">
        <div class="stat-title text-xs">${window.MSG['taskTracker.label.progressRate']}</div>
        <div class="stat-value text-lg">${goal.progressPercentage || 0}%</div>
      </div>
      <div class="stat bg-base-200 rounded-lg p-4">
        <div class="stat-title text-xs">${window.MSG['taskTracker.label.totalAmount']}</div>
        <div class="stat-value text-lg">${goal.currentAmount || 0}/${goal.totalAmount || 0}</div>
        <div class="stat-desc">${goal.unit || ''}</div>
      </div>
    </div>
  `;

  if (goal.totalAmount > 0) {
    html += `
      <div class="mb-6">
        <progress class="progress ${(goal.progressPercentage || 0) >= 100 ? 'progress-success' : 'progress-primary'} w-full h-4"
                  value="${goal.progressPercentage || 0}" max="100"></progress>
      </div>
    `;
  }

  if (progressList && progressList.length > 0) {
    html += `
      <div class="divider">${window.MSG['taskTracker.detail.recentProgress']}</div>
      <div class="overflow-x-auto max-h-64">
        <table class="table table-sm">
          <thead>
            <tr>
              <th>${window.MSG['taskTracker.table.date']}</th>
              <th>${window.MSG['taskTracker.table.content']}</th>
              <th>${window.MSG['taskTracker.table.range']}</th>
            </tr>
          </thead>
          <tbody>
    `;

    progressList.slice(0, 10).forEach(function(p) {
      const progressDate = p.progressDate ? new Date(p.progressDate).toLocaleDateString('ko-KR') : '-';
      const range = (p.startAmount != null && p.endAmount != null) ? `${p.startAmount} ~ ${p.endAmount}` : '-';
      html += `
        <tr>
          <td>${progressDate}</td>
          <td>${escapeHtml(p.content || '-')}</td>
          <td>${range}</td>
        </tr>
      `;
    });

    html += `
          </tbody>
        </table>
      </div>
    `;
  }

  detailContent.html(html);
}

/**
 * 진행 기록 로드
 */
function loadProgress() {
  const goalId = $('#progressGoalFilter').val();
  const params = goalId ? { taskGoalId: goalId } : {};

  sendFormRequest('/api/task/progress/list', params,
    function(response) {
      renderProgress(response.progressList || []);
    },
    function(xhr, status, error) {
      console.error('진행 기록 로드 실패:', error);
      $('#progressTable').html('<tr><td colspan="7" class="text-center py-8 text-gray-500">' + window.MSG['taskTracker.toast.loadFailed'] + '</td></tr>');
    }
  );
}

/**
 * 진행 기록 렌더링
 */
function renderProgress(progressList) {
  const tableBody = $('#progressTable');
  const cardContainer = $('#progressCards');
  const emptyMessage = $('#emptyProgressMessage');

  if (!progressList || progressList.length === 0) {
    tableBody.closest('table').addClass('hidden');
    cardContainer.html('<div class="text-center py-8 text-gray-500">' + window.MSG['taskTracker.empty.noProgress'] + '</div>');
    emptyMessage.removeClass('hidden');
    return;
  }

  tableBody.closest('table').removeClass('hidden');
  emptyMessage.addClass('hidden');
  tableBody.empty();
  cardContainer.empty();

  progressList.forEach(function(progress) {
    const progressDate = progress.progressDate ? new Date(progress.progressDate).toLocaleDateString('ko-KR') : '-';
    const goalTitle = progress.taskGoalTitle || progress.goalTitle || '-';
    const range = (progress.startAmount != null && progress.endAmount != null)
      ? progress.startAmount + ' ~ ' + progress.endAmount
      : '-';

    // 데스크탑 테이블 행
    const row = $('<tr>');
    row.append($('<td>').text(progressDate));
    row.append($('<td>').append($('<span class="badge badge-ghost badge-sm">').text(goalTitle)));
    row.append($('<td>').text(progress.content || '-'));
    row.append($('<td>').text(range));

    const memoCell = $('<td>');
    if (progress.memo) {
      memoCell.append($('<span class="truncate max-w-32 inline-block" title="' + escapeHtml(progress.memo) + '">').text(progress.memo));
    } else {
      memoCell.text('-');
    }
    row.append(memoCell);

    const actionCell = $('<td>');
    const deleteBtn = $('<button class="btn btn-xs btn-ghost text-red-500">').attr('title', window.MSG['taskTracker.button.delete']);
    deleteBtn.append($('<i class="fa-solid fa-trash">'));
    deleteBtn.attr('onclick', 'deleteProgress("' + progress.taskProgressId + '")');
    actionCell.append(deleteBtn);
    row.append(actionCell);

    tableBody.append(row);

    // 모바일 카드
    const card = $('<div class="card bg-base-100 border border-gray-200 shadow-sm">');
    const cardBody = $('<div class="card-body p-3">');

    const headerRow = $('<div class="flex justify-between items-start mb-2">');
    headerRow.append($('<span class="badge badge-ghost badge-sm">').text(goalTitle));
    headerRow.append($('<span class="text-xs text-gray-400">').text(progressDate));
    cardBody.append(headerRow);

    if (progress.content) {
      cardBody.append($('<p class="text-sm font-medium text-gray-800 mb-2">').text(progress.content));
    }

    const infoRow = $('<div class="flex flex-wrap gap-2 text-xs text-gray-500 mb-2">');
    if (range !== '-') {
      infoRow.append($('<span class="badge badge-outline badge-xs">').text(window.MSG['taskTracker.label.rangePrefix'] + range));
    }
    cardBody.append(infoRow);

    if (progress.memo) {
      cardBody.append($('<p class="text-xs text-gray-400 line-clamp-2">').text(progress.memo));
    }

    const actionRow = $('<div class="flex justify-end mt-2">');
    const mobileDeleteBtn = $('<button class="btn btn-xs btn-ghost text-red-500">');
    mobileDeleteBtn.append($('<i class="fa-solid fa-trash">'));
    mobileDeleteBtn.append(' ' + window.MSG['taskTracker.button.delete']);
    mobileDeleteBtn.attr('onclick', 'deleteProgress("' + progress.taskProgressId + '")');
    actionRow.append(mobileDeleteBtn);
    cardBody.append(actionRow);

    card.append(cardBody);
    cardContainer.append(card);
  });
}

/**
 * 진행 기록 추가 모달 표시
 */
function showAddProgressModal(goalId) {
  $('#progressForm')[0].reset();
  $('#progressId').val('');
  $('#progressModalTitle').html('<i class="fa-solid fa-clock text-blue-500"></i> ' + window.MSG['taskTracker.progress.addTitle']);

  const today = new Date().toISOString().split('T')[0];
  $('#progressDate').val(today);

  if (goalId) {
    $('#progressGoalId').val(goalId);
    const goal = goals.find(g => g.taskGoalId === goalId);
    if (goal && goal.totalAmount > 0) {
      const lastEnd = goal.currentAmount || 0;
      initRangeSlider(goal.totalAmount, lastEnd, lastEnd);
    } else {
      initRangeSlider(100, 0, 0);
    }
  } else {
    initRangeSlider(100, 0, 0);
  }

  document.getElementById('progressModal').showModal();
}

/**
 * 진행 기록 저장
 */
function saveProgress() {
  const formData = new FormData($('#progressForm')[0]);
  formData.set('isCompleted', $('#progressIsCompleted').is(':checked'));

  sendFormRequest('/api/task/progress/save', Object.fromEntries(formData),
    function(response) {
      showToast(window.MSG['taskTracker.toast.progressSaved'], 'positive');
      document.getElementById('progressModal').close();
      loadGoals();
      loadProgress();
    },
    function(xhr, status, error) {
      let errorMessage = window.MSG['taskTracker.toast.saveFailed'];
      if (xhr.responseJSON && xhr.responseJSON.message) {
        errorMessage = xhr.responseJSON.message;
      }
      showToast(errorMessage, 'negative');
    }
  );
}

/**
 * 진행 기록 삭제
 */
function deleteProgress(progressId) {
  if (!confirm(window.MSG['taskTracker.confirm.deleteProgress'])) {
    return;
  }

  sendFormRequest('/api/task/progress/delete', { taskProgressId: progressId },
    function(response) {
      showToast(window.MSG['taskTracker.toast.progressDeleted'], 'positive');
      loadGoals();
      loadProgress();
    },
    function(xhr, status, error) {
      showToast(window.MSG['taskTracker.toast.deleteFailed'], 'negative');
    }
  );
}

/**
 * HTML 이스케이프 (XSS 방지)
 */
function escapeHtml(text) {
  if (typeof text !== 'string') return text;
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  };
  return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

/**
 * Range Slider 초기화
 */
function initRangeSlider(maxValue, startValue, endValue) {
  const sliderStart = $('#rangeSliderStart');
  const sliderEnd = $('#rangeSliderEnd');
  const rangeDisplay = $('#rangeDisplay');
  const rangeHighlight = $('#rangeHighlight');
  const rangeMin = $('#rangeMin');
  const rangeMax = $('#rangeMax');

  const max = maxValue || 100;
  sliderStart.attr('max', max);
  sliderEnd.attr('max', max);
  rangeMax.text(max);

  sliderStart.val(startValue || 0);
  sliderEnd.val(endValue || 0);

  updateRangeDisplay();

  sliderStart.off('input').on('input', function() {
    let startVal = parseInt($(this).val());
    let endVal = parseInt(sliderEnd.val());

    if (startVal > endVal) {
      $(this).val(endVal);
      startVal = endVal;
    }
    updateRangeDisplay();
  });

  sliderEnd.off('input').on('input', function() {
    let endVal = parseInt($(this).val());
    let startVal = parseInt(sliderStart.val());

    if (endVal < startVal) {
      $(this).val(startVal);
      endVal = startVal;
    }
    updateRangeDisplay();
  });
}

/**
 * Range 디스플레이 업데이트
 */
function updateRangeDisplay() {
  const sliderStart = $('#rangeSliderStart');
  const sliderEnd = $('#rangeSliderEnd');
  const rangeDisplay = $('#rangeDisplay');
  const rangeHighlight = $('#rangeHighlight');

  const startVal = parseInt(sliderStart.val()) || 0;
  const endVal = parseInt(sliderEnd.val()) || 0;
  const max = parseInt(sliderStart.attr('max')) || 100;

  rangeDisplay.text(startVal + ' ~ ' + endVal);

  const startPercent = (startVal / max) * 100;
  const endPercent = (endVal / max) * 100;

  rangeHighlight.css({
    'left': startPercent + '%',
    'width': (endPercent - startPercent) + '%'
  });
}
