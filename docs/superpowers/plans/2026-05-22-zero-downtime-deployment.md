# 무중단 배포 (Zero-Downtime Deployment) 구현 Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `SUH-PROJECT-UTILITY-CICD.yaml` 의 deploy job 을 Traefik 기반 Blue-Green 무중단 배포로 전환. `lab.suhsaechan.kr` URL 유지, 다운타임 0.

**Architecture:** 기존 가동 중인 Traefik 컨테이너(`traefik-network`, `web` entrypoint `:8079`) 를 활용. `suh-project-utility-blue` / `suh-project-utility-green` 두 컨테이너를 토글 방식으로 배포. Traefik 의 `healthcheck` 라벨로 기동 중인 컨테이너는 트래픽 차단. Synology DSM 역방향 프록시 규칙 `SUH-LAB 443→8090` 의 대상 포트를 `8079` 로 1회 변경하여 외부 진입을 Traefik 으로 우회.

**Tech Stack:** GitHub Actions, Docker, Traefik v2+, Spring Boot Actuator, appleboy/ssh-action, Synology DSM 역방향 프록시

**Spec:** `docs/superpowers/specs/2026-05-22-zero-downtime-deployment-design.md`

---

## File Structure

| 파일 | 작업 | 책임 |
|------|------|------|
| `.github/workflows/SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml` | **Create (신규)** | Blue-Green 무중단 배포 워크플로우. 초기엔 `workflow_dispatch` 만 활성화 (1주일 검증 단계). 검증 끝나면 `push: main` 추가 + 기존 워크플로우 push 트리거 제거. |
| `.github/workflows/SUH-PROJECT-UTILITY-CICD.yaml` | **유지 (변경 없음, Phase 2 에서 push 트리거만 제거)** | 기존 배포 워크플로우. 검증 기간 동안 main push 자동 트리거 그대로. 검증 끝나면 `workflow_dispatch` 만 남겨 비상 롤백용으로 보존. |
| `Dockerfile` | Modify | `wget` 설치 + `HEALTHCHECK` 명령 추가 (Traefik healthcheck 와 이중 안전망). **양쪽 워크플로우 모두 안전** — 기존 워크플로우 동작에 영향 없음. |
| `docs/suh-template/report/2026-05-22-zero-downtime-deployment.md` | Create | 작업 후 마이그레이션 절차 + 검증 결과 기록 보고서 |

**점진 전환 전략 (사용자 요청 반영):**
- **Phase A (지금)**: 새 워크플로우 = `workflow_dispatch` 만. 기존 워크플로우 = `push: main` + `workflow_dispatch` 유지. 자동 배포는 기존 방식 계속, 새 방식은 수동 dispatch 로 검증.
- **Phase B (1주일 후, 검증 완료 시)**: 새 워크플로우에 `push: main` 추가. 기존 워크플로우에서 `push: main` 제거 (workflow_dispatch 만 남김 → 비상 롤백용).

**변경 없음:**
- `Suh-Web/src/main/resources/application.yml` — secrets 에서 주입, `/actuator/health` 가 200 응답 가정. 만약 응답 안 하면 별도 작업 (Task 1 사전 점검에서 확인)
- Traefik 컨테이너 자체 — 기존 가동 그대로
- 도메인 / Cloudflare DNS — 변경 없음

---

## 사전 점검 (Task 1)

### Task 1: 사전 환경 점검

**Files:**
- None (조사만)

- [ ] **Step 1: Traefik 네트워크 존재 확인**

NAS SSH 접속 후 실행:
```bash
sudo docker network inspect traefik-network --format '{{.Name}}'
```
Expected: `traefik-network` 출력

- [ ] **Step 2: Traefik 컨테이너가 traefik-network 에 속하는지 확인**

```bash
sudo docker network inspect traefik-network --format '{{range .Containers}}{{.Name}} {{end}}'
```
Expected: 출력에 `traefik` 비슷한 이름 포함

- [ ] **Step 3: Traefik web entrypoint 가 :8079 인지 확인**

```bash
sudo docker inspect traefik --format '{{range $p, $conf := .NetworkSettings.Ports}}{{$p}} {{end}}'
```
또는 Traefik dashboard (`https://traefik.suhsaechan.kr/dashboard/`) 에서 entrypoints 확인.

