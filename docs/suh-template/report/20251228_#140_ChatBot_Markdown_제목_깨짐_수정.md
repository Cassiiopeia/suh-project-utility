### 작업 개요
챗봇 답변에서 마크다운 번호 리스트의 제목(볼드 텍스트)이 세로로 분리되어 표시되는 레이아웃 문제 수정

**이슈**: https://github.com/Cassiiopeia/suh-project-utility/issues/140

---

### 문제 분석

**증상**: PC 화면에서 챗봇 답변의 번호 리스트가 아래와 같이 깨져서 표시
```
1.    SUH-         이 사이트는 개발자가 필
      PROJECT      요할 때마다 만들어진 실
      -UTILITY     험적인 유틸리티 모음입니
      의 주요 기    다.
      능 안내:
```

**원인**: CSS의 `.chat-list-item`에 `display: flex` 적용으로 인해 내부의 `<strong>` 태그가 별도 flex item으로 분리되어 컬럼 형태로 렌더링됨

**마크다운 파싱 구조**:
- `chatbot.js`에서 `1. **제목:** 내용` 형식을 HTML로 변환
- 변환 결과: `<div class="chat-list-item"><span class="list-number">1.</span> <strong>제목:</strong> 내용</div>`
- `display: flex`가 `<span>`, `<strong>`, 텍스트를 각각 별도 flex item으로 취급

---

### 구현 내용

#### CSS 레이아웃 방식 변경
- **파일**: `Suh-Web/src/main/resources/static/css/common.css`
- **변경 내용**: flex 레이아웃을 block + inline 조합으로 변경
- **이유**: 번호와 제목, 내용이 자연스럽게 한 줄로 이어지도록 수정

---

### 주요 변경사항 상세

#### `.chat-list-item` 클래스 수정

**변경 전**:
```css
.chat-message .bubble .chat-list-item {
  display: flex;
  gap: 8px;
  margin: 4px 0;
  padding-left: 4px;
}
```

**변경 후**:
```css
.chat-message .bubble .chat-list-item {
  display: block;
  margin: 4px 0;
  padding-left: 4px;
}
```

#### `.list-number` 클래스 수정

**변경 전**:
```css
.chat-message .bubble .list-number {
  color: #3b82f6;
  font-weight: 600;
  min-width: 20px;
}
```

**변경 후**:
```css
.chat-message .bubble .list-number {
  display: inline;
  color: #3b82f6;
  font-weight: 600;
  margin-right: 8px;
}
```

#### `.list-bullet` 클래스 수정

**변경 전**:
```css
.chat-message .bubble .list-bullet {
  color: #3b82f6;
  font-weight: bold;
}
```

**변경 후**:
```css
.chat-message .bubble .list-bullet {
  display: inline;
  color: #3b82f6;
  font-weight: bold;
  margin-right: 8px;
}
```

---

### 수정 효과

**수정 전**:
```
1.    SUH-         이 사이트는...
      PROJECT
      -UTILITY
```

**수정 후**:
```
1. SUH-PROJECT-UTILITY의 주요 기능 안내: 이 사이트는 개발자가 필요할 때마다...
```

---

### 테스트 및 검증
- 브라우저 캐시 새로고침 (Ctrl+Shift+R / Cmd+Shift+R) 후 확인
- 챗봇에서 번호 리스트가 포함된 답변 테스트
- 볼드 텍스트가 포함된 리스트 아이템이 한 줄로 자연스럽게 표시되는지 확인

---

### 참고사항
- 불릿 리스트(`- item`)도 동일한 방식으로 수정하여 일관성 유지
- 다크모드에서도 정상 동작 (색상 관련 변경 없음)
