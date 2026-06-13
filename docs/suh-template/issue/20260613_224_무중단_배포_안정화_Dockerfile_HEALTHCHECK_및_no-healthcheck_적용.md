📝 현재 문제점
---

Blue-Green 배포 워크플로우(`SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml`)는 이미 도입되어 있으나, 배포 도중 **Cloudflare host error(502/521)** 가 발생하며 무중단이 깨집니다.

요청 경로는 다음과 같습니다.

```
Cloudflare → 시놀로지 역방향 프록시(443) → traefik(8079) → 컨테이너(8080)
```

근본 원인은 **컨테이너 healthcheck 상태가 traefik 라우팅을 방해**하는 것입니다. RomRom-BE 이슈 #728("무중단 배포 안정화")에서 동일 증상을 이미 규명·해결했으며, 현재 프로젝트에는 그 수정 두 가지가 누락되어 있습니다.

- 원인 1: `Dockerfile`에 `HEALTHCHECK`가 없어, 컨테이너 상태를 traefik이 빠르게 판단할 기준이 불명확
- 원인 2: 배포 워크플로우 `docker run`에 `--no-healthcheck`가 없어, healthcheck `starting` 상태가 traefik 라우팅 판단에 영향

new 컨테이너가 traefik 라우팅 풀에 합류하기 전에 old가 `docker rm -f`로 제거되면, **둘 다 라우팅 안 되는 순간**이 발생해 502/521이 Cloudflare까지 전파됩니다.

🛠️ 해결 방안 / 제안 기능
---

RomRom-BE #728의 정상본과 동일하게 두 파일을 수정합니다.

- `Dockerfile`: `curl` 설치 + `HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=3` 추가 → 평상시 traefik이 컨테이너 상태를 30초 내 신속 감지
- `SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml`: `docker run`에 `--no-healthcheck` 추가 → 배포 순간 healthcheck 상태를 traefik 라우팅에서 격리

두 변경은 역할이 다릅니다. Dockerfile HEALTHCHECK는 *평상시 모니터링*, 워크플로우 `--no-healthcheck`는 *배포 중 격리*용입니다. 런타임 `--no-healthcheck`가 Dockerfile HEALTHCHECK를 덮어쓰므로 충돌이 없습니다.

⚙️ 작업 내용
---

- `Dockerfile` HEALTHCHECK 추가 (curl 설치 포함)
- `SUH-PROJECT-UTILITY-CICD-BLUEGREEN.yaml` `docker run`에 `--no-healthcheck` 추가
- 배포 중 `https://lab.suhsaechan.kr/actuator/health` 반복 호출로 무중단 검증

🙋‍♂️ 담당자
---

- 백엔드: Cassiiopeia