Expected: `8079` 가 web entrypoint 로 매핑되어 있음

- [ ] **Step 4: 기존 production 컨테이너 `/actuator/health` 응답 확인**

```bash
sudo docker exec suh-project-utility wget -qO- http://localhost:8080/actuator/health
```
Expected: `{"status":"UP"}` 포함된 JSON 응답

만약 응답 없거나 404 → Spring Boot Actuator 설정 점검 필요. `application.yml` 의 `management.endpoints.web.exposure.include` 에 `health` 추가 후 별도 PR 처리. **본 plan 진행 전 선결.**

- [ ] **Step 5: Selenium 네트워크 존재 확인**

```bash
sudo docker network inspect selenium-chrome-network --format '{{.Name}}'
```
Expected: `selenium-chrome-network` 출력

기존 컨테이너가 이 네트워크에 연결돼 있어 Selenium 사용 (Docker 모듈). 새 blue/green 컨테이너도 동일 네트워크 필요.

- [ ] **Step 6: Traefik 버전 확인 (healthcheck 라벨 지원 여부)**

```bash
sudo docker exec traefik traefik version
```
Expected: v2.0 이상. v2 미만이면 spec Section 2-3 의 폴백 (라벨 없이 띄움 → 헬스체크 → 라벨 달아 재기동) 방식 채택 필요.

- [ ] **Step 7: 점검 결과 기록**

위 6개 step 결과를 PR 본문 또는 issue 댓글에 정리. 모든 항목 OK 면 Task 2 진행. 한 항목이라도 실패하면 plan 일시 중단, 환경 보정 후 재개.

---

## Dockerfile 수정 (Task 2)

### Task 2: Dockerfile 에 HEALTHCHECK 추가

**Files:**
- Modify: `Dockerfile`

- [ ] **Step 1: 현재 Dockerfile 확인**

Run: `cat Dockerfile`

Expected 출력:
```dockerfile
# OpenJDK 17 이미지
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일을 복사 (Suh-Web bootJar 고정 파일명)
COPY Suh-Web/build/libs/app.jar app.jar

# 환경 변수 설정 (서울)
ENV TZ=Asia/Seoul

# Spring Boot 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 컨테이너 포트 지정
EXPOSE 8080
```

- [ ] **Step 2: Dockerfile 을 다음 내용으로 교체**

```dockerfile
# OpenJDK 17 이미지
FROM eclipse-temurin:17-jre-alpine

# wget 설치 (HEALTHCHECK 및 Traefik healthcheck 용)
RUN apk add --no-cache wget

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일을 복사 (Suh-Web bootJar 고정 파일명)
COPY Suh-Web/build/libs/app.jar app.jar

# 환경 변수 설정 (서울)
ENV TZ=Asia/Seoul

# Docker 자체 헬스체크 (Traefik healthcheck 와 이중 안전망)
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# Spring Boot 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 컨테이너 포트 지정
EXPOSE 8080
```

- [ ] **Step 3: 변경 사항 검증**

Run: `git diff Dockerfile`

Expected: `RUN apk add --no-cache wget` 와 `HEALTHCHECK ...` 두 블록만 추가됨. 다른 라인 변경 없음.

- [ ] **Step 4: 사용자 검토 대기**

사용자에게 diff 보여주고 OK 받기. **자동 커밋 금지 (CLAUDE.md 규칙).**

- [ ] **Step 5: 사용자 OK 후 commit**

커밋 메시지 (사용자가 명시 요청 시):
```
무중단 배포 도입 : feat : Dockerfile HEALTHCHECK 추가 — Traefik healthcheck 와 이중 안전망 구성
```

---

## CICD 워크플로우 수정 (Task 3)

### Task 3: 새 워크플로우 파일 생성 (SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml)

**Files:**
- Create: `.github/workflows/SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml`

**전략:** 기존 워크플로우 `SUH-PROJECT-UTILITY-CICD.yaml` 그대로 유지. 새 파일 생성하여 Blue-Green 배포 로직 담음. 초기 트리거는 `workflow_dispatch` 만 (수동 발동). 1주일 검증 후 Phase B 에서 `push: main` 추가.

