# Spring + Thymeleaf 한/영 i18n 기반 구축 — 설계 문서

- 작성일: 2026-06-16
- 대상 프로젝트: suh-project-utility (Spring Boot 3.4.2, Thymeleaf)
- 목표 범위: **기반 인프라 구축 + 샘플 페이지 검증** (전체 페이지 일괄 적용 아님)

---

## 1. 배경 및 목표

현재 프로젝트는 i18n 설정이 전혀 없으며(`messages.properties`, `MessageSource`, `LocaleResolver` 부재), 모든 HTML 텍스트가 한국어로 하드코딩되어 있다.

본 작업의 목표는 **Spring 표준 i18n 인프라를 구축**하고, HTML에서 메시지 프로퍼티(`#{...}`)로 한/영을 처리하는 **방법/패턴을 확립**하는 것이다. 샘플로 공통 fragment(header/footer)와 dashboard 페이지에 적용해 동작을 검증한다. 나머지 페이지는 동일 패턴으로 점진 확장한다.

### 결정 사항 요약

| 항목 | 결정 |
|------|------|
| 작업 범위 | 기반 인프라 구축 + 샘플 검증 |
| 언어 전환 방식 | 헤더 토글 버튼 + 쿠키 저장 (`CookieLocaleResolver`) |
| 기본 언어 | 한국어 고정 (쿠키 없을 시) |
| 샘플 적용 대상 | header/footer fragment + dashboard 페이지 |

---

## 2. 아키텍처

```
HTML(#{key}) → Thymeleaf → MessageSource → messages_{locale}.properties
                                ↑
                        LocaleResolver(쿠키에서 현재 언어 결정)
                                ↑
              LocaleChangeInterceptor(?lang=ko/en 요청 시 쿠키 갱신)
```

- 메시지 키는 HTML에서 `#{...}` 표현식으로 참조한다.
- 실제 텍스트는 `messages.properties`(한국어, 기본/fallback)와 `messages_en.properties`(영어)로 분리한다.
- 현재 로케일은 쿠키 기반(`CookieLocaleResolver`)으로 결정하며, 쿠키가 없으면 한국어로 fallback한다.

---

## 3. 구성 요소

| 구성요소 | 위치 | 역할 |
|---------|------|------|
| `messages.properties` | `Suh-Web/src/main/resources/` | 한국어 (기본/fallback) |
| `messages_en.properties` | `Suh-Web/src/main/resources/` | 영어 |
| `I18nConfig` | `Suh-Web/src/main/java/me/suhsaechan/web/config/` | `CookieLocaleResolver`(기본 KOREAN) + `LocaleChangeInterceptor`(`lang` 파라미터) Bean 등록 |
| `application.yml` | `Suh-Web/src/main/resources/` | `spring.messages.basename`, `encoding=UTF-8` 설정 |
| 헤더 토글 | `fragments/header.html` | 한/영 버튼 (데스크탑·모바일 2곳, `?lang=` 링크) |

### 프로젝트 규약 준수 사항

- Config 클래스는 `Suh-Web/.../web/config/`에 배치 (CLAUDE.md "Config 설정 위치" 규칙).
- 인라인 스타일 금지 — 토글 버튼은 클래스 기반 스타일링 (CSP 준수).
- `MessageSource`는 Spring Boot 자동 구성(`spring.messages.*`)을 활용하고, `LocaleResolver`/`LocaleChangeInterceptor`만 직접 Bean 등록한다.

---

## 4. 데이터 흐름 (언어 전환)

1. 사용자가 헤더의 "EN" 클릭 → `?lang=en`이 붙은 현재 URL로 이동
2. `LocaleChangeInterceptor`가 `lang=en` 감지 → `CookieLocaleResolver`가 쿠키에 `en` 저장
3. 이후 모든 요청은 쿠키 기준으로 로케일 결정 → `#{...}`가 영어로 렌더링
4. 쿠키가 없으면 한국어(기본값)로 렌더링

---

## 5. 메시지 키 네이밍 규칙

`{영역}.{컴포넌트}.{의미}` 형태로 통일한다.

```properties
# messages.properties (한국어)
header.nav.translator=번역기
header.nav.dashboard=대시보드
footer.copyright=© 2026 SUH 프로젝트 유틸리티
dashboard.title=대시보드
```

```properties
# messages_en.properties (영어)
header.nav.translator=Translator
header.nav.dashboard=Dashboard
footer.copyright=© 2026 SUH Project Utility
dashboard.title=Dashboard
```

> 실제 키 목록은 구현 시 header/footer/dashboard의 하드코딩 텍스트를 추출하며 확정한다.

---

## 6. 적용 범위 (이번 작업)

**포함:**
- 인프라: `I18nConfig`, `messages.properties`, `messages_en.properties`, application.yml 설정
- 샘플 적용: `header.html`, `footer.html`, `dashboard.html`의 하드코딩 한글 텍스트 → `#{key}` 치환
- 검증: 토글 클릭 시 헤더/푸터/대시보드가 한↔영 전환되는지 확인

**제외:**
- 나머지 13개 페이지 (동일 패턴으로 추후 점진 확장)

---

## 7. 에러 처리 / 엣지 케이스

- **키 누락 시**: `messages.properties`(한국어)가 fallback. 영어 파일에 키가 없으면 한국어로 표시된다.
- **인코딩**: properties 파일은 UTF-8로 명시 (한글 깨짐 방지). Spring Boot의 `spring.messages.encoding=UTF-8` 사용.
- **다크모드와의 공존**: 언어 토글은 다크모드(`localStorage`)와 독립적인 쿠키 기반 메커니즘 — 서로 간섭하지 않는다.

---

## 8. 테스트 방법

`./gradlew bootRun --args='--spring.profiles.active=dev'` 실행 후:

1. 대시보드 접속 → 한국어 표시 확인
2. 헤더 "EN" 클릭 → 헤더/푸터/대시보드 영어 전환 확인
3. 다른 페이지 이동 후 재접속 → 영어 유지(쿠키) 확인
4. 쿠키 삭제 후 재접속 → 한국어 복귀 확인
