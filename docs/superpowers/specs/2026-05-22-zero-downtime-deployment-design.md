# Suh-Project-Utility 무중단 배포(Zero-Downtime Deployment) 설계

## 배경

기존 `SUH-PROJECT-UTILITY-CICD.yaml` 워크플로우는 다음 절차로 배포 수행:

1. GitHub Actions 에서 Docker 이미지 빌드 & Push
2. SSH 로 Synology NAS 접속
3. `docker rm -f suh-project-utility` (기존 컨테이너 즉시 삭제)
4. `docker run -d -p 8090:8080 ... suh-project-utility` (새 컨테이너 기동)

**문제점:**
- 3단계 `docker rm -f` 부터 4단계 컨테이너의 Spring Boot 가 `Started` 로그를 찍을 때까지 (보통 30~60초) `lab.suhsaechan.kr` 접속 불가
- 사용자 입장에서 매 배포마다 명확한 다운타임 발생
- 본 프로젝트는 활성 사용 도구라 다운타임 체감도 높음

## 현 인프라 분석

- **외부 진입**: `lab.suhsaechan.kr` (Cloudflare CNAME → `suh-project.synology.me` → Synology NAS `220.85.152.63`)
- **Synology 역방향 프록시 규칙 `SUH-LAB 443→8090`**: `lab.suhsaechan.kr:443` → `localhost:8090`
- **production 컨테이너**: 포트 8090 직접 바인딩, Traefik 미경유
- **Traefik 컨테이너 기존 가동 중**: `traefik-network` Docker 네트워크, `web` entrypoint = `:8079`
- **Traefik 활용 사례**: PR Preview 워크플로우 (`PROJECT-SPRING-SYNOLOGY-PR-PREVIEW.yaml`) 가 `*.pr.suhsaechan.kr` 도메인 라우팅에 활용 중. production 만 Traefik 미경유 상태.

## 목표

- `lab.suhsaechan.kr` 도메인 그대로 유지 (사용자 URL 불변)
- 배포 다운타임 0 (사용자가 배포를 인지할 수 없음)
- 기존 Traefik 인프라 활용, 별도 reverse proxy 컨테이너 추가 없음
- Synology DSM 역방향 프록시 규칙 1회만 수정 (대상 포트 8090 → 8079)
- 이후 모든 배포는 워크플로우 자체에서 Blue-Green 토글로 처리

## 비목표

- Traefik 컨테이너 재구성/재시작 (기존 가동 그대로 사용)
- Synology DSM nginx 원본 설정 파일 수정 (DSM GUI 클릭 1회만)
- 로그 영속화 (logback 파일 appender + 볼륨) — 본 설계 범위 외, 추후 별도 진행
- 멀티 인스턴스 분산 락, 세션 클러스터링 추가 작업 (Redis 기반 Spring Session 이미 사용 중)
- 자동 롤백 (헬스체크 실패 시 새 컨테이너만 삭제, old 컨테이너는 그대로 유지하므로 사실상 롤백 효과)

## 결정 사항

| 항목 | 결정 |
|------|------|
| 무중단 패턴 | Blue-Green Deployment, Traefik label 기반 트래픽 스위칭 |
| 외부 진입점 | `lab.suhsaechan.kr` (변경 없음) |
| Synology 역방향 프록시 | 대상 포트 `8090` → `8079` (Traefik web entrypoint) 1회 수정 |
| Traefik entrypoint | 기존 `web` (`:8079`) 그대로 사용, 추가 entrypoint 안 만듦 |
| Docker 네트워크 | `traefik-network` (기존 PR Preview 와 동일) |
| 컨테이너 명명 | `suh-project-utility-blue`, `suh-project-utility-green` |
| 활성 토글 결정 | `docker ps` 로 현재 살아있는 쪽 확인, 반대편을 새 배포 대상으로 |
| 트래픽 스위치 메커니즘 | Traefik label `traefik.enable=true` + `Host(\`lab.suhsaechan.kr\`)` 부여/제거 |
| 헬스체크 | 기존 PR Preview 패턴 그대로 (HTTP `/actuator/health` → 로그 패턴 폴백, 최대 120초) |
| in-flight 처리 대기 | 라벨 제거 후 10초 sleep (Traefik 라벨 동기화 + 기존 요청 처리 여유) |
| 첫 마이그레이션 | 1회성 절차 — Section "마이그레이션 절차" 참조 |
| 로그 보존 | 본 설계 범위 외, 기본 `docker logs` 그대로 사용 |

