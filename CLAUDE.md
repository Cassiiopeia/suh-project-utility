# SUH 프로젝트 유틸리티 - Claude AI 가이드

## Global Instructions

> Think clearly. Act honestly. Code minimally. Verify rigorously. Learn continuously.

> 본 섹션은 본 프로젝트 모든 코딩 컨벤션·도메인 규칙·작업 Flow 보다 **하위 우선순위**다. 본 프로젝트 절대 규칙(예: `@OneToMany 금지`, `git commit 자동 실행 금지`)이 본 섹션과 충돌하면 항상 프로젝트 절대 규칙이 우선한다.

---

### Hard Rules (Non-Negotiable)

#### Git Conventions
- **`Co-Authored-By` 태그 절대 금지.** 커밋 메시지에 추가하지 않는다.

---

### Core Discipline (Priority Order)

원칙들이 충돌할 때 아래 순서로 해결한다:

1. **Truthfulness** — 절대 지어내지 않는다. 모르면 모른다고 말한다. 편집 전 읽고, 완료 선언 전 검증한다.
2. **Correctness** — 실제로 동작하는가?
3. **Simplicity** — 문제를 푸는 가장 적은 움직이는 부품.
4. **Surgical scope** — 코드베이스에 미치는 영향 최소화.
5. **Elegance** — 위 4개가 만족된 후에만.

> "우아함을 추구하라"는 절대 "단순성 우선"을 덮지 않는다. 우아함은 *불필요한 복잡성의 부재*이지, *영리한 추상화의 존재*가 아니다.

---

### 1. Pre-Execution — Think & Decide

#### 1.1 Ask vs. Act

**ASK 해야 하는 경우:**
- 요구사항/의도가 불명확할 때
- 여러 유효한 해석이 존재할 때 — 옵션을 제시하고, 임의로 선택하지 않는다
- 요청보다 더 단순한 접근법이 존재할 때 — 만들기 전에 먼저 말한다

**ACT 자율적으로 해야 하는 경우:**
- 버그 리포트, 실패한 CI, 에러 로그가 주어졌을 때 — 조사 후 수정한다
- 신호가 명확할 때는 직접 해결할 수 있는 일에 손잡이를 요구하지 않는다

**항상:**
- 확인 없이 진행할 때는 가정을 명시적으로 진술한다
- 진정으로 혼란스러울 때는 멈추고 무엇이 불명확한지 말한다 — 절대 컨텍스트를 지어내지 않는다

#### 1.2 Plan Mode (non-trivial 작업용)

트리거: 3단계 이상, 아키텍처 결정, 또는 여러 파일을 건드리는 작업.

- 구현 코드를 쓰기 전에 점검 가능한 계획을 펼친다
- 실행이 어긋나면 **멈추고 재계획한다** — 즉석 패치 금지
- 단순한 1단계 수정에는 plan mode 생략

#### 1.3 Subagent Delegation

main context 를 깨끗하게 유지하기 위해 서브에이전트 활용. 좋은 후보:
- 리서치 및 코드베이스 탐색
- 독립 파일들의 병렬 분석
- context 를 부풀릴 장기 조사

서브에이전트당 한 가지 집중 과제. 단순 작업에는 생략 — 오버헤드는 공짜가 아니다.

---

### 2. Execution — Simplicity & Surgery

#### 2.1 Simplicity First

문제를 푸는 최소한의 코드. 추측성 코드 금지.

- 요청되지 않은 기능 추가 금지
- 1회 사용 코드에 대한 추상화 금지
- 요청되지 않은 "유연성"이나 "구성 가능성" 금지
- 발생 불가능한 시나리오에 대한 에러 처리 금지
- 200줄이 50줄이 될 수 있다면 다시 쓴다

**테스트:** 시니어 엔지니어가 이걸 과복잡하다고 말할까? 그렇다면 단순화한다.

#### 2.2 Surgical Changes

