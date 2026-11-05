# 아이디어 보따리 설계 (#21)

## 의도

일상에서 "이거 만들어볼까?" 하고 떠오른 아이디어를 키워드 한 줄로 바로 적어 두는 개인 저장소. 상용 서비스가 아니라 본인 기록용이다.
이슈 요구: 대시보드에서 바로 접근, 세부 UI는 미정. 사용자가 질문 없이 기본안으로 진행하라고 해서, 아래 결정은 모두 구현자(Claude)의 판단이다.

## 목표

- 대시보드(로그인 영역)에 빠른 입력 카드: 키워드 입력 + Enter로 즉시 저장, 최근 5개 표시, 전체 보기 링크
- `/ideas` 페이지: 전체 목록, 추가·수정·삭제, 상태 변경, 키워드 검색(화면 내 필터)
- 항목: 키워드(필수, 한 줄), 메모(선택, 여러 줄), 상태
- 상태 4개: 아이디어(IDEA), 진행중(IN_PROGRESS), 완료(DONE), 보류(DROPPED)

## 하지 않는 것

- 태그·카테고리, 첨부파일·영상 업로드, 우선순위 정렬
- 비로그인 공개
- 아이디어를 Task 목표로 변환하는 기능

## 구조

새 도메인 모듈 `Suh-Domain-Idea` (패키지 `me.suhsaechan.idea`), Task 모듈과 같은 구성.

| 계층 | 내용 |
|---|---|
| entity | `IdeaItem` (BasePostgresEntity 상속): `ideaItemId`(UUID), `keyword`(not null, 200자), `memo`(TEXT), `ideaStatus`(enum, 기본 IDEA) |
| constant | `IdeaStatus` enum (표시 이름 포함) |
| repository | `IdeaItemRepository`: 최신순 전체, 최근 N개 |
| service | `IdeaService`: 생성, 목록, 최근 목록, 수정, 상태 변경, 삭제 |
| dto | `IdeaRequest`(ideaItemId, keyword, memo, ideaStatus), `IdeaResponse`(ideaItems, ideaItem) |
| web | `IdeaController` `/api/idea/*`, `PageController` `/ideas`, `pages/ideaPocket.html` |

### API (POST multipart)

| 경로 | 응답 |
|---|---|
| `/api/idea/create` | 201 + ideaItem |
| `/api/idea/list` | 200 + ideaItems |
| `/api/idea/recent` | 200 + ideaItems (최근 5개) |
| `/api/idea/update` | 200 + ideaItem (키워드·메모·상태) |
| `/api/idea/delete` | 204 |

모두 로그인 필요. 키워드 공백이면 `IDEA_EMPTY_KEYWORD`(400), 없는 항목은 `IDEA_ITEM_NOT_FOUND`(404).

### 스키마

Flyway `V{현재 version.yml}__create_idea_item.sql`, `CREATE TABLE IF NOT EXISTS`. 같은 버전 파일이 이미 있으면 다음 배포 버전 번호를 쓴다.

## 화면

- DaisyUI 시맨틱 토큰, 상태는 `badge` (IDEA neutral, IN_PROGRESS info, DONE success, DROPPED ghost)
- 수정은 `modal`
- 문구 i18n, 알림 `showToast`, 출력 `escapeHtml`

## 검증

- 서비스 단위 테스트: 생성, 빈 키워드 거부, 상태 변경, 삭제, 없는 항목 404
- `./gradlew test`, `npm run build:css`
- 배포 후 운영에서 로그인 리다이렉트 확인