## 아키텍처

### 변경 후 트래픽 흐름

```
사용자 브라우저
   │
   │ https://lab.suhsaechan.kr
   ▼
Cloudflare (Proxied, CNAME → suh-project.synology.me)
   │
   ▼
Synology NAS (220.85.152.63)
   │
   ▼ DSM 역방향 프록시 규칙 SUH-LAB 443→8079
   │ lab.suhsaechan.kr:443 → localhost:8079
   ▼
Traefik 컨테이너 (traefik-network, entrypoint web=:8079)
   │
   ▼ Host(`lab.suhsaechan.kr`) 라우터 매칭
   │
   ├──→ suh-project-utility-blue  (label 있으면 트래픽 받음)
   │     └─ 헬스체크 통과 한 인스턴스만 라우팅 대상
   │
   └──→ suh-project-utility-green (label 있으면 트래픽 받음)
         └─ 헬스체크 통과 한 인스턴스만 라우팅 대상
```

**핵심 원리:**
- 같은 `Host` 룰이 붙은 컨테이너 N개 = Traefik 자동 로드밸런싱
- 라벨 부여 = 트래픽 합류, 라벨 제거 = 트래픽 차단
- Traefik 은 Docker socket watch 로 라벨 변경 즉시 감지

### Blue-Green 배포 사이클

```
[T0] 평상시
  suh-project-utility-blue  (active, label O) ◄── 100% 트래픽
  suh-project-utility-green (없음)

[T1] 새 배포 시작
  suh-project-utility-blue  (active, label O) ◄── 100% 트래픽
  suh-project-utility-green (기동 중, label X)   기동 대기

[T2] green 헬스체크 통과 + label 부여
  suh-project-utility-blue  (active, label O) ◄── 50%
  suh-project-utility-green (active, label O) ◄── 50%
                                                  Traefik 자동 분배

[T3] blue label 제거 (10초 sleep)
  suh-project-utility-blue  (label X)          0% (in-flight 처리)
  suh-project-utility-green (active, label O) ◄── 100%

[T4] blue 컨테이너 삭제
  suh-project-utility-green (active, label O) ◄── 100%

[다음 배포] green ↔ blue 역할 스왑, 동일 사이클
```

**다운타임 = 0초.** T1~T4 전 구간에서 항상 최소 1개 인스턴스가 트래픽 처리.

## 컴포넌트별 변경 사항

### 1. Synology DSM 역방향 프록시 (사용자 수동 작업, 1회)

DSM → 제어판 → 로그인 포털 → 고급 → 역방향 프록시

**기존 규칙 `SUH-LAB 443→8090` 수정:**

| 필드 | 현재 | 변경 후 |
|------|------|---------|
| 역방향 프록시 이름 | `SUH-LAB 443→8090` | `SUH-LAB 443→8079` (가독성, 선택) |
| 소스 프로토콜 | HTTPS | (그대로) |
| 소스 호스트 | `lab.suhsaechan.kr` | (그대로) |
| 소스 포트 | `443` | (그대로) |
| HSTS | 활성 | (그대로) |
| 대상 프로토콜 | HTTP | (그대로) |
| 대상 호스트 | `localhost` | (그대로) |
| **대상 포트** | **`8090`** | **`8079`** |

`SUH-LAB 80→443` redirect 규칙은 그대로 유지.

### 2. `.github/workflows/SUH-PROJECT-UTILITY-CICD.yaml` (코드 변경)

#### 2-1. build job
**변경 없음.** Docker 이미지 빌드 & DockerHub push 로직 그대로 유지.

#### 2-2. deploy job (전면 재작성)

