# Blue-Green 무중단 배포 안정화 설계 (RomRom-BE #728 패턴 이식)

## 배경

`suh-project-utility`는 이미 RomRom-BE와 동일한 구조의 Blue-Green 배포 워크플로우
(`SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml`)를 갖추고 있다. 그러나 배포 도중
**Cloudflare host error(502/521)** 가 발생하며 무중단이 깨진다.

RomRom-BE는 동일 증상을 이슈 **#728 "무중단 배포 안정화"** 에서 이미 겪고 해결했다.
본 설계는 그 수정안을 현재 프로젝트에 동일하게 이식한다.

## 문제 진단

### 요청 경로
```
Cloudflare → 시놀로지 역방향 프록시(443) → traefik(8079) → 컨테이너(8080)
```

### 근본 원인 (RomRom-BE #728이 규명한 것과 동일)

| # | 원인 | 현재 프로젝트 상태 |
|---|------|------------------|
| 1 | Dockerfile에 `HEALTHCHECK`가 없거나 interval이 길면, 컨테이너가 배포 후 `health: starting` 상태에 머무름 → traefik이 unhealthy로 판단해 라우팅 풀에서 제외 | **HEALTHCHECK 자체가 없음** |
| 2 | `docker run`에 `--no-healthcheck`가 없으면, healthcheck 상태가 traefik 라우팅 판단에 영향 | **`--no-healthcheck` 미적용** |

new 컨테이너가 traefik 라우팅 풀에 실제 합류하기 전에 old가 `docker rm -f`로 제거되면,
**둘 다 라우팅 안 되는 순간**이 생겨 502/521이 Cloudflare까지 전파된다.

## 수정 방안 (RomRom-BE 정상본과 동일하게)

### 변경 1 — `Dockerfile`
- alpine에 `curl` 설치
- `HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=3` 추가
  - 평상시 컨테이너 상태를 traefik이 30초 내 빠르게 감지
- JAR 경로(`/app/app.jar`)와 healthcheck 엔드포인트는 현재 프로젝트 값 유지

### 변경 2 — `SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml`
- `docker run -d` 블록에 `--no-healthcheck` 한 줄 추가
  - 배포 순간만큼은 healthcheck 상태를 traefik 라우팅에서 격리
  - 런타임 `--no-healthcheck`가 Dockerfile HEALTHCHECK를 덮어쓰므로 충돌 없음

> 두 변경은 역할이 다르다: Dockerfile HEALTHCHECK = 평상시 모니터링,
> 워크플로우 `--no-healthcheck` = 배포 중 격리. RomRom-BE는 둘 다 적용했다.

## 영향 범위
- `Dockerfile` (1개)
- `.github/workflows/SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml` (1개)
- 애플리케이션 코드 변경 없음

## 검증 방법
- main push로 배포 트리거 후, 배포 진행 중
  `https://lab.suhsaechan.kr/actuator/health`를 반복 호출(예: `while`)하여
  502/521 끊김이 없는지 확인
- 배포 후 컨테이너 status가 `Up`(`health: starting` 아님)인지 `docker inspect` 확인

## 비고
- 본 변경은 인프라/CI 파일 변경으로 앱 동작에 영향이 없는 메타성 변경에 해당한다.