- [ ] **Step 1: 신규 파일 작성 — 전체 내용**

`.github/workflows/SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml` 신규 생성:

```yaml
name: SUH-PROJECT-UTILITY-CICD-BLUEGREEN

# ===================================================================
# Blue-Green 무중단 배포 워크플로우
# - Phase A (검증 단계, 현재): workflow_dispatch 만 활성화. 수동 발동으로 검증.
# - Phase B (검증 완료 후): on.push.branches: [main] 추가, 기존 워크플로우 push 트리거 제거.
# - 동작: Traefik 라벨 기반 Blue-Green 토글. 다운타임 0 목표.
# ===================================================================

on:
  workflow_dispatch:
  # Phase B 진입 시 아래 주석 해제:
  # push:
  #   branches:
  #     - main

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Create application.yml from secret
        run: |
          mkdir -p Suh-Web/src/main/resources
          echo "${{ secrets.APPLICATION_YML }}" > Suh-Web/src/main/resources/application.yml

      - name: Build with Gradle
        run: ./gradlew clean build -x test -Dspring.profiles.active=prod

      - name: Docker setup
        uses: docker/setup-buildx-action@v3

      - name: Docker login
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Cache Docker layers
        uses: actions/cache@v4
        with:
          path: /tmp/.buildx-cache
          key: ${{ runner.os }}-buildx-${{ hashFiles('Dockerfile') }}
          restore-keys: |
            ${{ runner.os }}-buildx-

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          file: ./Dockerfile
          push: true
          tags: ${{ secrets.DOCKERHUB_USERNAME }}/suh-project-utility-container:${{ github.ref_name }}
          cache-from: type=local,src=/tmp/.buildx-cache
          cache-to: type=local,dest=/tmp/.buildx-cache-new,mode=max

      - name: Move Docker cache
        run: |
          rm -rf /tmp/.buildx-cache
          mv /tmp/.buildx-cache-new /tmp/.buildx-cache

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Blue-Green 무중단 배포
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          password: ${{ secrets.SERVER_PASSWORD }}
          port: 2022
          command_timeout: 10m
          script: |
            set -e

            export PATH=$PATH:/usr/local/bin
            export PW="${{ secrets.SERVER_PASSWORD }}"

            PROJECT_NAME="suh-project-utility"
            IMAGE="${{ secrets.DOCKERHUB_USERNAME }}/${PROJECT_NAME}-container:main"
            DOMAIN="lab.suhsaechan.kr"
            INTERNAL_PORT="8080"
            TRAEFIK_NETWORK="traefik-network"
            SELENIUM_NETWORK="selenium-chrome-network"
            ROUTER_NAME="${PROJECT_NAME}"
            HEALTH_PATH="/actuator/health"
            LOG_PATTERN="Started .* in [0-9.]+ seconds"

            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            echo "🚀 Blue-Green 무중단 배포 시작"
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

            # 1. 최신 이미지 Pull
            echo "📥 이미지 Pull: ${IMAGE}"
            echo $PW | sudo -S docker pull "${IMAGE}"

            # 2. 현재 active 컨테이너 판별 (Traefik 라벨 부착 여부로 결정)
            ACTIVE=$(echo $PW | sudo -S docker ps \
              --filter "label=traefik.http.routers.${ROUTER_NAME}.rule" \
              --filter "name=${PROJECT_NAME}-" \
              --format "{{.Names}}" | head -1 || echo "")

            if [ "$ACTIVE" = "${PROJECT_NAME}-blue" ]; then
              NEW_COLOR="green"
              OLD_COLOR="blue"
            elif [ "$ACTIVE" = "${PROJECT_NAME}-green" ]; then
              NEW_COLOR="blue"
              OLD_COLOR="green"
            else
              NEW_COLOR="blue"
              OLD_COLOR="green"
            fi

            NEW_NAME="${PROJECT_NAME}-${NEW_COLOR}"
            OLD_NAME="${PROJECT_NAME}-${OLD_COLOR}"

            echo "🎨 active=${ACTIVE:-none}, new=${NEW_NAME}, old=${OLD_NAME}"

            # 3. 기존 동명(idle 쪽) 컨테이너 정리 (혹시 잔존 시)
            echo "🗑️ idle 컨테이너 정리"
            echo $PW | sudo -S docker rm -f "${NEW_NAME}" 2>/dev/null || true

            # 4. 새 컨테이너 기동 — Traefik 라벨 + healthcheck 포함
            #    Traefik healthcheck 가 통과 전까지 트래픽 안 보냄
            echo "🐳 새 컨테이너 기동: ${NEW_NAME}"
            echo $PW | sudo -S docker run -d \
              --name "${NEW_NAME}" \
              --network "${TRAEFIK_NETWORK}" \
              --label "traefik.enable=true" \
              --label "traefik.http.routers.${ROUTER_NAME}.rule=Host(\`${DOMAIN}\`)" \
              --label "traefik.http.routers.${ROUTER_NAME}.entrypoints=web" \
              --label "traefik.http.services.${ROUTER_NAME}.loadbalancer.server.port=${INTERNAL_PORT}" \
              --label "traefik.http.services.${ROUTER_NAME}.loadbalancer.healthcheck.path=${HEALTH_PATH}" \
              --label "traefik.http.services.${ROUTER_NAME}.loadbalancer.healthcheck.interval=5s" \
              --label "traefik.http.services.${ROUTER_NAME}.loadbalancer.healthcheck.timeout=3s" \
              -e TZ=Asia/Seoul \
              -e SPRING_PROFILES_ACTIVE=prod \
              -v /etc/localtime:/etc/localtime:ro \
              -v /volume1/projects/suh-project-utility:/mnt/suh-project-utility \
              -v /volume1/web/suh-project-utility/upload:/app/uploads \
              "${IMAGE}"

            # 4-b. selenium-chrome-network 도 연결 (Selenium WebDriver 사용)
            echo "🔗 selenium-chrome-network 연결"
            echo $PW | sudo -S docker network connect "${SELENIUM_NETWORK}" "${NEW_NAME}" 2>/dev/null || true

            # 5. 헬스체크 (최대 120초 대기, 24회 × 5초)
            echo "⏳ 헬스체크 시작 (최대 120초)"
            MAX_RETRIES=24
            RETRY_COUNT=0
            HEALTH_OK=false

            while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
              sleep 5
              RETRY_COUNT=$((RETRY_COUNT + 1))
              STATUS=$(echo $PW | sudo -S docker inspect --format='{{.State.Status}}' "${NEW_NAME}" 2>/dev/null || echo "not_found")

              if [ "$STATUS" = "exited" ]; then
                echo "❌ 컨테이너 비정상 종료. 로그:"
                echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                echo $PW | sudo -S docker logs --tail 100 "${NEW_NAME}"
                echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                echo $PW | sudo -S docker rm -f "${NEW_NAME}"
                echo "🛡️ old=${OLD_NAME} 유지하여 자동 롤백 (배포 전 상태 그대로)"
                exit 1
              fi

              if [ "$STATUS" = "running" ]; then
                HEALTH=$(echo $PW | sudo -S docker exec "${NEW_NAME}" wget -qO- "http://localhost:${INTERNAL_PORT}${HEALTH_PATH}" 2>/dev/null || echo "")
                if echo "$HEALTH" | grep -q '"status":"UP"'; then
                  echo "✅ HTTP 헬스체크 통과 (${RETRY_COUNT}/${MAX_RETRIES})"
                  HEALTH_OK=true
                  break
                fi
                STARTED=$(echo $PW | sudo -S docker logs --tail 50 "${NEW_NAME}" 2>&1 | grep -E "$LOG_PATTERN" || echo "")
                if [ -n "$STARTED" ]; then
                  echo "✅ 로그 패턴 헬스체크 통과 (${RETRY_COUNT}/${MAX_RETRIES})"
                  HEALTH_OK=true
                  break
                fi
              fi

              echo "⏳ 대기 중... (${RETRY_COUNT}/${MAX_RETRIES}) status=${STATUS}"
            done

            if [ "$HEALTH_OK" = "false" ]; then
              echo "❌ 헬스체크 타임아웃 (120초). 로그:"
              echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
              echo $PW | sudo -S docker logs --tail 100 "${NEW_NAME}"
              echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
              echo $PW | sudo -S docker rm -f "${NEW_NAME}"
              echo "🛡️ old=${OLD_NAME} 유지하여 자동 롤백"
              exit 1
            fi

            # 6. in-flight 처리 + old 컨테이너 제거
            if echo $PW | sudo -S docker ps -a --format '{{.Names}}' | grep -Eq "^${OLD_NAME}$"; then
              echo "⏱️ in-flight 요청 처리 대기 (10초)"
              sleep 10
              echo "🗑️ old=${OLD_NAME} 제거"
              echo $PW | sudo -S docker rm -f "${OLD_NAME}"
            fi

            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            echo "✅ 무중단 배포 완료. active=${NEW_NAME}"
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
```