꼭 건드려야 하는 것만 건드린다. 자기가 만든 흔적만 정리한다.

**편집 전에 읽는다.** 수정 전에 항상 현재 파일 상태를 확인한다 — 절대 기억이나 추정에 의존하지 않는다.

기존 코드를 편집할 때:
- 인접 코드, 주석, 포맷을 "개선"하지 않는다
- 망가지지 않은 것을 리팩토링하지 않는다
- 기존 스타일을 따른다, 다르게 했을 것 같아도
- 무관한 dead code 를 발견하면 **언급한다 — 삭제하지 않는다**

변경이 고아를 만들 때:
- *내 변경이* 사용하지 않게 만든 import/변수/함수만 제거한다
- 요청 없이는 기존 dead code 를 제거하지 않는다

**테스트:** 모든 변경 라인이 사용자 요청과 직접 연결되는가?

#### 2.3 Elegance Check (non-trivial 변경에만)

동작하는 솔루션이 존재한 후 **한 번** 멈추고 묻는다: "더 우아한 방법이 있는가?"

- 수정이 hacky 하다고 느껴지면: "지금 알고 있는 모든 것을 알면, 우아한 솔루션을 구현하라."
- 단순하고 명백한 수정에는 생략 — 과잉 엔지니어링이 여기서의 실패 양식이다
- 우아함은 §2.1 의 제약을 받는다 — 추상화를 위한 추상화는 절대 금지

---

### 3. Verification — Goal-Driven Done

#### 3.1 Define Success Before Starting

| 대신... | 이렇게 변환 |
|---------|-----------|
| "validation 추가" | "유효하지 않은 입력에 대한 테스트를 쓰고, 통과시킨다" |
| "버그 수정" | "재현 테스트를 쓰고, 통과시킨다" |
| "X 리팩토링" | "전후로 테스트가 통과하는지 확인한다" |
| "더 빠르게" | "현재를 벤치마크; N% 감소 목표; 검증" |

#### 3.2 Verify Before Marking Done

동작 증명 없이 절대 작업을 완료로 표시하지 않는다.

- 테스트 실행, 로그 확인, 정확성 시연
- 관련 시 전후 동작 diff
- "완료"는 **증거** 기반으로 주장한다, 절대 믿음 기반이 아니다
- 묻는다: "스태프 엔지니어가 이걸 승인할까?"

---

### 4. Learning — Self-Improvement Loop

사용자가 무언가를 교정할 때:
1. 실수를 일으킨 패턴을 식별한다
2. 재발을 방지하는 구체적 규칙을 공식화한다
3. 그 패턴에 대한 실수율이 떨어질 때까지 가차없이 반복한다

프로젝트가 lessons/notes 파일을 제공하면, 세션 시작 시 검토하고 이전 교정에 대한 컨텍스트를 다시 로드한다.

---

### Quick Reference — The Five Tests

| Phase | 자기 점검 |
|-------|--------------|
| 코딩 전 | "내 가정을 진술했는가 — 추측하고 있는가?" |
| 코딩 중 | "시니어 엔지니어가 이걸 과복잡하다고 부를까?" |
| 편집 중 | "모든 변경 라인이 사용자 요청에 연결되는가?" |
| 완료 전 | "이게 동작함을 증명했는가 — 그저 믿고 있는가?" |
| 교정 후 | "교훈을 포착했는가?" |

---

## 프로젝트 개요
SUH 프로젝트 유틸리티는 Spring Boot 3.4.2 기반의 멀티모듈 웹 애플리케이션입니다. 다양한 도메인 기능(Docker, GitHub, Notice 등)을 제공하며, PostgreSQL과 Redis를 데이터 저장소로 사용합니다.