기존 `docker rm -f` + `docker run` 단순 흐름을 다음 Blue-Green 절차로 교체:

```bash
set -e

export PATH=$PATH:/usr/local/bin
export PW="${{ secrets.SERVER_PASSWORD }}"

PROJECT_NAME="suh-project-utility"
IMAGE="${{ secrets.DOCKERHUB_USERNAME }}/${PROJECT_NAME}-container:main"
DOMAIN="lab.suhsaechan.kr"
INTERNAL_PORT="8080"
TRAEFIK_NETWORK="traefik-network"
ROUTER_NAME="${PROJECT_NAME}"  # Traefik router 이름 고정 (Host 룰 공유용)

# 1. 최신 이미지 Pull
echo $PW | sudo -S docker pull "${IMAGE}"

# 2. 현재 active 컨테이너 판별 (label 부착된 쪽)
ACTIVE=$(echo $PW | sudo -S docker ps \
  --filter "label=traefik.http.routers.${ROUTER_NAME}.rule" \
  --filter "name=${PROJECT_NAME}-" \
  --format "{{.Names}}" | head -1)

if [ "$ACTIVE" = "${PROJECT_NAME}-blue" ]; then
  NEW_COLOR="green"
  OLD_COLOR="blue"
elif [ "$ACTIVE" = "${PROJECT_NAME}-green" ]; then
  NEW_COLOR="blue"
  OLD_COLOR="green"
else
  # 최초 배포 또는 어떤 컨테이너도 라벨 없음 → blue 부터 시작
  NEW_COLOR="blue"
  OLD_COLOR="green"
fi

NEW_NAME="${PROJECT_NAME}-${NEW_COLOR}"
OLD_NAME="${PROJECT_NAME}-${OLD_COLOR}"

echo "[배포] active=${ACTIVE:-none}, new=${NEW_NAME}, old=${OLD_NAME}"

# 3. 기존 동명 (idle 쪽) 컨테이너 정리
echo $PW | sudo -S docker rm -f "${NEW_NAME}" 2>/dev/null || true

# 4. 새 컨테이너 기동 (라벨 없이 → 트래픽 안 받음)
echo $PW | sudo -S docker run -d \
  --name "${NEW_NAME}" \
  --network "${TRAEFIK_NETWORK}" \
  -e TZ=Asia/Seoul \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /etc/localtime:/etc/localtime:ro \
  -v /volume1/projects/suh-project-utility:/mnt/suh-project-utility \
  -v /volume1/web/suh-project-utility/upload:/app/uploads \
  "${IMAGE}"

# 5. 헬스체크 (최대 120초, PR Preview 패턴 그대로)
HEALTH_PATH="/actuator/health"
LOG_PATTERN="Started .* in [0-9.]+ seconds"
MAX_RETRIES=24
RETRY_COUNT=0
HEALTH_OK=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  sleep 5
  RETRY_COUNT=$((RETRY_COUNT + 1))
  STATUS=$(echo $PW | sudo -S docker inspect --format='{{.State.Status}}' "${NEW_NAME}" 2>/dev/null || echo "not_found")

  if [ "$STATUS" = "exited" ]; then
    echo "[실패] 컨테이너 비정상 종료"
    echo $PW | sudo -S docker logs --tail 100 "${NEW_NAME}"
    echo $PW | sudo -S docker rm -f "${NEW_NAME}"
    exit 1
  fi

  if [ "$STATUS" = "running" ]; then
    HEALTH=$(echo $PW | sudo -S docker exec "${NEW_NAME}" wget -qO- "http://localhost:${INTERNAL_PORT}${HEALTH_PATH}" 2>/dev/null || echo "")
    if echo "$HEALTH" | grep -q '"status":"UP"'; then
      HEALTH_OK=true
      break
    fi
    STARTED=$(echo $PW | sudo -S docker logs --tail 50 "${NEW_NAME}" 2>&1 | grep -E "$LOG_PATTERN" || echo "")
    if [ -n "$STARTED" ]; then
      HEALTH_OK=true
      break
    fi
  fi
done

if [ "$HEALTH_OK" = "false" ]; then
  echo "[실패] 헬스체크 타임아웃 (120초)"
  echo $PW | sudo -S docker logs --tail 100 "${NEW_NAME}"
  echo $PW | sudo -S docker rm -f "${NEW_NAME}"
  exit 1
fi

# 6. 새 컨테이너에 Traefik 라벨 부여 → 트래픽 합류
#    docker run 시점에 라벨을 못 바꾸는 게 아니므로,
#    실제 구현에서는 "라벨을 처음부터 달고 띄우되 헬스체크 통과 전엔
#    Traefik 가 자체 health 로 거르도록" 하는 게 더 단순.
#    아래 단순화된 방식 채택 (스펙 v1).
echo "[전환] ${NEW_NAME} 트래픽 합류"
# docker label 사후 추가는 불가능 → 다음 절차로 처리:
# 6-a. 새 컨테이너를 라벨 포함하여 재기동 (이미 이미지/볼륨 동일)
echo $PW | sudo -S docker rm -f "${NEW_NAME}"
echo $PW | sudo -S docker run -d \
  --name "${NEW_NAME}" \
  --network "${TRAEFIK_NETWORK}" \
  --label "traefik.enable=true" \
  --label "traefik.http.routers.${ROUTER_NAME}.rule=Host(\`${DOMAIN}\`)" \
  --label "traefik.http.routers.${ROUTER_NAME}.entrypoints=web" \
  --label "traefik.http.services.${ROUTER_NAME}.loadbalancer.server.port=${INTERNAL_PORT}" \
  -e TZ=Asia/Seoul \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /etc/localtime:/etc/localtime:ro \
  -v /volume1/projects/suh-project-utility:/mnt/suh-project-utility \
  -v /volume1/web/suh-project-utility/upload:/app/uploads \
  "${IMAGE}"

# 6-b. 재기동 후 다시 짧은 헬스체크 (캐시된 이미지라 보통 빠름, 60초 한도)
HEALTH_OK=false
RETRY_COUNT=0
while [ $RETRY_COUNT -lt 12 ]; do
  sleep 5
  RETRY_COUNT=$((RETRY_COUNT + 1))
  HEALTH=$(echo $PW | sudo -S docker exec "${NEW_NAME}" wget -qO- "http://localhost:${INTERNAL_PORT}${HEALTH_PATH}" 2>/dev/null || echo "")
  if echo "$HEALTH" | grep -q '"status":"UP"'; then
    HEALTH_OK=true; break
  fi
done

if [ "$HEALTH_OK" = "false" ]; then
  echo "[실패] 라벨 부여 후 재기동 헬스체크 실패. old 컨테이너(${OLD_NAME}) 유지하여 자동 롤백."
  echo $PW | sudo -S docker rm -f "${NEW_NAME}"
  exit 1
fi

# 7. old 컨테이너 정리 (있다면)
if echo $PW | sudo -S docker ps -a --format '{{.Names}}' | grep -Eq "^${OLD_NAME}$"; then
  echo "[정리] old=${OLD_NAME} 제거 (10초 in-flight 대기 후)"
  sleep 10
  echo $PW | sudo -S docker rm -f "${OLD_NAME}"
fi

echo "[완료] active=${NEW_NAME}, 다운타임 없음"
```

