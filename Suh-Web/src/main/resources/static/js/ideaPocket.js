/**
 * 아이디어 보따리: 추가 > 목록(최신순) > 화면 내 검색 > 상태 변경·수정(모달)·삭제
 */

const IDEA_STATUSES = ['IDEA', 'IN_PROGRESS', 'DONE', 'DROPPED'];
// 상태 > 배지 색 (Tailwind 스캔용 리터럴)
const IDEA_STATUS_BADGE_CLASS = {
  IDEA: 'badge-neutral',
  IN_PROGRESS: 'badge-info',
  DONE: 'badge-success',
  DROPPED: 'badge-ghost'
};

let ideaItems = [];
let ideaItemById = {};

$(document).ready(function() {
  loadIdeaItems();

  $('#ideaCreateForm').on('submit', function(event) {
    event.preventDefault();
    createIdea();
  });
  $('#refreshIdeaListButton').on('click', loadIdeaItems);
  $('#ideaSearchInput').on('input', renderIdeaItems);
  $('#saveIdeaEditButton').on('click', saveIdeaEdit);
  $('#ideaEditForm').on('submit', function(event) {
    event.preventDefault();
    saveIdeaEdit();
  });

  // 목록 버튼 (edit, delete) + 상태 선택
  $('#ideaList').on('click', '[data-action]', function() {
    const ideaItemId = $(this).closest('[data-item-id]').data('item-id');
    const action = $(this).data('action');
    if (action === 'edit') {
      openIdeaEditModal(ideaItemId);
    } else if (action === 'delete') {
      deleteIdea(ideaItemId);
    }
  });
  $('#ideaList').on('change', '[data-status-select]', function() {
    const ideaItemId = $(this).closest('[data-item-id]').data('item-id');
    changeIdeaStatus(ideaItemId, $(this).val());
  });
});

function createIdea() {
  const keyword = $('#ideaKeywordInput').val();
  if (!keyword || !keyword.trim()) {
    showToast(window.MSG['ideaPocket.toast.emptyKeyword'], 'warning');
    return;
  }
  sendFormRequest('/api/idea/create', { keyword: keyword, memo: $('#ideaMemoInput').val() }, function() {
    showToast(window.MSG['ideaPocket.toast.saved'], 'positive');
    $('#ideaKeywordInput').val('').trigger('focus');
    $('#ideaMemoInput').val('');
    loadIdeaItems();
  });
}

function loadIdeaItems() {
  sendFormRequest('/api/idea/list', {}, function(response) {
    ideaItems = response.ideaItems || [];
    ideaItemById = {};
    ideaItems.forEach(function(item) {
      ideaItemById[item.ideaItemId] = item;
    });
    renderIdeaItems();
  }, function() {
    $('#ideaList').html('<p class="text-center py-8 text-base-content/60">'
      + escapeHtml(window.MSG['ideaPocket.toast.loadFailed']) + '</p>');
  });
}

// 검색어(키워드·메모, 대소문자 무시)로 걸러서 렌더
function renderIdeaItems() {
  const listContainer = $('#ideaList');
  const emptyMessage = $('#ideaEmptyMessage');
  const searchText = ($('#ideaSearchInput').val() || '').trim().toLowerCase();
  const visibleItems = ideaItems.filter(function(item) {
    return !searchText
      || item.keyword.toLowerCase().includes(searchText)
      || (item.memo || '').toLowerCase().includes(searchText);
  });
  listContainer.empty();

  if (visibleItems.length === 0) {
    $('#ideaEmptyText').text(window.MSG[ideaItems.length === 0 ? 'ideaPocket.empty' : 'ideaPocket.emptySearch']);
    emptyMessage.removeClass('hidden');
    return;
  }
  emptyMessage.addClass('hidden');
  visibleItems.forEach(function(item) {
    listContainer.append(renderIdeaItem(item));
  });
}