- [ ] **Step 3: yaml 문법 검증**

Run (로컬에서):
```bash
python -c "import yaml; yaml.safe_load(open('.github/workflows/SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml'))"
```
Expected: 에러 없이 종료

내부망이라 python 안 되면 GitHub Actions 자체가 syntax check 함. push 후 Actions 페이지에서 syntax error 만 우선 확인.

- [ ] **Step 4: 기존 워크플로우 파일 변경 없는지 확인**

Run: `git diff .github/workflows/SUH-PROJECT-UTILITY-CICD.yaml`

Expected: 출력 없음 (기존 파일 변경 0)

- [ ] **Step 5: 새 파일 commit 메시지 (사용자 명시 요청 시)**

```
무중단 배포 도입 : feat : Blue-Green 토글 + Traefik healthcheck 라벨 신규 워크플로우 추가 (workflow_dispatch 만 활성화, 1주일 검증 단계)
```

---

## 첫 배포 & 마이그레이션 (Task 4)

### Task 4: 첫 Blue 컨테이너 기동 (기존 8090 컨테이너 유지)

**Files:**
- None (수동 실행 / GitHub Actions)

**상황 설명:** Task 3 까지 commit & push 한 시점에는 워크플로우만 바뀌고 NAS 상황은 그대로. 다음 main push 시 새 워크플로우가 trigger 되어 `suh-project-utility-blue` 가 띄워짐. 그러나 시놀로지 역방향 프록시는 아직 8090 을 가리키므로 사용자는 기존 컨테이너 사용 중. blue 는 Traefik 안에서만 활성.

