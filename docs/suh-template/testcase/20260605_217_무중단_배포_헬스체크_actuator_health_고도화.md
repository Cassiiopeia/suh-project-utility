## 테스트케이스: 무중단 배포 헬스체크 actuator 고도화

| 구분 | 내용 |
|------|------|
| 이슈 번호 | #217 |
| 대상 | 배포 파이프라인 (Traefik Blue-Green) + actuator health |
| 담당자 | @Cassiiopeia |
| 작성일 | 2026-06-05 |

---

### TC-01: actuator health 공개 접근

| 항목 | 내용 |
|------|------|
| 전제조건 | prod 컨테이너 가동 중, 비로그인 상태 |
| 절차 | `curl -H "Host: lab.suhsaechan.kr" http://localhost:8079/actuator/health` (시놀로지 내부) 또는 `https://lab.suhsaechan.kr/actuator/health` 호출 |
| 기대 | HTTP `200` + `{"status":"UP"}` 반환. 302 리다이렉트(로그인 페이지) 발생하지 않음 |
| 결과 | |

### TC-02: actuator health 외 엔드포인트 미노출

| 항목 | 내용 |
|------|------|
| 전제조건 | management.exposure.include=health 설정 적용 |
| 절차 | `/actuator/env`, `/actuator/beans`, `/actuator` 등 health 외 엔드포인트 호출 |
| 기대 | health 외 엔드포인트는 노출되지 않음 (404 또는 인증 리다이렉트). 민감 정보 노출 없음 |
| 결과 | |

### TC-03: 배포 시 actuator 헬스체크 통과 후 슬롯 전환

| 항목 | 내용 |
|------|------|
| 전제조건 | `main` 브랜치 정상 빌드 가능 상태 |
| 절차 | `main`에 push → `SUH-PROJECT-UTILITY-CICD-BLUEGREEN` 워크플로우 실행 관찰 |
| 기대 | 신규 슬롯 컨테이너가 `/actuator/health` 200 응답 시 통과 로그(`헬스체크 통과 HTTP=200`) 출력 → 슬롯 전환(`blue`↔`green`) 완료 |
| 결과 | |

### TC-04: 무중단 유지 검증

| 항목 | 내용 |
|------|------|
| 전제조건 | 배포 진행 중 |
| 절차 | 배포 중 `lab.suhsaechan.kr` 루트 `/`를 1~5초 간격으로 연속 호출 (예: `for i in {1..20}; do curl -s -o /dev/null -w "%{http_code}\n" https://lab.suhsaechan.kr/; sleep 3; done`) |
| 기대 | 전 구간 HTTP `200` 유지. 502/503/연결 끊김 없음 |
| 결과 | |

### TC-05: 헬스체크 실패 시 자동 롤백

| 항목 | 내용 |
|------|------|
| 전제조건 | 의도적으로 기동 실패하는 커밋(예: 잘못된 설정)으로 배포 트리거 |
| 절차 | 배포 실행 → 신규 컨테이너가 `/actuator/health` 200을 반환하지 못하는 상황 유도 |
| 기대 | 헬스체크 타임아웃(36회×5초) 후 워크플로우 실패. 신규 컨테이너 유지(디버깅용), 기존 슬롯 그대로 유지되어 서비스 정상 |
| 결과 | |

### TC-06: old 컨테이너 자동 정리

| 항목 | 내용 |
|------|------|
| 전제조건 | 정상 배포 완료 직후 |
| 절차 | `docker ps --format '{{.Names}}' | grep suh-project-utility` 실행 |
| 기대 | in-flight 10초 대기 후 이전 슬롯 제거됨. `suh-project-utility-*` 컨테이너가 1개만 존재 |
| 결과 | |

### TC-07: ForwardedHeaderFilter HTTPS URL 생성

| 항목 | 내용 |
|------|------|
| 전제조건 | nginx → Traefik → Spring 경로, ForwardedHeaderFilter 빈 적용 |
| 절차 | `https://lab.suhsaechan.kr` 접속 → 페이지 내 생성 링크/리소스 URL의 스킴 확인, 브라우저 콘솔 확인 |
| 기대 | 생성 URL이 `https://`로 출력됨. Mixed Content 경고 없음 |
| 결과 | |

### TC-08: 빌드/배포 정상 (Spring 공통)

| 항목 | 내용 |
|------|------|
| 절차 | `SUH-PROJECT-UTILITY-CICD-BLUEGREEN` 워크플로우 빌드 단계 → Gradle 빌드 + Docker 이미지 Push 확인 |
| 기대 | 빌드 `success`. `application.yml`이 `APPLICATION_YML` secret(management 포함)으로 정상 생성되어 actuator health 활성화 |
| 결과 | |
