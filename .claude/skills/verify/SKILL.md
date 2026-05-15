---
name: verify
description: |
  Local CI verification skill with 3 levels (L1 quick / L2 standard / L3 full).
  Trigger: "/verify", "/verify quick", "/verify l1", "/verify standard", "/verify l2", "/verify full", "/verify l3",
  or when the user says "검증해줘", "빌드 확인", "테스트 돌려줘", "CI 돌려봐", "run checks".
  Always use this skill when the user wants to check if their code compiles, passes tests, or is ready for PR.
  Branches on language via reference. Always runs against the ENTIRE project (modules can depend on each other).
argument-hint: "[language] [quick|standard|full] (language optional if inferable)"
---

# Verify — Local CI Pipeline

References (read the matching one for exact commands):
- `references/verify/{language}.md` — concrete commands per step (java-gradle / python / go / ...)

## Overview

Run local CI checks matching the project's pipeline. All checks run against the **entire project** because modules / packages typically share a common core — changes in one module can break others. Default level = L2 (Standard).

## Level Selection

Parse `$ARGUMENTS` to determine the verification level:

| Argument | Level | When to use |
|----------|-------|-------------|
| `quick`, `l1`, or empty with scaffolding context | L1 | Mockup, scaffolding, initial structure |
| `standard`, `l2`, empty (default) | L2 | Feature implementation complete |
| `full`, `l3` | L3 | Before push / PR — final gate |

L3 is the final gate before `git push` or PR creation. Integration tests take several minutes and typically require infrastructure containers (testcontainers / docker), so run L3 only when the feature is complete and you're ready to share the code. During active development, L1/L2 is sufficient.

## Generic Step Definitions

Concrete commands per step come from `references/verify/{language}.md`. The generic step contract:

### L1 — Quick

| Step | What it checks |
|------|----------------|
| 1 | Compile / type-check (all modules) |
| 2 | Format / lint static checks |
| 3 | Architecture / dependency rules (if the stack has them) |

### L2 — Standard (includes L1)

| Step | What it checks |
|------|----------------|
| 4 | Unit tests across all modules |
| 5 | Full build / package (artifact production) |

### L3 — Full (includes L2)

| Step | What it checks |
|------|----------------|
| 6 | Integration tests (real infra via testcontainers / docker / equivalent) |
| 7 | (Optional) End-to-end / smoke tests if the project defines them |

## Execution

Run each step sequentially. **Stop immediately on first failure** — report the error and skip remaining steps.

For each step:
1. Record the start time
2. Run the command (from `references/verify/{language}.md`)
3. Calculate elapsed time
4. Report the result immediately

### Progress (print after each step)

```
[L{level} {current}/{total}] {step name}... ({time}) {OK|FAIL}
```

On failure:
- Show the **last 30 lines** of output to help diagnose the error
- Mark all remaining steps as SKIPPED
- Proceed to the summary

### Final Summary (always print)

```
Verification Summary (L2 - Standard)
=====================================
Step                    Status    Time
Compile / type-check    OK        12s
Lint / format           OK        8s
Architecture rules      OK        15s
Unit tests              OK        45s
Build / package         OK        30s
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

Adjust per-step shell timeouts accordingly (compile / lint: short; tests: medium; integration: long).

## Important

- **Never run module-specific checks** — always full project scope. One module's change can break another.
- L3 integration tests typically require Docker / infra containers. If unavailable, report clearly and suggest falling back to L2.
- If `references/verify/{language}.md` does not exist for the project's language, ask the user to confirm the verification commands before running.

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "Let me just run quick" | If you've completed a Task, run standard. quick is for scaffolding. |
| "Skip integration tests, they're slow" | They catch bugs unit tests cannot. Run them before push. |
| "I only changed one module" | Other modules may depend on it. Always full project scope. |
| "The test failed but it's flaky" | Flaky tests mask real bugs. Use `/debug` to investigate. |

## Red Flags

- Running module-specific checks instead of full project
- Skipping L3 before push / PR
- Reporting "PASS" without showing the summary
- Continuing after a step failure (should STOP immediately)

## Verification

After running:

- [ ] All steps for the requested level completed
- [ ] Summary printed with status per step
- [ ] On failure: last 30 lines of output shown, remaining steps marked SKIPPED
- [ ] Total time reported

## Runtime Boundary — HARD STOP

This skill ENDS after the Verification checklist and final report are completed.

For codex and any runtime without an enforced skill-return boundary:
- MUST stop the assistant turn here.
- MUST NOT invoke, load, or execute any next GSL skill in the same response turn.
- MUST NOT continue into `/next-flow`, `/define`, `/plan`, `/implement`, `/test`, `/verify`, `/debug`, or `/self-review`.
- MAY print exactly one suggested next command as plain text.
- MUST wait for the user's next message before running any next skill.

If the user says only "continue", treat that as permission to report the next recommended command, not permission to execute it.

---

## Lifecycle Integration

**Before this skill:** if `plan/conventions.md` does not exist, run `/scan-conventions` first — analysis relies on knowing the project's actual conventions (naming, layering, test patterns, build system).

**After this skill:** the next GSL skill is started by the user, not by this skill — see the Runtime Boundary section above. `/next-flow` may be suggested for lifecycle diagnosis but is not auto-invoked. Runtime note: some environments expose slash commands as UI commands; codex loads GSL skills from `.agents/skills/`. In both cases, the next GSL skill requires a new explicit user message.
