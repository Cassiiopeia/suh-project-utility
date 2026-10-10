# Spring + Thymeleaf 한/영 i18n 기반 구축 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring 표준 i18n 인프라를 구축하고, HTML에서 메시지 프로퍼티(`#{...}`)로 한/영을 처리하는 패턴을 header/footer/dashboard 핵심 섹션에 적용해 검증한다.

**Architecture:** `CookieLocaleResolver`로 쿠키 기반 로케일을 결정하고(기본 한국어), `LocaleChangeInterceptor`로 `?lang=` 파라미터 전환을 처리한다. 텍스트는 `messages_ko.properties`(한국어) / `messages_en.properties`(영어)로 분리하고 Thymeleaf `#{key}`로 참조한다.

**Tech Stack:** Spring Boot 3.4.2, Thymeleaf, Spring MVC i18n (`MessageSource`, `LocaleResolver`, `LocaleChangeInterceptor`)

---

## 파일 구조

| 파일 | 책임 | 작업 |
|------|------|------|
| `Suh-Web/src/main/resources/messages_ko.properties` | 한국어 메시지 (기본 로케일) | Create |
| `Suh-Web/src/main/resources/messages_en.properties` | 영어 메시지 | Create |
| `Suh-Web/src/main/java/me/suhsaechan/web/config/I18nConfig.java` | `LocaleResolver` + `LocaleChangeInterceptor` Bean 정의 | Create |
| `Suh-Web/src/main/java/me/suhsaechan/web/config/WebConfig.java` | 기존 인터셉터 등록부에 `localeChangeInterceptor` 합류 | Modify |
| `Suh-Web/src/main/resources/application.yml` | `spring.messages` 설정 | Modify |
| `Suh-Web/src/main/resources/templates/fragments/header.html` | 언어 토글 버튼 추가 + 샘플 텍스트 `#{}` 치환 | Modify |
| `Suh-Web/src/main/resources/templates/fragments/footer.html` | 샘플 텍스트 `#{}` 치환 | Modify |
| `Suh-Web/src/main/resources/templates/pages/dashboard.html` | 상단 핵심 섹션 텍스트 `#{}` 치환 | Modify |

**주의:** 이 프로젝트는 내부망 환경이라 Gradle 빌드/실행은 사용자가 별도 환경에서 직접 수행한다. 각 Task의 "실행 검증" 스텝은 사용자가 `./gradlew bootRun`을 직접 돌리는 것을 전제로 한다. 구현 에이전트는 코드 작성·정합성 확인까지만 책임지고, 런타임 검증은 사용자 확인을 기다린다.

**커밋 주의:** CLAUDE.md 절대 규칙상 사용자 명시 승인 없이 `git add`/`git commit` 금지. 각 Task의 커밋 스텝은 사용자 승인 후에만 실행하며, 승인 전에는 diff를 보여주고 대기한다.

---

## Task 1: 메시지 프로퍼티 파일 생성

**Files:**
- Create: `Suh-Web/src/main/resources/messages_ko.properties`
- Create: `Suh-Web/src/main/resources/messages_en.properties`

키는 `{영역}.{컴포넌트}.{의미}` 규칙을 따른다. 이번 작업의 샘플 범위(header/footer/dashboard 핵심)에 필요한 키만 정의한다. fallback 기본 번들(`messages.properties`)은 만들지 않는다 — 로케일은 `LocaleChangeInterceptor`로 `ko`/`en`만 전환되고 기본값이 한국어이므로 항상 둘 중 하나로 결정된다.

- [ ] **Step 1: 한국어 메시지 파일 작성 (messages_ko.properties)**

Create `Suh-Web/src/main/resources/messages_ko.properties`:

