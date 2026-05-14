---
name: next-flow
description: |
  Diagnoses where in the GSL lifecycle the work currently is, proposes the single next command, and may auto-trigger ONLY read-only verification (never any write).
  Trigger: "/next-flow", or when the user says "다음 단계", "이제 뭐 해야 해", "next step", "what's next".
  Use after completing any GSL step (`/define`, `/plan`, `/implement` Task, `/test`, `/verify`, `/debug`) to find out what to do next.
  Strictly read-only: this skill never writes files, edits code, or modifies plan documents.
argument-hint: "[plan-file-path or current-step]"
---

# Next Flow — GSL Lifecycle Continuation Helper

## Overview

GSL's biggest known friction is **P1: gaps between steps don't self-advance** — after `/implement` the user has to manually invoke `/test`, after `/test` manually invoke `/verify`, and so on. This skill closes that gap **without violating GSL gates**: it reads the plan document, diagnoses where the work is, proposes exactly one next command, and may run a read-only verification on the user's behalf — but **never writes anything**.

Hard policy (from codex consultation): *auto-progression is restricted to read-only verification + next-command suggestion. Any write-side follow-up (new test files, code edits, plan document updates, commits, pushes) requires explicit user invocation.* See "Common Rationalizations" for why this matters.

## When to Use

- Right after any GSL step appears to have completed
- When the user is unsure what to do next ("다음에 뭐 하면 돼?")
- When a plan document exists but the user has lost track of the progress log
- Before invoking `/test` / `/verify` / commit — to confirm preconditions are met

## When NOT to Use

- Before `/define` (no plan document yet — start with `/define` directly)
- When a `/debug` recovery is in progress (let `/debug` finish first)
- When the user has already named the next step explicitly
- For `/plan` transition — `/plan` is a Claude Code UI command and cannot be auto-invoked from a skill; this skill will only print a notice telling the user to type `/plan` themselves

## Process

### Step 1: Locate the Plan Document

- If user passed a path, use it
- Otherwise scan `plan/*.md` for the IN PROGRESS document (one with unchecked Tasks)
- If 0 candidates → tell user to run `/define`. STOP.
- If 2+ candidates → ask user which one. STOP.

### Step 2: Diagnose Current Step

Read Overview, Tasks, Progress Log. Classify the work into exactly one of:

| Signal | Current step | Next step |
|--------|--------------|-----------|
| Plan doc has Overview only, no Tasks | post-`/define` | `/plan` (UI command — print notice) |
| Tasks exist, Progress Log empty | post-`/plan` | `/implement` |
| Some Tasks done, more remain | mid-`/implement` | continue `/implement` with next Task |
| All Tasks done, no integration tests written | post-`/implement` | `/test` (integration tests) |
| Integration tests exist, `/verify full` not yet run | post-`/test` | `/verify full` |
| `/verify full` passed | post-`/verify` | commit + plan stamp (manual) |
| Last log entry is a `/debug` failure | mid-`/debug` | continue `/debug` |
| `/verify` failed | mid-`/debug` | enter `/debug` |

### Step 3: Check Preconditions (read-only)

Before proposing the next command, verify its preconditions in a read-only way:

| Next command | Read-only precondition check |
|--------------|------------------------------|
| `/plan` | Overview section is complete, user has approved |
| `/implement` | At least one Task is defined |
| `/test` | All Tasks have commits in Progress Log |
| `/verify full` | `git status` clean OR uncommitted work is intentional |
| commit / PR | `/verify full` last run = PASS |
| `/debug` | A reproducible failure is recorded |

If any precondition fails, **do not auto-progress**. Report the failure and let the user decide.

**Red test state — hard precondition.** If `git status` or the latest test output indicates uncommitted test failures, OR the last recorded `/verify` run is FAIL, do NOT propose any next step except `/debug`. If no reproducible failure is recorded yet, STOP and ask the user to first reproduce the failure (this is `/debug` Step 2). Never propose `/test` / `/verify` / commit / push while a known red state exists.

### Step 4: Optional Auto-Run (read-only only)

This is the ONLY auto-progression allowed by this skill. Allowed read-only actions:

- `/verify quick` or `/verify standard` (these run tests but do not write source files)
- `git status` / `git log --oneline` / `git diff` to confirm preconditions
- Reading existing test output

**Never allowed:**
- Editing or creating files (source, test, plan, anywhere)
- Running commit, push, branch, merge
- Invoking write-side skills (`/define`, `/implement`, `/test` write-mode, etc.) without user action
- Modifying the plan document
- Calling external systems (DB, deploy, package registries)

### Step 5: Propose the Next Command

Output exactly one of:

```
Next: /<command> <args>
Reason: <one line — why this is next>
Preconditions: PASS
[Run it? You can type /<command> yourself, or reply "yes" to have me run it only if it is read-only.]
```

For `/plan` and other UI commands, replace the last line with:

```
/plan is a Claude Code UI command — please type it in the input box yourself; I cannot trigger it from a skill.
```

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "All Tasks done, I'll just auto-write the integration tests" | `/test` writes files. That violates GSL's user-gate. Propose `/test` and stop. |
| "User said 'continue', so I can just push through commit" | Commit is a write boundary and an externally-visible action. Propose, never auto-run. |
| "It's only a small edit to plan doc Progress Log" | Plan document is the single source of truth. Only `/implement` updates it after a Task commit. |
| "/verify full passed — let me start the next feature" | Closing the current feature (stamp + plan/complete/ move) is a manual decision. Propose, don't act. |
| "I'll auto-call /plan since /implement needs it" | `/plan` is a UI command. You cannot. Print the notice and stop. |

## Red Flags

- Auto-progression has caused any file to be created, edited, or deleted (immediate STOP — this skill should never do that)
- Multiple next-step candidates proposed (the table in Step 2 should yield exactly one)
- Preconditions skipped because "the user implied it was fine"
- Proposing `/plan` for auto-invocation (impossible — print notice instead)
- **Bypassing the UI-command notice by invoking `/plan` (or any other Claude Code UI command) through Bash, shell scripts, `subprocess`, MCP tools, or other wrappers — anti-pattern, STOP.** The notice is the only correct output for UI-command transitions.
- The next command requires user approval (e.g., post-`/define` → `/plan`) and you tried to invoke it anyway

## Verification

After running:

- [ ] Exactly one next command was proposed
- [ ] All preconditions were checked in a read-only way
- [ ] If any auto-action was run, it was read-only only (verify / status / log / diff)
- [ ] No file was created, edited, or deleted by this skill
- [ ] If the next step is `/plan`, the UI-command notice was printed instead of attempting auto-invoke
- [ ] The proposal includes the reason and the user's explicit invocation prompt
