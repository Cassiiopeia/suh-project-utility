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

  // 다크모드 테마 초기화
  initTheme();
});

/**
 * 다크모드 테마 초기화 및 localStorage 연동
 */
function initTheme() {
  const savedTheme = localStorage.getItem('theme');
  const isDark = savedTheme === 'dark';

  // HTML 태그에 data-theme 속성 설정
  if (isDark) {
    document.documentElement.setAttribute('data-theme', 'dark');
  } else {
    document.documentElement.removeAttribute('data-theme');
  }

  // 모든 테마 토글 체크박스 동기화
  const themeToggles = document.querySelectorAll('.theme-controller');
  themeToggles.forEach(function(toggle) {
    toggle.checked = isDark;
  });

  // 테마 변경 이벤트 리스너 등록
  themeToggles.forEach(function(toggle) {
    toggle.addEventListener('change', function() {
      const newTheme = this.checked ? 'dark' : 'light';
      localStorage.setItem('theme', newTheme);

      if (this.checked) {
        document.documentElement.setAttribute('data-theme', 'dark');
      } else {
        document.documentElement.removeAttribute('data-theme');
      }

      // 다른 토글 체크박스들도 동기화
      themeToggles.forEach(function(otherToggle) {
        if (otherToggle !== toggle) {
          otherToggle.checked = toggle.checked;
        }
      });
    });
  });
}

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
 * 암호화 함수 (랜덤 IV 사용)
 * @param {string} str - 암호화할 Str 값
 * @returns {Promise<string>} 암호화된 문자열 (IV + 암호문이 Base64로 인코딩됨)
 */
async function encryptStr(str) {
  const key = CryptoJS.enc.Utf8.parse(window.key);
  
  // 랜덤 IV 생성 (16바이트)
  const iv = CryptoJS.lib.WordArray.random(16);

  const encrypted = CryptoJS.AES.encrypt(str, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  });

  // IV + 암호문을 결합하여 Base64로 반환
  const combined = iv.concat(encrypted.ciphertext);
  return CryptoJS.enc.Base64.stringify(combined);
}

/**
 * 복호화 함수 (IV가 포함된 암호문 복호화)
 * @param {string} encryptedStr - 복호화할 암호화된 문자열 (IV + 암호문이 Base64로 인코딩됨)
 * @returns {Promise<string>} 복호화된 문자열
 */
async function decryptStr(encryptedStr) {
  const key = CryptoJS.enc.Utf8.parse(window.key);
  
  // Base64 디코딩
  const combined = CryptoJS.enc.Base64.parse(encryptedStr);
  
  // IV와 암호문 분리 (첫 16바이트가 IV)
  const iv = CryptoJS.lib.WordArray.create(combined.words.slice(0, 4)); // 16바이트 = 4워드
  const ciphertext = CryptoJS.lib.WordArray.create(combined.words.slice(4));
  
  const decrypted = CryptoJS.AES.decrypt(
    CryptoJS.lib.CipherParams.create({ ciphertext: ciphertext }),
    key,
    {
      iv: iv,
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }
  );

  return decrypted.toString(CryptoJS.enc.Utf8);
}

/**
 * HTML 이스케이프 함수 - XSS 공격 방지
 * @param {string} str - 이스케이프할 문자열
 * @returns {string} 이스케이프된 문자열
 */
function escapeHtml(str) {
  if (str == null) return '';
  return String(str).replace(/[&<>"'`=\/]/g, function(char) {
    return {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
      '/': '&#x2F;',
      '`': '&#x60;',
      '=': '&#x3D;'
    }[char];
  });
}

/**
 * 커밋 로그 행을 안전하게 렌더링하는 함수
 * @param {Object} log - 커밋 로그 객체
 * @returns {string} 안전하게 렌더링된 HTML 문자열
 */
function renderLogRow(log) {
  const commitTime = log.commitTime 
    ? new Date(log.commitTime).toLocaleString('ko-KR') 
    : '-';
  
  const statusLabel = log.isSuccess 
    ? '<div class="ui green label">성공</div>' 
    : '<div class="ui red label">실패</div>';
  
  const levelColor = 'level-' + escapeHtml(log.commitLevel || 0);
  const levelBlock = '<span class="contribution-level ' + levelColor + '"></span>';
  
  return [
    '<tr>',
    '<td>', escapeHtml(commitTime), '</td>',
    '<td>', escapeHtml(log.githubUsername || '-'), '</td>',
    '<td>', escapeHtml(log.repositoryName || '-'), '</td>',
    '<td>', escapeHtml(log.commitMessage || '-'), '</td>',
    '<td>', statusLabel, '</td>',
    '<td class="contribution-level-cell">', levelBlock, ' Level ', escapeHtml(log.commitLevel || 0), '</td>',
    '</tr>'
  ].join('');
}

