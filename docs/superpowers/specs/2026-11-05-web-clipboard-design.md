# 웹 클립보드 설계 (#25)

## 의도

망분리 환경에서는 같은 OS 안의 클립보드로 다른 PC와 내용을 주고받을 수 없다. 웹을 매개로 텍스트·이미지·파일을 올리고, 다른 PC에서 바로 복사하거나 내려받는다.
사용자가 질문 없이 기본안으로 진행하라고 해서, 아래 결정은 모두 구현자(Claude)의 판단이다.

## 목표

- 로그인한 사용자만 쓰는 `/clipboard` 페이지
- 올리기 3가지: 텍스트 입력 후 저장, 페이지에서 Ctrl+V(텍스트·이미지·파일), 파일 끌어다 놓기·선택
- 목록: 최신순, 항목마다 종류·생성 시각·크기
  - 텍스트: 내용 미리보기 + 복사 버튼
  - 이미지: 썸네일 미리보기 + 내려받기 버튼 (+ 가능하면 이미지 클립보드 복사)
  - 파일: 파일명·크기 + 내려받기 버튼
- 항목 하나씩 삭제 (파일도 함께 삭제)
- 대시보드 도구 영역에 진입 카드

## 하지 않는 것

- 여러 사용자 간 공유·권한 구분 (로그인 사용자 전체가 같은 목록)
- 자동 만료·용량 정리 스케줄러
- HTML 서식 보존 (HTML은 텍스트로 저장)
- 실시간 동기화 (새로고침 버튼으로 다시 조회)

## 반실패 조건

- 클립보드 파일이 로그인 없이 열리면 실패. 기존 `/uploads/**`는 공개 경로라 쓰지 않는다
- 사용자 파일명이 저장 경로에 들어가면 실패 (경로 이동 공격)

## 구조

새 도메인 모듈 `Suh-Domain-Clipboard` (패키지 `me.suhsaechan.clipboard`), Task 모듈과 같은 구성.

| 계층 | 내용 |
|---|---|
| entity | `ClipboardItem` (BasePostgresEntity 상속): `clipboardItemId`(UUID), `itemType`(enum TEXT·IMAGE·FILE), `textContent`(TEXT), `originalFileName`, `storedFileName`(UUID 기반), `contentType`, `fileSizeBytes` |
| repository | `ClipboardItemRepository`: 최신순 전체 조회 |
| service | `ClipboardService`: 텍스트 저장, 파일 저장(이미지 여부는 contentType `image/`로 판정), 목록, 삭제, 파일 로드 |
| dto | `ClipboardRequest`(textContent, file, clipboardItemId), `ClipboardResponse`(items, item) |
| web | `ClipboardController` `/api/clipboard/*`, `PageController` `/clipboard`, `pages/clipboard.html` |

### API

| 경로 | 방식 | 응답 |
|---|---|---|
| `/api/clipboard/create` | POST multipart (textContent 또는 file) | 201 + item |
| `/api/clipboard/list` | POST multipart | 200 + items |
| `/api/clipboard/delete` | POST multipart (clipboardItemId) | 204 |
| `/api/clipboard/file/{clipboardItemId}` | GET | 파일 본문 (이미지 미리보기·내려받기) |

파일 조회만 GET이다. `<img src>`와 내려받기 링크가 그대로 쓸 수 있게 하기 위함. 로그인 필요 경로이므로 공개 목록에 넣지 않는다.

### 저장소

- 경로: `application.yml` `clipboard.storage-dir: ${CLIPBOARD_STORAGE_DIR:/mnt/suh-project-utility/clipboard}`
  - 운영 컨테이너 볼륨 `/volume1/projects/suh-project-utility:/mnt/suh-project-utility`에 있어 재배포 후에도 유지
  - 인프라 경로라 ServerOptionKey 대신 `@Value`
- 저장 파일명: `{UUID}` (확장자 없음). 원래 이름은 DB에만 저장
- 디렉터리 없으면 생성
- 저장 실패 시 DB 행을 남기지 않음, 삭제 시 파일 삭제 실패는 로그만

### 오류

`ErrorCode`에 추가: `CLIPBOARD_ITEM_NOT_FOUND`(404), `CLIPBOARD_EMPTY_CONTENT`(400), `CLIPBOARD_FILE_STORAGE_FAILED`(500).

### 스키마

Flyway `V2_5_115__create_clipboard_item.sql`, `CREATE TABLE IF NOT EXISTS`.

## 화면

- DaisyUI 시맨틱 토큰만 사용, 인라인 style 금지
- 문구는 `messages_ko/en.properties` + `header.html` `window.MSG`
- 알림은 `showToast`, HTML 출력은 `common.js`의 `escapeHtml`
- 텍스트 복사는 `navigator.clipboard.writeText`

## 검증

- 서비스 단위 테스트: 텍스트 저장, 빈 입력 거부, 파일 저장 시 UUID 파일명, 삭제 시 파일 제거, 없는 항목 404
- `./gradlew test` (아키텍처 규칙 테스트 포함), `npm run build:css`
- 배포 후 운영에서 페이지 로그인 리다이렉트, API 동작 확인