```properties
# ===== Language Toggle =====
lang.korean=한국어
lang.english=English

# ===== Header =====
header.nav.aboutMe=About Me
header.nav.dashboard=대시보드
header.nav.issueHelper=깃헙 이슈
header.nav.issueHelperMobile=깃헙 이슈 도우미
header.nav.devopsTemplate=DevOps 템플릿
header.nav.translator=번역기
header.nav.translatorMobile=커스텀 AI 번역기
header.nav.sejongAuth=세종대 인증
header.nav.suhRandom=SUH 랜덤
header.nav.suhRandomMobile=SUH 랜덤 엔진
header.nav.noticeManagement=공지사항 관리
header.nav.dockerLogs=컨테이너 로그
header.nav.grassPlanter=Grass Planter
header.nav.taskTracker=Task 트래커
header.nav.busReservation=버스 예약
header.nav.aiServer=AI 서버 관리
header.nav.chatbotManagement=챗봇 문서 관리
header.menu.title=메뉴
header.action.logout=로그아웃
header.action.login=로그인

# ===== Footer =====
footer.link.aboutMe=About Me
footer.link.contact=Contact
footer.copyright=Copyright © 2025 SUHSAECHAN. All rights reserved.

# ===== Dashboard =====
dashboard.header.title=새찬 서버 실험실
dashboard.header.subtitle=개발에 대한 나의 방향성을 탐구하는 실험실. 이번엔 뭘 만들어볼까?
dashboard.notice.title=공지사항
dashboard.notice.viewAll=전체보기
dashboard.notice.loading=로딩중...
dashboard.summary.title=서버 통계
```

- [ ] **Step 2: 영어 메시지 파일 작성**

Create `Suh-Web/src/main/resources/messages_en.properties`:

```properties
# ===== Language Toggle =====
lang.korean=한국어
lang.english=English

# ===== Header =====
header.nav.aboutMe=About Me
header.nav.dashboard=Dashboard
header.nav.issueHelper=GitHub Issues
header.nav.issueHelperMobile=GitHub Issue Helper
header.nav.devopsTemplate=DevOps Template
header.nav.translator=Translator
header.nav.translatorMobile=Custom AI Translator
header.nav.sejongAuth=Sejong Auth
header.nav.suhRandom=SUH Random
header.nav.suhRandomMobile=SUH Random Engine
header.nav.noticeManagement=Notice Management
header.nav.dockerLogs=Container Logs
header.nav.grassPlanter=Grass Planter
header.nav.taskTracker=Task Tracker
header.nav.busReservation=Bus Reservation
header.nav.aiServer=AI Server
header.nav.chatbotManagement=Chatbot Docs
header.menu.title=Menu
header.action.logout=Logout
header.action.login=Login

# ===== Footer =====
footer.link.aboutMe=About Me
footer.link.contact=Contact
footer.copyright=Copyright © 2025 SUHSAECHAN. All rights reserved.

# ===== Dashboard =====
dashboard.header.title=Saechan Server Lab
dashboard.header.subtitle=A lab exploring my direction in development. What shall I build this time?
dashboard.notice.title=Notice
dashboard.notice.viewAll=View All
dashboard.notice.loading=Loading...
dashboard.summary.title=Server Statistics
```

- [ ] **Step 3: 두 파일의 키 집합 일치 확인**

Run: `comm -3 <(grep -oP '^[^#=]+(?==)' Suh-Web/src/main/resources/messages_ko.properties | sort) <(grep -oP '^[^#=]+(?==)' Suh-Web/src/main/resources/messages_en.properties | sort)`
Expected: 출력 없음 (양쪽 키 집합이 완전히 동일)

- [ ] **Step 4: 커밋 (사용자 승인 후)**

```bash
git add Suh-Web/src/main/resources/messages_ko.properties Suh-Web/src/main/resources/messages_en.properties
git commit -m "한/영 i18n 기반 구축 : feat : 메시지 프로퍼티 파일(한국어/영어) 추가"
```

---

## Task 2: i18n 설정 클래스 추가 (LocaleResolver + Interceptor)

**Files:**
- Create: `Suh-Web/src/main/java/me/suhsaechan/web/config/I18nConfig.java`
- Modify: `Suh-Web/src/main/java/me/suhsaechan/web/config/WebConfig.java`
- Modify: `Suh-Web/src/main/resources/application.yml`

