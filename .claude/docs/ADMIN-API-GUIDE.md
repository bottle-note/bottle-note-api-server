# Admin API 구현 가이드

> 이 문서는 Admin API를 구현할 때 따라야 할 표준 패턴과 컨벤션을 정의합니다.

## 목차
- [아키텍처 개요](#아키텍처-개요)
- [구현 단계](#구현-단계)
- [DTO 작성 규칙](#dto-작성-규칙)
- [Service 작성 규칙](#service-작성-규칙)
- [Controller 작성 규칙](#controller-작성-규칙)
- [테스트 작성 규칙](#테스트-작성-규칙)
- [문서화 규칙](#문서화-규칙)

---

## 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────┐
│                    admin-api (Kotlin)                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Controller (presentation)               │   │
│  │  - REST 엔드포인트 정의                              │   │
│  │  - 요청/응답 처리                                    │   │
│  │  - GlobalResponse 래핑                               │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────┘
                             │ 의존
┌────────────────────────────▼────────────────────────────────┐
│                    mono (Java)                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    Service                           │   │
│  │  - 비즈니스 로직 처리                                │   │
│  │  - 트랜잭션 관리                                     │   │
│  │  - AdminResultResponse 반환                          │   │
│  └────────────────────────────┬────────────────────────┘   │
│  ┌────────────────────────────▼────────────────────────┐   │
│  │                   Repository                         │   │
│  │  - JPA + QueryDSL                                    │   │
│  │  - 도메인 레포지토리 인터페이스 구현                 │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                     DTO                              │   │
│  │  - Request: Java record + validation                 │   │
│  │  - Response: Java record                             │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 구현 단계

### Phase 1: 구현

| 순서 | 작업 | 모듈 | 언어 |
|------|------|------|------|
| 1 | Request/Response DTO 작성 | mono | Java |
| 2 | Service 작성 | mono | Java |
| 3 | Repository 확장 (필요 시) | mono | Java |
| 4 | Controller 작성 | admin-api | Kotlin |

### Phase 2: 테스트

| 순서 | 작업 | 위치 |
|------|------|------|
| 1 | Test Helper 작성 | `app/helper/{domain}/` |
| 2 | Integration Test 작성 | `app/integration/{domain}/` |

### Phase 3: 문서화

| 순서 | 작업 | 위치 |
|------|------|------|
| 1 | RestDocs Test 작성 | `app/docs/{domain}/` |
| 2 | AsciiDoc 작성 | `src/docs/asciidoc/{domain}.adoc` |

---

## DTO 작성 규칙

### 위치
```
bottlenote-mono/src/main/java/app/bottlenote/{domain}/dto/
├── request/
│   ├── Admin{Domain}SearchRequest.java    # 목록 조회
│   ├── Admin{Domain}CreateRequest.java    # 생성
│   ├── Admin{Domain}UpdateRequest.java    # 수정
│   └── Admin{Domain}{Action}Request.java  # 특수 액션
└── response/
    ├── Admin{Domain}ListResponse.java     # 목록 항목
    └── Admin{Domain}DetailResponse.java   # 상세 조회
```

### Request DTO 패턴

```java
// 검색 요청 (GET 파라미터)
public record Admin{Domain}SearchRequest(
    String keyword,
    {FilterType} filter,
    Integer page,
    Integer size) {

  @Builder
  public Admin{Domain}SearchRequest {
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}

// 생성/수정 요청 (POST/PUT body)
public record Admin{Domain}CreateRequest(
    @NotBlank(message = "이름은 필수입니다.") String name,
    String description,
    @NotNull(message = "타입은 필수입니다.") {Type} type) {

  @Builder
  public Admin{Domain}CreateRequest {
    // 기본값 설정 (선택)
  }
}
```

### Response DTO 패턴

```java
// 목록 응답
public record Admin{Domain}ListResponse(
    Long id,
    String name,
    {Type} type,
    Boolean isActive,
    LocalDateTime createdAt) {}

// 상세 응답
public record Admin{Domain}DetailResponse(
    Long id,
    String name,
    String description,
    // ... 상세 필드
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {

  public static Admin{Domain}DetailResponse from({Domain} entity) {
    return new Admin{Domain}DetailResponse(
        entity.getId(),
        entity.getName(),
        // ...
    );
  }
}
```

### 페이징 방식

| API 유형 | 방식 | 파라미터 |
|----------|------|----------|
| Admin (관리자) | 오프셋 페이징 | `page`, `size` |
| Product (클라이언트) | 커서 페이징 | `cursor`, `pageSize` |

---

## Service 작성 규칙

### 위치
```
bottlenote-mono/src/main/java/app/bottlenote/{domain}/service/Admin{Domain}Service.java
```

### 기본 구조

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class Admin{Domain}Service {

  private final {Domain}Repository repository;

  // 목록 조회
  @Transactional(readOnly = true)
  public Page<Admin{Domain}ListResponse> search(Admin{Domain}SearchRequest request) {
    PageRequest pageable = PageRequest.of(request.page(), request.size());
    return repository.searchForAdmin(request, pageable);
  }

  // 상세 조회
  @Transactional(readOnly = true)
  public Admin{Domain}DetailResponse getDetail(Long id) {
    {Domain} entity = repository.findById(id)
        .orElseThrow(() -> new {Domain}Exception({DOMAIN}_NOT_FOUND));
    return Admin{Domain}DetailResponse.from(entity);
  }

  // 생성
  @Transactional
  public AdminResultResponse create(Admin{Domain}CreateRequest request) {
    // 중복 검사 (필요 시)
    if (repository.existsByName(request.name())) {
      throw new {Domain}Exception({DOMAIN}_DUPLICATE_NAME);
    }

    {Domain} entity = {Domain}.create(request.name(), ...);
    {Domain} saved = repository.save(entity);
    return AdminResultResponse.of({DOMAIN}_CREATED, saved.getId());
  }

  // 수정
  @Transactional
  public AdminResultResponse update(Long id, Admin{Domain}UpdateRequest request) {
    {Domain} entity = repository.findById(id)
        .orElseThrow(() -> new {Domain}Exception({DOMAIN}_NOT_FOUND));

    entity.update(request.name(), ...);
    return AdminResultResponse.of({DOMAIN}_UPDATED, id);
  }

  // 삭제
  @Transactional
  public AdminResultResponse delete(Long id) {
    {Domain} entity = repository.findById(id)
        .orElseThrow(() -> new {Domain}Exception({DOMAIN}_NOT_FOUND));

    repository.delete(entity);
    return AdminResultResponse.of({DOMAIN}_DELETED, id);
  }
}
```

### AdminResultResponse 사용

CUD 작업은 `AdminResultResponse`로 통일:

```java
// global/dto/response/AdminResultResponse.java
public record AdminResultResponse(String code, String message, Long targetId, String responseAt) {
  public static AdminResultResponse of(ResultCode code, Long targetId) { ... }

  public enum ResultCode {
    {DOMAIN}_CREATED("{도메인}이(가) 등록되었습니다."),
    {DOMAIN}_UPDATED("{도메인}이(가) 수정되었습니다."),
    {DOMAIN}_DELETED("{도메인}이(가) 삭제되었습니다."),
    // 커스텀 액션
    {DOMAIN}_{ACTION}("{도메인} {액션}이(가) 완료되었습니다.");
  }
}
```

---

## Controller 작성 규칙

### 위치
```
bottlenote-admin-api/src/main/kotlin/app/bottlenote/{domain}/presentation/Admin{Domain}Controller.kt
```

### 기본 구조

```kotlin
@RestController
@RequestMapping("/{resources}")  // 복수형 리소스명 (kebab-case)
class Admin{Domain}Controller(
    private val admin{Domain}Service: Admin{Domain}Service
) {

    // 목록 조회
    @GetMapping
    fun list(@ModelAttribute request: Admin{Domain}SearchRequest): ResponseEntity<*> {
        return GlobalResponse.ok(admin{Domain}Service.search(request))
    }

    // 상세 조회
    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ResponseEntity<*> {
        return GlobalResponse.ok(admin{Domain}Service.getDetail(id))
    }

    // 생성
    @PostMapping
    fun create(@RequestBody @Valid request: Admin{Domain}CreateRequest): ResponseEntity<*> {
        return GlobalResponse.ok(admin{Domain}Service.create(request))
    }

    // 수정
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody @Valid request: Admin{Domain}UpdateRequest
    ): ResponseEntity<*> {
        return GlobalResponse.ok(admin{Domain}Service.update(id, request))
    }

    // 삭제
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<*> {
        return GlobalResponse.ok(admin{Domain}Service.delete(id))
    }
}
```

### HTTP 메서드 규칙

| 작업 | Method | URL 패턴 | 예시 |
|------|--------|----------|------|
| 목록 조회 | GET | `/{resources}` | `GET /curations` |
| 상세 조회 | GET | `/{resources}/{id}` | `GET /curations/1` |
| 생성 | POST | `/{resources}` | `POST /curations` |
| 전체 수정 | PUT | `/{resources}/{id}` | `PUT /curations/1` |
| 부분 수정 | PATCH | `/{resources}/{id}/{field}` | `PATCH /curations/1/status` |
| 삭제 | DELETE | `/{resources}/{id}` | `DELETE /curations/1` |
| 하위 리소스 추가 | POST | `/{resources}/{id}/{sub}` | `POST /curations/1/alcohols` |
| 하위 리소스 삭제 | DELETE | `/{resources}/{id}/{sub}/{subId}` | `DELETE /curations/1/alcohols/5` |

### 인증이 필요한 경우

```kotlin
@PostMapping("/{id}/action")
fun action(
    @PathVariable id: Long,
    @RequestBody @Valid request: ActionRequest
): ResponseEntity<*> {
    val adminId = SecurityContextUtil.getAdminUserIdByContext()
        .orElseThrow { UserException(UserExceptionCode.REQUIRED_USER_ID) }
    return GlobalResponse.ok(service.action(id, adminId, request))
}
```

---

## 테스트 작성 규칙

### Test Helper

**위치**: `app/helper/{domain}/{Domain}Helper.kt`

```kotlin
object {Domain}Helper {

    fun createAdmin{Domain}ListResponse(
        id: Long = 1L,
        name: String = "테스트 {도메인}",
        isActive: Boolean = true
    ): Admin{Domain}ListResponse = Admin{Domain}ListResponse(
        id, name, /* ... */, isActive, LocalDateTime.now()
    )

    fun createAdmin{Domain}DetailResponse(
        id: Long = 1L,
        name: String = "테스트 {도메인}"
    ): Admin{Domain}DetailResponse = Admin{Domain}DetailResponse(
        id, name, /* ... */
    )

    fun create{Domain}Request(
        name: String = "새 {도메인}"
    ): Map<String, Any> = mapOf(
        "name" to name,
        // ...
    )
}
```

### Integration Test

**위치**: `app/integration/{domain}/Admin{Domain}IntegrationTest.kt`

```kotlin
@Tag("admin_integration")
@DisplayName("[integration] Admin {도메인} API 통합 테스트")
class Admin{Domain}IntegrationTest : IntegrationTestSupport() {

    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        val admin = adminUserTestFactory.persistRootAdmin()
        accessToken = getAccessToken(admin)
    }

    @Nested
    @DisplayName("{도메인} 목록 조회 API")
    inner class List{Domain}s {

        @Test
        @DisplayName("{도메인} 목록을 조회할 수 있다")
        fun listSuccess() {
            // given - 테스트 데이터 준비

            // when & then
            assertThat(
                mockMvcTester.get().uri("/{resources}")
                    .header("Authorization", "Bearer $accessToken")
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.content").isNotNull()
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        fun listUnauthorized() {
            assertThat(
                mockMvcTester.get().uri("/{resources}")
            )
                .hasStatus(HttpStatus.UNAUTHORIZED)
        }
    }

    @Nested
    @DisplayName("{도메인} 생성 API")
    inner class Create{Domain} {

        @Test
        @DisplayName("{도메인}을 생성할 수 있다")
        fun createSuccess() {
            val request = {Domain}Helper.create{Domain}Request()

            assertThat(
                mockMvcTester.post().uri("/{resources}")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.code").isEqualTo("{DOMAIN}_CREATED")
        }

        @Test
        @DisplayName("필수 필드 누락 시 400을 반환한다")
        fun createValidationFail() {
            val request = mapOf("name" to "")

            assertThat(
                mockMvcTester.post().uri("/{resources}")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))
            )
                .hasStatus(HttpStatus.BAD_REQUEST)
        }
    }

    // @Nested inner class Update{Domain} { ... }
    // @Nested inner class Delete{Domain} { ... }
}
```

### 테스트 케이스 체크리스트

| API | 필수 테스트 |
|-----|-----------|
| 목록 조회 | 성공, 인증 실패, 필터링 |
| 상세 조회 | 성공, 인증 실패, 존재하지 않는 ID |
| 생성 | 성공, 인증 실패, 필수 필드 누락, 중복 검사 |
| 수정 | 성공, 인증 실패, 존재하지 않는 ID |
| 삭제 | 성공, 인증 실패, 존재하지 않는 ID |

---

## 문서화 규칙

### RestDocs Test

**위치**: `app/docs/{domain}/Admin{Domain}ControllerDocsTest.kt`

```kotlin
@WebMvcTest(
    controllers = [Admin{Domain}Controller::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("[docs] Admin {도메인} API 문서화")
class Admin{Domain}ControllerDocsTest {

    @Autowired private lateinit var mvc: MockMvc
    @Autowired private lateinit var mapper: ObjectMapper
    @MockitoBean private lateinit var admin{Domain}Service: Admin{Domain}Service

    @Test
    @DisplayName("{도메인} 목록 조회")
    fun list{Domain}s() {
        // given
        val response = PageImpl(listOf({Domain}Helper.createAdmin{Domain}ListResponse()))
        given(admin{Domain}Service.search(any())).willReturn(response)

        // when & then
        mvc.get("/{resources}") {
            param("page", "0")
            param("size", "20")
        }.andExpect {
            status { isOk() }
        }.andDo {
            document(
                "admin/{resources}/list",
                queryParameters(
                    parameterWithName("page").description("페이지 번호").optional(),
                    parameterWithName("size").description("페이지 크기").optional()
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("data.content[]").description("목록"),
                    // ...
                )
            )
        }
    }
}
```

### AsciiDoc

**위치**: `src/docs/asciidoc/{domain}.adoc`

```asciidoc
= {도메인} 관리 API

== {도메인} 목록 조회
operation::admin/{resources}/list[snippets='http-request,query-parameters,http-response,response-fields']

== {도메인} 상세 조회
operation::admin/{resources}/detail[snippets='http-request,path-parameters,http-response,response-fields']

== {도메인} 생성
operation::admin/{resources}/create[snippets='http-request,request-fields,http-response,response-fields']

== {도메인} 수정
operation::admin/{resources}/update[snippets='http-request,path-parameters,request-fields,http-response,response-fields']

== {도메인} 삭제
operation::admin/{resources}/delete[snippets='http-request,path-parameters,http-response,response-fields']
```

---

## 예외 처리

### ExceptionCode 추가

```java
// {domain}/exception/{Domain}ExceptionCode.java
public enum {Domain}ExceptionCode implements ExceptionCode {
  {DOMAIN}_NOT_FOUND(HttpStatus.NOT_FOUND, "{도메인}을(를) 찾을 수 없습니다."),
  {DOMAIN}_DUPLICATE_NAME(HttpStatus.CONFLICT, "동일한 이름의 {도메인}이(가) 이미 존재합니다."),
  {DOMAIN}_INVALID_STATE(HttpStatus.BAD_REQUEST, "잘못된 상태입니다.");
  // ...
}
```

---

## 빠른 시작 체크리스트

새로운 Admin API 구현 시 다음 순서로 진행:

- [ ] **DTO**: Request/Response record 작성 (mono)
- [ ] **Exception**: ExceptionCode enum 추가 (mono)
- [ ] **ResultCode**: AdminResultResponse.ResultCode 추가 (mono)
- [ ] **Repository**: 필요 시 Admin 전용 쿼리 메서드 추가 (mono)
- [ ] **Service**: Admin{Domain}Service 작성 (mono)
- [ ] **Controller**: Admin{Domain}Controller 작성 (admin-api, Kotlin)
- [ ] **Helper**: {Domain}Helper object 작성 (test)
- [ ] **Integration Test**: Admin{Domain}IntegrationTest 작성 (test)
- [ ] **RestDocs Test**: Admin{Domain}ControllerDocsTest 작성 (선택)
- [ ] **AsciiDoc**: {domain}.adoc 작성 (선택)
- [ ] **빌드 검증**: `./gradlew :bottlenote-admin-api:build`
