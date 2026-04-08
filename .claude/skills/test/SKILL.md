---
name: test
description: |
  Test implementation guide for bottle-note-api-server (product-api & admin-api).
  Trigger: "/test", or when the user says "테스트 작성", "테스트 구현", "테스트 추가", "write tests", "implement tests".
  Guides through unit test (Fake/Stub), integration test, and RestDocs test creation.
  Supports both product-api (Java) and admin-api (Kotlin) modules.
argument-hint: "[domain] [product|admin] [unit|integration|restdocs|all]"
---

# Test Implementation

References:
- `references/test-infra.md` — shared test utilities, TestContainers, existing Fake/InMemory list
- `references/test-patterns.md` — unit, integration, RestDocs code patterns

## Overview

Write tests that prove code works. This skill guides you through creating unit tests (Fake/Stub pattern), integration tests (TestContainers), and optionally RestDocs tests. Tests are proof — "seems right" is not done.

## When to Use

- After `/implement` completes, to add integration tests
- When adding test coverage to existing untested code
- When the user explicitly requests test creation
- During `/implement` Task cycle, for writing unit tests alongside implementation

## When NOT to Use

- Running existing tests (use `/verify`)
- Debugging test failures (use `/debug`)
- Requirements unclear (use `/define` first)

## Relationship with `/implement`

- **Unit tests**: ideally written together with implementation during a Task in `/implement`
- **Integration tests**: written after all Tasks are implemented, via separate `/test` invocation
- **RestDocs tests**: only when user explicitly requests API documentation
- Slice-level compile checks in `/implement` run existing tests — this skill WRITES new tests

## Test Types and Timing

| Test Type | Tag | When to Write | When to Run | Command |
|-----------|-----|---------------|-------------|---------|
| **Unit test** | `@Tag("unit")` | With `/implement` Task | Task commit | `./gradlew unit_test` |
| **Architecture rules** | `@Tag("rule")` | Already exists (ArchUnit) | Task commit | `./gradlew check_rule_test` |
| **Integration test** | `@Tag("integration")` | After feature complete | `/verify full` | `./gradlew integration_test` |
| **Admin integration** | `@Tag("admin_integration")` | After feature complete | `/verify full` | `./gradlew admin_integration_test` |
| **RestDocs** | (none) | User request only | Documentation build | `./gradlew restDocsTest` |

## Test Pattern Selection

```
New test needed:
├── Service logic?
│   ├── Fake/InMemory exists for this domain?
│   │   ├── Yes -> Use Fake pattern (reuse existing InMemory)
│   │   └── No -> Create InMemory implementation first, then Fake pattern
│   └── Mockito is LAST RESORT (ask user before using)
├── API endpoint?
│   ├── product -> IntegrationTestSupport + mockMvcTester (Java)
│   └── admin -> IntegrationTestSupport + mockMvcTester (Kotlin)
└── API documentation?
    └── RestDocs (only when user explicitly requests)
```

## Argument Parsing

Parse `$ARGUMENTS` to determine:
- **domain**: target domain (e.g., `alcohols`, `rating`, `review`)
- **module**: `product` (default) or `admin`
- **scope**: `unit`, `integration`, `restdocs`, or `all` (default: `unit` + `integration`)

## Process

### Phase 0: Explore

Before writing tests, understand the implementation:

1. Read the service class to identify testable methods and branches
2. Check existing test infrastructure:
   - Fake/InMemory repositories for the domain (see `references/test-infra.md`)
   - TestFactory for the domain
   - ObjectFixture for the domain
3. Report findings: what exists, what needs to be created

### Phase 1: Scenario Definition

Define test scenario lists based on service methods and API endpoints.
Write each scenario in `@DisplayName` format (Korean: `~할 때 ~한다`) and get user approval before implementation.

**Unit test scenarios** (per service method):
- Success: expected behavior with valid input
- Failure: exception conditions (not found, unauthorized, duplicate, etc.)
- Edge cases: null, empty, boundary values

**Integration test scenarios** (per API endpoint):
- Authenticated request + successful response
- Authentication failure (401)
- Business validation failure (400, 404, 409, etc.)

