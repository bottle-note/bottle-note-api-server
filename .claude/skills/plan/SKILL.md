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

## When to Use

- After `/define` has created a plan document with Overview
- When 3+ files need changes
- When work spans multiple domains or modules
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
- Impact Scope: which modules and domains are affected

### Step 2: Dependency Analysis

Map what depends on what. Implementation order follows the dependency graph bottom-up:

```
Entity / Domain model
    |
    +-- Repository (Domain -> JPA -> QueryDSL)
    |       |
    |       +-- Service (uses Repository + Facade)
    |       |       |
    |       |       +-- Controller (delegates to Service)
    |       |
    |       +-- Facade (if cross-domain access needed)
    |
    +-- DTO (Request / Response)
    |
    +-- Exception (ExceptionCode + Exception class)
```

Identify:
- What must be built first (foundation)
- What can be built in parallel (independent pieces)
- What depends on cross-domain Facades (coordination needed)

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
Task 1: Rating statistics DTO + Repository + query
Task 2: Rating statistics Service + unit test
Task 3: Rating statistics Controller endpoint
Task 4: Integration test
```

### Step 4: Size Validation

Check every Task:
- Is it L-sized (8+ files)? -> Split it
- Does the title contain "and"? -> Probably two Tasks
- Can acceptance criteria be described in 3 or fewer bullets? -> If not, too broad
- Does it touch two independent subsystems? -> Split by subsystem

### Step 5: Write Tasks to Plan Document

Append the Tasks section and empty Progress Log to the existing plan document.

**Task entry format:**
```markdown
### Task N: [제목]
- 수용 기준: [구체적, 테스트 가능한 조건]
- 검증: [명령어 또는 확인 방법]
- 파일: [변경 예상 파일 목록]
- 크기: [S | M]
- 상태: [ ] 미완료

## Progress Log
(empty - filled during /implement)
```

Insert a checkpoint after every 2-3 Tasks:

```markdown
### Checkpoint: Task 1-3 완료 후
- [ ] 컴파일 통과
- [ ] 단위 테스트 통과
- [ ] 아키텍처 규칙 통과
```

### Step 6: User Approval Gate

Present the task list with estimated order. Get explicit approval before proceeding to `/implement`.

```
Tasks added to plan/{feature-name}.md

Summary:
- [N] tasks defined (sizes: S x [n], M x [n])
- Estimated commits: [N]
- Dependencies: [brief description]

Approve to proceed to /implement?
```

## Plan Document Lifecycle

```
/define creates document    -> Status: IN PROGRESS
/plan adds Tasks            -> Tasks section populated
/implement checks off Tasks -> Progress Log updated per Task commit
All Tasks done              -> Add stamp from plan/stamp-template.st
                            -> Move to plan/complete/
```

**One feature = one document.** Do not split a feature across multiple plan files. Do not create a new document if one already exists for this feature.

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "I'll figure it out as I go" | That is how you end up with rework. 10 minutes of planning saves hours. |
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
- Dependency order not considered (building controllers before services)
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