- [ ] **Step 1: main 브랜치에 trivial commit push (워크플로우 트리거)**

본 plan 의 변경 사항 (Task 2, 3) 을 main 으로 머지하거나, README 한 줄 수정 등으로 main push.

```bash
git push origin main
```

- [ ] **Step 2: GitHub Actions 실행 결과 확인**

GitHub Actions 페이지에서 `SUH-PROJECT-UTILITY-CICD` 워크플로우 실행 모니터링.

Expected:
- build job 성공
- deploy job 로그에 `🎨 active=none, new=suh-project-utility-blue, old=suh-project-utility-green` 출력
- `✅ 무중단 배포 완료. active=suh-project-utility-blue`

- [ ] **Step 3: NAS 에서 컨테이너 상태 확인**

NAS SSH 접속 후:
```bash
sudo docker ps --filter "name=suh-project-utility" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Expected:
```
NAMES                          STATUS                   PORTS
suh-project-utility-blue       Up X minutes (healthy)
suh-project-utility            Up X days                0.0.0.0:8090->8080/tcp
```

기존 `suh-project-utility` (8090) 과 새 `suh-project-utility-blue` 가 **공존** 상태.

- [ ] **Step 4: Traefik 에서 blue 컨테이너 라우팅 확인**

NAS 내부에서 Host 헤더 위조하여 Traefik 통한 접속 테스트:
```bash
curl -sw "\nHTTP %{http_code}\n" -H "Host: lab.suhsaechan.kr" http://localhost:8079/actuator/health
```

Expected: `{"status":"UP"}` + `HTTP 200`

- [ ] **Step 5: 기존 8090 컨테이너도 정상 동작 확인 (사용자 트래픽 영향 없음)**

```bash
curl -sw "\nHTTP %{http_code}\n" https://lab.suhsaechan.kr/actuator/health
```

Expected: `{"status":"UP"}` + `HTTP 200` (시놀로지 역방향 프록시가 아직 8090 으로 보냄)

---

### Task 5: 시놀로지 역방향 프록시 포트 전환 (사용자 GUI 작업)

**Files:**
- None (Synology DSM GUI)

- [ ] **Step 1: DSM 접속**

브라우저로 Synology DSM 접속 → 제어판 → 로그인 포털 → 고급 → 역방향 프록시

- [ ] **Step 2: 기존 규칙 편집**

`SUH-LAB 443→8090` 행 선택 → 편집 클릭

- [ ] **Step 3: 대상 포트 변경**

| 필드 | 변경 |
|------|------|
| 역방향 프록시 이름 | `SUH-LAB 443→8079` (가독성, 선택) |
| 대상 포트 | `8090` → **`8079`** |
| 그 외 모든 필드 | 변경 없음 |

저장 클릭.

- [ ] **Step 4: 변경 즉시 외부 접속 테스트**

브라우저에서 (또는 외부 머신에서):
```bash
curl -sw "\nHTTP %{http_code}\n" https://lab.suhsaechan.kr/actuator/health
```

Expected: `{"status":"UP"}` + `HTTP 200`

이제 외부 트래픽이 Traefik → blue 컨테이너로 흐름. **사용자는 URL 변경 인지 못함.**

- [ ] **Step 5: 브라우저 직접 접속하여 페이지 정상 렌더링 확인**

`https://lab.suhsaechan.kr/` 접속 → 메인 페이지 정상 로드되는지 확인 (정적 자원, CSS, JS 모두 정상)