#### 2-3. 단순화 노트 — 라벨 재기동 vs 처음부터 라벨 부착

위 6단계의 "라벨 없이 띄움 → 헬스체크 → 삭제 후 라벨 달아 재기동" 절차는 안전하지만 컨테이너를 2번 띄움.

**더 단순한 대안 (v1 채택):** 처음부터 Traefik 라벨 달고 띄움. Traefik 은 컨테이너 등록 직후 트래픽 보내려 시도 → Spring Boot 미기동 상태에서 502 가 발생할 수 있음.

**해결책:** 다음 중 하나로 한 번에 해결:

- **A.** Docker HEALTHCHECK + Traefik의 `traefik.http.services.X.loadbalancer.healthcheck` 옵션 추가 → Traefik 이 healthy 컨테이너만 라우팅
- **B.** 위 코드처럼 라벨 없이 띄워 헬스체크 후 라벨 달고 재기동 (더 안전, 절차 명확)

**v1 채택: A (HEALTHCHECK + Traefik healthcheck label).** 컨테이너 1번만 띄우고 깔끔.

#### 2-4. 최종 deploy job 흐름 (v1 확정)

```bash
# (변수 설정, ACTIVE 판별은 위와 동일)

# 3. 기존 동명 컨테이너 정리
docker rm -f "${NEW_NAME}" 2>/dev/null || true

# 4. 새 컨테이너 기동 — Traefik 라벨 + healthcheck 한 번에
docker run -d \
  --name "${NEW_NAME}" \
  --network "${TRAEFIK_NETWORK}" \
  --label "traefik.enable=true" \
  --label "traefik.http.routers.${ROUTER_NAME}.rule=Host(\`${DOMAIN}\`)" \
  --label "traefik.http.routers.${ROUTER_NAME}.entrypoints=web" \
  --label "traefik.http.services.${ROUTER_NAME}.loadbalancer.server.port=${INTERNAL_PORT}" \
  --label "traefik.http.services.${ROUTER_NAME}.loadbalancer.healthcheck.path=/actuator/health" \
  --label "traefik.http.services.${ROUTER_NAME}.loadbalancer.healthcheck.interval=5s" \
  --label "traefik.http.services.${ROUTER_NAME}.loadbalancer.healthcheck.timeout=3s" \
  -e TZ=Asia/Seoul \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /etc/localtime:/etc/localtime:ro \
  -v /volume1/projects/suh-project-utility:/mnt/suh-project-utility \
  -v /volume1/web/suh-project-utility/upload:/app/uploads \
  "${IMAGE}"

# 5. 헬스체크 (외부에서 별도 확인, 최대 120초)
#    Traefik healthcheck 와 별개로 워크플로우가 직접 컨테이너 상태 확인
#    → 통과 시점에 트래픽이 안전하게 합류 중임을 보장
HEALTH_PATH="/actuator/health"
LOG_PATTERN="Started .* in [0-9.]+ seconds"
MAX_RETRIES=24
RETRY_COUNT=0
HEALTH_OK=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  sleep 5
  RETRY_COUNT=$((RETRY_COUNT + 1))
  STATUS=$(docker inspect --format='{{.State.Status}}' "${NEW_NAME}" 2>/dev/null || echo "not_found")

  if [ "$STATUS" = "exited" ]; then
    echo "[실패] 컨테이너 비정상 종료"
    docker logs --tail 100 "${NEW_NAME}"
    docker rm -f "${NEW_NAME}"
    exit 1
  fi

  if [ "$STATUS" = "running" ]; then
    HEALTH=$(docker exec "${NEW_NAME}" wget -qO- "http://localhost:${INTERNAL_PORT}${HEALTH_PATH}" 2>/dev/null || echo "")
    if echo "$HEALTH" | grep -q '"status":"UP"'; then
      HEALTH_OK=true; break
    fi
    STARTED=$(docker logs --tail 50 "${NEW_NAME}" 2>&1 | grep -E "$LOG_PATTERN" || echo "")
    if [ -n "$STARTED" ]; then
      HEALTH_OK=true; break
    fi
  fi
done

if [ "$HEALTH_OK" = "false" ]; then
  echo "[실패] 헬스체크 타임아웃 (120초). old=${OLD_NAME} 유지하여 자동 롤백."
  docker logs --tail 100 "${NEW_NAME}"
  docker rm -f "${NEW_NAME}"
  exit 1
fi

# 6. old 컨테이너 정리 (10초 대기 후 삭제)
if docker ps -a --format '{{.Names}}' | grep -Eq "^${OLD_NAME}$"; then
  sleep 10
  docker rm -f "${OLD_NAME}"
fi
```

