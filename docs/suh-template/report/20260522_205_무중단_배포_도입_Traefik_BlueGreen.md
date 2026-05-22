# 무중단 배포 도입 — Traefik Blue-Green 패턴 적용

- **작성일**: 2026-05-22
- **이슈**: [#205](https://github.com/Cassiiopeia/suh-project-utility/issues/205)
- **작성자**: SUH SAECHAN
- **버전**: v2.5.37 예정

## 1. 배경

기존 `SUH-PROJECT-UTILITY-CICD.yaml` 배포 워크플로우는 다음 순서로 동작했다.

1. GitHub Actions 에서 Docker 이미지 빌드 & DockerHub Push
2. SSH 로 Synology NAS 접속
3. `docker rm -f suh-project-utility` (기존 컨테이너 즉시 삭제)
4. `docker run -d -p 8090:8080 ... suh-project-utility` (새 컨테이너 기동)

3단계 `docker rm -f` 시점부터 4단계 컨테이너의 Spring Boot 가 `Started` 로그를 찍을 때까지 사용자 접속 불가 상태가 발생했다. 보통 30~60초의 다운타임이 매 배포마다 노출되어, 활성 사용자 입장에서 배포를 명확히 인지하는 문제가 있었다.

## 2. 목표

- Traefik 기반 Blue-Green 무중단 배포 도입 (다운타임 0)
- 사용자 도메인 `lab.suhsaechan.kr` 그대로 유지
- 기존 워크플로우 보존하여 비상 롤백 안전망 확보
- 1주일 검증 단계 후 점진 전환

## 3. 현 인프라 분석

| 구성 요소 | 현 상태 |
|----------|--------|
| 외부 진입 | `lab.suhsaechan.kr` (Cloudflare CNAME → `suh-project.synology.me`) |
| Synology DSM 역방향 프록시 | `SUH-LAB 443→8090` 규칙: `lab.suhsaechan.kr:443` → `localhost:8090` |
| production 컨테이너 | `suh-project-utility` 단일, 포트 8090 직접 바인딩 |
| Traefik 컨테이너 | 기존 가동 중. `traefik-network` 네트워크, `web` entrypoint `:8079` |
| Traefik 활용 사례 | PR Preview 워크플로우 (`PROJECT-SPRING-SYNOLOGY-PR-PREVIEW.yaml`) 가 `*.pr.suhsaechan.kr` 라우팅에 활용. production 만 Traefik 미경유 상태였음. |
| Selenium 네트워크 | `selenium-chrome-network` — Docker 모듈의 WebDriver 통신용. production 컨테이너가 연결되어 있음. |

## 4. 설계 결정

| 항목 | 결정 |
|------|------|
| 무중단 패턴 | Blue-Green Deployment, Traefik 라벨 기반 트래픽 스위칭 |
| 외부 진입점 | `lab.suhsaechan.kr` (변경 없음) |
| Synology 역방향 프록시 | 대상 포트 `8090` → `8079` (Traefik web entrypoint) 1회 변경 |
| 새 워크플로우 | `.github/workflows/SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml` 신규 |
| 기존 워크플로우 | `SUH-PROJECT-UTILITY-CICD.yaml` 그대로 유지 (롤백 안전망) |
| 컨테이너 명명 | `suh-project-utility-blue`, `suh-project-utility-green` |
| 활성 토글 결정 | `docker ps` 의 Traefik 라벨 부착 여부로 active 색 판별 |
| Docker 네트워크 | `traefik-network` (필수) + `selenium-chrome-network` (Selenium 사용) |
| 헬스체크 | Traefik healthcheck 라벨 + 워크플로우 직접 HTTP 헬스체크 + 로그 패턴 폴백 |
| in-flight 처리 대기 | 라벨 부착 후 10초 sleep (Traefik 라벨 동기화 + 기존 요청 처리 여유) |
| 자동 롤백 | 헬스체크 실패 시 new 만 삭제, old 유지 → 사실상 롤백 |
| Dockerfile HEALTHCHECK | `wget /actuator/health` 호출하여 컨테이너 상태 가시화 |

## 5. 점진 전환 전략

### Phase A — 검증 단계 (현재)
- 새 워크플로우 `SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml` 작성 완료
- 트리거: `workflow_dispatch` 만 활성화 → 수동 발동으로 검증
- 기존 `SUH-PROJECT-UTILITY-CICD.yaml` 의 `push: main` 자동 트리거 유지 → 평상 배포는 기존 방식 계속
- 새 워크플로우 수동 실행 시 `suh-project-utility-blue` 컨테이너가 Traefik 안에서만 활성화됨. 시놀로지 역방향 프록시가 이미 `8079` 로 변경되어 있으므로 새 컨테이너가 사용자 트래픽을 받음. 기존 8090 컨테이너는 시놀로지 변경 시점부터 사용자 트래픽을 받지 못하므로 별도로 정리하면 됨.

### Phase B — 전환 단계 (1주일 안정 운영 후)
- 새 워크플로우의 `# push: branches: [main]` 주석 해제 → 자동 트리거 활성화
- 기존 워크플로우의 `on.push.branches` 제거, `workflow_dispatch` 만 남김 → 비상 롤백 전용으로 보존

## 6. 동작 흐름

### 6-1. 트래픽 경로 (변경 후)

```
사용자 브라우저
   │ https://lab.suhsaechan.kr
   ▼
Cloudflare (Proxied, CNAME → suh-project.synology.me)
   │
   ▼
Synology NAS (220.85.152.63)
   │ DSM 역방향 프록시 SUH-LAB 443→8079
   ▼
localhost:8079 (Traefik web entrypoint)
   │ Host(`lab.suhsaechan.kr`) 라우터 매칭
   ▼
suh-project-utility-blue 또는 -green
(헬스체크 통과한 인스턴스만 트래픽 받음)
```

### 6-2. Blue-Green 배포 사이클

```
[T0] 평상시
  suh-project-utility-blue  (active, label O) ◄── 100% 트래픽
  suh-project-utility-green (없음)

[T1] 새 배포 시작 (워크플로우 실행)
  blue  (active, label O)  ◄── 100% 트래픽 유지
  green (기동 중, label X) 기동 대기

[T2] green 헬스체크 통과 + 라벨 부여
  blue  (active, label O)  ◄── 50%
  green (active, label O)  ◄── 50%  Traefik 자동 분배

[T3] blue 라벨 제거 + 10초 sleep
  blue  (label X)          0% (in-flight 처리)
  green (active, label O)  ◄── 100%

[T4] blue 컨테이너 삭제
  green (active, label O)  ◄── 100%

[다음 배포] green ↔ blue 역할 스왑, 동일 사이클
```

**다운타임 0초** — T1~T4 전 구간에서 최소 1개 인스턴스가 트래픽 처리.

## 7. 변경 사항

### 7-1. 신규 파일
- `.github/workflows/SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml`
  - Phase A 트리거: `workflow_dispatch` 만 (주석으로 Phase B 진입 안내 포함)
  - build job: 기존과 동일한 Gradle 빌드 + Docker 이미지 Push
  - deploy job: Blue-Green 토글 스크립트 (active 판별 → new 기동 → 헬스체크 → old 정리)
  - Traefik healthcheck 라벨로 기동 중인 컨테이너는 자동 트래픽 차단
- `docs/superpowers/specs/2026-05-22-zero-downtime-deployment-design.md` — 설계 spec
- `docs/superpowers/plans/2026-05-22-zero-downtime-deployment.md` — 구현 plan
- 본 보고서

### 7-2. 수정 파일
- `Dockerfile`: `wget` 설치 + `HEALTHCHECK` 추가
  - Docker `docker ps` 에서 health 상태 표시 가능
  - Traefik healthcheck 라벨과 독립적인 이중 안전망

### 7-3. 변경 없는 파일
- `.github/workflows/SUH-PROJECT-UTILITY-CICD.yaml` — 기존 워크플로우 보존
- `Suh-Web/src/main/resources/application.yml` — Actuator `/actuator/health` 가 이미 200 응답하므로 변경 불필요

### 7-4. 인프라 변경 (수동, 완료)
- Synology DSM 역방향 프록시 `SUH-LAB 443→8090` 규칙의 대상 포트 `8090` → `8079` 변경 (사용자가 GUI 에서 적용 완료)

## 8. 리스크 & 완화책

| 리스크 | 완화책 |
|--------|--------|
| Traefik healthcheck 라벨 미지원 (v2 미만) | Phase A 검증에서 확인. v2 미만이면 라벨 없이 기동 → 헬스체크 후 재기동 방식 폴백. |
| 새 컨테이너 기동 실패 | old 유지, new 만 삭제 → 자동 롤백 효과. 워크플로우 exit 1 로 알림. |
| Flyway 마이그레이션 비호환 (blue/green 동시 가동 시점) | `ADD COLUMN IF NOT EXISTS` 등 backward-compatible 마이그레이션만 허용 (CLAUDE.md 규칙). DROP COLUMN 은 단계 분리. |
| 세션 휘발 | Spring Session + Redis 이미 사용 중 → 안전. |
| 파일 업로드 동시성 | blue/green 이 같은 볼륨 공유. 기존 단일 컨테이너 멀티 스레드 쓰기와 동등 수준. |
| 첫 마이그레이션 시 시놀로지 포트 변경 순간 | 1회성 수 초 다운타임 가능. 이후 모든 배포는 다운타임 0. |
| Traefik dashboard 라우터 이름 충돌 | router 이름 `suh-project-utility` 로 단일화. PR Preview 는 `suh-project-utility-pr-N` 이라 충돌 없음. |
| 기존 워크플로우와 새 워크플로우 동시 main push 트리거 | Phase A 에서는 새 워크플로우 `workflow_dispatch` 만 활성화 → 충돌 원천 차단. Phase B 진입 시 기존 push 트리거 제거. |

## 9. 검증 계획

### 9-1. Phase A — 수동 검증
1. main 에 본 변경 사항 commit & push
2. GitHub Actions 페이지에서 `SUH-PROJECT-UTILITY-CICD-BLUEGREEN` 워크플로우 수동 실행 (Run workflow)
3. 로그에 다음 출력 확인
   - `🎨 active=none → new=suh-project-utility-blue, old=suh-project-utility-green`
   - `✅ HTTP 헬스체크 통과` 또는 `✅ 로그 패턴 헬스체크 통과`
   - `✅ 무중단 배포 완료. active=suh-project-utility-blue`
4. NAS SSH 로 컨테이너 상태 확인
   ```
   sudo docker ps --filter "name=suh-project-utility" \
     --format "table {{.Names}}\t{{.Status}}"
   ```
5. 외부 접속 테스트
   ```
   curl -sw "\nHTTP %{http_code}\n" https://lab.suhsaechan.kr/actuator/health
   ```

### 9-2. 무중단 검증 (1주일 안정 운영 중 1회)
별도 머신에서 1초 간격 응답 모니터링 실행:
```bash
while true; do
  CODE=$(curl -sw "%{http_code}" -o /dev/null --max-time 5 https://lab.suhsaechan.kr/actuator/health)
  echo "[$(date '+%H:%M:%S')] HTTP=${CODE}"
  sleep 1
done
```
동시에 `SUH-PROJECT-UTILITY-CICD-BLUEGREEN` 워크플로우 수동 실행. 배포 전체 시간 동안 HTTP 200 만 출력되면 무중단 성공.

### 9-3. 롤백 검증 (선택)
의도적 깨진 commit 으로 배포 시도 → 120초 헬스체크 타임아웃 → old 컨테이너 유지 → 사용자 트래픽 정상 응답 확인.

## 10. Phase B 진입 조건

다음 모두 충족 시 Phase B 진입:
- Phase A 수동 검증 (9-1) 모든 단계 통과
- 무중단 검증 (9-2) HTTP 200 끊김 0회
- 1주일 (2026-05-29 까지) 안정 운영 (수동 배포 5회 이상 성공)

Phase B 작업:
1. `.github/workflows/SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml` 의 `# push: branches: [main]` 주석 해제
2. `.github/workflows/SUH-PROJECT-UTILITY-CICD.yaml` 의 `on.push.branches` 항목 제거 (`workflow_dispatch` 만 남김)
3. PR & merge

## 11. 후속 작업 (별도 이슈 권장)

- 로그 영속화: logback 파일 appender + 볼륨 마운트 + log rotation
- Slack/Discord 배포 알림 통합
- 자동 롤백 메커니즘 강화: 헬스체크 실패 시 이전 commit 으로 자동 재배포
- Traefik dashboard 의 production 라우터 가시성 점검

## 12. 참고

- 설계 spec: [`docs/superpowers/specs/2026-05-22-zero-downtime-deployment-design.md`](../../superpowers/specs/2026-05-22-zero-downtime-deployment-design.md)
- 구현 plan: [`docs/superpowers/plans/2026-05-22-zero-downtime-deployment.md`](../../superpowers/plans/2026-05-22-zero-downtime-deployment.md)
- 기존 워크플로우: `.github/workflows/SUH-PROJECT-UTILITY-CICD.yaml`
- 신규 워크플로우: `.github/workflows/SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml`
- Traefik healthcheck 공식 문서: https://doc.traefik.io/traefik/routing/services/#health-check
