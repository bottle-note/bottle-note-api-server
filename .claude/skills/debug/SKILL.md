---
name: debug
description: |
  Systematic root-cause debugging for build failures, test failures, and runtime errors.
  Trigger: "/debug", or when the user says "에러 났어", "테스트 실패", "빌드 안 돼", "왜 안 되지", "debug this".
  Follows a structured 6-step process: STOP, REPRODUCE, LOCALIZE, FIX, GUARD, VERIFY.
  Use when anything unexpected happens — do not guess at fixes.
argument-hint: "[error description or test name]"
---

# Debugging and Error Recovery

## Overview

When something breaks, stop adding features, preserve evidence, and follow a structured process to find and fix the root cause. Guessing wastes time. This skill works for build errors, test failures, runtime bugs, and unexpected behavior in the bottle-note-api-server project.

## When to Use

- Build fails (`compileJava`, `compileKotlin`, `spotlessApply`)
- Tests fail (`unit_test`, `integration_test`, `check_rule_test`, `admin_integration_test`)
- Runtime behavior does not match expectations
- An error appears in logs or console
- Something worked before and stopped working

## When NOT to Use

- Implementing new features (use `/implement`)
- Writing new tests (use `/test`)
- Code cleanup or refactoring (use `/self-review` for review, `/implement` for changes)

## Process

### Step 1: STOP

Stop all other changes immediately.

- Do NOT push past a failing test to work on the next feature
- Preserve the error output — copy the full message before doing anything
- If you have uncommitted work in progress, stash it: `git stash`
- Read the COMPLETE error message before forming any hypothesis

### Step 2: REPRODUCE

Make the failure happen reliably. If you cannot reproduce it, you cannot fix it with confidence.

```
Can you reproduce the failure?
├── YES -> Proceed to Step 3
└── NO
    ├── Check environment differences (Docker running? submodule initialized?)
    ├── Run in isolation (single test, clean build)
    └── If truly non-reproducible, document conditions and monitor
```

**Common reproduction commands:**
```bash
# Specific test
./gradlew :bottlenote-product-api:test --tests "app.bottlenote.{domain}.{TestClass}.{testMethod}"

# Test by tag
./gradlew unit_test
./gradlew integration_test
./gradlew admin_integration_test
./gradlew check_rule_test

# Full clean build
./gradlew clean build -x test -x asciidoctor

# Compile only
./gradlew compileJava compileTestJava
./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin
```

### Step 3: LOCALIZE

Narrow down WHERE the failure happens. Use the project-specific triage tree:

```
Build failure:
├── Java compile error
│   ├── In bottlenote-mono -> check domain/service/repository code
│   ├── In bottlenote-product-api -> check controller code
│   └── In test source -> check test fixtures, InMemory implementations
├── Kotlin compile error
│   └── In bottlenote-admin-api -> check Kotlin controller/test code
├── Spotless format error
│   └── Run: ./gradlew spotlessApply (auto-fixes formatting)
└── Dependency resolution error
    └── Check gradle/libs.versions.toml for version conflicts

Test failure:
├── @Tag("unit")
│   ├── Fake/InMemory implementation out of sync with domain repo interface?
│   ├── Service logic changed but test not updated?
│   └── New dependency not wired in @BeforeEach setup?
├── @Tag("integration")
│   ├── Docker running? (TestContainers requires Docker)
│   ├── Database schema changed? (check Liquibase changelogs)
│   ├── Test data setup missing? (check TestFactory)
│   └── Auth token issue? (check TestAuthenticationSupport)
├── @Tag("rule")
│   ├── Package dependency violation? (check ArchUnit rules)
│   ├── New class in wrong package?
│   └── Circular dependency introduced?
└── @Tag("admin_integration")
    ├── Admin auth setup correct?
    ├── context-path /admin/api/v1 accounted for in test?
    └── Kotlin-Java interop issue?
```

**For stack traces:** read bottom-up, find the first line referencing `app.bottlenote.*`.

### Step 4: FIX

Fix the ROOT CAUSE, not the symptom.

```
Symptom: "Test expects 3 items but gets 2"

Symptom fix (bad):
  -> Change assertion to expect 2

Root cause fix (good):
  -> The query has a WHERE clause that filters out soft-deleted items
  -> Fix the test data setup to not include soft-deleted items
```

Rules:
- One change at a time — compile after each change
- If fix requires more than 5 files, reconsider whether the diagnosis is correct
- Do NOT suppress errors (`@Disabled`, empty catch blocks, `@SuppressWarnings`)
- Do NOT delete or skip failing tests

### Step 5: GUARD

Write a regression test that would have caught this bug.

- Use `@DisplayName` in Korean describing the bug scenario
- The test should FAIL without the fix and PASS with it
- If the fix changed a domain Repository interface, update the corresponding `InMemory{Domain}Repository`
- If the fix changed a Facade interface, update the corresponding `Fake{Domain}Facade`

### Step 6: VERIFY

Run verification to confirm the fix and check for regressions.

| Original failure | Minimum verification |
|-----------------|---------------------|
| Compile error | `./gradlew compileJava compileTestJava` |
| Unit test | `./gradlew unit_test` |
| Integration test | `./gradlew integration_test` (requires Docker) |
| Architecture rule | `./gradlew check_rule_test` |
| Admin test | `./gradlew admin_integration_test` |
| Unknown/broad | `/verify standard` or `/verify full` |

## Quick Reference: Diagnostic Commands

| Situation | Command |
|-----------|---------|
| What changed recently | `git log --oneline -10` |
| What files are modified | `git status` |
| Diff of uncommitted changes | `git diff` |
| Find which commit broke it | `git bisect start && git bisect bad HEAD && git bisect good <sha>` |
| Check Java compile | `./gradlew compileJava compileTestJava` |
| Check Kotlin compile | `./gradlew :bottlenote-admin-api:compileKotlin` |
| Auto-fix formatting | `./gradlew spotlessApply` |
| Run single test class | `./gradlew test --tests "app.bottlenote.{domain}.{TestClass}"` |
| Check dependency versions | `cat gradle/libs.versions.toml` |
| Check Docker status | `docker info` |

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "I know what the bug is, I'll just fix it" | You might be right 70% of the time. The other 30% costs hours. Reproduce first. |
| "The failing test is probably wrong" | Verify that assumption. If the test is wrong, fix the test. Do not skip it. |
| "Let me just revert and redo everything" | Reverting destroys diagnostic information. Understand WHAT broke before reverting. |
| "This is a flaky test, ignore it" | Flaky tests mask real bugs. Fix the flakiness or understand why it is intermittent. |
| "I'll fix it in the next commit" | Fix it now. The next commit will introduce new issues on top of this one. |

## Red Flags

- Changing more than 5 files to fix a "simple" bug (diagnosis is likely wrong)
- Fixing without reproducing first
- Multiple stacked fixes without verifying between each one
- Suppressing errors instead of fixing root cause (`@Disabled`, empty catch, lint-disable)
- Changing test assertions to match wrong behavior
- "It works now" without understanding what changed
- No regression test added after a bug fix

## Verification

After fixing a bug:

- [ ] Root cause identified and understood (not just symptom)
- [ ] Fix addresses the root cause specifically
- [ ] Regression test exists that fails without the fix
- [ ] All existing tests pass
- [ ] Build succeeds
- [ ] InMemory/Fake implementations updated if interfaces changed
- [ ] Original failure scenario verified end-to-end