**핵심:** Traefik healthcheck 라벨로 컨테이너 기동 중에는 자동으로 트래픽 안 보냄. 두 번 띄울 필요 없음.

### 3. Dockerfile (변경 검토)

**현재:**
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY Suh-Web/build/libs/app.jar app.jar
ENV TZ=Asia/Seoul
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
EXPOSE 8080
```

**변경 권장 (선택, 안정성 향상):**

```dockerfile
FROM eclipse-temurin:17-jre-alpine

# wget 설치 (Traefik healthcheck 와 별개로 Docker HEALTHCHECK 용)
RUN apk add --no-cache wget

WORKDIR /app
COPY Suh-Web/build/libs/app.jar app.jar
ENV TZ=Asia/Seoul

# Docker 자체 헬스체크 (Traefik healthcheck 와 이중 안전망)
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
EXPOSE 8080
```

**HEALTHCHECK 추가 이유:**
- `docker ps` 에 health 상태 표시 → 운영 가시성
- Traefik healthcheck 와 독립적으로 동작 → 이중 안전망

### 4. application.yml (변경 확인)

`management.endpoints.web.exposure.include` 에 `health` 포함 필요. 기존에 Spring Boot Actuator `/actuator/health` 가 200 응답하면 변경 불필요.

**확인 필요:** `Suh-Web/src/main/resources/application.yml` 에 `management:` 블록 설정 상태. (운영 secret 이라 GitHub Secrets `APPLICATION_YML` 로 주입 — 구현 단계에서 확인)

## 마이그레이션 절차 (1회성)

### Phase 0: 사전 확인
- [ ] Traefik 컨테이너가 `traefik-network` 네트워크에 속하는지 확인 (`docker network inspect traefik-network`)
- [ ] Traefik entrypoint `web` 이 `:8079` 인지 확인 (Traefik dashboard 또는 컨테이너 설정)
- [ ] `application.yml` 에 `/actuator/health` 가 200 응답하는지 확인 (현재 운영 컨테이너에서 `curl http://localhost:8090/actuator/health`)

