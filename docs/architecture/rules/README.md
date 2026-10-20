# 아키텍처 규칙

규칙 하나 = `<id>.rule` 파일 하나. `Suh-Web/src/test/java/architecture/ArchitectureRulesTest.java`가 이 폴더를 읽어 `./gradlew test`에서 판정한다.

## `<id>.rule`

```
description=new JSch() 직접 생성 금지 (SshCommandExecutor.openSession 사용)
paths=Suh-Common/src/main/java/,Suh-Domain-Docker/src/main/java/
extensions=.java
exclude=Suh-Common/src/main/java/me/suhsaechan/common/util/SshCommandExecutor.java
pattern=new JSch\(\)
```

- `paths`·`exclude`: 레포 루트 기준 경로 접두사, 쉼표 구분. glob 아님
- `pattern`: 정규식 (여러 줄 모드). 첫 `=` 뒤 전부가 값
- `paths` 비우면 레포 전체
- 단일 원본 파일(공통 지점 자체)은 `exclude`로 뺀다

## `<id>.allow` (동결 목록, 현재 없음)

- 규칙을 만들 때 고치지 못한 기존 위반 파일 목록. 한 줄에 경로 하나, `#` 주석
- **줄을 지울 수만 있고 더할 수 없다.** 새 위반은 코드를 고친다
- 고친 파일이 목록에 남아 있으면 테스트가 실패한다 — 그 줄을 지운다

판정: 위반 − 목록 = 새 위반(실패), 목록 − 위반 = 고쳐진 항목(목록에서 지우라고 실패).
