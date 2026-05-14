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

Evaluate every change across these dimensions. Project-specific concrete checks live in the relevant references — consult `implement/references/languages/{your-language}.md` and `implement/references/types/{your-type}.md` for the exact items.

#### Correctness

Does the code do what it claims to do?

- Does it match the spec or task acceptance criteria?
- Are edge cases handled (null, empty, boundary values, error inputs)?
- Are error paths handled (not just the happy path)?
- Are domain events / side effects emitted where expected?
- Do existing tests still pass?

#### Readability

Can another engineer understand this without explanation?

- Names follow the project's naming conventions
- Test display names follow the project's convention (often the user's natural language describing behavior)
- Comments are single-line and brief — no stating the obvious
- Data structures use the language-idiomatic immutability primitives (record/dataclass/struct)
- No deep nesting (3+ levels) — use guard clauses

#### Architecture

Does the change fit the system design?

- **Module boundary**: respects the project's layering rules — consult `implement/references/types/{your-type}.md` for the specific layering (e.g., service-vs-controller thinness, repository tiers, public-vs-internal API surface)
- **Cross-module access**: goes through the documented seam (Facade, port, public function), never bypasses
- **Custom conventions**: language/framework-specific annotations or markers used correctly (`implement/references/languages/{your-language}.md`)

#### Security

Does the change introduce vulnerabilities?

- Authenticated endpoints / commands use the project's auth primitive (not ad-hoc)
- Inputs validated at the system boundary (request DTOs, CLI args)
- No raw SQL / shell string concatenation — parameterized queries / safe APIs
- Sensitive data not logged (passwords, tokens, PII)
- No secrets in source code

#### Performance

Does the change introduce performance problems?

- No N+1 query patterns (use fetch joins / dataloader / batched APIs)
- List endpoints / queries have pagination
- Read-only operations marked as such (transaction modifiers, etc.) where the language supports it
- Caching considered for frequently accessed, rarely changed data
- No unbounded queries / collections (always limit)

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
[Critical] (Architecture) service.py:42 — Directly imports another module's repository instead of going through its facade
[Important] (Correctness) handler.go:28 — Missing nil check for optional userId
[Nit] (Readability) service.java:15 — Variable name 'r' should be descriptive
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
- **All findings are Nit** — you are not looking hard enough
- No findings at all on a multi-file change (review more carefully)
- Ignoring Critical findings to meet a deadline
- Reviewing without understanding the feature's intent or acceptance criteria
- Module boundaries violated (cross-module direct access)
- Test doubles (InMemory/Fake) not updated when an interface they implement changed

## Verification

After completing the review:

- [ ] All Critical issues are resolved
- [ ] All Important issues are resolved or explicitly deferred with justification
- [ ] Module boundaries respected
- [ ] Security checks in place at boundaries
- [ ] No new N+1 / unbounded patterns introduced
- [ ] Code compiles / type-checks (see `/verify` quick)
- [ ] Unit tests pass (see `/verify` standard)

---

## Lifecycle Integration

**Before this skill:** if `plan/conventions.md` does not exist, run `/scan-conventions` first — analysis relies on knowing the project's actual conventions (naming, layering, test patterns, build system).

**After this skill:** invoke `/next-flow` to diagnose lifecycle state and propose the next command. `/next-flow` auto-progresses read-only verification only and never writes files. Note: `/plan` is a Claude Code UI command and cannot be auto-invoked — the user must type it themselves; `/next-flow` will print a notice in that case.
