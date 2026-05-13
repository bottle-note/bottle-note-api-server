---
name: implement
description: |
  Incremental feature implementation for product-api and admin-api modules.
  Trigger: "/implement", or when the user says "API 추가", "엔드포인트 구현", "기능 구현", "feature implementation", "기능 개발".
  Guides through the implementation flow: mono module (domain/service) -> API module (controller).
  Supports both product-api (Java) and admin-api (Kotlin) via module argument.
  For test implementation, use /test after this skill completes.
argument-hint: "[domain] [product|admin] [crud|search|action]"
---

# Incremental Implementation

## Overview

Build features in thin vertical slices — implement one piece, verify it compiles, then expand. Each Task is a committable unit; each Slice within a Task is a compile-check unit. This skill unifies product-api (Java) and admin-api (Kotlin) implementation through a shared workflow with module-specific branching.

All business logic lives in `bottlenote-mono`. API modules (`bottlenote-product-api`, `bottlenote-admin-api`) contain only thin controllers that delegate to mono services/facades.

## When to Use

- Implementing any new feature or endpoint
- Adding CRUD operations to an existing domain
- Extending an existing domain with new service methods
- Any multi-file implementation work in product-api or admin-api

## When NOT to Use

- Bug fixing with clear reproduction (use `/debug`)
- Test-only work (use `/test`)
- Requirements unclear or ambiguous (use `/define` first)
- Single-file config changes or documentation updates

## Argument Parsing

Parse `$ARGUMENTS` to identify:
- **domain**: target domain (e.g., `alcohols`, `rating`, `review`, `support`)
- **module**: `product` (default) or `admin`
- **work type**: `crud`, `search`, `action` (informational, guides exploration scope)

## Process

### Phase 0: Explore

Before writing any code, understand what already exists and what will be affected.

**Codebase scan:**
1. Check if the domain exists in mono: `bottlenote-mono/src/main/java/app/bottlenote/{domain}/`
2. Check existing services, facades, and repositories
3. Check if the target API module already has controllers for this domain
4. Identify reusable code vs. what needs to be created

**Impact analysis:**
5. **Events** — related domain events (publish/subscribe), whether new events are needed
6. **Transactions** — propagation policy when calling across Facades, need for `@Async` separation
7. **Ripple scope** — files affected if Repository/Facade interfaces change (other services, InMemory implementations, tests)
8. **Cache** — whether the target data is cached (`@Cacheable`, Caffeine, Redis), invalidation strategy needed
9. **Schema** — whether Entity changes require a Liquibase migration

Report both findings and impact to the user before proceeding.

### Phase 1: Mono Module (Domain & Business Logic)

All business logic belongs in `bottlenote-mono`. Read `references/mono-patterns.md` for detailed patterns.

**Order of implementation:**
1. **Entity/Domain** (if new domain) — `{domain}/domain/`
2. **Repository** (3-tier) — Domain repo -> JPA repo -> QueryDSL (if needed)
3. **DTO** — Request/Response records in `{domain}/dto/request/`, `{domain}/dto/response/`
4. **Exception** — `{domain}/exception/{Domain}Exception.java` + `{Domain}ExceptionCode.java`
5. **Service** — `{Domain}Service` (single service is the default; Command/Query split is optional)
6. **Facade** (when cross-domain access is needed) — `{Domain}Facade` interface + `Default{Domain}Facade`

**Module-specific service naming:**
- **product**: `{Domain}Service` or `{Domain}CommandService` / `{Domain}QueryService`
- **admin**: `Admin{Domain}Service` (separate service with `adminId` parameter pattern)

### Phase 2: API Controller

Read the appropriate reference for your target module:
- **product**: `references/product-patterns.md`
- **admin**: `references/admin-patterns.md`

**Product (Java):**
- Path: `@RequestMapping("/api/v1/{plural-resource}")`
- Auth required: `SecurityContextUtil.getUserIdByContext().orElseThrow(...)`
- Auth optional (read): `.orElse(-1L)`
- Response: `GlobalResponse.ok(response)` or `GlobalResponse.ok(response, metaInfos)`
- Pagination: `CursorPageable` + `PageResponse`

**Admin (Kotlin):**
- Context path: `/admin/api/v1` (configured in application)
- Path: `@RequestMapping("/{plural-resource}")`
- Auth: `SecurityContextUtil.getAdminUserIdByContext()`
- Response: `GlobalResponse.ok(service.method())` or `ResponseEntity.ok(service.search(request))`
- Pagination: Offset-based (`page`, `size`)

### Phase 3: Task-Slice-Commit Cycle

Each Task from the `/plan` is implemented through Slices:

```
Task  = Commit unit (logical goal agreed with user)
Slice = Execution unit (compile check before exceeding ~100 lines)
```

