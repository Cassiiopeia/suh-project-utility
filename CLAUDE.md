# SUH 프로젝트 유틸리티 - Claude AI 가이드

## 프로젝트 개요
SUH 프로젝트 유틸리티는 Spring Boot 3.4.2 기반의 멀티모듈 웹 애플리케이션입니다. 다양한 도메인 기능(Docker, GitHub, Notice, Study 등)을 제공하며, PostgreSQL과 Redis를 데이터 저장소로 사용합니다.

## 프로젝트 구조
```
suh-project-utility/
├── Suh-Common/           # 공통 유틸리티, 엔티티, 설정
├── Suh-Domain-Docker/    # Docker 관련 기능
├── Suh-Domain-Github/    # GitHub 이슈 헬퍼 기능
├── Suh-Domain-Notice/    # 공지사항 및 댓글 기능
├── Suh-Domain-Module/    # 모듈 버전 관리
├── Suh-Domain-Study/     # 스터디 노트 관리
├── Suh-Module-Translate/ # 번역 서비스
├── Suh-Application/      # 애플리케이션 서비스
└── Suh-Web/             # 웹 컨트롤러 및 UI (Config 포함)
```

## 기술 스택
- **Backend**: Java 17, Spring Boot 3.4.2, Spring Data JPA
- **Database**: PostgreSQL, Redis
- **Frontend**: Thymeleaf, Semantic UI, JavaScript
- **Build**: Gradle 8.12.1
- **Deployment**: Docker, GitHub Actions CI/CD

## 개발 환경 설정
- **Profile**: `dev` (개발), `prod` (운영)
- **Local DB**: PostgreSQL (localhost:5432), Redis (localhost:6379)
- **Production DB**: PostgreSQL (suh-project.synology.me:5430)

## 코딩 컨벤션

### 엔티티(Entity) 생성 규칙
```java
@Entity
@Getter
@Setter  // 또는 필요시만 사용
@SuperBuilder  // BasePostgresEntity 상속시
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)  // BaseEntity 상속시
public class EntityName extends BasePostgresEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID entityNameId;  // {엔티티명}Id 형식
    
    // Boolean 필드는 반드시 is 접두사 사용
    @Column(nullable = false)
    private Boolean isActive;
    
    @Column(nullable = false)
    private Boolean isImportant;
    
    // 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;
    
    // 컬렉션은 Builder.Default로 초기화
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();
}
```

### 네이밍 규칙
- **Controller**: `{도메인}Controller` (예: `StudyManagementController`)
- **Service**: `{도메인}Service` (예: `StudyPostService`)
- **Repository**: `{도메인}Repository` (예: `StudyPostRepository`)
- **Entity**: 도메인 객체명 (예: `StudyPost`, `NoticeComment`)
- **Boolean 필드**: 반드시 `is` 접두사 사용 (예: `isActive`, `isPublic`)

### 폴더 구조 규칙
- **깊이 제한**: 최대 2단계 깊이까지만 허용
- **일관성**: 모든 도메인 모듈이 동일한 구조 사용

```
me/suhsaechan/{domain}/
├── dto/                    # 모든 DTO 클래스
│   ├── StudyRequest.java   # API 요청 DTO
│   ├── StudyResponse.java  # API 응답 DTO
│   ├── CategoryDto.java    # 세부 데이터 DTO (독립 클래스)
│   ├── PostDto.java        # 세부 데이터 DTO (독립 클래스)
│   └── AttachmentDto.java  # 세부 데이터 DTO (독립 클래스)
├── entity/                 # JPA 엔티티
├── repository/             # Repository 인터페이스
└── service/               # Service 클래스
```

### Request/Response 패턴
- **단일 Request/Response 원칙**: 각 도메인 모듈은 하나의 Request와 Response 클래스만 사용
- **Request**: `{도메인}Request` - 모든 API 입력을 통합 관리
- **Response**: `{도메인}Response` - 모든 API 출력을 통합 관리
- **세부 DTO**: 독립적인 DTO 클래스로 분리 (static class 사용 금지)
- **Builder 패턴**: 모든 Response는 Builder 패턴으로 반환