function renderIdeaItem(item) {
  const memoHtml = item.memo
    ? '<p class="whitespace-pre-wrap break-all text-sm text-base-content/70">' + escapeHtml(item.memo) + '</p>'
    : '';
  return '<div class="border border-base-300 rounded-xl p-4 flex flex-col gap-2" data-item-id="'
    + escapeHtml(item.ideaItemId) + '">'
    + '<div class="flex flex-wrap items-center gap-2">'
    + renderIdeaStatusBadge(item.ideaStatus)
    + '<span class="font-semibold text-base-content break-all">' + escapeHtml(item.keyword) + '</span>'
    + '<span class="text-xs text-base-content/60 ml-auto">' + escapeHtml(formatIdeaDate(item.createdDate)) + '</span>'
    + '</div>'
    + memoHtml
    + '<div class="flex flex-wrap items-center justify-end gap-2">'
    + renderStatusSelect(item.ideaStatus)
    + '<button type="button" class="btn btn-sm btn-outline" data-action="edit">'
    + '<i class="fa-solid fa-pen"></i> ' + escapeHtml(window.MSG['ideaPocket.button.edit']) + '</button>'
    + '<button type="button" class="btn btn-sm btn-error btn-outline" data-action="delete">'
    + '<i class="fa-solid fa-trash"></i> ' + escapeHtml(window.MSG['ideaPocket.button.delete']) + '</button>'
    + '</div>'
    + '</div>';
}

function renderIdeaStatusBadge(ideaStatus) {
  return '<span class="badge badge-sm ' + (IDEA_STATUS_BADGE_CLASS[ideaStatus] || 'badge-neutral') + '">'
    + escapeHtml(window.MSG['ideaPocket.status.' + ideaStatus]) + '</span>';
}

function renderStatusSelect(currentStatus) {
  const optionsHtml = IDEA_STATUSES.map(function(status) {
    return '<option value="' + status + '"' + (status === currentStatus ? ' selected' : '') + '>'
      + escapeHtml(window.MSG['ideaPocket.status.' + status]) + '</option>';
  }).join('');
  return '<select class="select select-sm w-auto" data-status-select>' + optionsHtml + '</select>';
}

// yyyy-MM-ddTHH:mm:ss > yyyy-MM-dd HH:mm
function formatIdeaDate(dateText) {
  return dateText ? dateText.replace('T', ' ').substring(0, 16) : '';
}

// 상태만 변경 (키워드·메모는 기존 값 그대로 전송)
function changeIdeaStatus(ideaItemId, ideaStatus) {
  const item = ideaItemById[ideaItemId];
  updateIdea({
    ideaItemId: ideaItemId,
    keyword: item.keyword,
    memo: item.memo,
    ideaStatus: ideaStatus
  });
}

function openIdeaEditModal(ideaItemId) {
  const item = ideaItemById[ideaItemId];
  $('#editIdeaItemId').val(ideaItemId);
  $('#editIdeaKeyword').val(item.keyword);
  $('#editIdeaMemo').val(item.memo || '');
  $('#editIdeaStatus').val(item.ideaStatus);
  document.getElementById('ideaEditModal').showModal();
}

function saveIdeaEdit() {
  const keyword = $('#editIdeaKeyword').val();
  if (!keyword || !keyword.trim()) {
    showToast(window.MSG['ideaPocket.toast.emptyKeyword'], 'warning');
    return;
  }
  updateIdea({
    ideaItemId: $('#editIdeaItemId').val(),
    keyword: keyword,
    memo: $('#editIdeaMemo').val(),
    ideaStatus: $('#editIdeaStatus').val()
  }, function() {
    document.getElementById('ideaEditModal').close();
  });
}

function updateIdea(params, onUpdated) {
  sendFormRequest('/api/idea/update', params, function() {
    showToast(window.MSG['ideaPocket.toast.updated'], 'positive');
    if (onUpdated) {
      onUpdated();
    }
    loadIdeaItems();
  }, loadIdeaItems);
}

function deleteIdea(ideaItemId) {
  if (!confirm(window.MSG['ideaPocket.confirm.delete'])) {
    return;
  }
  sendFormRequest('/api/idea/delete', { ideaItemId: ideaItemId }, function() {
    showToast(window.MSG['ideaPocket.toast.deleted'], 'positive');
    loadIdeaItems();
  });
}