`MessageSource`는 Spring Boot 자동 구성(`spring.messages.*`)을 사용한다. 우리는 `LocaleResolver`(쿠키, 기본 한국어)와 `LocaleChangeInterceptor`(`lang` 파라미터) Bean만 직접 등록한다. 인터셉터 등록은 기존 `WebConfig.addInterceptors`에 합류한다(WebMvcConfigurer 중복 생성 회피).

- [ ] **Step 1: application.yml에 messages 설정 추가**

Modify `Suh-Web/src/main/resources/application.yml` — `spring:` 블록 하위에 다음을 추가한다(기존 `spring:` 키 들여쓰기에 맞춰 삽입):

```yaml
spring:
  messages:
    basename: messages
    encoding: UTF-8
    fallback-to-system-locale: false
```

> `fallback-to-system-locale: false` — 시스템 로케일(서버 OS)로 새지 않도록 차단. 로케일은 LocaleResolver(기본 한국어) 기준으로만 결정되어 항상 `messages_ko`/`messages_en` 중 하나를 탄다.

- [ ] **Step 2: I18nConfig 생성**

Create `Suh-Web/src/main/java/me/suhsaechan/web/config/I18nConfig.java`:

```java
package me.suhsaechan.web.config;

import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * 다국어(i18n) 설정
 * 쿠키 기반 로케일 결정(기본 한국어) + lang 파라미터 전환 인터셉터
 */
@Configuration
public class I18nConfig {

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("LOCALE");
        resolver.setDefaultLocale(Locale.KOREAN);
        resolver.setCookieMaxAge(java.time.Duration.ofDays(365));
        resolver.setCookiePath("/");
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }
}
```

> `CookieLocaleResolver("LOCALE")` — Spring 6의 생성자 기반 쿠키명 지정. setter 기반 `setCookieName`은 deprecated.

- [ ] **Step 3: WebConfig에 인터셉터 등록**

Modify `Suh-Web/src/main/java/me/suhsaechan/web/config/WebConfig.java`:

필드에 인터셉터를 주입하고:

```java
    private final PageVisitInterceptor pageVisitInterceptor;
    private final PublicEndpointConfig publicEndpointConfig;
    private final LocaleChangeInterceptor localeChangeInterceptor;
```

`addInterceptors`에 등록을 추가한다:

```java
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(pageVisitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(publicEndpointConfig.getInterceptorExcludedPaths());

        registry.addInterceptor(localeChangeInterceptor);
    }
```

- [ ] **Step 4: 컴파일 검증 (사용자 환경)**

Run (사용자): `./gradlew :Suh-Web:compileJava`
Expected: BUILD SUCCESSFUL

> 내부망 Gradle 제약으로 에이전트가 직접 실행 불가. 사용자 확인 대기. 에이전트는 import/타입 정합성을 코드 리뷰로 확인한다.

- [ ] **Step 5: 커밋 (사용자 승인 후)**

```bash
git add Suh-Web/src/main/java/me/suhsaechan/web/config/I18nConfig.java Suh-Web/src/main/java/me/suhsaechan/web/config/WebConfig.java Suh-Web/src/main/resources/application.yml
git commit -m "한/영 i18n 기반 구축 : feat : LocaleResolver(쿠키)·LocaleChangeInterceptor 및 messages 설정 추가"
```

---

## Task 3: 헤더에 언어 토글 + 샘플 텍스트 i18n 치환

**Files:**
- Modify: `Suh-Web/src/main/resources/templates/fragments/header.html`

데스크탑(65줄 부근 `hidden lg:flex` 블록)과 모바일(124줄 부근 `lg:hidden` 블록)의 다크모드 토글 옆에 언어 토글을 추가하고, 검증용으로 "대시보드", "로그아웃", "로그인" 텍스트를 `#{}`로 치환한다.

언어 토글은 인라인 스타일 금지(CSP) — DaisyUI 클래스만 사용. 현재 페이지를 유지하며 `?lang=` 전환을 위해 `th:href="@{''(lang='en')}"` 사용(현재 URL에 lang 파라미터만 부착).

- [ ] **Step 1: 데스크탑 다크모드 토글 다음에 언어 토글 추가**

