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
    
    // 관계 매핑 - 오직 @ManyToOne만 허용 (절대 규칙)
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;
    
    // ⚠️⚠️⚠️ 절대 금지: @OneToMany, @OneToOne은 어떤 경우에도 사용 금지!!!
    // 자식 엔티티 조회는 반드시 Repository에서 직접 수행
}
```

### LocalDateTime 직렬화 규칙 (Entity 직접 반환)
- **Entity 직접 반환 허용**: Response에서 Entity를 직접 반환해도 됨 (DTO 변환 불필요)
- **@JsonFormat 필수**: `LocalDateTime` 필드에는 반드시 `@JsonFormat` 어노테이션 추가
- **BasePostgresEntity**: `createdDate`, `updatedDate`는 이미 `@JsonFormat` 적용됨
- **Entity 자체 날짜 필드**: Entity에 별도 `LocalDateTime` 필드가 있으면 직접 `@JsonFormat` 추가

```java
// BasePostgresEntity - 이미 적용됨
@CreatedDate
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
@Column(nullable = false, updatable = false)
private LocalDateTime createdDate;

// Entity 자체 LocalDateTime 필드 - 직접 추가 필요
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
@Column
private LocalDateTime startDate;

// Response에서 Entity 직접 반환 OK
@Data
@Builder
public class NoticeResponse {
    private List<SuhProjectUtilityNotice> notices;  // Entity 직접 사용 OK
    private SuhProjectUtilityNotice notice;         // Entity 직접 사용 OK
}
```

### JPA 관계 매핑 규칙 ⚠️⚠️⚠️ 절대적 금지 사항 ⚠️⚠️⚠️
- **@OneToMany 절대 금지**: 어떤 경우에도, 어떤 이유로도 @OneToMany는 절대 사용 금지!!!
- **@OneToOne 절대 금지**: 양방향 관계 복잡도로 인해 절대 사용 금지!!!
- **@ManyToOne만 허용**: 오직 단방향 @ManyToOne 관계만 사용 가능
- **자식 엔티티 조회**: 반드시 Repository에서 직접 조회 메서드 구현
- **CASCADE 절대 금지**: cascade 옵션은 어떤 경우에도 사용 금지

```java
// ❌❌❌ 절대 금지 - @OneToMany는 어떤 경우에도 사용 금지!!!
@OneToMany(mappedBy = "post")
private List<Comment> comments;  // 절대 사용하지 마세요!!!

// ❌❌❌ 절대 금지 - @OneToOne도 사용 금지!!!
@OneToOne(mappedBy = "member")
private Profile profile;  // 절대 사용하지 마세요!!!

// ✅✅✅ 유일하게 허용된 관계 매핑 - 오직 @ManyToOne만!!!
@ManyToOne(fetch = FetchType.LAZY)
private Post post;  // 이것만 사용하세요!

// ✅✅✅ 올바른 예시 - Repository에서 자식 엔티티 조회
@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPost(Post post);
    List<Comment> findByPostPostId(UUID postId);
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
- **상태 코드**: HTTP 상태 코드로만 표현

### Controller 작성 패턴
- **단일 Request/Response**: 하나의 Controller에서는 하나의 Request, Response 클래스만 사용
- **어노테이션**: `@PostMapping` + `consumes = MediaType.MULTIPART_FORM_DATA_VALUE` 고정
- **파라미터**: `@ModelAttribute`로 Request 객체 받기
- **로깅**: `@LogMonitor` 어노테이션으로 자동 로깅

```java
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notice")
public class NoticeController {
    private final NoticeService noticeService;

    @PostMapping(value = "/get/active", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogMonitor
    public ResponseEntity<NoticeResponse> getActiveNotices() {
        return ResponseEntity.ok(noticeService.getActiveNotices());
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogMonitor
    public ResponseEntity<NoticeResponse> createNotice(@ModelAttribute NoticeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noticeService.createNotice(request));
    }

    @PostMapping(value = "/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogMonitor
    public ResponseEntity<Void> deleteNotice(@ModelAttribute NoticeRequest request) {
        noticeService.deleteNotice(request.getNoticeId());
        return ResponseEntity.noContent().build();
    }
}
```

### API 응답 규칙 (⚠️절대 준수⚠️)
- **Response DTO 금지 필드**: 
  - `isSuccess` 필드 사용 금지
  - `message` 필드 사용 금지
  - `errorMessage` 필드 사용 금지
- **응답 패턴**:
  - 성공: 데이터만 포함한 Response 반환
  - 실패: CustomException 또는 RuntimeException 발생
  - 에러 메시지는 GlobalExceptionHandler에서 처리
