---
name: debug
description: |
  Systematic root-cause debugging for build failures, test failures, and runtime errors.
  Trigger: "/debug", or when the user says "에러 났어", "테스트 실패", "빌드 안 돼", "왜 안 되지", "debug this", "this is broken".
  Follows a structured 6-step process: STOP, REPRODUCE, LOCALIZE, FIX, GUARD, VERIFY.
  상태 기반: 컴파일·테스트·ArchUnit 룰 실패 출력이 있을 때, /verify가 FAIL을 보고했을 때, 이전에 되던 것이 안 될 때.
  Use when anything unexpected happens — do not guess at fixes.
argument-hint: "[error description or test name]"
---

# Debugging and Error Recovery

## Overview

When something breaks, stop adding features, preserve evidence, and follow a structured process to find and fix the root cause. Guessing wastes time. This skill works for build errors, test failures, runtime bugs, and unexpected behavior across any language or stack.

## When to Use

- Build / compile / type-check fails
- Tests fail (unit, integration, architecture rule, lint)
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
├── YES → Proceed to Step 3
└── NO
    ├── Check environment differences (containers running? submodules / deps initialized?)
    ├── Run in isolation (single test, clean build)
    └── If truly non-reproducible, document conditions and monitor
```

For exact reproduction commands, see `verify/references/verify/java-gradle.md`.

### Step 3: LOCALIZE

Narrow down WHERE the failure happens. Triage by failure category:

```
Build / type-check failure:
├── Compile error → check the affected module's source
├── Format / lint error → run the project's auto-fix command (see verify references)
└── Dependency resolution → check the project's lockfile / version manifest

Test failure:
├── Unit test
│   ├── Test double (Fake/InMemory) out of sync with the interface it implements?
│   ├── Service logic changed but test not updated?
│   └── New dependency not wired in test setup?
├── Integration test
│   ├── Required infrastructure running? (DB, queue, cache — testcontainers / docker / etc.)
│   ├── Schema / migration up to date?
│   ├── Test data setup missing or stale?
│   └── Auth / token setup correct?
└── Architecture / lint rule
    ├── Boundary violation (cross-module direct access)?
    ├── New code in wrong package / layer?
    └── Cyclic dependency introduced?
```

**For stack traces:** read bottom-up, find the first line in your own code (not framework / vendor).

For project-specific triage trees, consult `implement/references/languages/java-spring.md` and `bottlenote-patterns.md`.

### Step 4: FIX

Fix the ROOT CAUSE, not the symptom.

```
Symptom: "Test expects 3 items but gets 2"

Symptom fix (bad):
  → Change assertion to expect 2

Root cause fix (good):
  → The query has a WHERE clause that filters out soft-deleted items
  → Fix the test data setup to not include soft-deleted items
```

Rules:
- One change at a time — compile / type-check after each change
- If a fix requires more than 5 files, reconsider whether the diagnosis is correct
- Do NOT suppress errors (disabled annotations, empty catch blocks, lint-disable comments)
- Do NOT delete or skip failing tests

### Step 5: GUARD

Write a regression test that would have caught this bug.

- Use a descriptive test display name in the project's convention
- The test should FAIL without the fix and PASS with it
- If the fix changed an interface, **update the corresponding test doubles** (InMemory/Fake implementations) — this is the most common source of cascading breakage

### Step 6: VERIFY

Run verification to confirm the fix and check for regressions. See `/verify` for level selection:

| Original failure | Minimum verification |
|------------------|----------------------|
| Compile / type-check | `/verify quick` |
| Unit test | `/verify standard` |
| Integration test | `/verify full` |
| Architecture rule | `/verify standard` (rule tests usually run in L1/L2) |
| Unknown / broad | `/verify full` |

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
- Suppressing errors instead of fixing root cause
- Changing test assertions to match wrong behavior
- "It works now" without understanding what changed
- No regression test added after a bug fix

## Verification

After fixing a bug:

- [ ] Root cause identified and understood (not just symptom)
- [ ] Fix addresses the root cause specifically
- [ ] Regression test exists that fails without the fix
- [ ] All existing tests pass
- [ ] Build / type-check succeeds
- [ ] Test doubles updated if interfaces changed
- [ ] Original failure scenario verified end-to-end

## 종료

지침의 **GSL Execution Mode** 규칙을 따른다. 근본 원인·수정 내용·회귀 테스트를 보고한다. step-by-step이면 턴을 끝내고, delegated면 중단됐던 단계로 복귀한다 (단, 가정 붕괴가 원인이었다면 재개봉 프로토콜이 우선한다).