header.html 데스크탑 메뉴에서 다크모드 `</label>` (77줄) 바로 다음, About Me 링크(80줄) 앞에 삽입:

```html
        <!-- 언어 토글 (데스크탑) -->
        <div class="dropdown dropdown-end">
          <div tabindex="0" role="button" class="btn btn-ghost btn-sm hover:bg-base-200">
            <i class="fa-solid fa-globe"></i>
            <span th:text="#{lang.korean}" th:if="${#locale.language == 'ko'}">한국어</span>
            <span th:text="#{lang.english}" th:unless="${#locale.language == 'ko'}">English</span>
          </div>
          <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box shadow-lg z-50 w-32 p-2 mt-2 border border-base-300">
            <li><a th:href="@{''(lang='ko')}" th:text="#{lang.korean}">한국어</a></li>
            <li><a th:href="@{''(lang='en')}" th:text="#{lang.english}">English</a></li>
          </ul>
        </div>
```

- [ ] **Step 2: 데스크탑 "대시보드" 텍스트 치환**

header.html 87줄의 `대시보드` 텍스트를 치환. `<a href="/dashboard" ...>` 안의:

```html
          <i class="fa-solid fa-clipboard-list"></i>
          대시보드
```
→
```html
          <i class="fa-solid fa-clipboard-list"></i>
          <span th:text="#{header.nav.dashboard}">대시보드</span>
```

- [ ] **Step 3: 모바일 다크모드 토글 다음에 언어 토글 추가**

header.html 모바일 메뉴에서 다크모드 `</label>` (134줄) 다음, 프로필 링크(137줄) 앞에 삽입:

```html
        <!-- 언어 토글 (모바일) -->
        <div class="dropdown dropdown-end">
          <div tabindex="0" role="button" class="btn btn-ghost btn-circle">
            <i class="fa-solid fa-globe text-base-content"></i>
          </div>
          <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box shadow-lg z-50 w-32 p-2 mt-3 border border-base-300">
            <li><a th:href="@{''(lang='ko')}" th:text="#{lang.korean}">한국어</a></li>
            <li><a th:href="@{''(lang='en')}" th:text="#{lang.english}">English</a></li>
          </ul>
        </div>
```

- [ ] **Step 4: 모바일 "메뉴" 타이틀 및 로그인/로그아웃 치환**

header.html 147줄 `메뉴`:
```html
            <li class="menu-title text-base-content/60 text-xs font-semibold px-4 py-2">메뉴</li>
```
→
```html
            <li class="menu-title text-base-content/60 text-xs font-semibold px-4 py-2" th:text="#{header.menu.title}">메뉴</li>
```

167줄 `로그아웃` (모바일 form 내 button 텍스트):
```html
                  <i class="fa-solid fa-right-from-bracket w-5"></i>
                  로그아웃
```
→
```html
                  <i class="fa-solid fa-right-from-bracket w-5"></i>
                  <span th:text="#{header.action.logout}">로그아웃</span>
```

175줄 `로그인`:
```html
                <i class="fa-solid fa-right-to-bracket w-5"></i>
                로그인
```
→
```html
                <i class="fa-solid fa-right-to-bracket w-5"></i>
                <span th:text="#{header.action.login}">로그인</span>
```

- [ ] **Step 5: 정합성 확인**

Run: `grep -n 'th:text="#{' Suh-Web/src/main/resources/templates/fragments/header.html`
Expected: lang.korean/lang.english, header.nav.dashboard, header.menu.title, header.action.logout, header.action.login 키가 등장. 모든 키는 messages_ko.properties에 정의되어 있어야 함.

Run: `grep -oP '#\{[^}]+\}' Suh-Web/src/main/resources/templates/fragments/header.html | sed 's/[#{}]//g' | sort -u | while read k; do grep -q "^$k=" Suh-Web/src/main/resources/messages_ko.properties || echo "MISSING: $k"; done`
Expected: 출력 없음 (모든 사용 키가 프로퍼티에 존재)

- [ ] **Step 6: 커밋 (사용자 승인 후)**

