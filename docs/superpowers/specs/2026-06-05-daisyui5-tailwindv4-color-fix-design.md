# DaisyUI 5 순정 색 복구 (Tailwind v4 전환) — 설계

## 배경 / 문제

전 페이지에서 DaisyUI 버튼·배지 색이 표시되지 않는다. `btn-primary`(번역하기)가 파란 배경 없이 테두리만, `btn-outline`(복사하기)은 거의 안 보임.

### 근본 원인

`fragments/header.html`의 CDN 셋업이 비호환 조합이다.

```
https://cdn.tailwindcss.com            → Tailwind v3 Play CDN
daisyui@5.4.2/daisyui.css              → DaisyUI 5 (Tailwind v4 전용)
```

DaisyUI 5의 `daisyui.css`는 Tailwind v4의 `@theme`/`@plugin` 토큰 시스템으로 색을 깐다. Tailwind v3 CDN은 이를 처리하지 못해 `.btn` 모양(정적 CSS)만 적용되고 `.btn-primary` 등 색 토큰이 누락된다.

## 목표

DaisyUI 5를 유지하면서 Tailwind v4 CDN으로 전환해 순정 색을 복구한다. DaisyUI 컴포넌트 클래스는 그대로 두고(다운그레이드 없음), 셋업만 정합으로 맞춘다.

## 영향 범위 조사 결과

| 항목 | 현황 | 비고 |
|------|------|------|
| `tailwind.config = {}` JS 설정 | header 1곳 (darkMode만) | v4 CSS-first 방식으로 전환 |
| 임의값 `-[...]` 클래스 | 2개 (text 크기, header) | v4 호환 OK |
| `bg-opacity-*` 등 제거 유틸 | 0개 | 없음 |
| `flex-shrink-*` | 11개 (dashboard 8 / header 1 / dockerLogs 1 / noticeManagement 1) | `shrink-*`로 치환 |
| DaisyUI 컴포넌트 | btn/badge/card 다량 | v5 유지 → 안 깨짐 |
| common.css `--color-primary` oklch | 이미 정의 | v4 친화적, 손대지 않음 |

깨질 만한 레거시 유틸이 0개라 마이그레이션 부담이 작다.

## 변경 사항

### 변경 1 — header.html CDN 셋업 교체 (핵심)

기존 (21~27줄):
```html
<script src="https://cdn.tailwindcss.com"></script>
<link href="https://cdn.jsdelivr.net/npm/daisyui@5.4.2/daisyui.css" rel="stylesheet" type="text/css"/>
<script>
  tailwind.config = { darkMode: ['class', '[data-theme="dark"]'] }
</script>
```

변경 후:
```html
<script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>
<style type="text/tailwindcss">
  @import "tailwindcss";
  @plugin "daisyui";
  @custom-variant dark (&:where([data-theme="dark"], [data-theme="dark"] *));
</style>
```

- `@tailwindcss/browser@4` — Tailwind v4 브라우저 빌드 (CSS-first, `@plugin` 처리 가능)
- `@plugin "daisyui"` — DaisyUI 5 정식 연결로 색 토큰 적용
- `tailwind.config` JS 제거 — v4는 CSS 내 `@custom-variant`로 다크모드 처리 (`data-theme="dark"` 유지)
- 기존 `daisyui.css` `<link>` 제거 (플러그인이 대체)

### 변경 2 — `flex-shrink-*` → `shrink-*` (11개)

v4에서 `flex-shrink-0`이 폐기되고 `shrink-0`으로 바뀐다. 대상: dashboard(8) / header(1) / dockerLogs(1) / noticeManagement(1).

### 변경하지 않는 것

- `common.css` — `--color-primary` oklch 변수가 이미 v4 친화적이라 그대로 동작 예상
- DaisyUI 컴포넌트 클래스 — v5 유지라 안 깨짐
- 임의값 `text-[...]` 2개 — v4 호환

## 리스크

- **런타임 컴파일**: `@tailwindcss/browser@4`는 페이지 로드 시 JS가 CSS를 생성한다. v3 Play CDN보다 초기 렌더가 느릴 수 있고 FOUC 가능성. 전 페이지 영향.
- **다크모드 셀렉터 충돌**: common.css의 `[data-theme]` 셀렉터가 v4 `@custom-variant`와 충돌하지 않는지 눈으로 확인 필요.
- **빌드 검증 불가**: 내부망이라 직접 띄워 전 페이지(대시보드·번역·docker-logs) 색/다크모드를 검증해야 한다.

## 성공 기준

- 번역하기 버튼이 파란 배경, 복사하기가 회색 테두리로 표시
- 다크모드 토글 정상 동작
- 기존 레이아웃이 깨지지 않음
