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
});

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
