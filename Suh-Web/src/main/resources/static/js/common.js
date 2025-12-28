// src/main/resources/static/js/common.js
/**
 * showToast(message, typeOrDuration, duration)
 * DaisyUI 5 방식의 Toast 메시지 표시
 * @param {string} message   - 표시할 메시지
 * @param {string|number} typeOrDuration - 메시지 타입 ('positive', 'negative', 'warning', 'info') 또는 duration (밀리초)
 * @param {number} duration  - 표시 후 사라질 때까지의 밀리초 (기본 3000)
 */
function showToast(message, typeOrDuration, duration) {
  // 파라미터 타입에 따라 type과 duration 결정
  let type = 'info';
  let finalDuration = 3000;
  
  if (typeof typeOrDuration === 'string') {
    // 두 번째 파라미터가 문자열이면 타입으로 인식
    type = typeOrDuration;
    finalDuration = duration || 3000;
  } else if (typeof typeOrDuration === 'number') {
    // 두 번째 파라미터가 숫자면 duration으로 인식 (기존 호환성)
    finalDuration = typeOrDuration;
  } else if (duration !== undefined) {
    finalDuration = duration;
  }

  // Toast 컨테이너 찾기 또는 생성 (DaisyUI 5 방식)
  let container = document.querySelector('.toast.toast-top.toast-end');
  
  if (!container) {
    // 컨테이너가 없으면 생성
    container = document.createElement('div');
    container.className = 'toast toast-top toast-end';
    container.style.zIndex = '9999';
    document.body.appendChild(container);
  }

  // 타입별 alert 클래스 매핑
  const typeMap = {
    'positive': 'alert-success',
    'negative': 'alert-error',
    'warning': 'alert-warning',
    'info': 'alert-info'
  };
  
  const alertClass = typeMap[type] || 'alert-info';

  // Toast 아이템 생성 (DaisyUI 5 alert 컴포넌트 사용)
  const toast = document.createElement('div');
  toast.className = `alert ${alertClass} shadow-lg mb-2`;
  toast.setAttribute('role', 'alert');
  
  // 메시지 텍스트 (XSS 방지)
  const messageText = document.createTextNode(message);
  toast.appendChild(messageText);

  // 닫기 버튼 추가 (선택사항)
  const closeBtn = document.createElement('button');
  closeBtn.className = 'btn btn-sm btn-circle btn-ghost';
  closeBtn.innerHTML = '<i class="fa-solid fa-xmark"></i>';
  closeBtn.onclick = function() {
    toast.remove();
  };
  toast.appendChild(closeBtn);

  // 컨테이너에 추가
  container.appendChild(toast);

  // duration 후 자동 제거
  setTimeout(() => {
    if (toast.parentNode) {
      toast.remove();
    }
  }, finalDuration);
}