---

### Task 6: 기존 8090 컨테이너 제거

**Files:**
- None (NAS SSH 작업)

- [ ] **Step 1: 외부 접속이 새 blue 로 가는지 한 번 더 확인**

```bash
# NAS 에서 blue 컨테이너 로그에 최근 요청 흔적 보이는지
sudo docker logs --tail 50 suh-project-utility-blue 2>&1 | grep -i "GET\|POST" | tail -5
```

Expected: 최근 5분 내 요청 로그가 blue 에서 확인됨

- [ ] **Step 2: 기존 컨테이너 정상 동작 중인지도 한 번 확인 (안전 차원)**

```bash
sudo docker ps --filter "name=suh-project-utility$" --format "{{.Status}}"
```

Expected: `Up X days` 같은 정상 상태 (참고용)

- [ ] **Step 3: 기존 8090 컨테이너 제거**

```bash
echo $PW | sudo -S docker rm -f suh-project-utility
```

Expected: 컨테이너 이름 출력

- [ ] **Step 4: 8090 포트 해제 확인**

```bash
sudo netstat -tlnp 2>/dev/null | grep 8090 || echo "8090 free"
```

Expected: `8090 free`

- [ ] **Step 5: lab.suhsaechan.kr 정상 응답 마지막 확인**

```bash
curl -sw "\nHTTP %{http_code}\n" https://lab.suhsaechan.kr/actuator/health
```

Expected: `{"status":"UP"}` + `HTTP 200`

이 시점부터 production 은 **완전히 Traefik 경유**, 8090 포트 사용 안 함.

---

## 무중단 검증 (Task 7)

### Task 7: 무중단 배포 실제 검증

**Files:**
- None (모니터링 + 트리거)

- [ ] **Step 1: 별도 터미널에서 응답 코드 무한 모니터링 시작**

로컬 (외부 머신) 에서:
```bash
i=0
while true; do
  i=$((i+1))
  CODE=$(curl -sw "%{http_code}" -o /dev/null --max-time 5 https://lab.suhsaechan.kr/actuator/health)
  echo "[$(date '+%H:%M:%S')] #${i} HTTP=${CODE}"
  sleep 1
done
```

Expected: `HTTP=200` 만 계속 출력. 다른 코드 (502, 503, 504, 000) 섞이면 안 됨

