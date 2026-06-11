### 📌 작업 개요

SUH-RANDOM-ENGINE 모듈 테스트 페이지(`/suh-random`)를 사이트 전체에 연결하고, 대시보드에 SUH 랜덤 사용 통계를 표시하는 기능 추가. 기존 6개 통계 카드에서 세종대 인증을 분리하여 "기능별 상세 통계" 섹션으로 이동하고, SUH 랜덤 통계와 함께 표시.

**보고서 파일**: `.report/20260109_#146_SUH_RANDOM_ENGINE_테스트사이트_연결_및_통계추가.md`

---

### 🎯 구현 목표

- `/suh-random` 테스트 페이지를 대시보드, 프로필, 헤더 메뉴에서 접근 가능하도록 연결
- 대시보드에 SUH 랜덤 사용 통계 표시 (전체/오늘)
- 통계 UI 개선: 메인 카드 5개 + 기능별 상세 통계 섹션 구성

---

### ✅ 구현 내용

#### 1. 대시보드 - Dev Tools 섹션에 SUH 랜덤 엔진 카드 추가
- **파일**: `Suh-Web/src/main/resources/templates/pages/dashboard.html`
- **변경 내용**: Internal Tools 그리드에 SUH 랜덤 엔진 카드 추가
- **이유**: 사용자가 대시보드에서 바로 테스트 페이지로 이동 가능하도록 구현

#### 2. 대시보드 - 서버 통계 UI 개편
- **파일**: `Suh-Web/src/main/resources/templates/pages/dashboard.html`
- **변경 내용**:
  - 메인 통계 그리드를 6열에서 5열로 변경
  - 세종대 인증 카드를 메인에서 제거
  - "기능별 상세 통계" 섹션 신규 추가 (세종대 인증 + SUH 랜덤)
- **이유**: 기능별 통계를 별도 섹션으로 분리하여 확장성 확보

#### 3. 대시보드 - SUH 랜덤 통계 표시 로직 추가
- **파일**: `Suh-Web/src/main/resources/templates/pages/dashboard.html` (JavaScript)
- **변경 내용**: `loadDashboardSummary`, `refreshDashboardSummary` 함수에 SUH 랜덤 통계 표시 로직 추가
- **이유**: API 응답의 `featureUsageCounts`, `todayFeatureUsageCounts`에서 SUH_RANDOM 값을 읽어 표시

#### 4. 헤더 메뉴 - SUH 랜덤 링크 추가
- **파일**: `Suh-Web/src/main/resources/templates/fragments/header.html`
- **변경 내용**: Desktop/Mobile 유틸리티 드롭다운 메뉴에 "SUH 랜덤" 메뉴 항목 추가
- **이유**: 어떤 페이지에서든 헤더를 통해 테스트 페이지 접근 가능

#### 5. 프로필 페이지 - 테스트 링크 버튼 추가
- **파일**: `Suh-Web/src/main/resources/templates/pages/profile.html`
- **변경 내용**: Nexus Modules 섹션의 suh-random-engine 카드에 테스트 페이지 바로가기 버튼 추가
- **이유**: 모듈 카드에서 바로 테스트 페이지로 이동 가능

#### 6. 백엔드 - 오늘 기능별 통계 필드 추가
- **파일**: `Suh-Domain-Statistics/src/main/java/me/suhsaechan/statistics/dto/DashboardSummaryDto.java`
- **변경 내용**: `todayFeatureUsageCounts` 필드 추가
- **이유**: SUH 랜덤의 오늘 사용 횟수를 프론트엔드에 전달하기 위함

#### 7. 백엔드 - DashboardService에 오늘 통계 조회 추가
- **파일**: `Suh-Application/src/main/java/me/suhsaechan/application/service/DashboardService.java`
- **변경 내용**:
  - 정상 케이스에 `.todayFeatureUsageCounts(statisticsService.getTodayFeatureUsageCounts())` 추가
  - 에러 케이스에 빈 Map 반환 로직 추가
- **이유**: 오늘 기능별 통계를 API 응답에 포함

---

### 🔧 주요 변경사항 상세

#### dashboard.html - 통계 UI 구조 변경

기존 6개 메인 카드에서 세종대 인증을 제거하고 5개로 축소. 새로운 "기능별 상세 통계" 섹션을 추가하여 세종대 인증과 SUH 랜덤 통계를 함께 표시. 카드 스타일은 메인 통계 카드와 동일한 `card bg-base-100 shadow-sm` 적용.

**특이사항**:
- 기능별 상세 통계 카드는 메인 카드보다 약간 작은 크기 (`p-4` vs `p-5`)
- 향후 다른 기능 통계 추가 시 이 섹션에 카드만 추가하면 됨

#### DashboardSummaryDto.java - 필드 추가

기존 `featureUsageCounts`(전체 통계)에 더해 `todayFeatureUsageCounts`(오늘 통계) 필드 추가. 프론트엔드에서 "오늘: +N" 형태로 표시하는 데 사용.

**특이사항**:
- `StatisticsService.getTodayFeatureUsageCounts()` 메서드는 이미 구현되어 있었음
- `DashboardSummaryDto`에 필드만 누락되어 있었던 것

---

### 📦 의존성 변경

없음 (기존 라이브러리만 사용)

---

### 🧪 테스트 및 검증

1. 대시보드(`/dashboard`) 확인:
   - Dev Tools 섹션에 SUH 랜덤 엔진 카드 표시되는지 확인
   - 서버 통계가 5개 카드로 표시되는지 확인
   - 기능별 상세 통계 섹션에 세종대 인증 + SUH 랜덤 카드 표시되는지 확인
   - 새로고침 버튼 클릭 시 통계 업데이트되는지 확인

2. 프로필(`/profile`) 확인:
   - Nexus Modules의 suh-random-engine 카드에 테스트 버튼 있는지 확인

3. 헤더 메뉴 확인:
   - Desktop/Mobile 유틸리티 드롭다운에 SUH 랜덤 메뉴 있는지 확인

4. `/suh-random` 페이지에서 닉네임 생성 후 대시보드 새로고침하여 통계 증가 확인

---

### 📌 참고사항

- 테스트 페이지(`/suh-random`)는 이전 커밋에서 이미 구현됨 (8a90885)
- 통계 기록은 `RandomNicknameController`에서 `statisticsService.logFeatureUsageAsync(FeatureType.SUH_RANDOM, ...)` 호출로 처리됨
- RAG 문서 `document/SUH_RANDOM_GUIDE.md` 추가됨

---

### 📁 수정 파일 목록

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `dashboard.html` | Modified | Dev Tools 카드 추가, 통계 UI 개편, JS 로직 추가 |
| `header.html` | Modified | Desktop/Mobile 메뉴에 SUH 랜덤 링크 추가 |
| `profile.html` | Modified | suh-random-engine 카드에 테스트 버튼 추가 |
| `DashboardSummaryDto.java` | Modified | `todayFeatureUsageCounts` 필드 추가 |
| `DashboardService.java` | Modified | 오늘 기능별 통계 조회 로직 추가 |
