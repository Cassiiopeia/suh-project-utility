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
  useStreaming: true,  // 스트리밍 모드 사용 여부
  currentEventSource: null,  // 현재 SSE 연결
  streamingTextMap: {},  // 스트리밍 중인 원본 텍스트 저장 (messageId -> text)

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

    // 닫기 버튼 클릭
    $(document).on('click', '#chatbot-close-btn', function() {
      self.toggle();
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
      $('body').addClass('chatbot-open');
      $('#chatbot-input').focus();
    } else {
      $('.chatbot-panel').removeClass('active');
      $('.chatbot-fab').removeClass('active');
      $('body').removeClass('chatbot-open');
    }
  },

  /**
   * 메시지 전송 (스트리밍/비스트리밍 자동 선택)
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

    // 웰컴 메시지 숨기기
    $('.chatbot-welcome').hide();

    // 스트리밍 모드 사용 여부에 따라 분기
    if (this.useStreaming) {
      this.sendMessageStream(message);
    } else {
      this.sendMessageNonStream(message);
    }
  },

  /**
   * 스트리밍 메시지 전송 (SSE)
   */
  sendMessageStream: function(message) {
    const self = this;

    // 타이핑 인디케이터 대신 빈 AI 메시지 버블 생성
    const aiMessageId = 'ai-stream-' + Date.now();
    this.appendStreamingMessage(aiMessageId);

    // API 요청
    this.isLoading = true;
    this.streamCompleted = false;  // 스트리밍 완료 플래그
    this.hasReceivedData = false;  // 데이터 수신 플래그
    $('#chatbot-send-btn').prop('disabled', true);

    // SSE URL 구성
    const params = new URLSearchParams({
      message: message,
      topK: 3,
      minScore: 0.5
    });
    if (this.sessionToken) {
      params.append('sessionToken', this.sessionToken);
    }

    const url = '/api/chatbot/chat/stream?' + params.toString();

    // 기존 연결이 있으면 종료
    if (this.currentEventSource) {
      this.currentEventSource.close();
    }

    // SSE 연결
    const eventSource = new EventSource(url);
    this.currentEventSource = eventSource;

    // 연결 확인 이벤트 (세션 토큰 수신)
    eventSource.addEventListener('connected', function(e) {
      console.log('🔗 SSE 연결 확인:', e.data);
      try {
        // 세션 토큰 파싱 및 저장
        const data = JSON.parse(e.data);
        if (data.sessionToken) {
          self.sessionToken = data.sessionToken;
          localStorage.setItem('chatbot_session_token', data.sessionToken);
          console.log('✅ 세션 토큰 저장됨:', data.sessionToken);
        }
      } catch (parseError) {
        // 레거시 포맷 지원 (단순 문자열)
        console.log('SSE 연결 (레거시):', e.data);
      }
    });

    // Thinking 이벤트 (Agent 단계 진행 상황)
    eventSource.addEventListener('thinking', function(e) {
      try {
        const event = JSON.parse(e.data);
        console.log('💭 Thinking 이벤트:', event);
        self.handleThinkingEvent(aiMessageId, event);
      } catch (parseError) {
        console.error('Thinking 이벤트 파싱 오류:', parseError);
      }
    });

    // 메시지 수신
    eventSource.addEventListener('message', function(e) {
      // 첫 메시지 수신 시 thinking 패널 숨기기
      if (!self.hasReceivedData) {
        self.hideThinkingPanel(aiMessageId);
      }
      self.hasReceivedData = true;
      try {
        // JSON 형식으로 감싸진 데이터 파싱 (공백 보존을 위해)
        const data = JSON.parse(e.data);
        const chunk = data.text;
        console.log('SSE 메시지 수신:', chunk.substring(0, 50));
        self.appendToStreamingMessage(aiMessageId, chunk);
      } catch (parseError) {
        // JSON 파싱 실패 시 원본 데이터 사용 (하위 호환성)
        console.log('SSE 메시지 수신 (raw):', e.data.substring(0, 50));
        self.appendToStreamingMessage(aiMessageId, e.data);
      }
    });

    // 완료 이벤트
    eventSource.addEventListener('done', function(e) {
      self.streamCompleted = true;
      eventSource.close();
      self.currentEventSource = null;
      self.isLoading = false;
      $('#chatbot-send-btn').prop('disabled', false);
      self.finalizeStreamingMessage(aiMessageId);
      console.log('Streaming completed');
    });

    // 에러 이벤트
    eventSource.addEventListener('error', function(e) {
      // 이미 완료된 경우 무시 (정상 종료 후 error 이벤트가 발생할 수 있음)
      if (self.streamCompleted) {
        return;
      }

      eventSource.close();
      self.currentEventSource = null;
      self.isLoading = false;
      $('#chatbot-send-btn').prop('disabled', false);

      // 데이터를 한 번도 받지 못했을 때만 에러 메시지 표시
      const bubble = document.querySelector('#' + aiMessageId + ' .bubble');
      if (bubble && !self.hasReceivedData) {
        bubble.innerHTML = '<span class="error-message"><i class="fa-solid fa-circle-exclamation"></i> 죄송합니다. 응답 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.</span>';
        console.error('SSE error:', e);
      } else if (bubble) {
        // 데이터를 받았지만 완료되지 않은 경우 - 현재 내용 유지하고 마무리
        self.finalizeStreamingMessage(aiMessageId);
      }
    });
  },

  /**
   * 비스트리밍 메시지 전송
   */
  sendMessageNonStream: function(message) {
    const self = this;

    // 타이핑 인디케이터 표시
    this.showTypingIndicator();

    // API 요청
    this.isLoading = true;
    $('#chatbot-send-btn').prop('disabled', true);

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
   * 스트리밍용 빈 메시지 버블 생성
   */
  appendStreamingMessage: function(messageId) {
    const messagesContainer = $('#chatbot-messages');
    const time = new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });

    // 원본 텍스트 저장소 초기화
    this.streamingTextMap[messageId] = '';

    const html = `
      <div class="chat-message assistant" id="${messageId}">
        <div class="thinking-panel">
          <div class="thinking-steps">
            <div class="thinking-step" data-step="1">
              <span class="step-icon"><i class="fa-solid fa-magnifying-glass-chart"></i></span>
              <span class="step-text">질문 분석</span>
              <span class="step-status"></span>
            </div>
            <div class="thinking-step" data-step="2">
              <span class="step-icon"><i class="fa-solid fa-book-open"></i></span>
              <span class="step-text">문서 검색</span>
              <span class="step-status"></span>
            </div>
            <div class="thinking-step" data-step="3">
              <span class="step-icon"><i class="fa-solid fa-pen-to-square"></i></span>
              <span class="step-text">응답 생성</span>
              <span class="step-status"></span>
            </div>
          </div>
          <div class="thinking-detail"></div>
        </div>
        <div class="bubble hide"></div>
        <div class="time">${time}</div>
      </div>
    `;

    messagesContainer.append(html);
    this.scrollToBottom();
  },

  /**
   * Thinking 이벤트 처리
   */
  handleThinkingEvent: function(messageId, event) {
    const stepElement = $(`#${messageId} .thinking-step[data-step="${event.step}"]`);
    const detailElement = $(`#${messageId} .thinking-detail`);

    if (!stepElement.length) return;

    // 이전 단계들 완료 처리 (네트워크 지연으로 completed 이벤트 누락 시 대비)
    if (event.status === 'in_progress') {
      $(`#${messageId} .thinking-step`).each(function() {
        const currentStep = parseInt($(this).data('step'));
        if (currentStep < event.step && !$(this).hasClass('completed') && !$(this).hasClass('skipped')) {
          $(this).removeClass('active retrying').addClass('completed');
          $(this).find('.step-status').html('<i class="fa-solid fa-check"></i>');
        }
      });
    }

    // 현재 단계 상태 업데이트
    stepElement.removeClass('active completed retrying skipped');

    switch (event.status) {
      case 'in_progress':
        stepElement.addClass('active');
        break;
      case 'completed':
        stepElement.addClass('completed');
        break;
      case 'retrying':
        stepElement.addClass('retrying');
        break;
      case 'skipped':
        stepElement.addClass('skipped');
        break;
    }

    // 상태 아이콘 업데이트
    const statusIcon = this.getStatusIcon(event.status);
    stepElement.find('.step-status').html(statusIcon);

    // 상세 정보 표시
    let detailHtml = '';
    if (event.title) {
      detailHtml += `<span class="detail-title">${this.escapeHtml(event.title)}</span>`;
    }
    if (event.detail) {
      detailHtml += `<span class="detail-info">${this.escapeHtml(event.detail)}</span>`;
    }
    if (event.searchQuery && event.step === 2) {
      detailHtml += `<span class="detail-query"><i class="fa-solid fa-search"></i> ${this.escapeHtml(event.searchQuery)}</span>`;
    }
    detailElement.html(detailHtml);

    this.scrollToBottom();
  },

  /**
   * 상태별 아이콘 반환
   */
  getStatusIcon: function(status) {
    switch (status) {
      case 'in_progress':
        return '<i class="fa-solid fa-spinner fa-spin"></i>';
      case 'completed':
        return '<i class="fa-solid fa-check"></i>';
      case 'retrying':
        return '<i class="fa-solid fa-rotate"></i>';
      case 'skipped':
        return '<i class="fa-solid fa-forward"></i>';
      default:
        return '';
    }
  },

  /**
   * Thinking 패널 숨기기 및 버블 표시
   */
  hideThinkingPanel: function(messageId) {
    const thinkingPanel = $(`#${messageId} .thinking-panel`);
    const bubble = $(`#${messageId} .bubble`);

    if (!thinkingPanel.length) return;

    thinkingPanel.addClass('fade-out');
    setTimeout(() => {
      const panel = $(`#${messageId} .thinking-panel`);
      const bbl = $(`#${messageId} .bubble`);
      if (panel.length) panel.addClass('hide');
      if (bbl.length) bbl.removeClass('hide');
    }, 300);
  },

  /**
   * 스트리밍 메시지에 텍스트 추가
   */
  appendToStreamingMessage: function(messageId, chunk) {
    const bubble = document.querySelector('#' + messageId + ' .bubble');
    if (bubble) {
      // hide 클래스가 있으면 제거 (thinking 패널 → 버블 전환)
      bubble.classList.remove('hide');

      // 분석 중 인디케이터 제거
      const analyzingIndicator = bubble.querySelector('.analyzing-indicator');
      if (analyzingIndicator) {
        analyzingIndicator.remove();
      }

      // 원본 텍스트에 청크 추가 (DOM에서 읽지 않고 별도 저장소 사용)
      this.streamingTextMap[messageId] = (this.streamingTextMap[messageId] || '') + chunk;
      const fullText = this.streamingTextMap[messageId];

      // 스트리밍 중에는 간단한 렌더링만 (줄바꿈 + 커서)
      bubble.innerHTML = this.formatMessageStreaming(this.escapeHtml(fullText)) + '<span class="streaming-cursor">|</span>';

      this.scrollToBottom();
    }
  },

  /**
   * 스트리밍 메시지 완료 처리
   */
  finalizeStreamingMessage: function(messageId) {
    const bubble = document.querySelector('#' + messageId + ' .bubble');
    if (bubble) {
      // 분석 중 인디케이터 제거
      const analyzingIndicator = bubble.querySelector('.analyzing-indicator');
      if (analyzingIndicator) {
        analyzingIndicator.remove();
      }

      // 원본 텍스트에서 최종 마크다운 렌더링
      const fullText = this.streamingTextMap[messageId] || '';
      bubble.innerHTML = this.formatMessage(this.escapeHtml(fullText));

      // 메모리 정리
      delete this.streamingTextMap[messageId];
    }
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
   * 스트리밍 중 메시지 포맷팅 (실시간 마크다운 렌더링)
   */
  formatMessageStreaming: function(text) {
    if (!text) return '';

    // 스트리밍 중에도 마크다운 실시간 적용
    return this.formatMessage(text);
  },

  /**
   * 메시지 포맷팅 (마크다운 → HTML)
   */
  formatMessage: function(text) {
    if (!text) return '';

    let result = text;

    // 코드 블록 (```code```) - 먼저 처리
    result = result.replace(/```(\w*)\n?([\s\S]*?)```/g, function(match, lang, code) {
      return '<pre class="code-block"><code>' + code.trim() + '</code></pre>';
    });

    // 인라인 코드 (`code`) - 코드 블록 처리 후
    result = result.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');

    // 헤더 (### 제목)
    result = result.replace(/^### (.+)$/gm, '<strong class="chat-h3">$1</strong>');
    result = result.replace(/^## (.+)$/gm, '<strong class="chat-h2">$1</strong>');
    result = result.replace(/^# (.+)$/gm, '<strong class="chat-h1">$1</strong>');

    // 볼드 (**text**)
    result = result.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');

    // 이탤릭 (*text*) - 볼드 처리 후
    result = result.replace(/\*([^*]+)\*/g, '<em>$1</em>');

    // 번호 리스트 (1. 2. 3.)
    result = result.replace(/^(\d+)\. (.+)$/gm, '<div class="chat-list-item"><span class="list-number">$1.</span> $2</div>');

    // 불릿 리스트 (- item)
    result = result.replace(/^- (.+)$/gm, '<div class="chat-list-item"><span class="list-bullet">•</span> $1</div>');

    // 줄바꿈 (마지막에 처리)
    result = result.replace(/\n/g, '<br>');

    return result;
  }
};

// 페이지 로드 시 초기화
$(document).ready(function() {
  ChatbotWidget.init();
});
