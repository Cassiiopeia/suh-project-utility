### 📌 작업 개요
Dark/Light Mode 지원, 로그인 form validation 개선, Footer UI 개선 작업 진행

**이슈**: #121 - Dark / Light Mode 지원 및 로그인 form 개선, Footer UI 개선 필요

---

### 🎯 구현 목표
1. 로그인 form에 DaisyUI Validator 적용
2. 다크모드 토글 UI 추가 (DaisyUI Theme Controller)
3. Footer UI를 DaisyUI 스타일로 개선

---

### ✅ 구현 내용

#### 1. 로그인 form Validation 개선
- **파일**: `Suh-Web/src/main/resources/templates/pages/login.html`
- **변경 내용**:
  - Username/Password 필드에 DaisyUI `validator` 클래스 적용
  - `required` 속성 추가로 빈 필드 검증
  - `validator-hint` 힌트 메시지 추가 ("Username is required", "Password is required")
  - 비밀번호 보기/숨기기 토글 기능 추가 (`setupPasswordToggle()` 함수)
  - 비밀번호 눈 아이콘 사라짐 버그 수정 (`z-20` 추가)
  - 아이콘에 `z-10 pointer-events-none` 추가로 레이어 충돌 방지
  - Font Awesome 아이콘 추가 (사용자 아이콘, 자물쇠 아이콘, 눈 아이콘)
  - padding 조정 (`pl-10`, `pr-12`로 아이콘 공간 확보)

#### 2. 다크모드 토글 UI 추가
- **파일**: `Suh-Web/src/main/resources/templates/fragments/header.html`
- **변경 내용**:
  - Desktop/Mobile 네비게이션에 다크모드 토글 버튼 추가
  - DaisyUI `swap swap-rotate` 컴포넌트 사용
  - `theme-controller` 클래스로 테마 전환 연동
  - 해/달 아이콘으로 현재 모드 표시

- **파일**: `Suh-Web/src/main/resources/static/js/common.js`
- **변경 내용**:
  - `initTheme()` 함수 추가
  - localStorage 기반 테마 저장/복원
  - 다중 토글 체크박스 동기화 (Desktop/Mobile)
  - `data-theme` 속성으로 DaisyUI 테마 전환

#### 3. Footer UI 개선
- **파일**: `Suh-Web/src/main/resources/templates/fragments/footer.html`
- **변경 내용**:
  - DaisyUI `footer footer-horizontal footer-center` 클래스 적용
  - 소셜 아이콘 추가 (GitHub, Instagram, Email) - SVG로 구현
  - 텍스트 링크 추가 (About Me, Contact, Suh-Project-Utility)
  - 중앙 정렬 레이아웃으로 개선
  - 그라데이션 배경 (`bg-gradient-to-br from-slate-50 to-blue-50`)
  - 저작권 정보 추가 ("Copyright © 2025 SUHSAECHAN. All rights reserved.")
  - 호버 효과 추가 (`hover:text-primary transition-colors`)

---

### 🔧 주요 변경사항 상세

#### login.html - Validation 적용
```html
<!-- Username 필드 -->
<input type="text" name="username" placeholder="Enter your username"
       class="input input-bordered w-full pl-10 validator" required />
<p class="validator-hint text-error text-xs mt-1">Username is required</p>

<!-- Password 필드 - 눈 아이콘 z-index 수정 -->
<span id="togglePassword" class="absolute right-3 top-1/2 -translate-y-1/2 cursor-pointer hover:opacity-70 z-20">
```

**특이사항**:
- 비밀번호 눈 아이콘이 입력 중 사라지는 버그를 `z-20`으로 해결
- 좌측 아이콘에 `pointer-events-none` 추가로 클릭 이벤트 무시
- 비밀번호 보기/숨기기 토글 기능: `setupPasswordToggle()` 함수로 구현, 눈 아이콘 클릭 시 `fa-eye` ↔ `fa-eye-slash` 전환

#### header.html - 다크모드 토글
```html
<label class="swap swap-rotate btn btn-ghost btn-sm">
  <input type="checkbox" class="theme-controller" value="dark" />
  <!-- 해 아이콘 (라이트 모드) -->
  <svg class="swap-off h-5 w-5 fill-current">...</svg>
  <!-- 달 아이콘 (다크 모드) -->
  <svg class="swap-on h-5 w-5 fill-current">...</svg>
</label>
```

#### common.js - 테마 관리 로직
```javascript
function initTheme() {
  const savedTheme = localStorage.getItem('theme');
  const isDark = savedTheme === 'dark';

  if (isDark) {
    document.documentElement.setAttribute('data-theme', 'dark');
  }
  // 토글 체크박스 동기화 및 이벤트 리스너 등록
}
```

**특이사항**:
- Desktop/Mobile 토글이 분리되어 있어 양쪽 동기화 로직 구현
- 페이지 로드 시 localStorage에서 테마 복원

