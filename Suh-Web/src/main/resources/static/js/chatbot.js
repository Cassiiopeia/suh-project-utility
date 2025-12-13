// src/main/resources/static/js/chatbot.js
/**
 * 챗봇 위젯 JavaScript
 * 플로팅 버튼 + 채팅 패널 + API 통신
 */

// 챗봇 상태
const ChatbotWidget = {
  sessionToken: null,
  isOpen: false,
  isLoading: false,

  /**
   * 초기화
   */
  init: function() {
    // 세션 토큰 복원 (localStorage)
    this.sessionToken = localStorage.getItem('chatbot_session_token');

    // 이벤트 바인딩
    this.bindEvents();

    console.log('Chatbot widget initialized');
  },

  /**
   * 이벤트 바인딩
   */
  bindEvents: function() {
    const self = this;

    // 플로팅 버튼 클릭
    $(document).on('click', '.chatbot-fab', function() {
      self.toggle();
    });

    // 전송 버튼 클릭
    $(document).on('click', '#chatbot-send-btn', function() {
      self.sendMessage();
    });

    // 입력 필드 Enter 키
    $(document).on('keydown', '#chatbot-input', function(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        self.sendMessage();
      }
    });

    // 입력 필드 자동 높이 조절
    $(document).on('input', '#chatbot-input', function() {
      this.style.height = 'auto';
      this.style.height = Math.min(this.scrollHeight, 120) + 'px';
    });

    // 피드백 버튼 클릭
    $(document).on('click', '.chat-feedback button', function() {
      const messageId = $(this).closest('.chat-message').data('message-id');
      const isHelpful = $(this).hasClass('btn-helpful');
      self.sendFeedback(messageId, isHelpful);

      // UI 업데이트
      $(this).siblings().removeClass('active');
      $(this).addClass('active');
    });

    // 추천 질문 버튼 클릭
    $(document).on('click', '.suggestion-btn', function() {
      const message = $(this).data('message');
      if (message) {
        $('#chatbot-input').val(message);
        self.sendMessage();
      }
    });

    // 초기화 버튼 클릭
    $(document).on('click', '#chatbot-reset-btn', function() {
      self.resetChat();
    });
  },

  /**
   * 채팅 초기화 (새 대화 시작)
   */
  resetChat: function() {
    // 세션 토큰 삭제
    this.sessionToken = null;
    localStorage.removeItem('chatbot_session_token');

    // 메시지 영역 초기화 (웰컴 메시지만 남기고 삭제)
    $('#chatbot-messages .chat-message').remove();

    // 웰컴 메시지 다시 표시
    $('.chatbot-welcome').show();

    // 입력 필드 초기화
    $('#chatbot-input').val('').css('height', 'auto');

    // 상태 초기화
    this.isLoading = false;
    $('#chatbot-send-btn').prop('disabled', false);

    console.log('Chat reset - new session');
  },

  /**
   * 챗봇 패널 토글
   */
  toggle: function() {
    this.isOpen = !this.isOpen;

    if (this.isOpen) {
      $('.chatbot-panel').addClass('active');
      $('.chatbot-fab').addClass('active');
      $('#chatbot-input').focus();
    } else {
      $('.chatbot-panel').removeClass('active');
      $('.chatbot-fab').removeClass('active');
    }
  },

  /**
   * 메시지 전송
   */
  sendMessage: function() {
    const input = $('#chatbot-input');
    const message = input.val().trim();

    if (!message || this.isLoading) {
      return;
    }

    // 입력 필드 초기화
    input.val('');
    input.css('height', 'auto');

    // 사용자 메시지 표시
    this.appendMessage('user', message);

    // 타이핑 인디케이터 표시
    this.showTypingIndicator();

    // API 요청
    this.isLoading = true;
    $('#chatbot-send-btn').prop('disabled', true);

    const self = this;
    const params = {
      sessionToken: this.sessionToken,
      message: message,
      topK: 3,
      minScore: 0.5
    };

    sendFormRequest('/api/chatbot/chat', params,
      function(response) {
        self.hideTypingIndicator();
        self.isLoading = false;
        $('#chatbot-send-btn').prop('disabled', false);

        if (response) {
          // 세션 토큰 저장
          if (response.sessionToken) {
            self.sessionToken = response.sessionToken;
            localStorage.setItem('chatbot_session_token', response.sessionToken);
          }

          // AI 응답 표시
          self.appendMessage('assistant', response.message, response.messageId, response.references);
        }
      },
      function(xhr, status, error) {
        self.hideTypingIndicator();
        self.isLoading = false;
        $('#chatbot-send-btn').prop('disabled', false);

        self.appendMessage('assistant', '죄송합니다. 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
        console.error('Chatbot API error:', error);
      }
    );
  },

  /**
   * 메시지 추가
   */
  appendMessage: function(role, content, messageId, references) {
    const messagesContainer = $('#chatbot-messages');
    const time = new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });

    // 웰컴 메시지 숨기기
    $('.chatbot-welcome').hide();

    // HTML 이스케이프 (XSS 방지)
    const escapedContent = this.escapeHtml(content);
    // 마크다운 스타일 변환 (간단한 형식)
    const formattedContent = this.formatMessage(escapedContent);

    let html = `
      <div class="chat-message ${role}" ${messageId ? `data-message-id="${messageId}"` : ''}>
        <div class="bubble">${formattedContent}</div>
        <div class="time">${time}</div>
    `;

    // 참조 문서 (AI 응답인 경우)
    if (role === 'assistant' && references && references.length > 0) {
      html += `
        <div class="chat-references">
          <div class="ref-title"><i class="fa-solid fa-book-open"></i> 참조 문서</div>
          ${references.map(ref => `
            <div class="ref-item">
              <i class="fa-solid fa-file-lines"></i> [${this.escapeHtml(ref.category)}] ${this.escapeHtml(ref.title)}
            </div>
          `).join('')}
        </div>
      `;
    }

    // 피드백 버튼 (AI 응답이고 messageId가 있는 경우)
    if (role === 'assistant' && messageId) {
      html += `
        <div class="chat-feedback">
          <button class="btn-helpful" title="도움이 됐어요">
            <i class="fa-solid fa-thumbs-up"></i>
          </button>
          <button class="btn-not-helpful" title="도움이 안됐어요">
            <i class="fa-solid fa-thumbs-down"></i>
          </button>
        </div>
      `;
    }

    html += '</div>';

    messagesContainer.append(html);

    // 스크롤 아래로
    this.scrollToBottom();
  },

  /**
   * 타이핑 인디케이터 표시
   */
  showTypingIndicator: function() {
    const html = `
      <div class="chat-message assistant" id="typing-indicator">
        <div class="typing-indicator">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    `;
    $('#chatbot-messages').append(html);
    this.scrollToBottom();
  },

  /**
   * 타이핑 인디케이터 숨기기
   */
  hideTypingIndicator: function() {
    $('#typing-indicator').remove();
  },

  /**
   * 스크롤 아래로
   */
  scrollToBottom: function() {
    const container = document.getElementById('chatbot-messages');
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  },

  /**
   * 피드백 전송
   */
  sendFeedback: function(messageId, isHelpful) {
    if (!messageId) return;

    const params = {
      messageId: messageId,
      isHelpful: isHelpful
    };

    sendFormRequest('/api/chatbot/feedback', params,
      function(response) {
        console.log('Feedback submitted:', messageId, isHelpful);
      },
      function(xhr, status, error) {
        console.error('Feedback error:', error);
      }
    );
  },

  /**
   * HTML 이스케이프 (XSS 방지)
   */
  escapeHtml: function(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  },

  /**
   * 메시지 포맷팅 (간단한 마크다운)
   */
  formatMessage: function(text) {
    if (!text) return '';

    return text
      // 줄바꿈
      .replace(/\n/g, '<br>')
      // 볼드 (**text**)
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      // 이탤릭 (*text*)
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      // 코드 (`code`)
      .replace(/`(.+?)`/g, '<code style="background:#f3f4f6;padding:2px 4px;border-radius:4px;">$1</code>');
  }
};

// 페이지 로드 시 초기화
$(document).ready(function() {
  ChatbotWidget.init();
});