```java
// dto/PostDto.java (독립적인 DTO 클래스)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private UUID id;
    private String title;
    private Boolean isPublic;  // Boolean은 is 접두사
}

// dto/StudyResponse.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyResponse {
    private List<PostDto> posts;  // 독립 DTO 참조
    private PostDto post;
}

// Service에서 Builder 패턴 사용
return StudyResponse.builder()
    .posts(postDtoList)
    .totalPosts(totalCount)
    .build();
```

### API 설계 원칙
- **기본 HTTP Method**: POST (파일 업로드 지원을 위해)
- **Content-Type**: `MediaType.MULTIPART_FORM_DATA_VALUE`
- **엔드포인트**: `/api/{도메인}/{기능}` (예: `/api/study/post/create`)
- **응답**: `ResponseEntity<T>` 사용
- **상태 코드**: HTTP 상태 코드로만 표현 (별도 success 필드 없음)

### 예외 처리 패턴
```java
// ErrorCode enum 정의
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // COMMON - 공통 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "잘못된 파라미터입니다."),
    
    // GITHUB - GitHub 도메인 오류
    GITHUB_ISSUE_NOT_FOUND(HttpStatus.NOT_FOUND, "GitHub 이슈를 찾을 수 없습니다."),
    GITHUB_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub API 호출 중 오류가 발생했습니다."),
    
    // STUDY - Study 도메인 오류
    STUDY_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "포스트를 찾을 수 없습니다.");
    
    private final HttpStatus httpStatus;
    private final String message;  // 한글 메시지 사용
}

// 사용 예시
try {
    Response response = okHttpClient.newCall(request).execute();
    if (!response.isSuccessful()) {
        if (response.code() == 404) {
            throw new CustomException(ErrorCode.GITHUB_ISSUE_NOT_FOUND);
        } else {
            throw new CustomException(ErrorCode.GITHUB_API_ERROR);
        }
    }
} catch (IOException e) {
    log.error("GitHub 페이지 로드 실패: {}", e.getMessage(), e);
    throw new CustomException(ErrorCode.GITHUB_API_ERROR);
}
```

### Config 설정 위치
- **Bean 설정**: `Suh-Web/src/main/java/me/suhsaechan/web/config/` 하위에 위치
- **예시**: `WebConfig`, `DatabaseConfig`, `RedisConfig`, `WebSecurityConfig`
- **모든 Configuration 클래스는 Web 모듈에 집중**

### 의존성 주입
- **생성자 주입**: `@RequiredArgsConstructor` + `final` 필드
- **모든 Service/Repository**: 생성자 주입 패턴 적용

```java
@Service
@RequiredArgsConstructor
public class StudyPostService {
    private final StudyPostRepository postRepository;
    private final StudyCategoryService categoryService;
}
```

## 주요 도메인 모듈

### Study 모듈
- **기능**: 마크다운 기반 스터디 노트 관리
- **엔티티**: StudyPost, StudyCategory, StudyAttachment
- **특징**: 
  - 계층형 카테고리 구조
  - 파일 첨부 지원
  - 조회수 관리
  - 태그 기능

### Notice 모듈
- **기능**: 공지사항 및 댓글 관리
- **엔티티**: SuhProjectUtilityNotice, NoticeComment
- **특징**:
  - 중요 공지 표시
  - 기간별 활성화
  - 익명 댓글 지원

### Docker 모듈
- **기능**: Docker 컨테이너 모니터링
- **특징**:
  - SSH 원격 명령 실행
  - 실시간 로그 스트리밍 (SSE)
  - 컨테이너 상태 조회

### GitHub 모듈
- **기능**: GitHub 이슈 헬퍼
- **엔티티**: GithubRepository, GithubIssueHelper
- **특징**:
  - 이슈 자동 생성
  - PR 관리 지원

## UI/UX 가이드라인