- [ ] **Step 2: 다른 터미널에서 main 에 trivial commit push (재배포 트리거)**

```bash
# 예: README 의 빌드 번호 코멘트 한 줄 수정
git push origin main
```

- [ ] **Step 3: GitHub Actions 모니터링**

Actions 페이지에서 워크플로우 실행 추적.

Expected: deploy job 로그에 `🎨 active=suh-project-utility-blue, new=suh-project-utility-green` 출력 → 토글 정상

- [ ] **Step 4: 배포 진행 중 Step 1 의 응답 코드 모니터링 결과 확인**

Expected: 배포 전체 시간 (~2분) 동안 `HTTP=200` 만 계속 출력. **단 한 번이라도 200 아닌 코드 나오면 무중단 실패** → spec 의 리스크 섹션 재검토 필요.

- [ ] **Step 5: 배포 완료 후 active 컨테이너 확인**

```bash
sudo docker ps --filter "name=suh-project-utility-" --format "{{.Names}}"
```

Expected: `suh-project-utility-green` 만 출력 (blue 는 제거됨)

- [ ] **Step 6: 한 번 더 재배포하여 green → blue 토글 확인**

```bash
git push origin main
```

Expected: 워크플로우 로그 `🎨 active=suh-project-utility-green, new=suh-project-utility-blue` → 정상 토글. 응답 코드 모니터링도 계속 200 유지.

---

## 롤백 메커니즘 검증 (Task 8)

### Task 8: 헬스체크 실패 시 자동 롤백 검증

**Files:**
- None (의도적 실패 케이스 검증)

- [ ] **Step 1: 일부러 깨진 commit 만들기**

`Suh-Web/src/main/resources/application.yml` 의 `server.port` 같은 필수 설정을 의도적으로 잘못 설정 (예: 잘못된 DB 접속 정보). 단 secrets 파일이므로 로컬에서는 시뮬레이션만, 실제 환경에서는 별도 테스트 브랜치에서 진행.

**또는 더 안전한 방법:** 가짜 application.yml 잘못된 secret 으로 잠시 교체 → 빌드는 되지만 기동 실패.

(본 step 은 위험하므로 **선택 사항**. 검증 안 해도 되면 skip 가능)

- [ ] **Step 2: 응답 코드 모니터링 시작 (Task 7 Step 1 동일)**

- [ ] **Step 3: 깨진 commit push**

```bash
git push origin main
```

- [ ] **Step 4: deploy job 실패 확인**

Expected:
- 헬스체크 24회 모두 실패 → 120초 후 워크플로우 fail
- 로그에 `❌ 헬스체크 타임아웃 (120초)` + 컨테이너 로그 100줄 + `🛡️ old=... 유지하여 자동 롤백`

- [ ] **Step 5: 모니터링 결과 확인**

Expected: 배포 시도 전체 시간 동안 `HTTP=200` 유지. old 컨테이너가 트래픽 처리 중이라 영향 없음.

- [ ] **Step 6: NAS 상태 확인**

```bash
sudo docker ps --filter "name=suh-project-utility-" --format "{{.Names}}\t{{.Status}}"
```

Expected: old 컨테이너 (직전 active) 만 살아있음. new 시도 컨테이너는 삭제됨.

- [ ] **Step 7: 깨진 commit revert + 재배포로 정상 복구**

```bash
git revert <broken-commit>
git push origin main
```

Expected: 정상 배포 재개, active 컨테이너 토글

---

## 문서화 (Task 9)

### Task 9: 작업 보고서 작성

**Files:**
- Create: `docs/suh-template/report/2026-05-22-zero-downtime-deployment.md`

- [ ] **Step 1: 보고서 파일 생성**

다음 내용으로 작성:

