# Spring Boot PR Preview 자동 배포 시스템 구현 보고서

**이슈**: #129
**작업일**: 2026-01-03
**작업자**: Claude Code

---

## 📌 작업 개요

GitHub Actions 기반의 PR Preview 자동 배포 시스템 구현. PR 코멘트에서 `@suh-lab pr {명령어}`를 입력하면 Synology NAS에 Traefik 리버스 프록시를 통해 동적으로 Preview 환경이 배포됨.

**주요 구현 내용**:
- GitHub Actions 워크플로우 파일 생성 (`PROJECT-COMMON-SPRING-SYNOLOGY-PR-PREVIEW.yaml`)
- 봇 명령어 시스템: `@suh-lab pr build`, `@suh-lab pr destroy`, `@suh-lab pr status`
- Traefik 동적 라우팅 연동
- 다른 프로젝트에서 재사용 가능한 구조 설계

---

## 🎯 구현 목표

1. PR별 독립된 Preview 환경 자동 생성
2. 코멘트 기반 간편한 배포/삭제 명령어
3. 멀티 프로젝트 지원을 위한 컨테이너 네이밍 규칙
4. Traefik 리버스 프록시 통한 동적 도메인 라우팅
5. PR 머지/종료 시 자동 리소스 정리

---

## ✅ 구현 내용

### 1. 인프라 구성

#### DNS 설정 (Cloudflare)
| Type | Name | Content |
|------|------|---------|
| CNAME | `*.pr` | `suh-project.synology.me` |

#### Traefik 설정 (기존 구축 완료)
- **네트워크**: `traefik-network`
- **대시보드**: `https://traefik.suhsaechan.kr/dashboard/#/`
- **Preview Entrypoint**: `web` (포트 8079)

---

### 2. 워크플로우 파일 구조

#### 파일 위치
- **경로**: `.github/workflows/PROJECT-COMMON-SPRING-SYNOLOGY-PR-PREVIEW.yaml`
- **총 530줄**

#### 환경 변수 (프로젝트별 커스터마이징)
```yaml
env:
  PROJECT_NAME: suh-project-utility
  JAVA_VERSION: '17'
  GRADLE_BUILD_CMD: './gradlew clean build -x test -Dspring.profiles.active=prod'
  JAR_PATH: 'Suh-Web/build/libs/app.jar'
  APPLICATION_YML_PATH: 'Suh-Web/src/main/resources/application.yml'
  DOCKERFILE_PATH: './Dockerfile'
  INTERNAL_PORT: '8080'
  TRAEFIK_NETWORK: traefik-network
  PREVIEW_DOMAIN_SUFFIX: pr.suhsaechan.kr
  PREVIEW_PORT: '8079'
  SSH_PORT: '2022'
```

---

### 3. 트리거 설정

| 이벤트 | 조건 | 동작 |
|--------|------|------|
| `issue_comment.created` | PR 댓글에 `@suh-lab pr {명령어}` | 해당 명령 실행 |
| `pull_request.closed` | PR 머지 또는 종료 | Preview 자동 삭제 |

---

### 4. Jobs 구성

#### Job 1: check-command (명령어 파싱)
- PR 댓글에서 `@suh-lab pr {command}` 패턴 감지
- 👀 리액션 추가로 봇 인식 표시
- 지원 명령어: `build`, `destroy`, `status`

#### Job 2: build-preview (빌드 & 배포)
| 단계 | 설명 |
|------|------|
| PR 정보 가져오기 | 브랜치명, SHA 추출 |
| 코드 체크아웃 | PR 브랜치 체크아웃 |
| JDK 설정 | Temurin 17 + Gradle 캐시 |
| application.yml 생성 | GitHub Secret에서 주입 |
| Gradle 빌드 | 테스트 스킵, prod 프로필 |
| Docker 이미지 빌드 | Buildx + GHA 캐시 |
| 서버 배포 | SSH로 컨테이너 교체 |
| 완료 코멘트 | Preview URL 안내 |

#### Job 3: destroy-preview (Preview 삭제)
- 컨테이너 및 이미지 삭제
- PR 종료 시 자동 실행 또는 명령어로 수동 실행

#### Job 4: check-status (상태 확인)
- 컨테이너 실행 여부 확인
- 상태별 다른 코멘트 작성

---

### 5. 리소스 네이밍 규칙

| 리소스 | 패턴 | 예시 |
|--------|------|------|
| 컨테이너 | `{PROJECT_NAME}-pr-{PR번호}` | `suh-project-utility-pr-123` |
| Docker 이미지 | `{DOCKERHUB_USERNAME}/{PROJECT_NAME}:pr-{PR번호}` | `cassiiopeia/suh-project-utility:pr-123` |
| 도메인 | `{PROJECT_NAME}-pr-{PR번호}.pr.suhsaechan.kr` | `suh-project-utility-pr-123.pr.suhsaechan.kr` |
| Preview URL | `http://{도메인}:8079` | `http://suh-project-utility-pr-123.pr.suhsaechan.kr:8079` |