## 프로젝트 구조
```
suh-project-utility/
├── Suh-Common/           # 공통 유틸리티, 엔티티, 설정
├── Suh-Domain-Docker/    # Docker 관련 기능
├── Suh-Domain-Github/    # GitHub 이슈 헬퍼 기능
├── Suh-Domain-Notice/    # 공지사항 및 댓글 기능
├── Suh-Domain-Module/    # 모듈 버전 관리
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
- **Controller**: `{도메인}Controller` (예: `NoticeController`)
- **Service**: `{도메인}Service` (예: `NoticeService`)
- **Repository**: `{도메인}Repository` (예: `NoticeCommentRepository`)
- **Entity**: 도메인 객체명 (예: `NoticeComment`, `SuhProjectUtilityNotice`)
- **Boolean 필드**: 반드시 `is` 접두사 사용 (예: `isActive`, `isPublic`)

### 폴더 구조 규칙
- **깊이 제한**: 최대 2단계 깊이까지만 허용
- **일관성**: 모든 도메인 모듈이 동일한 구조 사용

```
me/suhsaechan/{domain}/
├── dto/                    # 모든 DTO 클래스
│   ├── NoticeRequest.java  # API 요청 DTO
│   ├── NoticeResponse.java # API 응답 DTO
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

// dto/NoticeResponse.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeResponse {
    private List<PostDto> posts;  // 독립 DTO 참조
    private PostDto post;
}

// Service에서 Builder 패턴 사용
return NoticeResponse.builder()
    .posts(postDtoList)
    .totalPosts(totalCount)
    .build();
```

### API 설계 원칙
- **기본 HTTP Method**: POST (파일 업로드 지원을 위해)
- **Content-Type**: `MediaType.MULTIPART_FORM_DATA_VALUE`
- **엔드포인트**: `/api/{도메인}/{기능}` (예: `/api/notice/comment/create`)
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
    
    // NOTICE - Notice 도메인 오류
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
    
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

### 시스템 동적 설정값 관리 (ServerOptionKey) ⚠️ 필수 준수
- **시스템 전역 동적 설정값은 반드시 `ServerOptionKey` enum + `ServerOption` entity 로 관리한다**
- `application.yml` 하드코딩 금지 (환경별 비밀값/인프라 값 제외)
- Service 상수(`private static final`) 하드코딩 금지
- `@Value` 주입은 빌드 타임 / 환경 단위 값(DB 호스트, 외부 API URL, 시크릿 키 등)에만 사용. 운영 중 변경 가능 값은 모두 `ServerOptionKey` 사용

#### 새 설정 추가 절차
1. `Suh-Common/src/main/java/me/suhsaechan/common/constant/ServerOptionKey.java` 에 키 추가 (description, defaultValue 명시)
2. Service 에서 `serverOptionService.getOption(ServerOptionKey.XXX)` 으로 조회
3. 기존 admin 페이지에서 자동으로 편집 가능 (enum 자동 노출)

#### 예시
```java
@Getter
@RequiredArgsConstructor
public enum ServerOptionKey {
  CHATBOT_CHUNK_SIZE("챗봇 청크 크기 (토큰 수)", "500"),
  CHATBOT_CHUNK_OVERLAP("챗봇 청크 중첩 크기 (토큰 수)", "100"),
  SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID("소만사 버스 노선 동기화 트리거 회원 loginId", "chan4760@somansa.com");

  private final String description;
  private final String defaultValue;
}
```

```java
// Service 사용 예
@RequiredArgsConstructor
public class SomansaBusRouteService {
  private final ServerOptionService serverOptionService;