- **HTTP 상태 코드**:
  - 생성(create/save/register): `201 CREATED`
  - 조회(get/list/find): `200 OK`
  - 수정(update/modify): `200 OK`
  - 삭제(delete/remove): `204 NO_CONTENT`
  - 에러: GlobalExceptionHandler가 적절한 상태 코드 자동 반환

```java
// ❌ 잘못된 예시 - 절대 사용 금지
return Response.builder()
    .isSuccess(true)
    .message("성공적으로 생성되었습니다.")
    .data(data)
    .build();

// ✅ 올바른 예시
// Controller
@PostMapping("/create")
public ResponseEntity<Response> create(@RequestBody Request request) {
    Response response = service.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

// Service
public Response create(Request request) {
    // 비즈니스 로직
    Entity entity = repository.save(newEntity);
    return Response.builder()
        .data(convertToDto(entity))
        .build();
}
```

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

### 의존성 관리 규칙 ⚠️ 필수 준수
- **외부 라이브러리**: 무조건 `Suh-Common/build.gradle`에만 선언 (`api`로 제공)
- **도메인 모듈 간 의존성**: 허용 (예: `implementation project(':Suh-Domain-Github')`)
- **도메인 모듈**: 외부 라이브러리 직접 선언 금지, Common 또는 다른 도메인 모듈만 의존 가능
- **Web 모듈**: 핵심 Spring Boot 의존성만 유지 (`spring-boot-starter`, `spring-boot-starter-web`, `spring-boot-starter-validation`)
- **중복 선언 금지**: Common에 있는 외부 라이브러리를 다른 모듈에서 다시 선언하지 않음

### 테스트 의존성 규칙 ⚠️ 필수 준수
- **통합 테스트**: 도메인 모듈에서 `@SpringBootTest` 사용 시 메인 애플리케이션 클래스 필요
- **테스트 의존성**: `testImplementation project(':{메인이 존재하는 모듈}')` 추가 필수
- **목적**: `@SpringBootTest(classes = {메인애플리케이션클래스}.class)` 사용을 위해 메인 모듈 의존성 필요
- **제외**: 메인 모듈 자체는 테스트 의존성 추가 불필요

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

### Optional 처리 패턴
- **null 체크 금지**: `== null` 비교 사용 금지
- **Optional 메서드 활용**: `orElseThrow()`, `orElse()`, `orElseGet()` 사용
- **Boolean 비교**: `Boolean.TRUE.equals()` 또는 `Boolean.FALSE.equals()` 사용