#### footer.html - DaisyUI 스타일
```html
<footer class="footer footer-horizontal footer-center bg-gradient-to-br from-slate-50 to-blue-50 text-gray-600 rounded p-10 mt-12">
  <nav>
    <div class="grid grid-flow-col gap-4">
      <!-- GitHub, Email SVG 아이콘 -->
    </div>
  </nav>
  <aside>
    <p>Copyright © 2025 SUHSAECHAN. All rights reserved.</p>
  </aside>
</footer>
```

#### 4. 다크모드 전체 페이지 스타일 적용
- **파일**: `Suh-Web/src/main/resources/static/css/common.css`
- **변경 내용**:
  - 대시보드 페이지 다크모드 스타일 추가
  - 챗봇 위젯 다크모드 스타일 추가
  - 추가 컴포넌트 다크모드 스타일 추가

**추가된 다크모드 스타일 (common.css)**:

1. **텍스트 색상 오버라이드**:
   - `.text-gray-900`, `.text-gray-800`, `.text-gray-700`, `.text-gray-600`, `.text-gray-500`, `.text-gray-400`

2. **배경색 오버라이드**:
   - `.bg-white` → `#1f2937`
   - `.bg-gray-50` → `#374151`
   - `.bg-gray-100`, `.bg-gray-200`
   - `.bg-*-100` (blue, purple, gray, orange, teal, green, violet, indigo, emerald, amber, pink, cyan, yellow)

3. **챗봇 위젯 다크모드**:
   - `.chatbot-panel` 배경 및 그림자
   - `.chatbot-messages` 그라데이션 배경
   - `.chatbot-welcome` 제목, 설명, 아바타
   - `.suggestion-btn` 추천 질문 버튼
   - `.chat-message.assistant .bubble` 어시스턴트 메시지
   - `.typing-indicator` 타이핑 표시기
   - `.chatbot-input-area` 입력 영역
   - `.chatbot-reset-btn`, `.chatbot-close-btn` 버튼들
   - `.chat-references` 참조 문서
   - `.chat-feedback` 피드백 버튼

4. **추가 컴포넌트**:
   - `.card-title`, `.card-body h3/p` 카드 텍스트
   - `#ai-server-status` AI 서버 상태 박스
   - `.collapse` 공지사항 아코디언
   - `.comment-item` 모달 내 댓글
   - `.skeleton` 스켈레톤 로더
   - `.btn-outline`, `.badge-ghost`

---

### 🔧 다크모드 CSS 구조

```css
/* 다크모드 - 대시보드 페이지 스타일 */
[data-theme="dark"] .text-gray-900 { color: #f3f4f6 !important; }
[data-theme="dark"] .bg-white { background-color: #1f2937 !important; }
[data-theme="dark"] .bg-gray-50 { background-color: #374151 !important; }
[data-theme="dark"] .bg-blue-100 { background-color: rgba(59, 130, 246, 0.2) !important; }

/* 다크모드 - 챗봇 위젯 스타일 */
[data-theme="dark"] .chatbot-panel { background: #1f2937 !important; }
[data-theme="dark"] .chatbot-messages { background: linear-gradient(180deg, #111827 0%, #1f2937 100%) !important; }
[data-theme="dark"] .chat-message.assistant .bubble { background: #374151 !important; color: #f3f4f6 !important; }

/* 다크모드 - 추가 컴포넌트 스타일 */
[data-theme="dark"] .collapse { background-color: #1f2937 !important; border-color: #374151 !important; }
[data-theme="dark"] #ai-server-status { background-color: #374151 !important; }
```

---

### 🧪 테스트 및 검증
- [x] 로그인 페이지에서 빈 필드 제출 시 validation 힌트 표시 확인
- [x] 비밀번호 입력 중 눈 아이콘 유지 확인
- [x] 비밀번호 보기/숨기기 토글 기능 동작 확인
- [x] 다크모드 토글 클릭 시 테마 전환 확인
- [x] 페이지 새로고침 후 테마 유지 확인 (localStorage)
- [x] Desktop/Mobile 다크모드 토글 동기화 확인
- [x] Footer 중앙 정렬 및 소셜 아이콘 표시 확인
- [x] Footer 다크모드 스타일 적용 확인
- [x] 대시보드 모든 카드 다크모드 적용 확인
- [x] 챗봇 위젯 다크모드 적용 확인
- [x] 공지사항 아코디언 다크모드 적용 확인
- [x] AI 서버 상태 박스 다크모드 적용 확인

---

### 📌 참고사항
- DaisyUI v5 사용 중
- Tailwind CSS Browser 버전과 함께 사용
- 다크모드는 localStorage에 저장되어 세션 간 유지됨
- 서버 사이드 검증은 별도로 유지 (클라이언트 검증은 UX 개선용)
- 다크모드 스타일은 `[data-theme="dark"]` 선택자로 적용
- 모든 하드코딩된 Tailwind 색상 클래스에 CSS 오버라이드 적용
- 다크모드 CSS는 `common.css`에 약 1,800줄 이상 추가됨
- 로그인 페이지의 "S.LAB" 문구는 이미지 내 문구로, 추후 수정 예정 (이슈 댓글 참고)
- 작업 완료 확인: 이슈 댓글에서 "정상 작동합니다", "다크모드 정상 작동합니다 이슈 닫습니다" 확인됨
