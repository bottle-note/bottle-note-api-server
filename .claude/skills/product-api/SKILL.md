---
name: product-api
description: |
  Product API feature implementation guide for the bottle-note-api-server project.
  Trigger: "/product-api", or when the user says "API 추가", "엔드포인트 구현", "product api", "클라이언트 API".
  Guides through the full implementation flow: mono module (domain/service) -> product-api (controller) -> tests.
  Always use this skill when implementing new features or endpoints in the product-api module.
argument-hint: "[domain] [crud|search|action]"
---

# Product API Implementation Guide

This skill guides you through implementing new features in the product-api module. The project follows a DDD-based multi-module architecture where business logic lives in `bottlenote-mono` and controllers live in `bottlenote-product-api`.

## Workflow

Parse `$ARGUMENTS` to identify the target domain and work type, then follow these phases:

### Phase 0: Explore

Before writing any code, understand what already exists.

1. Check if the domain exists in mono: `bottlenote-mono/src/main/java/app/bottlenote/{domain}/`
2. Check existing services, facades, and repositories
3. Check if product-api already has controllers for this domain
4. Identify reusable code vs. what needs to be created

Report your findings to the user before proceeding.

### Phase 1: Mono Module (Domain & Business Logic)

All business logic belongs in `bottlenote-mono`. Read `references/mono-patterns.md` for detailed patterns.

**Order of implementation:**
1. **Entity/Domain** (if new domain) - `{domain}/domain/`
2. **Repository** (3-tier) - Domain repo -> JPA repo -> QueryDSL (if needed)
3. **DTO** - Request/Response records in `{domain}/dto/request/`, `{domain}/dto/response/`
4. **Exception** - `{domain}/exception/{Domain}Exception.java` + `{Domain}ExceptionCode.java`
5. **Service** - `{Domain}Service` (Command/Query 분리는 필수가 아님, 아래 참고)
6. **Facade** (타 도메인 접근이 필요할 때) - `{Domain}Facade` interface + `Default{Domain}Facade`

**Service 구조에 대해:**
- Command/Query 분리(`CommandService`/`QueryService`)는 기존 코드에 존재하지만 필수 패턴이 아님
- 새로운 서비스는 `{Domain}Service` 하나로 작성해도 됨
- 기존 분리된 서비스를 합칠 필요는 없음, 신규 구현 시 판단

**Facade의 역할 - 도메인 간 경계를 보호:**
- Facade는 다른 도메인의 서비스를 직접 호출하지 않고, 해당 도메인의 Facade를 통해 요청하는 패턴
- 예: UserService가 랭킹 데이터를 직접 조회/수정하면 안 됨 → `RankingFacade`를 통해 요청
- 이렇게 하면 각 도메인이 자기 내부 구현을 자유롭게 변경할 수 있음
- Facade 인터페이스는 도메인이 외부에 노출하는 계약(contract)임

### Phase 2: Product API (Controller)

Controllers in product-api are thin - they delegate to mono services/facades.

**Controller structure:**
```java
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/{domain}")
public class {Domain}Controller {

    private final {Domain}CommandService commandService;
    private final {Domain}QueryService queryService;

    @GetMapping
    public ResponseEntity<?> list(@ModelAttribute PageableRequest request) {
        Long userId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
        // ...
        return GlobalResponse.ok(response, metaInfos);
    }

    @PostMapping
    public ResponseEntity<?> create(
        @RequestBody @Valid CreateRequest request
    ) {
        Long userId = SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));
        return GlobalResponse.ok(commandService.create(request, userId));
    }
}
```

**Key rules:**
- Path: `@RequestMapping("/api/v1/{plural-resource}")`
- Auth required: `SecurityContextUtil.getUserIdByContext().orElseThrow(...)`
- Auth optional (read): `.orElse(-1L)`
- Response: Always wrap with `GlobalResponse.ok()`
- Pagination: Use `CursorPageable` + `PageResponse` + `MetaInfos`

### Phase 3: Tests

Read `references/test-patterns.md` for detailed patterns and test infrastructure.

**Required (always implement):**
1. **Unit test** (`@Tag("unit")`) - Service logic with Fake/Stub pattern
2. **Integration test** (`@Tag("integration")`) - Full API flow with TestContainers

**Optional (user request only):**
3. **RestDocs test** (`@Tag("restdocs")`) - API documentation

Run `/verify quick` after each phase to catch compilation errors early.

### Phase 4: Verify

Use the `/verify` skill to validate at each stage:

| Timing | Command | What it checks |
|--------|---------|----------------|
| After Phase 1 (Mono) | `/verify quick` | Compile + architecture rules |
| After Phase 2 (Controller) | `/verify quick` | Compile + architecture rules |
| After Phase 3 (Tests) | `/verify standard` | Compile + unit tests + build |
| Before push/PR | `/verify full` | Full CI including integration tests |

## Endpoint Design

| HTTP Method | Purpose | URL Pattern | Example |
|-------------|---------|-------------|---------|
| GET | List | `/api/v1/{resources}` | `GET /api/v1/reviews` |
| GET | Detail | `/api/v1/{resources}/{id}` | `GET /api/v1/reviews/1` |
| POST | Create | `/api/v1/{resources}` | `POST /api/v1/reviews` |
| PUT | Full update | `/api/v1/{resources}/{id}` | `PUT /api/v1/reviews/1` |
| PATCH | Partial update | `/api/v1/{resources}/{id}` | `PATCH /api/v1/reviews/1` |
| DELETE | Delete | `/api/v1/{resources}/{id}` | `DELETE /api/v1/reviews/1` |

## Package Structure Reference

```
bottlenote-mono/src/main/java/app/bottlenote/{domain}/
├── constant/       # Enums, constants
├── domain/         # Entities, DomainRepository interface
├── dto/
│   ├── request/    # Request records (@Valid)
│   ├── response/   # Response records
│   └── dsl/        # QueryDSL criteria
├── event/          # Domain events
├── exception/      # {Domain}Exception + {Domain}ExceptionCode
├── facade/         # {Domain}Facade interface
├── repository/     # Jpa{Domain}Repository, Custom{Domain}Repository
└── service/        # {Domain}CommandService, {Domain}QueryService

bottlenote-product-api/src/main/java/app/bottlenote/{domain}/
└── controller/     # {Domain}Controller (thin, delegates to mono)
```