  public void syncRoutes() {
    String triggerLoginId = serverOptionService
        .getOption(ServerOptionKey.SOMANSA_BUS_SYNC_TRIGGER_LOGIN_ID)
        .getOptionValue();
    // ... triggerLoginId 사용
  }
}
```

#### 판단 가이드
| 상황 | 위치 |
|------|------|
| 운영 중 변경 가능, 코드 재배포 없이 바꾸고 싶음 | `ServerOptionKey` |
| 외부 API 호출 시 사용할 식별자/계정 등 비즈니스 설정값 | `ServerOptionKey` |
| AI 모델명, 청크 크기 같은 튜닝 파라미터 | `ServerOptionKey` |
| DB 호스트, Redis 비밀번호 같은 인프라 시크릿 | `application.yml` + `@Value` (환경변수) |
| 코드에서 절대 안 바뀌는 상수 (예: HTTP timeout=5000ms) | Service 내 `private static final` 허용 |

### 의존성 관리 규칙 ⚠️ 필수 준수
- **외부 라이브러리**: 무조건 `Suh-Common/build.gradle`에만 선언 (`api`로 제공)
- **도메인 모듈 간 의존성**: 허용 (예: `implementation project(':Suh-Domain-Github')`)
- **도메인 모듈**: 외부 라이브러리 직접 선언 금지, Common 또는 다른 도메인 모듈만 의존 가능
- **Web 모듈**: 핵심 Spring Boot 의존성만 유지 (`spring-boot-starter`, `spring-boot-starter-web`, `spring-boot-starter-validation`)
- **중복 선언 금지**: Common에 있는 외부 라이브러리를 다른 모듈에서 다시 선언하지 않음

### Flyway 마이그레이션 규칙 ⚠️ 필수 준수

**Entity 스키마 변경 시 반드시 Flyway 마이그레이션 파일을 함께 작성한다.**

#### 기본 원칙
- `hibernate.ddl-auto: update` 유지 — Flyway는 보조 수단, Hibernate가 실제 스키마 관리
- Flyway는 `ddl-auto`보다 먼저 실행되므로 스키마 변경을 선제적으로 적용 가능
- 기존 마이그레이션 파일은 절대 수정 금지 — 새 버전 파일로 추가

#### 파일 위치 및 네이밍
- **위치**: `Suh-Web/src/main/resources/db/migration/`
- **네이밍**: `V{version}__{설명}.sql` — `version.yml`의 `version` 값 사용, 점(`.`)은 언더스코어(`_`)로 치환
  - 예: `version: "2.5.27"` → `V2_5_27__add_is_shuttle_to_somansa_bus_route.sql`
- **버전당 파일 1개 원칙**: 같은 버전 prefix 파일 2개 생성 금지 (Flyway 에러 발생)
- **마이그레이션 전 반드시 `version.yml` 읽어 현재 버전 확인**

#### 초기화 안전 전략 ⚠️ 필수 준수
**DB가 완전 초기 상태(테이블 없음)에서 jar를 실행하는 경우에도 반드시 동작해야 한다.**

- **신규 테이블 추가**: `CREATE TABLE IF NOT EXISTS`로 전체 스키마 정의
- **기존 테이블 컬럼 추가**: `CREATE TABLE IF NOT EXISTS` + `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 둘 다 작성
  - 이유: 기존 DB엔 테이블 있고 컬럼만 없는 경우 / 완전 초기화 상태 둘 다 커버해야 함
- **컬럼 삭제**: `ALTER TABLE ... DROP COLUMN IF EXISTS`
- **인덱스**: `CREATE INDEX IF NOT EXISTS` / `DROP INDEX IF EXISTS`
- **절대 금지**: 조건 없는 `ALTER TABLE ADD COLUMN`, `CREATE TABLE`, `DROP TABLE`