```bash
git add Suh-Web/src/main/resources/templates/fragments/header.html
git commit -m "한/영 i18n 기반 구축 : feat : 헤더 언어 토글 버튼 추가 및 샘플 텍스트 i18n 치환"
```

---

## Task 4: 푸터 샘플 텍스트 i18n 치환

**Files:**
- Modify: `Suh-Web/src/main/resources/templates/fragments/footer.html`

- [ ] **Step 1: 푸터 링크/저작권 텍스트 치환**

footer.html 9줄 `About Me`:
```html
      <a href="/" class="link link-hover">About Me</a>
      <a href="mailto:chan4760@naver.com" class="link link-hover">Contact</a>
```
→
```html
      <a href="/" class="link link-hover" th:text="#{footer.link.aboutMe}">About Me</a>
      <a href="mailto:chan4760@naver.com" class="link link-hover" th:text="#{footer.link.contact}">Contact</a>
```

33줄 저작권:
```html
      <p>Copyright © 2025 SUHSAECHAN. All rights reserved.</p>
```
→
```html
      <p th:text="#{footer.copyright}">Copyright © 2025 SUHSAECHAN. All rights reserved.</p>
```

> `Suh-Project-Utility` 링크(11줄)는 고유명사이므로 치환하지 않는다.

- [ ] **Step 2: 정합성 확인**

Run: `grep -oP '#\{[^}]+\}' Suh-Web/src/main/resources/templates/fragments/footer.html | sed 's/[#{}]//g' | sort -u | while read k; do grep -q "^$k=" Suh-Web/src/main/resources/messages_ko.properties || echo "MISSING: $k"; done`
Expected: 출력 없음

- [ ] **Step 3: 커밋 (사용자 승인 후)**

```bash
git add Suh-Web/src/main/resources/templates/fragments/footer.html
git commit -m "한/영 i18n 기반 구축 : feat : 푸터 샘플 텍스트 i18n 치환"
```

---

## Task 5: 대시보드 상단 핵심 섹션 i18n 치환

**Files:**
- Modify: `Suh-Web/src/main/resources/templates/pages/dashboard.html`

890-920줄 범위의 헤더 섹션·공지사항·서버 통계 제목만 치환한다(검증 목적). 그 외 섹션은 이번 범위 제외.

- [ ] **Step 1: 헤더 섹션 제목/부제 치환**

dashboard.html 890-891줄:
```html
      <h1 class="text-3xl font-bold text-base-content mb-2">새찬 서버 실험실</h1>
      <p class="text-sm text-base-content/70">개발에 대한 나의 방향성을 탐구하는 실험실. 이번엔 뭘 만들어볼까?</p>
```
→
```html
      <h1 class="text-3xl font-bold text-base-content mb-2" th:text="#{dashboard.header.title}">새찬 서버 실험실</h1>
      <p class="text-sm text-base-content/70" th:text="#{dashboard.header.subtitle}">개발에 대한 나의 방향성을 탐구하는 실험실. 이번엔 뭘 만들어볼까?</p>
```

- [ ] **Step 2: 공지사항 섹션 치환**

dashboard.html 898줄:
```html
        <h2 class="text-lg font-semibold text-base-content/80">공지사항</h2>
```
→
```html
        <h2 class="text-lg font-semibold text-base-content/80" th:text="#{dashboard.notice.title}">공지사항</h2>
```

899-901줄 전체보기 링크 — 아이콘이 텍스트와 같은 `<a>` 안에 있으므로 텍스트 노드만 span으로 감싼다:
```html
        <a href="/notice" class="text-sm text-base-content/60 hover:text-primary transition-colors">
          전체보기 <i class="fa-solid fa-chevron-right text-xs"></i>
        </a>
```
→
```html
        <a href="/notice" class="text-sm text-base-content/60 hover:text-primary transition-colors">
          <span th:text="#{dashboard.notice.viewAll}">전체보기</span> <i class="fa-solid fa-chevron-right text-xs"></i>
        </a>
```