### Phase 1: 워크플로우 수정 & 첫 Blue 컨테이너 띄우기
- [ ] `.github/workflows/SUH-PROJECT-UTILITY-CICD.yaml` 수정 (위 2-4 참조)
- [ ] (선택) `Dockerfile` 에 HEALTHCHECK 추가
- [ ] main 브랜치 push → 워크플로우 실행
- [ ] **이 시점에 `suh-project-utility-blue` 컨테이너 기동, 8079 의 Traefik 이 라우팅 시작**
- [ ] **하지만 기존 `suh-project-utility` (8090) 컨테이너도 그대로 가동 중** → 사용자는 여전히 기존 거 사용
- [ ] 접속 테스트: `curl -H "Host: lab.suhsaechan.kr" http://localhost:8079/actuator/health` (NAS 내부에서)

### Phase 2: Synology 역방향 프록시 전환 (사용자 GUI 작업)
- [ ] DSM → 제어판 → 로그인 포털 → 고급 → 역방향 프록시
- [ ] `SUH-LAB 443→8090` 규칙 편집 → 대상 포트 `8090` → `8079`
- [ ] 저장
- [ ] 브라우저에서 `https://lab.suhsaechan.kr` 접속 → 정상 동작 확인 (이제 새 blue 컨테이너가 응답)

### Phase 3: 기존 컨테이너 제거
- [ ] `ssh nas docker rm -f suh-project-utility` (기존 8090 컨테이너)
- [ ] 이후 8090 포트 사용 안 함

### Phase 4: 검증
- [ ] 임의로 더미 commit → main push → 워크플로우 실행
- [ ] 배포 중 `while true; do curl -sw "%{http_code}\n" https://lab.suhsaechan.kr/actuator/health -o /dev/null; sleep 1; done` 로 응답 코드 모니터링
- [ ] 200 이 끊김 없이 유지되면 무중단 성공

## 리스크 & 완화책