// AJAX 요청에 자동으로 CSRF 토큰 추가
$(function(){
  var token = $('meta[name="_csrf"]').attr('content');
  var header = $('meta[name="_csrf_header"]').attr('content');
  $(document).ajaxSend(function(e, xhr, options){
    xhr.setRequestHeader(header, token);
  });

  // 전역 AJAX 오류 핸들러 - 모든 AJAX 오류를 Toast로 표시
  $(document).ajaxError(function(event, xhr, settings, thrownError) {
    // 특정 URL은 자동 toast 제외 (예: health check, polling 등)
    const skipAutoToastUrls = [
      '/api/ai-server/suh-aider/health',
      '/api/docker-log/poll',
      '/api/member/client-hash'
    ];
    
    // 제외 목록에 있는 URL은 자동 toast 표시 안 함
    if (skipAutoToastUrls.some(url => settings.url && settings.url.includes(url))) {
      return;
    }
    
    // 서버 응답에서 에러 메시지 추출
    let errorMessage = '요청 처리 중 오류가 발생했습니다.';
    
    if (xhr.responseJSON && xhr.responseJSON.message) {
      errorMessage = xhr.responseJSON.message;
    } else if (xhr.responseText) {
      try {
        const parsed = JSON.parse(xhr.responseText);
        if (parsed.message) {
          errorMessage = parsed.message;
        }
      } catch (e) {
        // JSON 파싱 실패 시 기본 메시지 사용
      }
    }
    
    // HTTP 상태 코드별 메시지 개선
    if (xhr.status === 0) {
      errorMessage = '네트워크 연결을 확인해주세요.';
    } else if (xhr.status === 401) {
      errorMessage = '인증이 필요합니다. 다시 로그인해주세요.';
    } else if (xhr.status === 403) {
      errorMessage = '접근 권한이 없습니다.';
    } else if (xhr.status === 404) {
      errorMessage = '요청한 리소스를 찾을 수 없습니다.';
    } else if (xhr.status === 500) {
      errorMessage = errorMessage || '서버 오류가 발생했습니다.';
    }
    
    // Toast 표시
    if (typeof showToast === 'function') {
      showToast(errorMessage, 'negative', 5000);
    }
  });

  // 세션에서 clientHash 가져와 저장
  fetchAndStoreClientHash();

  // 다크모드 테마 초기화
  initTheme();

  // 버전 배지 자동 업데이트
  loadVersionFromChangelog();
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
 * 버전 태그 업데이트 (changelog.json에서)
 * 모든 버전 배지 요소를 자동으로 찾아 업데이트
 */
function loadVersionFromChangelog() {
  fetch('https://raw.githubusercontent.com/Cassiiopeia/suh-project-utility/main/CHANGELOG.json')
    .then(response => response.json())
    .then(data => {
      if (data.metadata && data.metadata.currentVersion) {
        const versionText = 'v' + data.metadata.currentVersion;
        
        // 클래스 기반으로 모든 버전 배지 텍스트 업데이트 (충돌 방지)
        const versionTagElements = document.querySelectorAll('.version-tag-text');
        versionTagElements.forEach(function(element) {
          element.textContent = versionText;
        });
        
        // ID 기반 업데이트 (하위 호환성)
        const versionTagById = document.getElementById('version-tag');
        if (versionTagById) {
          versionTagById.textContent = versionText;
        }
        
        const headerVersionTag = document.getElementById('header-version-tag');
        if (headerVersionTag) {
          headerVersionTag.textContent = versionText;
        }
      }
    })
    .catch(error => {
      console.error('버전 정보 로드 실패:', error);
      // 실패 시 기본값 유지
    });
}

/**
 * Changelog 모달 열기
 */
function openChangelogModal() {
  const modal = document.getElementById('changelog-modal');
  if (modal) {
    modal.showModal();
    loadChangelog();
  } else {
    console.warn('Changelog 모달을 찾을 수 없습니다. (id="changelog-modal")');
  }
}

/**
 * Changelog 데이터 로드
 */
function loadChangelog() {
  const loadingEl = document.getElementById('changelog-loading');
  const dataEl = document.getElementById('changelog-data');

  if (!loadingEl || !dataEl) {
    console.warn('Changelog 요소를 찾을 수 없습니다.');
    return;
  }

  // 이미 로드된 경우 스킵
  if (!dataEl.classList.contains('hidden')) return;

  // GitHub raw URL에서 CHANGELOG.json 가져오기
  fetch('https://raw.githubusercontent.com/Cassiiopeia/suh-project-utility/main/CHANGELOG.json')
    .then(response => response.json())
    .then(data => {
      renderChangelog(data);
      loadingEl.classList.add('hidden');
      dataEl.classList.remove('hidden');
    })
    .catch(error => {
      console.error('Changelog 로드 실패:', error);
      if (loadingEl) {
        loadingEl.innerHTML = `
          <div class="text-center text-gray-500">
            <i class="fa-solid fa-circle-exclamation text-2xl mb-2"></i>
            <p>변경 이력을 불러올 수 없습니다.</p>
          </div>
        `;
      }
    });
}

/**
 * Changelog 데이터 렌더링
 */
function renderChangelog(data) {
  const latestRelease = data.releases[0];
  const metadata = data.metadata;

  // 버전 정보
  const versionEl = document.getElementById('changelog-version');
  const dateEl = document.getElementById('changelog-date');
  const updatedEl = document.getElementById('changelog-updated');
  
  if (versionEl) {
    versionEl.textContent = 'v' + latestRelease.version;
  }
  if (dateEl) {
    dateEl.textContent = latestRelease.date;
  }

  // 마지막 업데이트
  if (updatedEl && metadata.lastUpdated) {
    const lastUpdated = new Date(metadata.lastUpdated);
    updatedEl.textContent = 'Last updated: ' + lastUpdated.toLocaleDateString('ko-KR');
  }

  // 변경사항 목록
  const itemsEl = document.getElementById('changelog-items');
  if (!itemsEl) return;
  
  itemsEl.innerHTML = '';

  // parsed_changes에서 아이템 추출
  const changes = latestRelease.parsed_changes;
  for (const category in changes) {
    const categoryData = changes[category];
    if (categoryData.items && categoryData.items.length > 0) {
      categoryData.items.forEach(function(item) {
        const li = document.createElement('li');
        li.className = 'flex items-start gap-2';
        li.innerHTML = `
          <span class="text-primary mt-1">•</span>
          <span>${escapeHtml(item)}</span>
        `;
        itemsEl.appendChild(li);
      });
    }
  }
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
 * @param {function} errorCallback - 에러 시 콜백 함수 (전역 핸들러가 toast를 표시하므로, 필요시 추가 처리만 수행)
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
      // errorCallback 실행 (전역 핸들러가 toast를 표시하므로, 여기서는 추가 처리만)
      if (errorCallback) {
        errorCallback(xhr, status, error);
      }
      // 전역 ajaxError 핸들러가 toast를 표시하므로 여기서는 로깅만 수행
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

