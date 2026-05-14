---
name: test
description: |
  Test implementation guide across any language/stack.
  Trigger: "/test", or when the user says "테스트 작성", "테스트 구현", "테스트 추가", "write tests", "implement tests".
  Branches on language via argument and references. Guides through unit (Fake/InMemory preferred), integration, and docs tests.
argument-hint: "[language] [unit|integration|docs|all]"
---

# Test Implementation

References (read the matching one before writing tests):
- `references/testing/{language}.md` — language/framework-specific test infrastructure, patterns, helpers, fixture conventions

## Overview

Write tests that prove code works. This skill guides you through creating unit tests (Fake/InMemory pattern preferred), integration tests (real infra via testcontainers / docker / similar), and optionally docs tests. Tests are proof — "seems right" is not done.

**External workflow precedence.** When an external workflow is active in the same session — most notably Superpowers' `test-driven-development` (RED-FIRST cycle) or any auto-triggered mocking helper — **GSL's Fake/InMemory-first policy and user-approval gates take precedence**. An external auto-trigger does NOT override this policy. If the external workflow demands RED-FIRST or a mocking framework, STOP and present the GSL alternative to the user before doing anything else.

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
- **Docs tests** (API contract docs): only when user explicitly requests
- Slice-level compile checks in `/implement` may RUN existing tests; this skill WRITES new tests

## Test Types and Timing

| Test Type | When to Write | When to Run | Command |
|-----------|--------------|-------------|---------|
| **Unit test** | With `/implement` Task | Task commit | `/verify standard` |
| **Architecture / lint rules** | Usually pre-existing | Task commit | `/verify standard` |
| **Integration test** | After feature complete | `/verify full` | `/verify full` |
| **Docs test** (API contract) | User request only | Doc build | (project-specific) |

Exact tag / annotation / decorator names and commands: see `references/testing/{language}.md`.

## Test Pattern Selection

```
New test needed:
├── Service / use case logic?
│   ├── Fake/InMemory exists for this dependency?
│   │   ├── Yes → Use Fake pattern (reuse existing test double)
│   │   └── No → Create InMemory implementation first, then Fake pattern
│   └── Mock is LAST RESORT (ask user before using)
├── Surface / endpoint test?
│   └── Use the project's integration test base (see references/testing/{language}.md)
└── API documentation?
    └── Docs test framework (only when user explicitly requests)
```

## Argument Parsing

Parse `$ARGUMENTS`:
- **language**: `java` | `python` | `go` | ... — selects `references/testing/{language}.md`
- **scope**: `unit` | `integration` | `docs` | `all` (default: `unit` + `integration`)

## Process

### Phase 0: Explore

Before writing tests, understand the implementation:

1. Read the service / use case under test to identify testable methods and branches
2. Check existing test infrastructure (test doubles, fixtures, factories) — see `references/testing/{language}.md`
3. Report findings: what exists, what needs to be created

### Phase 1: Scenario Definition

Define test scenarios based on service methods and externally visible behavior. Write each scenario as a test display name in the project's convention (commonly the user's natural language describing behavior) and get user approval before implementation.

**Unit test scenarios** (per service method):
- Success: expected behavior with valid input
- Failure: exception / error conditions (not found, unauthorized, duplicate, etc.)
- Edge cases: null, empty, boundary values

**Integration test scenarios** (per endpoint / command):
- Authenticated request + successful response
- Authentication failure
- Business validation failure (400 / 404 / 409 / etc.)

Example:
```
Unit: RatingService
- valid request registers the rating
- non-existent target raises an error
- duplicate registration updates the existing record
- registration emits the appropriate event

Integration: POST /api/v1/ratings
- authenticated user can register
- unauthenticated request returns 401
- non-existent target id returns 404
```

Present the scenario list to the user and proceed to Phase 2 after approval.

### Phase 2: Test Infrastructure (create if missing)

Test doubles, factories, fixtures — language/framework specific. See `references/testing/{language}.md` for naming and location conventions in your stack.