**Verification levels:**

| Timing | Level | What to run |
|--------|-------|-------------|
| After each Slice | Compile only | `./gradlew compileJava compileTestJava` (+ `compileKotlin` for admin) |
| After Task completion | Self-review + unit tests | `/self-review` -> `./gradlew unit_test check_rule_test` |
| After all Tasks complete | Integration tests | `/verify full` |

**Commit message format** (Task = title, Slices = bullets):
```
feat: rating 통계 API Service 구현

- RatingStatisticsResponse DTO 작성
- RatingRepository에 통계 조회 메서드 추가
- RatingService.getStatistics() 구현
- AlcoholFacade 연동
```

**Cycle per Task:**
1. Implement Slice -> compile check -> pass? continue : fix
2. Repeat until all Slices in the Task are done
3. Run `/self-review` on the Task's changes
4. Run `./gradlew unit_test check_rule_test`
5. Commit with descriptive message
6. Update plan document (check off Task, add to Progress Log)
7. Move to next Task

### Phase 4: Final Verification

After all Tasks are committed:

| Timing | Command | Purpose |
|--------|---------|---------|
| Implementation done | `/verify standard` | Compile + unit + build |
| Before push/PR | `/verify full` | Includes integration tests |

Then use `/test` if integration tests need to be written.

## Endpoint Design

| HTTP Method | Purpose | URL Pattern (product) | URL Pattern (admin) |
|-------------|---------|----------------------|---------------------|
| GET | List | `/api/v1/{resources}` | `/{resources}` |
| GET | Detail | `/api/v1/{resources}/{id}` | `/{resources}/{id}` |
| POST | Create | `/api/v1/{resources}` | `/{resources}` |
| PUT | Full update | `/api/v1/{resources}/{id}` | `/{resources}/{id}` |
| PATCH | Partial | `/api/v1/{resources}/{id}` | `/{resources}/{id}` |
| DELETE | Delete | `/api/v1/{resources}/{id}` | `/{resources}/{id}` |

## Package Structure

```
bottlenote-mono/src/main/java/app/bottlenote/{domain}/
├── constant/       # Enums, constants
├── domain/         # Entities, DomainRepository interface (@DomainRepository)
├── dto/
│   ├── request/    # Request records (@Valid)
│   ├── response/   # Response records
│   └── dsl/        # QueryDSL criteria
├── event/          # Domain events, @DomainEventListener
├── exception/      # {Domain}Exception + {Domain}ExceptionCode
├── facade/         # {Domain}Facade interface
├── repository/     # Jpa{Domain}Repository (@JpaRepositoryImpl), Custom repos
└── service/        # {Domain}Service (@Service), Default{Domain}Facade (@FacadeService)

bottlenote-product-api/src/main/java/app/bottlenote/{domain}/
└── controller/     # {Domain}Controller (thin, delegates to mono)

bottlenote-admin-api/src/main/kotlin/app/bottlenote/{domain}/
└── presentation/   # Admin{Domain}Controller.kt (thin, delegates to mono)
```

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "I'll test it all at the end" | Bugs compound. A bug in Slice 1 makes Slices 2-5 wrong. Compile-check each Slice. |
| "It's faster to do it all at once" | It feels faster until something breaks and you cannot find which of 500 changed lines caused it. |
| "I don't need a Facade for this" | If you are accessing another domain's Repository or Service directly, you need a Facade. Domain boundaries exist for a reason. |
| "This refactor is small enough to include" | Refactors mixed with features make both harder to review and debug. Separate them. |
| "The controller can handle this logic" | Controllers are thin. Business logic belongs in mono services. Always. |
| "I'll skip self-review, the code is straightforward" | Straightforward code still needs architecture and security checks. Run `/self-review`. |

## Red Flags

- More than 100 lines written without a compile check
- Cross-domain Repository or Service injected directly (bypassing Facade)
- Controller containing business logic instead of delegating to service
- Repository interface changed but InMemory test implementation not updated
- No error handling for domain-specific exceptions
- Skipping Phase 0 (Explore) and jumping straight to coding
- Multiple unrelated changes in a single Task
- Task completed without running `/self-review`

## Verification

After completing all Tasks for a feature:

- [ ] Each Task was individually reviewed (`/self-review`) and committed
- [ ] All Facade boundaries respected (no cross-domain direct access)
- [ ] Repository 3-Tier pattern followed for new repositories
- [ ] Unit tests pass: `./gradlew unit_test`
- [ ] Architecture rules pass: `./gradlew check_rule_test`
- [ ] Build succeeds: `./gradlew build -x test -x asciidoctor`
- [ ] Plan document updated (all Tasks checked, Progress Log filled)