```java
// ❌ 잘못된 예시 - null 체크 사용 금지
if (repository == null) {
    throw new RuntimeException("Repository not found");
}
if (isActive != null && !isActive) {
    return false;
}

// ✅ 올바른 예시 - Optional 패턴 사용
GithubRepository repository = repositoryRepository.findById(id)
    .orElseThrow(() -> new CustomException(ErrorCode.REPOSITORY_NOT_FOUND));

// Optional이 비어있을 때 기본값 반환
String description = repository.getDescription()
    .orElse("기본 설명");

// Optional이 비어있을 때 Supplier로 값 생성
GithubRepository repo = repositoryRepository.findByName(name)
    .orElseGet(() -> createDefaultRepository(name));

// Boolean null-safe 비교
if (Boolean.FALSE.equals(repository.getIsActive())) {
    throw new CustomException(ErrorCode.REPOSITORY_INACTIVE);
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

### CSS 관리 및 CSP 보안 규칙 ⚠️ 필수 준수
- **통합 CSS 파일**: `src/main/resources/static/css/common.css` 단일 파일만 사용
- **경로 주의**: 빌드 후 `out/production/resources/static/css/common.css`에 위치하지만, 편집은 반드시 src 디렉토리에서 수행
- **인라인 스타일 절대 금지**: CSP(Content Security Policy) 준수를 위해 `style=""` 속성 사용 금지
- **클래스 기반 스타일링**: 모든 스타일은 CSS 클래스로 정의하고 JavaScript에서 `classList.add()` / `classList.remove()` 사용
- **Tailwind CSS 우선 사용**: 간단한 스타일은 Tailwind 유틸리티 클래스 활용
- **커스텀 클래스**: Tailwind로 불가능한 복잡한 스타일만 `common.css`에 정의
- **클래스 명명 규칙**: `{페이지}-{컴포넌트}` (예: `study-page`, `dashboard-section-header`, `version-badge`)
- **유틸리티 클래스**:
  - 숨김: `.hide` (display: none)
  - JavaScript 사용: `element.classList.add('hide')` / `element.classList.remove('hide')`
- **새 페이지 스타일 추가 시**: 페이지별 섹션 주석 추가 (예: `/* Grass Planter Styles */`)
- **다크모드 지원**: `[data-theme="dark"]` 셀렉터로 다크모드 스타일 오버라이드

### Tailwind CSS 사용 규칙 ⚠️ 중요
- **하드코딩 값 금지**: `mb-[5px]`, `min-h-[70vh]`, `w-[10px]` 등 대괄호를 사용한 하드코딩 값 사용 금지
- **표준 클래스 사용**: Tailwind에서 제공하는 표준 유틸리티 클래스 사용
  - 간격: `mb-1` (4px), `mb-2` (8px), `mb-3` (12px), `mb-4` (16px) 등
  - 크기: `w-4` (16px), `w-8` (32px), `h-12` (48px) 등
  - 최소/최대 높이: `min-h-screen`, `max-h-full` 등
- **예외 상황**: 표준 클래스로 불가능한 경우에만 인라인 스타일 또는 common.css에 커스텀 클래스 정의
- **잘못된 예시**: `<div class="mb-[5px] min-h-[70vh]">` ❌
- **올바른 예시**: `<div class="mb-1 min-h-screen">` ✅ 또는 `<div style="min-height: 70vh;">` ✅

#### 스타일 적용 우선순위
1. Tailwind CSS 유틸리티 클래스 (예: `hidden`, `ml-6`, `max-h-12`)
2. common.css 커스텀 클래스 (예: `.hide`, `.version-badge`)
3. 동적 스타일은 Thymeleaf `th:style` 속성만 허용 (정적 인라인 style은 금지)

### 컴포넌트 규칙
- **카드**: `ui fluid card equal-height-card` 사용
- **버튼**: `ui blue fluid button shortcut-btn` 사용
- **그리드**: 4열 기본, 반응형 (doubling stackable)
- **로딩**: 스켈레톤 UI 제공

### 반응형 디자인
- **분기점**: 모바일(~767px), 태블릿(768px~991px), 데스크탑(992px~)
- **미디어 쿼리**: 화면 크기별 최적화

### 다크모드/라이트모드 CSS 관리 ⚠️ 중요

#### common.css 파일 구조
```
1-175      : CSS 변수, badge-soft (Light/Dark 함께)
176-828    : 컴포넌트별 스타일
829-974    : Light Mode Override Styles
975-1012   : Validator, Print Styles
1013-끝    : Dark Mode Override Styles
```

#### 새 컴포넌트 스타일 추가 시
1. **컴포넌트 섹션에 기본 스타일 추가** (176-828줄 사이)
2. **라이트모드 오버라이드 필요시** → "Light Mode Override Styles" 섹션에 추가
3. **다크모드 오버라이드** → "Dark Mode Override Styles" 섹션에 추가

#### 셀렉터 패턴
```css
/* 라이트모드 (3가지 셀렉터 사용) */
:root:not([data-theme="dark"]) .my-component,
html:not([data-theme="dark"]) .my-component,
[data-theme="light"] .my-component {
  background-color: #ffffff !important;
}

/* 다크모드 */
[data-theme="dark"] .my-component {
  background-color: #1f2937 !important;
}
```

#### 충돌 방지 규칙
- **같은 속성을 여러 곳에 정의하지 말 것** (중복 정의 금지)
- **badge-soft 등 색상별 스타일**: 파일 상단에 Light/Dark 함께 정의
- **Tailwind 배경색 (`bg-white`, `bg-gray-50`)**: 다크모드에서 자동 변환 안 됨 → CSS 오버라이드 필수

#### 테마 전환 시스템
- **속성**: `data-theme="dark"` / `data-theme="light"`
- **저장소**: `localStorage.getItem('theme')` / `localStorage.setItem('theme', 'dark'|'light')`
- **초기화**: `common.js`의 `initTheme()` 함수

### DaisyUI 5 버튼 스타일 오버라이드 ⚠️ 중요

#### 배경
- **CDN 버전**: DaisyUI 5 사용 중
- **문제**: DaisyUI 5 기본 스타일만으로는 색상이 제대로 표시되지 않음
- **해결**: `common.css`에서 주요 버튼 스타일을 직접 오버라이드

#### 오버라이드된 버튼 클래스
```css
.btn-primary   /* 파란색 #3b82f6 */
.btn-error     /* 빨간색 #ef4444 */
.btn-info      /* 청록색 #06b6d4 */
.btn-success   /* 녹색 #22c55e */
.btn-outline   /* 투명 배경 + 회색 테두리 #9ca3af */
.btn-ghost     /* 투명 (호버 시에만 배경) */
```

#### 공통 스타일 패턴
- **패딩**: `10px` 통일
- **border-radius**: `0.3rem` 통일
- **다크모드**: 모든 버튼에 hover 스타일 정의
- **위치**: `common.css` 22-145줄

#### 새 버튼 추가 시
1. `common.css`에 직접 배경색/테두리색 정의 필수
2. 기존 패턴 따라 hover 및 다크모드 스타일 추가
3. 패딩 `10px`, border-radius `0.3rem` 유지

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

## 커밋 메시지 컨벤션

모든 커밋 메시지는 아래 형식을 반드시 따른다. 이모지(이모디콘) 사용 금지.

```
{이슈제목} : {type} : {변경사항 설명} {이슈URL}
```

**type 목록**: `feat` / `fix` / `refactor` / `docs` / `chore` / `test` / `style`

**예시**:
```
소셜 로그인 구현 : feat : Google OAuth 연동 추가 https://github.com/.../issues/5
Swagger 접속 오류 수정 : fix : springdoc 버전 업그레이드 https://github.com/.../issues/12
```

- 이슈가 없는 경우 이슈 URL 생략 가능
- 이모지(특수기호 포함) 커밋 메시지 사용 금지

## 작업 계획 제시 규칙

### 코드 수정 전 필수 Plan 제시
모든 코드 수정이나 큰 작업을 시작하기 전에 반드시 구체적인 계획을 다음 형식으로 제시해야 합니다:

```markdown
## 🎯 작업 계획 (Plan)

