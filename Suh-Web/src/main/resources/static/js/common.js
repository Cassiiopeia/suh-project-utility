// src/main/resources/static/js/common.js
/**
 * showToast(message, duration)
 * @param {string} message   - 표시할 메시지
 * @param {number} duration  - 표시 후 사라질 때까지의 밀리초 (기본 3000)
 */
function showToast(message, duration = 3000) {
  const container = document.getElementById('toast-container');
  if (!container) {
    console.warn('Toast container element not found! (id="toast-container")');
    return;
  }

  // 1) Toast DOM 생성
  const toast = document.createElement('div');
  toast.classList.add('toast'); // 위에서 정의한 .toast CSS 사용
  toast.textContent = message;

  // 2) 컨테이너에 추가
  container.appendChild(toast);

  // 3) 애니메이션 종료 후 제거
  toast.addEventListener('animationend', () => {
    container.removeChild(toast);
  });
}


// AJAX 요청에 자동으로 CSRF 토큰 추가
$(function(){
  var token = $('meta[name="_csrf"]').attr('content');
  var header = $('meta[name="_csrf_header"]').attr('content');
  $(document).ajaxSend(function(e, xhr, options){
    xhr.setRequestHeader(header, token);
  });
  
  // 세션에서 clientHash 가져와 저장
  fetchAndStoreClientHash();
});

/**
 * 서버 세션에 저장된 clientHash를 가져와 세션 스토리지에 저장
 */
function fetchAndStoreClientHash() {
  // 서버로부터 clientHash 가져오기
  var formData = new FormData();
  $.ajax({
    url: '/api/member/client-hash',
    type: 'POST',
    data: formData,
    processData: false,
    contentType: false,
    success: function(response) {
      if (response && response.clientHash) {
        // 세션 스토리지에 저장
        sessionStorage.setItem('clientHash', response.clientHash);
        console.log('Client hash stored in session storage');
      }
    },
    error: function(xhr, status, error) {
      console.log('Unable to fetch client hash');
    }
  });
}

/**
 * 공통 AJAX 요청 함수 - FormData 방식
 * @param {string} url - 요청 URL
 * @param {Object} params - 요청 파라미터 (key-value 객체)
 * @param {function} successCallback - 성공 시 콜백 함수
 * @param {function} errorCallback - 에러 시 콜백 함수
 */
function sendFormRequest(url, params, successCallback, errorCallback) {
  // FormData 객체 생성
  const formData = new FormData();
  
  // 파라미터가 있으면 FormData에 추가
  if (params) {
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        formData.append(key, params[key]);
      }
    });
  }
  
  // AJAX 요청 실행
  $.ajax({
    url: url,
    type: 'POST',
    data: formData,
    processData: false,
    contentType: false,
    success: function(response) {
      if (successCallback) {
        successCallback(response);
      }
    },
    error: function(xhr, status, error) {
      console.error('AJAX 요청 실패:', url, status, error);
      if (errorCallback) {
        errorCallback(xhr, status, error);
      } else {
        // 기본 에러 처리
        if (typeof showToast === 'function') {
          showToast('요청 처리 중 오류가 발생했습니다: ' + error);
        } else {
          console.error('요청 처리 중 오류가 발생했습니다:', error);
        }
      }
    }
  });
}

/**
 * Formats a date string for display
 * @param {string} dateString - ISO date string
 * @returns {string} Formatted date string
 */
function formatDate(dateString) {
  if (!dateString) return '';

  try {
    const date = new Date(dateString);

    // Check if date is valid
    if (isNaN(date.getTime())) {
      return dateString; // Return original if parsing fails
    }

    return date.toISOString().split('T')[0]; // Returns YYYY-MM-DD format
  } catch (e) {
    console.error('Error formatting date:', e);
    return dateString; // Return original on error
  }
}

/**
 * 날짜를 상대적 시간으로 표시 (예: "5분 전", "3일 전")
 * @param {string|Date} dateInput - 날짜 문자열 또는 Date 객체
 * @returns {string} 상대적 시간 문자열
 */
function timeAgo(dateInput) {
  if (!dateInput) return '';
  
  const date = dateInput instanceof Date ? dateInput : new Date(dateInput);
  
  // 날짜가 유효하지 않으면 원래 값 반환
  if (isNaN(date.getTime())) {
    return String(dateInput);
  }
  
  const now = new Date();
  const diffInSeconds = Math.floor((now - date) / 1000);
  
  // 1분 미만
  if (diffInSeconds < 60) {
    return '방금 전';
  }
  
  // 1시간 미만
  const diffInMinutes = Math.floor(diffInSeconds / 60);
  if (diffInMinutes < 60) {
    return `${diffInMinutes}분 전`;
  }
  
  // 1일 미만
  const diffInHours = Math.floor(diffInMinutes / 60);
  if (diffInHours < 24) {
    return `${diffInHours}시간 전`;
  }
  
  // 1주일 미만
  const diffInDays = Math.floor(diffInHours / 24);
  if (diffInDays < 7) {
    return `${diffInDays}일 전`;
  }
  
  // 1개월 미만
  if (diffInDays < 30) {
    const diffInWeeks = Math.floor(diffInDays / 7);
    return `${diffInWeeks}주 전`;
  }
  
  // 1년 미만
  const diffInMonths = Math.floor(diffInDays / 30);
  if (diffInMonths < 12) {
    return `${diffInMonths}개월 전`;
  }
  
  // 1년 이상
  const diffInYears = Math.floor(diffInMonths / 12);
  return `${diffInYears}년 전`;
}

// SHA-256 해시 함수
async function sha256(message) {
  const msgBuffer = new TextEncoder().encode(message);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

/**
 * 암호화 함수
 * @param {string} str - 암호화할 Str 값
 * @returns {Promise<string>} 암호화된 문자열
 */
async function encryptStr(str) {
  const key = CryptoJS.enc.Utf8.parse(window.key);
  const iv = CryptoJS.enc.Utf8.parse(window.iv);

  const encrypted = CryptoJS.AES.encrypt(str, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  });

  return encrypted.toString();
}