Example:
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

### Phase 2: Test Infrastructure (create if missing)

**For Unit Tests:**
- `InMemory{Domain}Repository` in fixture package
- `{Domain}ObjectFixture` for pre-configured domain objects

**For Integration Tests (product):**
- `{Domain}TestFactory` in `bottlenote-mono/src/test/java/app/bottlenote/{domain}/fixture/`

**For Integration Tests (admin):**
- `{Domain}Helper` (Kotlin object) in `bottlenote-admin-api/src/test/kotlin/app/helper/{domain}/`

### Phase 3: Test Implementation

Read `references/test-patterns.md` for code examples before writing tests.

**Unit Test** (`@Tag("unit")`):

| Item | Product (Java) | Admin |
|------|---------------|-------|
| Location | `product-api/.../app/bottlenote/{domain}/service/` | N/A (business logic in mono) |
| Pattern | Fake/Stub (Mock is last resort, ask user first) | - |
| Naming | `Fake{Domain}ServiceTest` | - |

Structure:
- `@BeforeEach`: wire SUT with InMemory repos + Fake facades
- `@Nested` + `@DisplayName`: group by method/scenario
- Given-When-Then in each test

**Integration Test** (`@Tag("integration")` or `@Tag("admin_integration")`):

| Item | Product (Java) | Admin (Kotlin) |
|------|---------------|----------------|
| Location | `product-api/.../app/bottlenote/{domain}/integration/` | `admin-api/.../app/integration/{domain}/` |
| Base class | `IntegrationTestSupport` | `IntegrationTestSupport` |
| Tag | `@Tag("integration")` | `@Tag("admin_integration")` |
| API client | `mockMvcTester` | `mockMvcTester` |
| Auth | `getToken()` / `getToken(user)` | `getAccessToken(admin)` |
| Data setup | `{Domain}TestFactory` (`@Autowired`) | `{Domain}TestFactory` (`@Autowired`) |

**RestDocs Test** (optional, user request only):

| Item | Product (Java) | Admin (Kotlin) |
|------|---------------|----------------|
| Location | `product-api/.../app/docs/{domain}/` | `admin-api/.../app/docs/{domain}/` |
| Base class | `AbstractRestDocs` | `@WebMvcTest(excludeAutoConfiguration = [SecurityAutoConfiguration::class])` |
| Naming | `Rest{Domain}ControllerDocsTest` | `Admin{Domain}ControllerDocsTest` |
| Mocking | `@MockBean` services (acceptable here) | `@MockitoBean` services |

### Phase 4: Verify

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

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "I'll write tests after the code works" | Tests written after the fact test implementation, not behavior. Write them alongside. |
| "Mock is faster than building a Fake" | Mock couples tests to implementation details. Fakes survive refactoring. |
| "This is too simple to test" | Simple code gets complicated. The test documents expected behavior. |
| "Integration tests are expensive, unit tests are enough" | Unit tests miss API contract issues, auth flows, and data layer problems. Both are needed. |
| "RestDocs is always needed" | RestDocs is optional. Only create when the user explicitly requests documentation. |

## Red Flags

- Using Mockito without asking the user first
- Test class missing `@Tag` annotation
- `@DisplayName` not in Korean or not describing behavior
- Testing framework behavior instead of application logic
- No `@Nested` grouping on a test class with 5+ test methods
- Integration test not extending `IntegrationTestSupport`
- Tests that pass on the first run (may not be testing what you think)
- Skipping tests to make the suite pass

## Verification

After completing test implementation:

- [ ] Every new behavior has a corresponding test
- [ ] All test scenarios from Phase 1 are implemented
- [ ] Test names describe the behavior being verified (Korean `@DisplayName`)
- [ ] No tests were skipped or disabled
- [ ] Unit tests use Fake/InMemory pattern (not Mockito, unless approved)
- [ ] Integration tests extend `IntegrationTestSupport`
- [ ] All tests pass: `./gradlew unit_test` and/or `./gradlew integration_test`
- [ ] InMemory repositories updated if domain repo interface changed
