🚀[기능개선][CICD] Secret 파일 Synology 업로드 로직을 독립 워크플로우로 분리

## 본문

📝 현재 문제점
---

- `SUH-PROJECT-UTILITY-CICD.yaml` 배포 워크플로우 내에 `application.yml` Secret 파일을 Synology NAS에 업로드하는 로직이 포함되어 있음
- 배포와 Secret 파일 백업이 하나의 워크플로우에 결합되어 관심사 분리가 되지 않음
- Secret 파일 변경 이력 추적(타임스탬프 백업)이 불가능하여, 최신본 덮어쓰기만 수행됨

🛠️ 해결 방안 / 제안 기능
---

- 기존 `SUH-PROJECT-UTILITY-CICD.yaml`에서 `application.yml` 업로드 로직 제거
- `PROJECT-COMMON-SYNOLOGY-SECRET-FILE-UPLOAD.yaml` 워크플로우를 프로젝트에 맞게 커스텀하여 독립 실행
- 타임스탬프 기반 백업으로 Secret 파일 변경 히스토리 보존
- 메타데이터 JSON 생성으로 변경 이력 추적 가능

⚙️ 작업 내용
---

- `SUH-PROJECT-UTILITY-CICD.yaml` deploy job에서 `application.yml` 업로드 관련 4줄 삭제
- `PROJECT-COMMON-SYNOLOGY-SECRET-FILE-UPLOAD.yaml` 프로젝트 맞춤 설정
  - SSH Secret: `SERVER_HOST` / `SERVER_USER` / `SERVER_PASSWORD` 사용
  - 저장 경로: `/volume1/projects/suh-project-utility/github_secret/`
  - 대상 Secret: `APPLICATION_YML` → `application.yml` 1개
  - 타임스탬프 백업 및 메타데이터 JSON 생성 포함

🙋‍♂️ 담당자
---

- 백엔드: 이름
- 프론트엔드: 이름
- 디자인: 이름