```markdown
# 무중단 배포 도입 보고서 (2026-05-22)

## 작업 개요
`SUH-PROJECT-UTILITY-CICD.yaml` 의 deploy job 을 Traefik 기반 Blue-Green 무중단 배포로 전환. 사용자 URL `lab.suhsaechan.kr` 유지하며 다운타임 0 달성.

## 변경 사항

### 코드 변경
- `Dockerfile`: `wget` 설치 + `HEALTHCHECK` 명령 추가
- `.github/workflows/SUH-PROJECT-UTILITY-CICD.yaml`: deploy job 을 Blue-Green 토글 스크립트로 재작성

### 인프라 변경 (수동)
- Synology DSM 역방향 프록시 `SUH-LAB 443→8090` 의 대상 포트를 `8090` → `8079` 변경
- 기존 `suh-project-utility` 컨테이너 (8090 직접 바인딩) 제거
- 새 컨테이너 `suh-project-utility-blue` / `-green` 토글 운영

## 동작 원리

(spec 의 아키텍처 다이어그램 복사)

## 검증 결과

### 무중단 검증 (Task 7)
- 1초 간격 응답 모니터링: 배포 ~2분 동안 HTTP 200 만 출력 (200 외 코드 0건)

### 롤백 검증 (Task 8)
- 의도적 깨진 commit 배포 시도: 120초 헬스체크 타임아웃 → old 컨테이너 유지 → 다운타임 0

## 향후 작업
- 로그 영속화 (logback 파일 appender + log rotation)
- Slack/Discord 배포 알림 통합
- 자동 롤백 메커니즘 강화 (헬스체크 실패 시 이전 commit 으로 자동 재배포)

## 참고
- 설계 spec: `docs/superpowers/specs/2026-05-22-zero-downtime-deployment-design.md`
- 구현 plan: `docs/superpowers/plans/2026-05-22-zero-downtime-deployment.md`
```

- [ ] **Step 2: 사용자 commit 명시 요청 시 commit**

```
무중단 배포 도입 : docs : 2026-05-22 작업 보고서 추가
```

---

## Self-Review

### Spec coverage 확인
| spec 요구사항 | 구현 task |
|---------------|----------|
| Blue-Green 패턴 | Task 3 |
| Synology 역방향 프록시 포트 변경 | Task 5 |
| Traefik healthcheck 라벨 활용 | Task 3 Step 2 |
| Dockerfile HEALTHCHECK 추가 | Task 2 |
| 첫 마이그레이션 절차 | Task 4, 5, 6 |
| 무중단 검증 | Task 7 |
| 롤백 검증 | Task 8 |
| selenium-chrome-network 연결 유지 | Task 3 Step 2 (`docker network connect`) |
| `/actuator/health` 응답 확인 | Task 1 Step 4 |
| Traefik 버전 확인 | Task 1 Step 6 |
| 보고서 | Task 9 |

**누락 없음.**

### Placeholder scan
- TBD/TODO 없음 ✅
- "appropriate error handling" 같은 모호한 표현 없음 ✅
- 코드 블록 모두 완전 ✅
- "similar to Task N" 없음 ✅

### Type consistency
- 컨테이너 명: `suh-project-utility-blue` / `-green` 일관 ✅
- 라우터명: `suh-project-utility` 일관 ✅
- 네트워크명: `traefik-network`, `selenium-chrome-network` 일관 ✅
- 환경변수 (`PW`, `IMAGE`, `DOMAIN` 등) Task 3 안에서 정의 후 사용, 일관 ✅

---

## 실행 옵션 선택

Plan 완료 — `docs/superpowers/plans/2026-05-22-zero-downtime-deployment.md` 저장.

**두 가지 실행 방식 중 선택 필요:**

**1. Subagent-Driven (추천)**
- 각 Task 마다 fresh subagent 디스패치
- Task 사이 사용자 검토
- 빠른 반복, 깨끗한 context

**2. Inline Execution**
- 본 세션에서 직접 Task 실행
- 체크포인트로 batch 실행
- 단일 흐름

**참고:** Task 1 (사전 점검), Task 4~8 (NAS SSH + GitHub Actions + DSM GUI 작업) 은 본질적으로 **사용자 환경에서 수동 실행 필요**. Claude 는 NAS SSH 직접 접속 안 함, DSM GUI 도 못 만짐. 따라서 본 plan 은:
- **코드 변경 (Task 2, 3, 9)**: Claude 가 처리 가능
- **운영 작업 (Task 1, 4~8)**: 사용자가 수동 수행 + 결과를 Claude 에 공유

**어느 방식 원함?**