### 1. 현재 상황 분석
- 현재 코드/구조 상태
- 문제점 또는 개선 사항
- 관련 파일 목록

### 2. 목표
- 달성하고자 하는 결과
- 예상 변경 사항

### 3. 구현 단계
1) Step 1: [구체적인 작업 내용]
   - 영향받는 파일: 
   - 주요 변경사항:
2) Step 2: [구체적인 작업 내용]
   - 영향받는 파일:
   - 주요 변경사항:
3) ...

### 4. 고려사항
- 리스크 요소
- 대안 방법
- 테스트 방법

### 5. 예상 결과
- 최종 구조
- 기대 효과
```

이 계획을 사용자에게 먼저 제시하고 피드백을 받은 후 작업을 진행합니다.

## 코드 품질 규칙

### 주석 작성 규칙 ⚠️ 중요
- **불필요한 설명 주석 금지**: 코드 자체로 의미가 명확한 경우 주석 작성 금지
- **LLM 스타일 주석 금지**: "(enum의 fromEnglishNameOrCode 메서드 사용)", "(AI 응답 파싱용)" 등 불필요한 설명 금지
- **HTML 주석**: 구조적으로 필요하지 않은 설명 주석 (예: "<!-- DaisyUI Carousel (두 줄) -->") 금지
- **JavaDoc**: public API에만 작성하고, 구현 세부사항은 작성하지 않음
- **허용되는 주석**:
  - 복잡한 비즈니스 로직 설명
  - 의도적인 설계 결정 설명 (예: "// 성능 최적화를 위해 캐시 사용")
  - TODO, FIXME 등 개발 중 표시
  - 법적 요구사항이나 보안 관련 중요 정보

```java
// ❌ 잘못된 예시 - 불필요한 설명
// 응답 변환 (enum의 fromEnglishNameOrCode 메서드 사용)
TranslatorLanguage detectedLang = TranslatorLanguage.fromEnglishNameOrCode(aiResult.getDetectedLanguage());

// ✅ 올바른 예시 - 주석 없이 명확한 코드
TranslatorLanguage detectedLang = TranslatorLanguage.fromEnglishNameOrCode(aiResult.getDetectedLanguage());
```

## 주의사항
1. **파일 생성 최소화**: 기존 파일 수정 우선
2. **문서 자동 생성 금지**: 요청 시에만 생성
3. **commit 금지**: 명시적 요청 시에만 수행
4. **한글 로그**: 디버깅 용이성을 위해 한글 메시지 사용
5. **도메인별 단일 Request/Response**: API 일관성 유지
6. **Boolean 필드**: 반드시 `is` 접두사 사용
7. **작업 전 Plan 제시**: 모든 코드 수정 전 구체적인 계획 제시 필수
8. **@OneToMany/@OneToOne 절대 금지**: JPA 관계는 오직 @ManyToOne만 사용 가능. 이는 절대 규칙이며 예외 없음!!!
9. **주석 최소화**: 불필요한 LLM 스타일 주석이나 설명 주석 작성 금지
10. **Tailwind 하드코딩 금지**: `mb-[5px]` 같은 대괄호 값 사용 금지, 표준 클래스 사용

## 트러블슈팅
- **Gradle 빌드 오류**: JDK 17 버전 확인
- **DB 연결 실패**: PostgreSQL 서비스 상태 확인
- **Redis 연결 실패**: Redis 서비스 및 비밀번호 확인