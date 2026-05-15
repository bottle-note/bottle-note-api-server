---
name: define
description: |
  Clarifies requirements before any code is written. Creates a plan document with assumptions, success criteria, and impact scope.
  Trigger: "/define", or when the user says "이거 구현해줘", "기능 추가", "요구사항 정리", "define requirements", "spec this".
  Use when starting a new feature, when requirements are vague, or when the scope of a change is unclear.
  Do NOT write code during this skill — the output is a plan document, not implementation.
argument-hint: "[feature description]"
---

# Define Requirements

## Overview

Write a structured specification before writing any code. The plan document is the shared source of truth — it defines what we are building, why, and how we will know it is done. Code without a spec is guessing.

This skill creates `plan/{feature-name}.md` with an Overview section. The `/plan` skill later adds Tasks to the same document; `/implement` fills the Progress Log. One feature = one document, from define through commit.

## When to Use

- Starting a new feature or significant change
- Requirements are ambiguous or incomplete
- The change touches 3+ files or multiple components
- The user gives a vague request ("이거 구현해줘", "추가해줘", "add this feature")

## When NOT to Use

- Bug fixes with clear reproduction (use `/debug`)
- Single-file changes with obvious scope
- Requirements already documented in a plan file
- Test-only work (use `/test`)

## Process

### Step 0: Confirm Project Conventions Exist (hard gate)

**Before any other step**, check that `plan/conventions.md` exists at the project root.

- If it does NOT exist → **STOP**. Tell the user to run `/scan-conventions` first. Impact analysis (Step 4) depends on knowing the project's actual conventions (naming, layering, persistence, test patterns). Do NOT proceed to Step 1.
- If it exists but is older than ~30 days → ask whether to rescan via `/scan-conventions` or use as-is.
- If it exists and is current → proceed to Step 1 and treat the artifact as authoritative for downstream analysis.

### Step 1: Parse Request

Identify what the user wants. Do NOT assume scope.

- What domain or area is involved?
- Which modules / services / surfaces are touched? (API server, CLI, batch, library, frontend, ...)
- What is the expected externally visible behavior?
- Are there related features already implemented?

If anything is unclear, ask before proceeding. Do NOT fill in ambiguous requirements silently.

### Step 2: Surface Assumptions

List every assumption explicitly. Each assumption is something that could be wrong.

```
ASSUMPTIONS:
1. This feature targets the {module} (not {other-module})
2. Authentication is required (not a public surface)
3. The {entity} already exists and does not need schema changes
4. {Pagination style} follows the project default
-> Confirm or correct these before I proceed.
```

Do NOT proceed without user confirmation on assumptions.

**Superpowers absorption (if installed).** When Superpowers is active in the session, invoke its `brainstorming` skill as a sub-block before finalizing the assumption list — use the Socratic dialogue to surface deeper hidden assumptions, then merge its output into this section. The user-approval gate above still applies; brainstorming output does NOT bypass it. If Superpowers is not installed, skip this sub-block silently and proceed with GSL alone.

### Step 3: Define Success Criteria

Each criterion must be specific and testable. Translate vague requirements into concrete conditions.

```
REQUIREMENT: "add a usage statistics feature"

SUCCESS CRITERIA:
- {GET endpoint / CLI command} returns average, count, and distribution
- Response shape includes {field 1}, {field 2}, {field 3}
- Unauthenticated callers can {access / are rejected with 401}
- Latency target: < {N} ms at {scale} items
-> Are these the right targets?
```

Reject criteria that are not testable ("make it better", "improve performance").

### Step 4: Analyze Impact Scope

Check which parts of the system are affected. The exact checklist depends on the project type — consult `implement/references/types/{your-type}.md` for type-specific impact checklists. Generally:

- **Modules / surfaces**: which ones are involved?
- **Cross-component coupling**: any new dependency between previously independent units?
- **Persistence**: schema migration needed?
- **Async / events**: new events published or consumed?
- **Caching**: invalidation policy affected?
- **Tests**: which test layers will be needed?
- **Docs / API contracts**: external consumers impacted?

### Step 5: Create Plan Document

Create `plan/{feature-name}.md`. Use the user's preferred language for plan documents (the working convention is the user's natural language; English keywords for section headers are fine):

```markdown
# Plan: [feature name]

## Overview
[what we are building and why]

### Assumptions
- [assumption 1]
- [assumption 2]

### Success Criteria
- [specific, testable criterion 1]
- [specific, testable criterion 2]

### Impact Scope
- [modules / files affected, schema, events, cache, tests]
```

This document will be extended by `/plan` (Tasks section) and `/implement` (Progress Log).

### Step 6: User Approval Gate

Present the complete Overview to the user. Do NOT proceed to `/plan` or `/implement` without explicit approval.

```
Plan document created: plan/{feature-name}.md

Summary:
- Assumptions: [N] items listed
- Success criteria: [N] conditions defined
- Impact: [modules / surfaces affected]

Approve to proceed to /plan for task breakdown?
```

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "This is simple, I don't need a spec" | Simple tasks still need acceptance criteria. A 2-line spec is fine. |
| "I'll figure it out while coding" | That is how rework happens. 15 minutes of spec saves 3 hours of wrong implementation. |
| "Requirements will change anyway" | That is why the spec is a living document. Having one that changes is better than having none. |
| "The user knows what they want" | Even clear requests have implicit assumptions. The spec surfaces those. |
| "I can just start with /implement" | Without defined success criteria, how will you know when you are done? |

## Red Flags

- Jumping to code without user approval on assumptions
- Assumptions not listed explicitly
- Success criteria that are not testable
- Missing impact analysis (especially cross-component coupling)
- Proceeding to `/plan` without user approval on the Overview
- Creating multiple plan documents for a single feature

## Verification

Before proceeding to `/plan`:

- [ ] Plan document exists at `plan/{feature-name}.md`
- [ ] Assumptions are listed and confirmed by user
- [ ] Success criteria are specific and testable
- [ ] Impact scope identifies affected modules / surfaces / tests
- [ ] User has explicitly approved the Overview

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