#### 작업 체크리스트
- [ ] Entity 추가/변경 시 Flyway 마이그레이션 파일도 함께 작성했는가?
- [ ] 마이그레이션 파일 작성 전 `version.yml`을 읽어 현재 버전을 확인했는가?
- [ ] 동일 버전으로 파일이 2개 이상 생성되지 않았는가?
- [ ] `CREATE TABLE IF NOT EXISTS` + `ADD COLUMN IF NOT EXISTS` 둘 다 작성해 완전 초기화 상태도 커버했는가?

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
public class NoticeService {
    private final SuhProjectUtilityNoticeRepository noticeRepository;
    private final NoticeCommentRepository commentRepository;
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
- **클래스 명명 규칙**: `{페이지}-{컴포넌트}` (예: `notice-page`, `dashboard-section-header`, `version-badge`)
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

## 표준 작업 Flow (AgenticFlow)

이슈 기반 작업 시 **무조건 이 순서**를 따른다. 관련 issue: [#186](https://github.com/Cassiiopeia/suh-project-utility/issues/186)

### Phase 1 — 이슈 + 워크트리
- **신규 이슈**: `/cassiiopeia:issue` (이슈 작성·등록·브랜치명 계산·worktree 옵션까지 한 방)
- **이슈 이미 있고 브랜치만 분리**: `/cassiiopeia:init-worktree`
- **긴급 버그 등 신속 대응 케이스**: worktree 생성 생략 가능 (main 또는 메인 작업 디렉터리에서 바로 진행)

### Phase 2 — 설계 (무조건 superpowers)
- `/superpowers:brainstorming` — 요구사항 명확화 → 디자인 → spec 문서 작성
- `/superpowers:writing-plans` — plan 문서 작성 (Task 단위로 분해)

### Phase 3 — 구현
- `/superpowers:subagent-driven-development` — Task별 implementer + spec reviewer + quality reviewer 디스패치

### Phase 4 — Commit + PR
- `/cassiiopeia:commit` — 사용자 승인 후 commit. push는 사용자 명시 요청 시
- `/cassiiopeia:github` — PR 생성

### Phase 5 — 빌드 + QA
- `/cassiiopeia:github` — 이슈에 `@suh-lab server build` 댓글 추가
- `/cassiiopeia:testcase` — 테스트케이스 MD 작성
- `/cassiiopeia:github` — 테스트케이스 이슈 댓글로 게시

### Phase 6 — main 머지 후 배포
- `/cassiiopeia:changelog-deploy` — main push + deploy PR 생성 + 릴리스 노트 작성 + automerge

### 절대 규칙 — Skill 우회 금지
- **GitHub 작업** (이슈/PR/댓글) → 무조건 `/cassiiopeia:github` 거치기. `gh`/`curl`/`Invoke-RestMethod` 직접 호출 금지
- **Commit** → 무조건 `/cassiiopeia:commit` 거치기. `git commit` 직접 호출 금지
- **설계/구현** → 무조건 superpowers 3-skill 체인 (`brainstorming` → `writing-plans` → `subagent-driven-development`) 거치기. 바로 코드 작성 금지

### 본 flow 미적용 예외
- 단순 typo 수정 / 1줄 변경 같은 trivial fix는 brainstorming/plan 생략 가능. 단 commit/push/PR/댓글은 skill 거치기
- 긴급 버그는 worktree 생성 생략 가능
- **메타성 / 문서성 변경** (CLAUDE.md, 리포트, plan/spec md, commands·skill 정리, 문서 오타 수정 등 앱 동작 영향 없는 변경)은 **Phase 5 생략** — `@suh-lab server build` 댓글 불필요, 테스트케이스 작성 불필요. Phase 4 commit/PR까지만 처리

## Git 커밋 규칙

### ⛔ 절대 자동 커밋 금지 (가장 중요한 규칙)
- **Claude는 절대로, 어떤 상황에서도, 어떤 스킬/워크플로우를 따르더라도 사용자 명시적 허락 없이 `git commit`을 실행하지 않는다**
- **`git add`도 사용자 확인 없이 절대 실행 금지**
- 코드 수정 후 반드시 사용자가 diff를 확인할 수 있도록 대기한다
- 커밋은 사용자가 명시적으로 "커밋해줘"라고 요청했을 때만 수행한다
- 스킬(skill)이 커밋을 지시하더라도 이 규칙이 우선한다
- 서브에이전트(subagent)에게도 커밋하지 말라고 명시적으로 지시한다

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