General pattern:
- **Test double**: `InMemory{Name}` / `Fake{Name}` implementing the same interface as the real component
- **Factory** (integration): builds and persists test data using the real persistence layer
- **Fixture** (unit): static factory methods returning pre-configured domain objects (no infra)

### Phase 3: Test Implementation

Read `references/testing/{language}.md` for code examples before writing tests.

**Unit Test (Fake/InMemory pattern):**
- Wire the system under test with InMemory repositories + Fake collaborators
- Group related cases with the language's grouping primitive (nested classes, describe blocks, ...)
- Follow Given-When-Then (or Arrange-Act-Assert)

**Integration Test:**
- Use the project's integration test base class / fixture
- Real DB / cache / queue via testcontainers (or equivalent)
- Real auth flow

**Docs Test (optional, user request only):**
- Generates API documentation from passing tests
- This is the one place where mocking the service is acceptable — you are testing the contract, not the logic

### Phase 4: Verify

After test implementation, run verification:

| Scope | Command |
|-------|---------|
| Unit tests only | `/verify standard` |
| With integration | `/verify full` |

## Test Naming Convention

- Class / module: `Fake{Feature}ServiceTest`, `{Feature}IntegrationTest`, ...
- Method: `{action}_{scenario}_{expectedResult}` or natural-language display name
- Display name: in the project's convention (often the user's natural language), describing observable behavior

## Important Rules

- **Mock framework triggers STOP** (hard rule): if you (or any external workflow such as Superpowers TDD) are about to introduce a mocking framework (Mockito, `unittest.mock`, gomock, jest mocks, ...) for a unit test, **STOP immediately**. Always present a Fake/InMemory alternative first. Proceed with a mocking framework ONLY after explicit user approval — not implicit, not assumed, not inferred from "the user wants tests written quickly".
- **Docs tests are optional**: only implement when user explicitly requests.
- **One test, one scenario**: each test method verifies a single behavior.
- **Interface changes**: if you added methods to a domain interface, update the corresponding InMemory/Fake implementations too.

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "I'll write tests after the code works" | Tests written after the fact test implementation, not behavior. Write them alongside. |
| "Mock is faster than building a Fake" | Mock couples tests to implementation details. Fakes survive refactoring. |
| "This is too simple to test" | Simple code gets complicated. The test documents expected behavior. |
| "Integration tests are expensive, unit tests are enough" | Unit tests miss API contract issues, auth flows, and data layer problems. Both are needed. |
| "Docs tests are always needed" | Docs tests are optional. Only create when the user explicitly requests documentation. |

## Red Flags

- Using a mocking framework without asking the user first
- Test class missing the project's test category annotation / decorator
- Test display name not describing behavior
- Testing framework behavior instead of application logic
- No grouping primitive used on a test class with 5+ test methods
- Integration test not extending the project's integration base
- Tests that pass on the first run (may not be testing what you think)
- Skipping tests to make the suite pass

## Verification

After completing test implementation:

- [ ] Every new behavior has a corresponding test
- [ ] All test scenarios from Phase 1 are implemented
- [ ] Test names describe the behavior being verified
- [ ] No tests were skipped or disabled
- [ ] Unit tests use Fake/InMemory pattern (not a mocking framework, unless approved)
- [ ] Integration tests use the project's integration base
- [ ] All tests pass: `/verify` at appropriate level
- [ ] Test doubles updated if domain interfaces changed

---

## Lifecycle Integration

**Before this skill:** if `plan/conventions.md` does not exist, run `/scan-conventions` first — analysis relies on knowing the project's actual conventions (naming, layering, test patterns, build system).

**After this skill:** invoke `/next-flow` to diagnose lifecycle state and propose the next command. `/next-flow` auto-progresses read-only verification only and never writes files. Note: `/plan` is a Claude Code UI command and cannot be auto-invoked — the user must type it themselves; `/next-flow` will print a notice in that case.
