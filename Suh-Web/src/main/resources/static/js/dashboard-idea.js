/**
 * 대시보드 아이디어 빠른 입력: 키워드 Enter 저장 > 최근 5개 다시 조회 (로그인 시에만)
 */
(function() {
  const RECENT_IDEA_URL = '/api/idea/recent';
  const CREATE_IDEA_URL = '/api/idea/create';
  // 상태 > 배지 색 (Tailwind 스캔용 리터럴, ideaPocket.js 와 동일)
  const IDEA_STATUS_BADGE_CLASS = {
    IDEA: 'badge-neutral',
    IN_PROGRESS: 'badge-info',
    DONE: 'badge-success',
    DROPPED: 'badge-ghost'
  };
  let isSaving = false;

  $(document).ready(function() {
    if (!window.isAuthenticated) {
      return;
    }
    loadRecentIdeas();
    $('#dashboardIdeaKeywordInput').on('keydown', function(event) {
      // 한글 조합 중 Enter 는 무시 (조합 확정용)
      if (event.key !== 'Enter' || event.originalEvent.isComposing) {
        return;
      }
      event.preventDefault();
      saveQuickIdea($(this));
    });
  });

  function saveQuickIdea(keywordInput) {
    const keyword = keywordInput.val();
    if (!keyword || !keyword.trim()) {
      showToast(window.MSG['ideaPocket.toast.emptyKeyword'], 'warning');
      return;
    }
    if (isSaving) {
      return;
    }
    isSaving = true;
    sendFormRequest(CREATE_IDEA_URL, { keyword: keyword }, function() {
      isSaving = false;
      keywordInput.val('');
      showToast(window.MSG['ideaPocket.toast.saved'], 'positive');
      loadRecentIdeas();
    }, function() {
      isSaving = false;
    });
  }

  function loadRecentIdeas() {
    sendFormRequest(RECENT_IDEA_URL, {}, function(response) {
      renderRecentIdeas(response.ideaItems || []);
    }, function() {
      renderMessage(window.MSG['ideaPocket.toast.loadFailed']);
    });
  }

  function renderRecentIdeas(ideaItems) {
    if (ideaItems.length === 0) {
      renderMessage(window.MSG['ideaPocket.empty']);
      return;
    }
    $('#dashboardRecentIdeaList').html(ideaItems.map(function(item) {
      return '<li class="flex items-center gap-2 border border-base-300 rounded-lg px-3 py-2">'
        + '<span class="badge badge-sm ' + (IDEA_STATUS_BADGE_CLASS[item.ideaStatus] || 'badge-neutral') + '">'
        + escapeHtml(window.MSG['ideaPocket.status.' + item.ideaStatus]) + '</span>'
        + '<span class="text-sm text-base-content truncate">' + escapeHtml(item.keyword) + '</span>'
        + '</li>';
    }).join(''));
  }

  function renderMessage(message) {
    $('#dashboardRecentIdeaList').html('<li class="text-sm text-base-content/60 text-center py-2">'
      + escapeHtml(message) + '</li>');
  }
})();