| 리스크 | 영향 | 완화책 |
|--------|------|--------|
| Traefik healthcheck 라벨 미지원 | 컨테이너 기동 중 502 노출 | Traefik 버전 확인 (v2.0+ 지원). 구버전이면 위 Section 2-3 의 "B. 라벨 없이 띄움 → 헬스체크 → 재기동" 방식으로 폴백 |
| 새 컨테이너 기동 실패 | 배포 실패 | old 컨테이너 그대로 유지, 새 컨테이너만 삭제 → 자동 롤백 효과. 워크플로우 exit 1 로 알림 |
| Flyway 마이그레이션 비호환 (blue/green 동시 가동 시점) | 한 쪽 에러 | 사용자 동의 — `ADD COLUMN IF NOT EXISTS` 등 backward-compatible 마이그레이션만 사용 (CLAUDE.md 의 Flyway 규칙으로 이미 강제). DROP COLUMN 등은 v1→v2 단계 분리하여 진행 |
| 세션 휘발 | 사용자 로그아웃 | Spring Session + Redis 이미 사용 중 (CLAUDE.md 확인) → 안전 |
| 파일 업로드 동시성 | 두 컨테이너가 같은 볼륨 쓰기 | 기존 단일 컨테이너에서도 멀티 스레드 쓰기 했음. 컨테이너 단위 차이 없음 — 안전 |
| 첫 마이그레이션 시 Synology 포트 변경 순간 | 수 초 다운타임 | 1회성, Phase 2 에서만 발생. 이후 모든 배포는 다운타임 0 |
| Synology 자동 복구 정책으로 8090 점유 잔존 | 새 컨테이너 8090 점유 가능 | 8090 더 이상 publish 안 함 (`-p 8090:8080` 제거) → 충돌 없음 |
| Traefik dashboard 에서 라우터 이름 충돌 | PR Preview 와 production 이 같은 router 이름 쓸 가능성 | router 이름을 `suh-project-utility` (PR Preview 는 `suh-project-utility-pr-N`) 로 구분 → 충돌 없음 |

## 테스트 계획

1. **무중단 검증 (Phase 4 의 확장):**
   - 별도 터미널에서 `for i in $(seq 1 300); do curl -sw "%{http_code} " https://lab.suhsaechan.kr/actuator/health -o /dev/null; sleep 1; done` 실행
   - main push → 워크플로우 트리거
   - 응답 시퀀스에 `200` 이외의 코드 (502, 503, 504, 000) 가 섞이지 않는지 확인

2. **롤백 검증:**
   - 일부러 빌드 깨진 commit 으로 배포 시도
   - 헬스체크 실패 → 워크플로우 fail
   - `docker ps` 로 old 컨테이너 (이전 색) 가 그대로 살아있는지 확인
   - `lab.suhsaechan.kr` 가 정상 응답하는지 확인

3. **연속 배포 검증:**
   - blue → green → blue → green 4회 연속 배포
   - 각 배포 후 active 컨테이너 색이 정확히 토글되는지 확인
   - 컨테이너 누적 없이 항상 1개만 남는지 확인 (idle 색은 삭제됨)

4. **수동 헬스체크 직접 확인:**
   - `curl https://lab.suhsaechan.kr/actuator/health` → `{"status":"UP"}` 응답
   - Traefik dashboard 에서 `suh-project-utility` router 가 활성 컨테이너 가리키는지 확인

## 후속 작업 (별도 이슈 권장)

- 로그 영속화 (logback 파일 appender + 볼륨 마운트 + log rotation)
- Slack/Discord 배포 알림 통합
- 자동 롤백 메커니즘 강화 (헬스체크 실패 시 이전 commit 으로 자동 재배포)
- Traefik dashboard 의 production 라우터 가시성 점검 (현재 PR Preview 만 보임)

## 참고

- 기존 PR Preview 워크플로우: `.github/workflows/PROJECT-SPRING-SYNOLOGY-PR-PREVIEW.yaml`
- 기존 CICD 워크플로우 (대상): `.github/workflows/SUH-PROJECT-UTILITY-CICD.yaml`
- Traefik 공식 문서 (healthcheck): https://doc.traefik.io/traefik/routing/services/#health-check