### CSS 관리
- **통합 파일**: `common.css`에 모든 스타일 통합
- **클래스 명명**: `{페이지}-{컴포넌트}` (예: `study-page`, `dashboard-section-header`)
- **인라인 스타일 금지**: CSP 준수

### 컴포넌트 규칙
- **카드**: `ui fluid card equal-height-card` 사용
- **버튼**: `ui blue fluid button shortcut-btn` 사용
- **그리드**: 4열 기본, 반응형 (doubling stackable)
- **로딩**: 스켈레톤 UI 제공

### 반응형 디자인
- **분기점**: 모바일(~767px), 태블릿(768px~991px), 데스크탑(992px~)
- **미디어 쿼리**: 화면 크기별 최적화

## 빌드 및 실행

### 개발 환경

**매우 중요한 CLI 명령어 사용법**:
```bash 
source ~/.zshrc &&
```
를 붙여서 모든 명령어를 실행해야지 작동함

```bash
# 기본적으로 dev 모드로 프로필 지정 실행
source ~/.zshrc && ./gradlew bootRun --args='--spring.profiles.active=dev'

# 테스트 실행
source ~/.zshrc && ./gradlew test

# 빌드
source ~/.zshrc && ./gradlew clean build
```

### 린트 및 타입 체크
프로젝트에 정의된 린트/타입체크 명령이 있다면 반드시 실행:
- 현재 Java 프로젝트로 별도 린트 도구 미설정
- IDE의 코드 인스펙션 기능 활용 권장

## 보안 고려사항
- **민감 정보 관리**: application.yml의 민감 정보는 `@Value` 어노테이션으로 주입
- **인증**: Spring Security 기반 폼 로그인
- **CSP**: 인라인 스크립트/스타일 금지
- **파일 업로드**: 확장자 및 크기 제한 (200MB)

### @Value 어노테이션 사용 예시
민감한 정보들은 application.yml에서 환경변수로 관리하고 @Value로 주입합니다:

```java
@Service
@RequiredArgsConstructor
public class ExampleService {
    @Value("${admin.super.username}")
    private String superAdminUsername;
    
    @Value("${admin.super.password}")
    private String superAdminPassword;
    
    @Value("${aes.secret-key}")
    private String aesSecretKey;
    
    @Value("${spring.data.redis.password}")
    private String redisPassword;
    
    @Value("${file.username}")
    private String fileUsername;
    
    @Value("${file.password}")
    private String filePassword;
}
```

### application.yml 설정 예시
```yaml
admin:
  super:
    username: ${ADMIN_USERNAME:defaultUser}
    password: ${ADMIN_PASSWORD:defaultPass}

aes:
  secret-key: ${AES_SECRET_KEY:defaultKey}
  iv: ${AES_IV:defaultIV}

spring:
  data:
    redis:
      password: ${REDIS_PASSWORD:}

file:
  username: ${FILE_USERNAME:user}
  password: ${FILE_PASSWORD:pass}
```

## 테스트 전략
- **단위 테스트**: Service 레이어 중심
- **통합 테스트**: Controller 레이어
- **테스트 프레임워크**: JUnit 5, Mockito

## 배포
- **CI/CD**: GitHub Actions 사용
- **컨테이너**: Docker 이미지 빌드 및 배포
- **환경**: dev(개발), prod(운영) 프로필 분리

## 주의사항
1. **파일 생성 최소화**: 기존 파일 수정 우선
2. **문서 자동 생성 금지**: 요청 시에만 생성
3. **commit 금지**: 명시적 요청 시에만 수행
4. **한글 로그**: 디버깅 용이성을 위해 한글 메시지 사용
5. **도메인별 단일 Request/Response**: API 일관성 유지
6. **Boolean 필드**: 반드시 `is` 접두사 사용

## 트러블슈팅
- **Gradle 빌드 오류**: JDK 17 버전 확인
- **DB 연결 실패**: PostgreSQL 서비스 상태 확인
- **Redis 연결 실패**: Redis 서비스 및 비밀번호 확인