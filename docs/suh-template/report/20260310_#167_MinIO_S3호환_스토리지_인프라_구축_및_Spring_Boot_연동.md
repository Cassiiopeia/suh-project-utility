### 📌 작업 개요

시놀로지 NAS에 MinIO S3 호환 스토리지 서버 인프라 구축 및 Spring Boot 연동 코드 구현. Docker Compose로 MinIO 컨테이너 배포, 역방향 프록시 설정(콘솔 + API), DNS 연결, 버킷 생성, Spring Boot MinIO SDK 연동 코드까지 완료.

### 🎯 구현 목표

- 시놀로지 NAS를 S3 호환 스토리지 서버로 활용
- MinIO SDK를 통한 파일 업로드/다운로드/삭제/Presigned URL 기능 제공
- 기존 SMB/SSH 방식의 파일 접근 제약 해소 (공개/비공개 제어, 버킷 단위 관리)

### ✅ 구현 내용

#### 1. MinIO Docker Compose 설정
- **파일**: `docker-compose.yml` (신규 생성)
- **이미지**: `minio/minio:RELEASE.2025-04-22T22-12-26Z` (GUI 관리 기능 포함 버전)
- **포트**: API 9000, 웹 콘솔 9090
- **마운트**: `/volume1/s3:/data`
- **환경변수**: `MINIO_BROWSER_REDIRECT_URL`, `MINIO_SERVER_URL` 설정

**특이사항**:
- 최신 MinIO(2025.05 이후)에서 Access Policy, 사용자 관리 등 GUI 기능이 제거됨 (상용 AIStor로 전환)
- GUI 관리 기능이 필요하여 `RELEASE.2025-04-22T22-12-26Z` 버전으로 고정

#### 2. 시놀로지 역방향 프록시 설정 (4개 규칙)

**웹 콘솔 (minio.suhsaechan.kr → 9090):**
- `minio.suhsaechan.kr:80` → HTTPS 리다이렉트
- `minio.suhsaechan.kr:443` → `localhost:9090` (WebSocket 활성화)

**S3 API (s3.suhsaechan.kr → 9000):**
- `s3.suhsaechan.kr:80` → HTTPS 리다이렉트
- `s3.suhsaechan.kr:443` → `localhost:9000` (WebSocket 활성화)

#### 3. DNS 설정
- Cloudflare CNAME: `minio` → `suh-project.synology.me` (Proxied)
- Cloudflare CNAME: `s3` → `suh-project.synology.me` (Proxied)

#### 4. 버킷 생성
- `romrom` (PRIVATE) - RomRom 프로젝트용
- `suh-project-utility` (PRIVATE) - 현재 프로젝트용

#### 5. Spring Boot MinIO 연동 - Properties
- **파일**: `Suh-Common/.../properties/MinioProperties.java` (신규 생성)
- `@ConfigurationProperties(prefix = "minio")` 패턴 사용 (기존 `SshConnectionProperties`와 동일)
- 필드: `endpoint`, `accessKey`, `secretKey`

#### 6. Spring Boot MinIO 연동 - 유틸리티
- **파일**: `Suh-Common/.../util/MinioUtil.java` (신규 생성)
- `@Component` + 생성자 주입 패턴 (기존 `SshCommandExecutor`와 동일)
- `@PostConstruct`로 MinioClient 초기화

제공 메서드:

| 카테고리 | 메서드 | 설명 |
|---------|--------|------|
| 버킷 | `bucketExists` | 버킷 존재 확인 |
| 버킷 | `createBucket` | 버킷 생성 (이미 존재 시 스킵) |
| 버킷 | `removeBucket` | 버킷 삭제 |
| 버킷 | `listBuckets` | 전체 버킷 목록 조회 |
| 업로드 | `uploadFile` (InputStream) | 스트림 기반 파일 업로드 |
| 업로드 | `uploadFile` (MultipartFile) | Spring MultipartFile 업로드 |
| 다운로드 | `downloadFile` | InputStream 반환 |
| 삭제 | `deleteFile` | 오브젝트 삭제 |
| 조회 | `getFileInfo` | 파일 메타정보 (크기, 타입, ETAG) |
| 조회 | `listObjects` | 오브젝트 목록 (prefix 필터, recursive 지원) |
| URL | `getPresignedDownloadUrl` | 임시 다운로드 링크 (기본 1시간) |
| URL | `getPresignedUploadUrl` | 임시 업로드 링크 (기본 1시간) |

#### 7. 의존성 추가
- **파일**: `Suh-Common/build.gradle` (수정)
- `api 'io.minio:minio:8.5.17'` 추가

#### 8. application.yml 설정 추가
- **파일**: `Suh-Web/.../application.yml` (수정)
- 공통 설정 영역에 minio 섹션 추가

```yaml
minio:
  endpoint: http://localhost:9000
  access-key: {ACCESS_KEY}
  secret-key: {SECRET_KEY}
```

### 🔧 인프라 구성도

```
[외부 접근 - 웹 콘솔]
  https://minio.suhsaechan.kr
    → Cloudflare DNS (CNAME → suh-project.synology.me)
    → 시놀로지 역방향 프록시 (443 → 9090)
    → MinIO 웹 콘솔

[외부 접근 - S3 API]
  https://s3.suhsaechan.kr
    → Cloudflare DNS (CNAME → suh-project.synology.me)
    → 시놀로지 역방향 프록시 (443 → 9000)
    → MinIO S3 API

[내부 API 접근]
  http://localhost:9000
    → MinIO S3 API (Spring Boot에서 접근)

[파일 저장]
  /volume1/s3/ (시놀로지 디스크)
```

### 📦 의존성 변경

- `io.minio:minio:8.5.17` 추가 (Suh-Common/build.gradle, `api`로 제공)

### 🧪 테스트 및 검증

- MinIO 웹 콘솔(`https://minio.suhsaechan.kr`) 로그인 정상 확인
- 파일 업로드(웹 콘솔에서 이미지 업로드) 정상 동작 확인
- 버킷 목록, 오브젝트 목록 정상 표시 확인
- Spring Boot 빌드 및 MinioUtil 연동 테스트는 배포 후 확인 필요

### 📌 참고사항

- MinIO 버전 `RELEASE.2025-04-22T22-12-26Z`로 고정 사용 (GUI 관리 기능 포함 마지막 버전)
- 2025년 5월 이후 MinIO 커뮤니티 에디션에서 Access Policy UI, 사용자 관리, 설정 메뉴 등 제거됨
- 이미지 외부 공개 시 버킷 Access Policy를 Public으로 변경 필요 (GUI에서 Buckets → Summary → Access Policy)
- Public 버킷의 이미지 직접 접근: `https://s3.suhsaechan.kr/{버킷명}/{파일명}`
- 비공개 파일은 Presigned URL로 임시 공유 가능 (만료 시간 설정)
- `MINIO_BROWSER_REDIRECT_URL`과 `MINIO_SERVER_URL` 환경변수는 역방향 프록시 설정과 반드시 매칭 필요 (불일치 시 503 에러 발생)