910줄 로딩중:
```html
            <span class="text-base-content/50 text-sm">로딩중...</span>
```
→
```html
            <span class="text-base-content/50 text-sm" th:text="#{dashboard.notice.loading}">로딩중...</span>
```

- [ ] **Step 3: 서버 통계 제목 치환**

dashboard.html 920줄:
```html
        <h2 class="text-lg font-semibold text-base-content">서버 통계</h2>
```
→
```html
        <h2 class="text-lg font-semibold text-base-content" th:text="#{dashboard.summary.title}">서버 통계</h2>
```

- [ ] **Step 4: 정합성 확인**

Run: `grep -oP '#\{[^}]+\}' Suh-Web/src/main/resources/templates/pages/dashboard.html | sed 's/[#{}]//g' | sort -u | while read k; do grep -q "^$k=" Suh-Web/src/main/resources/messages_ko.properties || echo "MISSING: $k"; done`
Expected: 출력 없음

> 주의: dashboard.html에 이미 다른 `#{}`가 있을 수 있으니, 위 grep 결과의 모든 키가 프로퍼티에 존재하는지만 확인하면 된다. 이번에 추가한 dashboard.* 키 6종이 포함되어야 한다.

- [ ] **Step 5: 커밋 (사용자 승인 후)**

```bash
git add Suh-Web/src/main/resources/templates/pages/dashboard.html
git commit -m "한/영 i18n 기반 구축 : feat : 대시보드 상단 핵심 섹션 i18n 치환"
```

---

## Task 6: 통합 런타임 검증 (사용자 환경)

**Files:** (코드 변경 없음 — 검증 전용)

내부망 Gradle 제약으로 이 Task는 사용자가 직접 수행한다. 에이전트는 체크리스트를 안내하고 결과를 기다린다.

- [ ] **Step 1: 애플리케이션 실행**

Run (사용자): `./gradlew bootRun --args='--spring.profiles.active=dev'`
Expected: 정상 기동

- [ ] **Step 2: 기본 언어(한국어) 확인**

브라우저에서 `/dashboard` 접속 (쿠키 없는 상태).
Expected: "새찬 서버 실험실", "공지사항", "서버 통계", 헤더 "대시보드" 모두 한국어.

- [ ] **Step 3: 영어 전환 확인**

헤더 언어 토글 → "English" 선택 (`?lang=en`).
Expected: "Saechan Server Lab", "Notice", "Server Statistics", 헤더 "Dashboard", 푸터 저작권 영어로 전환.

- [ ] **Step 4: 쿠키 지속성 확인**

다른 페이지 이동 후 `/dashboard` 재접속 (lang 파라미터 없이).
Expected: 영어 유지 (LOCALE 쿠키 기준).

- [ ] **Step 5: 한국어 복귀 확인**

LOCALE 쿠키 삭제 후 재접속.
Expected: 한국어로 복귀 (기본값).

---

## Self-Review

**1. Spec coverage:**
- 인프라(MessageSource/LocaleResolver/Interceptor) → Task 1, 2 ✓
- 쿠키 기반 + 기본 한국어 → Task 2 `CookieLocaleResolver.setDefaultLocale(KOREAN)` ✓
- 헤더 토글 버튼(데스크탑+모바일) → Task 3 ✓
- 샘플 적용(header/footer/dashboard 핵심) → Task 3,4,5 ✓
- 키 누락 fallback → application.yml `fallback-to-system-locale: false` + 한국어 기본 번들 ✓
- UTF-8 인코딩 → application.yml `encoding: UTF-8` ✓
- 다크모드 공존 → 언어는 쿠키, 다크모드는 localStorage로 독립 ✓
- 테스트 시나리오 → Task 6 ✓

**2. Placeholder scan:** 모든 스텝에 실제 코드/명령 포함. TBD·"적절히 처리" 없음 ✓

**3. Type consistency:** `localeChangeInterceptor` Bean명이 Task 2(정의)와 Task 2 Step 3(WebConfig 주입)에서 일치. 메시지 키는 Task 1에서 정의한 것만 Task 3/4/5에서 사용 ✓
