---
name: implement-product-api
description: |
  Product API feature implementation guide for the bottle-note-api-server project.
  Trigger: "/implement-product-api", or when the user says "API 추가", "엔드포인트 구현", "product api", "클라이언트 API".
  Guides through the implementation flow: mono module (domain/service) -> product-api (controller).
  For test implementation, use /implement-test after this skill completes.
  Always use this skill when implementing new features or endpoints in the product-api module.
argument-hint: "[domain] [crud|search|action]"
---

# Product API Implementation Guide

This skill guides you through implementing new features in the product-api module. The project follows a DDD-based multi-module architecture where business logic lives in `bottlenote-mono` and controllers live in `bottlenote-product-api`.

## Workflow

Parse `$ARGUMENTS` to identify the target domain and work type, then follow these phases:

### Phase 0: Explore

Before writing any code, understand what already exists and what will be affected.

**Codebase scan:**
1. Check if the domain exists in mono: `bottlenote-mono/src/main/java/app/bottlenote/{domain}/`
2. Check existing services, facades, and repositories
3. Check if product-api already has controllers for this domain
4. Identify reusable code vs. what needs to be created

**Impact analysis:**
5. **Events** - related domain events (publish/subscribe), whether new events are needed
6. **Transactions** - propagation policy when calling across Facades, need for `@Async` separation
7. **Ripple scope** - files affected if Repository/Facade interfaces change (other services, InMemory implementations, tests)
8. **Cache** - whether the target data is cached (`@Cacheable`, Caffeine, Redis), invalidation strategy needed
9. **Schema** - whether Entity changes require a Liquibase migration

Report both findings and impact to the user before proceeding.

### Phase 1: Mono Module (Domain & Business Logic)

All business logic belongs in `bottlenote-mono`. Read `references/mono-patterns.md` for detailed patterns.

**Order of implementation:**
1. **Entity/Domain** (if new domain) - `{domain}/domain/`
2. **Repository** (3-tier) - Domain repo -> JPA repo -> QueryDSL (if needed)
3. **DTO** - Request/Response records in `{domain}/dto/request/`, `{domain}/dto/response/`
4. **Exception** - `{domain}/exception/{Domain}Exception.java` + `{Domain}ExceptionCode.java`
5. **Service** - `{Domain}Service` (Command/Query 분리는 필수가 아님, 아래 참고)
6. **Facade** (타 도메인 접근이 필요할 때) - `{Domain}Facade` interface + `Default{Domain}Facade`

**Service structure:**
- Command/Query split (`CommandService`/`QueryService`) exists in codebase but is not mandatory
- New services can be a single `{Domain}Service`
- No need to merge existing split services; decide per new implementation

**Facade role - protecting domain boundaries:**
- Facade prevents direct cross-domain service calls; request through the target domain's Facade instead
- Example: UserService must not query ranking data directly; use `RankingFacade`
- This allows each domain to freely change its internal implementation
- Facade interface is the contract a domain exposes to the outside

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

### Phase 3: Verify

Use the `/verify` skill to validate:

| Timing | Command | What it checks |
|--------|---------|----------------|
| After Phase 1 (Mono) | `/verify quick` | Compile + architecture rules |
| After Phase 2 (Controller) | `/verify quick` | Compile + architecture rules |
| Before push/PR | `/verify full` | Full CI including integration tests |

### Next: Tests

After implementation is verified, use `/implement-test {domain} product` to create tests.

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