/**
 * 커밋 로그 테이블을 안전하게 렌더링하는 함수
 * @param {Array} logs - 커밋 로그 배열
 * @param {string} tableId - 테이블 ID (기본값: 'commitLogTable')
 */
function renderCommitLogTable(logs, tableId = 'commitLogTable') {
  if (!logs || !Array.isArray(logs)) {
    console.warn('Invalid logs data provided to renderCommitLogTable');
    return;
  }
  
  const html = logs.map(renderLogRow).join('');
  $('#' + tableId).html(html);
}

// ==========================================
// 공통 유틸리티 함수들 (Grass Planter 함수들은 grassPlanter.html로 이동됨)
// ==========================================

// GrassPlanter 관련 함수들은 grassPlanter.html로 이동되었습니다.
// 공통으로 사용되는 함수들만 이곳에 남겨둡니다.

// /**
//  * 프로필 추가 모달 표시
//  */
// function showAddProfileModal() {
//   $('#profileForm')[0].reset();
//   $('#profileId').val('');
//   $('#profileModal').modal('show');
// }
// 
// /**
//  * 프로필 수정
//  */
// function editProfile(profileId) {
//   // TODO: 프로필 정보 로드 후 모달 표시
//   $('#profileId').val(profileId);
//   $('#profileModal').modal('show');
// }
// 
// /**
//  * 프로필 저장
//  */
// function saveProfile() {
//   const profileId = $('#profileId').val();
//   const url = profileId ? '/api/grass/profile/update' : '/api/grass/profile/create';
//   
//   // PAT 필드 검증
//   const pat = $('#personalAccessToken').val();
//   if (!pat || pat.trim() === '') {
//     if (typeof showToast === 'function') {
//       showToast('Personal Access Token을 입력해주세요.');
//     } else {
//       alert('Personal Access Token을 입력해주세요.');
//     }
//     return;
//   }
//   
//   // 파라미터 객체 생성
//   const params = {
//     profileId: profileId,
//     githubUsername: $('#githubUsername').val(),
//     personalAccessToken: pat,
//     repositoryFullName: $('#repositoryFullName').val(),
//     targetCommitLevel: $('#targetCommitLevel').val(),
//     ownerNickname: $('#ownerNickname').val(),
//     isActive: $('#isActive').is(':checked'),
//     isAutoCommitEnabled: $('#isAutoCommitEnabled').is(':checked')
//     // 임시 UUID 전송 제거 - 서버에서 처리하도록 함
//     // TODO: 실제 선택 UI 구성 후에만 유효한 식별자 전송
//   };
//   
//   // 공통 AJAX 함수 사용
//   sendFormRequest(url, params, 
//     function(response) {
//       if (typeof showToast === 'function') {
//         showToast('프로필이 저장되었습니다.');
//       } else {
//         alert('프로필이 저장되었습니다.');
//       }
//       location.reload();
//     },
//     function(xhr, status, error) {
//       let errorMessage = '프로필 저장 중 오류가 발생했습니다.';
//       if (xhr.responseJSON && xhr.responseJSON.message) {
//         errorMessage += ' (' + escapeHtml(xhr.responseJSON.message) + ')';
//       }
//       
//       if (typeof showToast === 'function') {
//         showToast(errorMessage);
//       } else {
//         alert(errorMessage);
//       }
//       console.error('Profile save error:', error);
//     }
//   );
// }
// 
// /**
//  * 프로필 삭제
//  */
// function deleteProfile(profileId) {
//   if (confirm('정말로 이 프로필을 삭제하시겠습니까?')) {
//     const params = { profileId: profileId };
//     
//     sendFormRequest('/api/grass/profile/delete', params,
//       function(response) {
//         if (typeof showToast === 'function') {
//           showToast('프로필이 삭제되었습니다.');
//         } else {
//           alert('프로필이 삭제되었습니다.');
//         }
//         location.reload();
//       },
//       function(xhr, status, error) {
//         let errorMessage = '프로필 삭제 중 오류가 발생했습니다.';
//         if (xhr.responseJSON && xhr.responseJSON.message) {
//           errorMessage += ' (' + escapeHtml(xhr.responseJSON.message) + ')';
//         }
//         
//         if (typeof showToast === 'function') {
//           showToast(errorMessage);
//         } else {
//           alert(errorMessage);
//         }
//         console.error('Profile delete error:', error);
//       }
//     );
//   }
// }
// 
// /**
//  * 커밋 실행 모달 표시
//  */
// function executeCommit(profileId) {
//   $('#commitProfileId').val(profileId);
//   $('#commitMessage').val('Manual commit from GrassPlanter');
//   $('#commitModal').modal('show');
// }
// 
// /**
//  * 커밋 실행 확인
//  */
// function confirmCommit() {
//   const params = {
//     profileId: $('#commitProfileId').val(),
//     commitMessage: $('#commitMessage').val()
//   };
//   
//   sendFormRequest('/api/grass/commit/execute', params,
//     function(response) {
//       if (typeof showToast === 'function') {
//         showToast('커밋이 성공적으로 실행되었습니다.');
//       } else {
//         alert('커밋이 성공적으로 실행되었습니다.');
//       }
//       $('#commitModal').modal('hide');
//       loadCommitLogs();
//     },
//     function(xhr, status, error) {
//       let errorMessage = '커밋 실행 중 오류가 발생했습니다.';
//       if (xhr.responseJSON && xhr.responseJSON.message) {
//         errorMessage += ' (' + escapeHtml(xhr.responseJSON.message) + ')';
//       }
//       
//       if (typeof showToast === 'function') {
//         showToast(errorMessage);
//       } else {
//         alert(errorMessage);
//       }
//       console.error('Commit execute error:', error);
//     }
//   );
// }
// 
// /**
//  * 커밋 로그 로드
//  */
// function loadCommitLogs() {
//   const params = {
//     page: 0,
//     size: 20
//   };
//   
//   sendFormRequest('/api/grass/commit/logs', params,
//     function(response) {
//       if (response.commitLogs && Array.isArray(response.commitLogs)) {
//         displayCommitLogs(response.commitLogs);
//       } else {
//         $('#commitLogTable').html('<tr><td colspan="6" class="center aligned">로그를 불러올 수 없습니다.</td></tr>');
//       }
//     },
//     function(xhr, status, error) {
//       console.error('커밋 로그 로드 실패:', error);
//       $('#commitLogTable').html('<tr><td colspan="6" class="center aligned">로그 로드 중 오류가 발생했습니다.</td></tr>');
//     }
//   );
// }
// 
// /**
//  * 커밋 로그 표시 (XSS 방지 적용) - jQuery를 사용한 안전한 DOM 조작
//  */
// function displayCommitLogs(logs) {
//   const tableBody = $('#commitLogTable');
//   
//   if (!logs || logs.length === 0) {
//     tableBody.html('<tr><td colspan="6" class="center aligned">커밋 로그가 없습니다.</td></tr>');
//     return;
//   }
//   
//   // 기존 내용 제거
//   tableBody.empty();
//   
//   logs.forEach(function(log) {
//     const row = $('<tr>');
//     
//     // 시간 (안전한 텍스트 삽입)
//     const timeCell = $('<td>');
//     const commitTime = log.commitTime ? new Date(log.commitTime).toLocaleString('ko-KR') : '-';
//     timeCell.text(commitTime);
//     row.append(timeCell);
//     
//     // GitHub 사용자명 (XSS 방지)
//     const usernameCell = $('<td>');
//     usernameCell.text(log.githubUsername || '-');
//     row.append(usernameCell);
//     
//     // 저장소명 (XSS 방지)
//     const repoCell = $('<td>');
//     repoCell.text(log.repositoryName || '-');
//     row.append(repoCell);
//     
//     // 커밋 메시지 (XSS 방지 - 가장 중요)
//     const messageCell = $('<td>');
//     messageCell.text(log.commitMessage || '-');
//     row.append(messageCell);
//     
//     // 상태 라벨 (HTTP 상태 기반)
//     const statusCell = $('<td>');
//     // HTTP 상태 코드나 응답 성공 여부로 판단
//     const isSuccess = log.isSuccess !== undefined ? log.isSuccess : true; // 기본값 처리
//     if (isSuccess) {
//       statusCell.html('<div class="ui green label">성공</div>');
//     } else {
//       statusCell.html('<div class="ui red label">실패</div>');
//     }
//     row.append(statusCell);
//     
//     // 레벨 표시
//     const levelCell = $('<td class="contribution-level-cell">');
//     const levelColor = 'level-' + (log.commitLevel || 0);
//     const levelBlock = $('<span class="contribution-level">').addClass(levelColor);
//     const levelText = ' Level ' + (log.commitLevel || 0);
//     levelCell.append(levelBlock).append(document.createTextNode(levelText));
//     row.append(levelCell);
//     
//     tableBody.append(row);
//   });
// }
// 
// // Grass Planter 페이지 초기화
// $(document).ready(function() {
//   // Grass Planter 페이지인지 확인
//   if ($('#main-content').hasClass('grass-planter-dashboard')) {
//     $('.ui.dropdown').dropdown();
//     $('.ui.checkbox').checkbox();
//     
//     // 페이지 로드 시 커밋 로그 로드
//     loadCommitLogs();
//   }
// });
