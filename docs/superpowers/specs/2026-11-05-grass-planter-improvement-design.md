# 잔디심기 제어·이력 개선 설계 (#62)

## 의도

#62의 원래 요구(별도 서버 > 이 프로젝트 안에서 실행·이력·제어 관리)는 `Suh-Domain-GrassPlanter`로 흡수돼 있다. 남은 문제는 실패를 추적할 수 없다는 점과 서비스 한 파일에 책임이 몰려 있다는 점이다.
사용자가 질문 없이 진행하라고 해서, 아래 범위는 모두 구현자(Claude)의 판단이다.

## 현재 문제 (GrassService.java, 558줄)

1. 자동 커밋(`executeAutoCommits`)에서 실패 이유가 남지 않음
   - `performGitHubCommit`이 `boolean`만 돌려줘서 실패해도 `errorMessage`가 비어 있음
   - 프로필별 예외(PAT 복호화 실패, 기여 수 조회 실패 등)는 로그만 찍고 이력 행이 없음
   - 메서드 전체가 `@Transactional`이라 한 프로필의 DB 오류가 다른 프로필 이력까지 롤백시킬 수 있음
2. 자동 커밋이 프로필의 `commitMessageTemplate`을 무시하고 고정 문구 `Auto-commit: Daily contribution`을 씀
3. 수동 커밋도 실패 이유가 `커밋 실행 실패` 고정 문구
4. 파일 558줄: 컨벤션상 500줄 초과는 분리 대상

## 목표

- 커밋 결과에 실패 사유를 담아 반환 (예: 기본 저장소 없음, CHANGELOG 조회 실패 상태 코드, PUT 실패 상태 코드, 예외 메시지). 수동·자동 모두 `GrassCommitLog.errorMessage`에 기록
- 자동 커밋에서 프로필별 예외가 나도 실패 이력 1건 저장 (성공·실패 공통 기록)
- 자동 커밋을 프로필 단위로 독립 저장 (메서드 전체 트랜잭션 제거, 수동 커밋과 같은 save 단위)
- 자동 커밋 메시지: 프로필 템플릿이 있으면 사용, 비어 있으면 기존 기본 문구. 템플릿 치환 규칙이 화면(`grassPlanter.html`)이나 수동 커밋에 이미 있으면 같은 규칙을 따르고, 없으면 템플릿 문자열을 그대로 사용
- GrassService를 책임별로 분리, 동작은 그대로:
  - 프로필 관리 (생성·수정·삭제·목록)
  - GitHub 연동 (기여 수 조회, CHANGELOG 커밋, 응답 파싱)
  - 커밋 실행·이력 (수동 커밋, 자동 커밋 스케줄, 이력 조회)
  - 컨트롤러·PageController가 쓰는 공개 메서드 시그니처는 유지하거나, 바뀌면 호출부를 함께 수정

## 하지 않는 것

- 실패 메일 알림 (이슈에서 후순위로 둔 항목)
- 스케줄 시각 변경 (현재 매일 23시)
- 화면 개편 (이력 목록에 이미 `errorMessage`가 표시되는지 확인만 하고, 표시되지 않으면 실패 사유 한 줄 표시만 추가)
- DB 스키마 변경 (기존 `error_message` 컬럼 사용, 길이 초과 대비 사유 문자열은 잘라서 저장)

## 검증

- 기존 `GrassServiceParseTest` 통과 (파싱 메서드가 옮겨지면 테스트도 함께 이동)
- 새 단위 테스트(Mockito): 자동 커밋에서 예외 프로필도 실패 이력 저장, 템플릿 사용/기본 문구, 수동 커밋 실패 사유 기록
- `./gradlew :Suh-Domain-GrassPlanter:test :Suh-Web:test --tests 'architecture.*' :Suh-Web:compileJava`
