---
name: implement-test
description: |
  Test implementation guide for bottle-note-api-server (product-api & admin-api).
  Trigger: "/implement-test", or when the user says "테스트 작성", "테스트 구현", "테스트 추가", "write tests", "implement tests".
  Guides through unit test (Fake/Stub), integration test, and RestDocs test creation.
  Supports both product-api (Java) and admin-api (Kotlin) modules.
argument-hint: "[domain] [product|admin] [unit|integration|restdocs|all]"
---

# Test Implementation Guide

References:
- `references/test-infra.md` - shared test utilities, TestContainers, existing Fake/InMemory list
- `references/test-patterns.md` - unit, integration, RestDocs code patterns

## Argument Parsing

Parse `$ARGUMENTS` to determine:
- **domain**: target domain (e.g., `alcohols`, `rating`, `review`)
- **module**: `product` (default) or `admin`
- **scope**: `unit`, `integration`, `restdocs`, or `all` (default: `unit` + `integration`)

## Phase 0: Explore

Before writing tests, understand the implementation:

1. Read the service class to identify testable methods and branches
2. Check existing test infrastructure:
   - Fake/InMemory repositories for the domain
   - TestFactory for the domain
   - ObjectFixture for the domain
3. Report findings: what exists, what needs to be created

## Phase 1: Scenario Definition

Define test scenario lists based on service methods and API endpoints.
Write each scenario in `@DisplayName` format (`when ~, should ~`) and get user approval before implementation.

**Unit test scenarios** (per service method):
- Success: expected behavior with valid input
- Failure: exception conditions (not found, unauthorized, duplicate, etc.)
- Edge cases: null, empty, boundary values

**Integration test scenarios** (per API endpoint):
- Authenticated request + successful response
- Authentication failure (401)
- Business validation failure (400, 404, 409, etc.)

Example output (scenarios must be written in Korean for `@DisplayName`):
```
Unit: RatingService
- 유효한 요청이면 평점을 등록할 수 있다
- 존재하지 않는 주류에 평점을 등록하면 예외가 발생한다
- 이미 평점이 있으면 기존 평점을 갱신한다
- 평점 등록 시 이벤트가 발행된다

Integration: POST /api/v1/ratings
- 인증된 사용자가 평점을 등록할 수 있다
- 인증 없이 요청하면 401을 반환한다
- 존재하지 않는 주류 ID로 요청하면 404를 반환한다
```

Present the scenario list to the user and proceed to Phase 2 after approval.

## Phase 2: Test Infrastructure (create if missing)

### For Unit Tests

Check and create if needed:
- `InMemory{Domain}Repository` in `bottlenote-product-api/src/test/java/app/bottlenote/{domain}/fixture/`
- `{Domain}ObjectFixture` in the same fixture package

### For Integration Tests

Check and create if needed:

**Product module:**
- `{Domain}TestFactory` in `bottlenote-mono/src/test/java/app/bottlenote/{domain}/fixture/`

**Admin module:**
- `{Domain}Helper` (Kotlin object) in `bottlenote-admin-api/src/test/kotlin/app/helper/{domain}/`

## Phase 3: Test Implementation

Read `references/test-patterns.md` for code examples before writing tests.

### Unit Test (`@Tag("unit")`)

**Required for:** Service classes with business logic.

| Item | Product (Java) | Admin |
|------|---------------|-------|
| Location | `product-api/.../app/bottlenote/{domain}/service/` | N/A (business logic is in mono) |
| Pattern | Fake/Stub (Mock is last resort, ask user first) | - |
| Naming | `Fake{Domain}ServiceTest` | - |

Structure:
- `@BeforeEach`: wire SUT with InMemory repos + Fake facades
- `@Nested` + `@DisplayName`: group by method/scenario
- Given-When-Then in each test

### Integration Test (`@Tag("integration")` or `@Tag("admin_integration")`)

**Required for:** All API endpoints.

| Item | Product (Java) | Admin (Kotlin) |
|------|---------------|----------------|
| Location | `product-api/.../app/bottlenote/{domain}/integration/` | `admin-api/.../app/integration/{domain}/` |
| Base class | `IntegrationTestSupport` | `IntegrationTestSupport` |
| Tag | `@Tag("integration")` | `@Tag("admin_integration")` |
| API client | `mockMvcTester` | `mockMvcTester` |
| Auth | `getToken()` / `getToken(user)` | `getAccessToken(admin)` |
| Data setup | `{Domain}TestFactory` (`@Autowired`) | `{Domain}TestFactory` (`@Autowired`) |

Key patterns:
- `@Nested` per API endpoint
- Auth success + failure cases for each endpoint
- `extractData(result, ResponseType.class)` for response parsing
- `Awaitility` for async event verification

### RestDocs Test (optional, user request only)

| Item | Product (Java) | Admin (Kotlin) |
|------|---------------|----------------|
| Location | `product-api/.../app/docs/{domain}/` | `admin-api/.../app/docs/{domain}/` |
| Base class | `AbstractRestDocs` | `@WebMvcTest(excludeAutoConfiguration = [SecurityAutoConfiguration::class])` |
| Naming | `Rest{Domain}ControllerDocsTest` | `Admin{Domain}ControllerDocsTest` |
| Mocking | `@MockBean` services (acceptable here) | `@MockitoBean` services |

## Phase 4: Verify

After test implementation, run verification:

| Scope | Command |
|-------|---------|
| Unit tests only | `/verify standard` (compile + unit + build) |
| With integration | `/verify full` (includes integration tests) |

## Test Naming Convention

- Class: `Fake{Feature}ServiceTest`, `{Feature}IntegrationTest`, `Rest{Domain}ControllerDocsTest`
- Method: `{action}_{scenario}_{expectedResult}` or Korean `@DisplayName`
- DisplayName: always in Korean, format `~할 때 ~한다`, `~하면 ~할 수 있다`

## Important Rules

- **Mock is last resort**: always prefer Fake/InMemory. Ask user before using Mockito.
- **RestDocs is optional**: only implement when user explicitly requests.
- **One test, one scenario**: each `@Test` verifies a single behavior.
- **Repository interface changes**: if you added methods to a domain repository, update the corresponding `InMemory{Domain}Repository` too.