---

### 6. 필수 GitHub Secrets

| Secret | 설명 |
|--------|------|
| `APPLICATION_YML` | Spring application.yml 전체 내용 |
| `DOCKERHUB_USERNAME` | Docker Hub 사용자명 |
| `DOCKERHUB_TOKEN` | Docker Hub 액세스 토큰 |
| `SERVER_HOST` | Synology NAS 호스트 (예: `suh-project.synology.me`) |
| `SERVER_USER` | SSH 사용자명 |
| `SERVER_PASSWORD` | SSH 비밀번호 |

---

### 7. Traefik 연동

Docker 컨테이너 실행 시 Traefik 레이블 자동 설정:

```bash
docker run -d \
  --name "${CONTAINER_NAME}" \
  --network "${TRAEFIK_NETWORK}" \
  --label "traefik.enable=true" \
  --label "traefik.http.routers.${CONTAINER_NAME}.rule=Host(\`${DOMAIN}\`)" \
  --label "traefik.http.routers.${CONTAINER_NAME}.entrypoints=web" \
  --label "traefik.http.services.${CONTAINER_NAME}.loadbalancer.server.port=${INTERNAL_PORT}" \
  "${IMAGE}"
```

---

### 8. 에러 처리

#### 빌드 실패 시
- 실패 코멘트 자동 작성
- GitHub Actions 로그 링크 제공
- 재시도 안내 (`@suh-lab pr build`)

---

## 🔧 주요 설계 결정

### 1. PROJECT_PATH 제거
**이유**: `application.yml`이 빌드 시점에 JAR에 포함되므로 서버 사이드 파일 저장 불필요

### 2. rebuild 명령어 통합
**결정**: `build` 명령어가 기존 컨테이너 자동 교체하도록 구현하여 `rebuild` 별도 명령어 불필요

### 3. 시놀로지 NAS 환경 대응
**추가**: `export PATH=$PATH:/usr/local/bin` - Docker 명령어 경로 설정

---

## 📋 사용 방법

### 명령어
| 명령어 | 설명 |
|--------|------|
| `@suh-lab pr build` | PR 빌드 및 배포 (기존 컨테이너 자동 교체) |
| `@suh-lab pr destroy` | Preview 환경 삭제 |
| `@suh-lab pr status` | 현재 상태 확인 |

### 다른 프로젝트 적용 시
1. 워크플로우 파일 복사
2. `env` 섹션의 변수들을 프로젝트에 맞게 수정
3. GitHub Secrets 설정
4. Cloudflare DNS에서 와일드카드 설정 확인

---

## 🧪 테스트 및 검증

- [x] 워크플로우 문법 검증
- [x] `@suh-lab pr build` 명령어 동작 확인
- [x] `@suh-lab pr destroy` 명령어 동작 확인
- [x] `@suh-lab pr status` 명령어 동작 확인
- [x] PR 종료 시 자동 삭제 동작 확인
- [x] Traefik 대시보드에서 라우터/서비스 확인

---

## 📌 참고사항

### 모니터링
- **Traefik 대시보드**: `https://traefik.suhsaechan.kr/dashboard/#/`
- 배포된 Preview는 HTTP Routers/Services에서 확인 가능

### 제한사항
- HTTP만 지원 (HTTPS 미적용)
- 포트 8079 고정 사용
- Synology NAS 전용 (SSH 포트 2022)

### 파일 위치
```
.github/workflows/
└── PROJECT-COMMON-SPRING-SYNOLOGY-PR-PREVIEW.yaml (530줄)
```

---

## 📊 아키텍처 다이어그램

```
┌─────────────────┐    PR Comment     ┌──────────────────┐
│   Developer     │ ────────────────> │  GitHub Actions  │
│  @suh-lab pr    │                   │   Workflow       │
│     build       │                   └────────┬─────────┘
└─────────────────┘                            │
                                               │ 1. Build JAR
                                               │ 2. Build Docker Image
                                               │ 3. Push to Docker Hub
                                               ▼
┌─────────────────┐    SSH (2022)    ┌──────────────────┐
│   Cloudflare    │ <──────────────  │  Synology NAS    │
│   DNS Proxy     │                  │                  │
│                 │                  │ ┌──────────────┐ │
│ *.pr.suhsaechan │                  │ │   Traefik    │ │
│     .kr         │                  │ │  :8079 web   │ │
└────────┬────────┘                  │ └──────┬───────┘ │
         │                           │        │         │
         │ HTTP :8079                │ ┌──────▼───────┐ │
         └───────────────────────────┼─│ project-pr-X │ │
                                     │ │   Container  │ │
                                     │ │    :8080     │ │
                                     │ └──────────────┘ │
                                     └──────────────────┘
```

---

**구현 완료**: PR Preview 시스템이 정상 동작하며, 다른 Spring Boot 프로젝트에서도 env 섹션만 수정하여 재사용 가능
