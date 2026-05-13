---
name: self-review
description: |
  Pre-commit quality gate with 5-axis code review.
  Trigger: "/self-review", or when the user says "리뷰해줘", "review this", "코드 리뷰", "self review".
  Use before every commit, after completing a Task in /implement, or when the user wants to review changes.
  Evaluates code across correctness, readability, architecture, security, and performance.
argument-hint: "[files or scope]"
---

# Self-Review

## Overview

Review your own changes before committing. This is a pre-commit quality gate that catches issues before they become technical debt. Every Task completion in `/implement` should invoke this skill before committing.

The goal is not perfection — it is continuous improvement. Approve a change when it clearly improves overall code health, even if it is not exactly how a staff engineer would have written it.

## When to Use

- Before every commit (mandatory in `/implement` workflow)
- After completing a Task in `/implement`
- When refactoring existing code
- When the user explicitly asks for a review

## When NOT to Use

- Debugging failures (use `/debug`)
- Writing new code (use `/implement`)
- Running tests (use `/verify`)
- Reviewing code that has not been written yet

## Process

### Step 1: Identify Scope

Determine which files to review.

- If called from `/implement`: review the current Task's changed files
- If standalone: use `git diff --staged` or `git diff` to identify changes
- List every changed file with a one-line summary of the change

### Step 2: Five-Axis Review

Evaluate every change across these dimensions. For each axis, check the project-specific items listed below.

#### Correctness

Does the code do what it claims to do?

- Does it match the spec or task acceptance criteria?
- Are edge cases handled (null, empty, boundary values)?
- Are error paths handled (not just the happy path)?
- Are domain events published where expected?
- Do existing tests still pass?

#### Readability

Can another engineer understand this without explanation?

- Names follow project conventions: `{Domain}Controller`, `Default{Domain}Facade`, `Jpa{Domain}Repository`
- `@DisplayName` uses Korean in format: `~할 때 ~한다`
- Comments are single-line and brief (no stating the obvious)
- DTOs use `record` for immutability
- No deep nesting (3+ levels) — use guard clauses

#### Architecture

Does the change fit the system design?

- **Facade boundary**: cross-domain access goes through Facade, never direct Repository/Service
- **Repository 3-Tier**: Domain repo (pure interface) → JPA repo (implementation) → QueryDSL (complex queries only)
- **Controller thinness**: controllers delegate to services/facades, no business logic
- **Module boundary**: business logic in `bottlenote-mono`, controllers in API modules
- **Custom annotations**: `@FacadeService`, `@DomainRepository`, `@JpaRepositoryImpl` used correctly

#### Security

Does the change introduce vulnerabilities?

- Authenticated endpoints use `SecurityContextUtil.getUserIdByContext()` (product) or `SecurityContextUtil.getAdminUserIdByContext()` (admin)
- Request DTOs use `@Valid` with Bean Validation annotations
- No raw SQL string concatenation (use parameterized queries or QueryDSL)
- Sensitive data not logged (passwords, tokens)
- No secrets in source code

#### Performance

Does the change introduce performance problems?

- No N+1 query patterns (use fetch joins, `@BatchSize`)
- List endpoints have pagination (`CursorPageable` for product, offset for admin)
- Read-only operations use `@Transactional(readOnly = true)`
- `@Cacheable` considered for frequently accessed, rarely changed data
- No unbounded queries (always limit results)

### Step 3: Report Findings

Categorize every finding with a severity label:

| Severity | Meaning | Action |
|----------|---------|--------|
| **Critical** | Blocks commit — security vulnerability, data loss, broken functionality | Must fix before commit |
| **Important** | Should fix — missing test, wrong abstraction, poor error handling | Fix or explicitly justify deferral |
| **Nit** | Optional — naming, style, minor optimization | Fix if easy, otherwise ignore |

Format each finding as:
```
[Severity] (Axis) file:line — description
```

Example:
```
[Critical] (Architecture) RatingService.java:42 — Directly injects AlcoholRepository instead of AlcoholFacade
[Important] (Correctness) RatingController.java:28 — Missing null check for optional userId
[Nit] (Readability) RatingService.java:15 — Variable name 'r' should be descriptive
```

### Step 4: Resolve or Recommend

- **Critical**: stop and fix immediately. Do not proceed to commit.
- **Important**: propose a fix, apply if straightforward, ask user if the fix changes behavior.
- **Nit**: fix silently if it is a one-line change. Otherwise, note it and move on.

After all Critical and Important issues are resolved, proceed to commit.

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "It's just a small change, no need to review" | Small changes introduce small bugs that compound. Review everything. |
| "The tests pass, so it must be correct" | Tests check behavior, not architecture, security, or readability. All five axes matter. |
| "I'll clean it up in the next commit" | Deferred cleanup rarely happens. Fix it now or file a task. |
| "This is internal code, security doesn't matter" | Internal code gets compromised too. Validate at boundaries always. |
| "The review slows me down" | A 2-minute review prevents a 2-hour debugging session tomorrow. |

## Red Flags

- Skipping review for "trivial" changes
- All findings are Nit (you are not looking hard enough)
- No findings at all on a multi-file change (review more carefully)
- Ignoring Critical findings to meet a deadline
- Reviewing without understanding the feature's intent or acceptance criteria
- Facade boundaries violated (cross-domain direct access)
- Repository interface changed but InMemory implementation not updated

## Verification

After completing the review:

- [ ] All Critical issues are resolved
- [ ] All Important issues are resolved or explicitly deferred with justification
- [ ] Architecture rules respected (Facade boundaries, Repository 3-Tier)
- [ ] Security checks in place for authenticated endpoints
- [ ] No new N+1 patterns introduced
- [ ] Code compiles: `./gradlew compileJava compileTestJava`
- [ ] Unit tests pass: `./gradlew unit_test`
- [ ] Architecture rules pass: `./gradlew check_rule_test`
