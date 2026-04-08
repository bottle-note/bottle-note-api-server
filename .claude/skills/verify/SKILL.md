---
name: verify
description: |
  Local CI verification skill with 3 levels based on implementation stage.
  Trigger: "/verify", "/verify quick", "/verify l1", "/verify standard", "/verify l2", "/verify full", "/verify l3",
  or when the user says "검증해줘", "빌드 확인", "테스트 돌려줘", "CI 돌려봐".
  Always use this skill when the user wants to check if their code compiles, passes tests, or is ready for PR.
  This runs checks against the ENTIRE project because all modules share the mono module.
argument-hint: "[quick|standard|full] or [l1|l2|l3]"
---

# Verify - Local CI Pipeline

Run local CI checks matching the project's GitHub Actions pipeline (`.github/workflows/ci_pipeline.yml`).
All checks run against the **entire project** because product-api, admin-api, and batch all depend on mono — changes in one module can break others.

## Level Selection

Parse `$ARGUMENTS` to determine the verification level:

| Argument | Level | When to use |
|----------|-------|-------------|
| `quick`, `l1`, or empty with scaffolding context | L1 | Mockup, scaffolding, initial structure |
| `standard`, `l2`, empty (default) | L2 | Feature implementation complete |
| `full`, `l3` | L3 | Push/PR 직전 최종 검증 |

If no argument is given, default to **L2 (Standard)**.

L3 is the final gate before `git push` or PR creation. Integration tests take several minutes and use TestContainers (Docker required), so run L3 only when the feature is complete and you're ready to share the code. During active development, L1/L2 is sufficient.

## Execution Steps

Run each step sequentially. **Stop immediately on first failure** — report the error and skip remaining steps.

### L1 - Quick

| Step | Command | What it checks |
|------|---------|----------------|
| 1 | `./gradlew compileJava compileTestJava` | Java compilation (all modules) |
| 2 | `./gradlew :bottlenote-admin-api:compileKotlin :bottlenote-admin-api:compileTestKotlin` | Kotlin compilation (admin-api) |
| 3 | `./gradlew check_rule_test` | Architecture rules (ArchUnit) |

### L2 - Standard (includes L1)

| Step | Command | What it checks |
|------|---------|----------------|
| 4 | `./gradlew unit_test` | Unit tests across all modules |
| 5 | `./gradlew build -x test -x asciidoctor --build-cache --parallel` | Full build (JAR packaging) |

### L3 - Full (includes L2)

| Step | Command | What it checks |
|------|---------|----------------|
| 6 | `./gradlew integration_test` | Product integration tests (TestContainers) |
| 7 | `./gradlew admin_integration_test` | Admin integration tests (TestContainers) |

## How to Run Each Step

For each step, use the Bash tool:

1. Record the start time
2. Run the Gradle command
3. Calculate elapsed time
4. Report the result immediately:

```
[L1 1/3] Compiling Java... (12s) OK
```

Or on failure:
```
[L1 2/3] Compiling Kotlin... (8s) FAIL
```

When a step fails:
- Show the **last 30 lines** of output to help diagnose the error
- Mark all remaining steps as SKIPPED
- Proceed to the summary

## Output Format

### Progress (print after each step)

```
[L{level} {current}/{total}] {step name}... ({time}) {OK|FAIL}
```

### Final Summary (always print)

```
Verification Summary (L2 - Standard)
=====================================
Step                    Status    Time
Compile Java            OK        12s
Compile Kotlin          OK        8s
Rule tests              OK        15s
Unit tests              OK        45s
Build                   OK        30s
-------------------------------------
Total                   PASS      1m50s
```

Use `PASS` if all steps succeeded, `FAIL` if any step failed.

## Timeouts

| Level | Steps | Expected max time |
|-------|-------|-------------------|
| L1 | 3 steps | ~2 min |
| L2 | 5 steps | ~5 min |
| L3 | 7 steps | ~15 min |

Set Bash timeout accordingly:
- Compile steps: 120000ms
- Unit/rule tests: 300000ms
- Integration tests: 600000ms
- Build: 300000ms

## Important

- Never run module-specific checks — always full project scope
- L3 integration tests require Docker (TestContainers)
- If Docker is not available for L3, report it clearly and suggest falling back to L2
