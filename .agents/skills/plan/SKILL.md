---
name: plan
description: |
  Breaks work into ordered, verifiable tasks with acceptance criteria.
  Trigger: "/plan", or when the user says "계획 세워줘", "태스크 분해", "plan this", "break it down".
  Use after /define when requirements are clear and a plan document exists.
  Adds the Tasks section to an existing plan document created by /define.
argument-hint: "[feature-name or plan file path]"
---

# Planning and Task Breakdown

## Overview

Decompose work into small, verifiable tasks with explicit acceptance criteria. Good task breakdown is the difference between an agent that completes work reliably and one that produces a tangled mess. Every task should be small enough to implement, test, and commit in a single focused session.

This skill adds the Tasks section to an existing plan document created by `/define`.

**External workflow precedence.** When Superpowers' `writing-plans` skill is active in the same session, GSL `/plan` takes precedence on the same prompt. Invoke `writing-plans` as a sub-block to enrich Dependency Analysis (Step 2) and the Task list (Step 3) — but it does NOT replace GSL's vertical-slice rule, L-size ban (8+ files), Checkpoint cadence, or user-approval gate. If `writing-plans` produces a horizontal task list, **STOP and rewrite as vertical slices** before presenting to the user.

## When to Use

- After `/define` has created a plan document with Overview
- When 3+ files need changes
- When work spans multiple modules or surfaces
- When the implementation order is not obvious

## When NOT to Use

- No plan document exists yet (use `/define` first)
- Single-file changes with obvious scope (just `/implement` directly)
- Bug fixes (use `/debug` directly)

## Process

### Step 1: Read Plan Document

Open `plan/{feature-name}.md`. If it does not exist, prompt the user to run `/define` first.

Read:
- Overview: what are we building and why
- Assumptions: confirmed constraints
- Success Criteria: what "done" looks like
- Impact Scope: which modules / surfaces / tests are affected

### Step 2: Dependency Analysis

Map what depends on what. Implementation order follows the dependency graph bottom-up.

For the actual dependency layers, consult `implement/references/types/{your-type}.md` — for example:
- **web-api**: entity / model → repository → service → controller / route; DTOs and exceptions in parallel
- **cli**: domain model → command handler → CLI binding
- **batch**: job step → job → scheduler binding
- **library**: internal helpers → public API surface

Identify:
- What must be built first (foundation)
- What can be built in parallel (independent pieces)
- What depends on cross-module boundaries (coordination needed)

### Step 3: Create Task List

Break work into Tasks. Each Task is a commit unit.

**Sizing guidelines:**

| Size | Files | Scope | Action |
|------|-------|-------|--------|
| **S** | 1-3 | Single component | Good as-is |
| **M** | 4-7 | One feature slice | Good as-is |
| **L** | 8+ | Multi-component | **Must split further** |

**Prefer vertical slices:** build one complete path through the stack rather than all layers at once.

Bad (horizontal):
```
Task 1: All DTOs
Task 2: All Repositories
Task 3: All Services
```

Good (vertical):
```
Task 1: Statistics DTO + Repository + query
Task 2: Statistics Service + unit test
Task 3: Statistics Controller / handler / command
Task 4: Integration test
```

### Step 4: Size Validation

Check every Task:
- Is it L-sized (8+ files)? → Split it
- Does the title contain "and"? → Probably two Tasks
- Can acceptance criteria be described in 3 or fewer bullets? → If not, too broad
- Does it touch two independent subsystems? → Split by subsystem

### Step 5: Write Tasks to Plan Document

Append the Tasks section and an empty Progress Log to the existing plan document.

**Task entry format:**
```markdown
### Task N: [title]
- Acceptance: [specific, testable condition]
- Verification: [command or check]
- Files: [expected file changes]
- Size: [S | M]
- Status: [ ] not done

## Progress Log
(empty — filled during /implement)
```

Insert a checkpoint after every 2-3 Tasks:

```markdown
### Checkpoint: after Tasks 1-3
- [ ] Compiles / type-checks
- [ ] Unit tests pass
- [ ] Project architecture / lint rules pass
```

### Step 6: User Approval Gate

Present the task list with estimated order. Get explicit approval before proceeding to `/implement`.

```
Tasks added to plan/{feature-name}.md

Summary:
- [N] tasks defined (sizes: S × [n], M × [n])
- Estimated commits: [N]
- Dependencies: [brief description]

Approve to proceed to /implement?
```

## Plan Document Lifecycle

```
/define creates document    → Status: IN PROGRESS
/plan adds Tasks            → Tasks section populated
/implement checks off Tasks → Progress Log updated per Task commit
All Tasks done              → Add completion stamp
                            → Move to plan/complete/ (or plan/stale/ if abandoned)
```

**One feature = one document.** Do not split a feature across multiple plan files. Do not create a new document if one already exists for this feature.

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "I'll figure it out as I go" | That is how rework happens. 10 minutes of planning saves hours. |
| "The tasks are obvious" | Write them down. Explicit tasks surface hidden dependencies and forgotten edge cases. |
| "Planning is overhead" | Planning IS the task. Implementation without a plan is just typing. |
| "I can hold it all in my head" | Context windows are finite. Written plans survive session boundaries and compaction. |
| "This is only 2 tasks, why bother" | Even 2 tasks benefit from acceptance criteria and verification commands. |

## Red Flags

- Starting `/implement` without a written task list
- Tasks that say "implement the feature" without acceptance criteria
- No verification commands in the plan
- All tasks are L-sized (should be split)
- No checkpoints between tasks
- Dependency order not considered (building UI before underlying service)
- Task title contains "and" (probably two tasks)

## Verification

Before starting `/implement`, confirm:

- [ ] Plan document has both Overview (from `/define`) and Tasks sections
- [ ] Every Task has acceptance criteria
- [ ] Every Task has a verification command
- [ ] No Task is L-sized (8+ files)
- [ ] Dependencies are ordered correctly (foundation first)
- [ ] Checkpoints exist between major groups of Tasks
- [ ] User has explicitly approved the task list

---

## Lifecycle Integration

**Before this skill:** if `plan/conventions.md` does not exist, run `/scan-conventions` first — analysis relies on knowing the project's actual conventions (naming, layering, test patterns, build system).

**After this skill:** invoke `/next-flow` to diagnose lifecycle state and propose the next command. `/next-flow` auto-progresses read-only verification only and never writes files. Note: `/plan` is a Claude Code UI command and cannot be auto-invoked — the user must type it themselves; `/next-flow` will print a notice in that case.
