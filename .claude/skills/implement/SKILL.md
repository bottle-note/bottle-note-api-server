---
name: implement
description: |
  Incremental feature implementation across any language/stack.
  Trigger: "/implement", or when the user says "API 추가", "기능 구현", "기능 개발", "feature implementation", "build this".
  Branches on project type (web-api / cli / batch / library) and language (java-spring / python / go / ...) via arguments and references.
  Enforces Task / Slice / Commit 3-level granularity. For test implementation, use /test after this skill completes.
argument-hint: "[type] [language] [work-type]"
---

# Incremental Implementation

References (read the matching ones before coding):
- `references/types/{type}.md` — type-specific patterns (web-api / cli / batch / library)
- `references/languages/{language}.md` — language/framework-specific patterns

## Overview

Build features in thin vertical slices — implement one piece, verify it compiles / type-checks, then expand. Each Task is a committable unit; each Slice within a Task is a compile-check unit. The workflow is the same across stacks; concrete patterns live in references.

## When to Use

- Implementing any new feature
- Adding operations to an existing domain / module
- Extending an existing component with new behavior
- Any multi-file implementation work

## When NOT to Use

- Bug fixing with clear reproduction (use `/debug`)
- Test-only work (use `/test`)
- Requirements unclear or ambiguous (use `/define` first)
- Single-file config changes or documentation updates

## Argument Parsing

Parse `$ARGUMENTS`:
- **type**: `web-api` | `cli` | `batch` | `library` — selects `references/types/{type}.md`
- **language**: `java-spring` | `python` | `go` | ... — selects `references/languages/{language}.md`
- **work-type**: `crud` | `search` | `action` | `read-only` (informational, guides exploration scope)

If type / language is omitted, infer from project structure and confirm with the user before proceeding.

**Missing language reference fallback.** If `references/languages/{language}.md` does not exist for the resolved language (e.g., `rust`, `kotlin-android`, `swift`, `zig`):

1. **STOP** before Phase 0.
2. Present the `references/types/{type}.md` applicable scope and ask the user to choose:
   - **(a)** Provide the language's idioms inline (1-screen summary covering: module layout, DI/error/test patterns, build tool). This skill treats it as a temporary reference for this feature only.
   - **(b)** Approve fallback to the nearest-language reference: `go` for systems langs, `python` for dynamic langs, `java-spring` for static JVM-like langs. The user must acknowledge that idioms may not fit perfectly.
3. Never silently proceed with a missing language reference. If neither (a) nor (b) is approved, STOP.

## Process

### Phase 0: Explore

**Hard gate — approved plan required.** BEFORE doing anything else, verify that an approved plan document with at least one Task exists at `plan/{feature-name}.md` (the Progress Log being empty or in-progress is fine; the existence of Tasks is what matters). If not → **STOP** and tell the user to run `/define` then `/plan` first. The only exception: a single-file obvious fix (typo, rename, one-line comment, formatting) — in that case skip the rest of Phase 0 and proceed directly to a minimal slice + `/self-review` + `/verify quick`. Multi-file work without an approved plan is never allowed.

Before writing any code, understand what already exists and what will be affected.

**Codebase scan:**
1. Locate the target module / package
2. Identify existing services / handlers / repositories that may be reused
3. Check what already exists vs. what needs to be created
4. Read the relevant references (`types/{type}.md`, `languages/{language}.md`)

**Impact analysis (general — extend with type-specific items from references):**
5. **Cross-module coupling** — files affected if a shared interface changes (other modules, test doubles)
6. **Persistence** — schema migration needed?
7. **Async / events** — new events published or consumed?
8. **Caching** — invalidation strategy needed?
9. **Public API contract** — external consumers impacted?

Report both findings and impact to the user before proceeding.

### Phase 1: Core / Business Logic

Build the foundation in the layer order documented in `references/types/{type}.md`. As a generic template:

1. **Domain model / entity** (if new)
2. **Persistence interface + implementation** (repository, data access)
3. **DTO / model** (request / response / event shapes)
4. **Error / exception types**
5. **Service / use case / handler logic**
6. **Cross-module seam** (facade / port) when other modules need access

Language-specific class/function naming, annotations, and idioms: see `references/languages/{language}.md`.

### Phase 2: Surface / Entry Point

Build the externally visible layer per `references/types/{type}.md`:
- **web-api**: HTTP controller / route handler — path, auth, request validation, response shape, pagination
- **cli**: command binding — flags, args, output format, exit codes
- **batch**: job step + scheduler binding
- **library**: public API surface + backwards-compat considerations

### Phase 3: Task-Slice-Commit Cycle

Each Task from the `/plan` is implemented through Slices:

```
Task  = Commit unit (logical goal agreed with user)
Slice = Execution unit (compile / type-check before exceeding ~100 lines)
```

**Verification levels:**

| Timing | Level | What to run |
|--------|-------|-------------|
| After each Slice | Compile / type-check only | `/verify quick` or equivalent |
| After Task completion | Self-review + unit tests | `/self-review` → `/verify standard` |
| After all Tasks complete | Integration tests | `/verify full` |

For exact commands per language, see `verify/references/verify/{your-language}.md`.

**Commit message format** (Task = title, Slices = bullets):
```
feat: rating statistics service

- RatingStatisticsResponse DTO
- repository query for statistics
- service.getStatistics() implementation
- facade wiring
```

**Cycle per Task:**
1. Implement Slice → compile / type-check → pass? continue : fix
2. Repeat until all Slices in the Task are done
3. Run a self-review pass on the Task's changes (the 5-axis check). For a full independent review, stop and recommend `/self-review` as a separate invocation — do not run its full skill body inside `/implement`.
4. Run the compile/type-check and unit-test commands needed for this Task. For full `/verify standard` or `/verify full`, stop and recommend `/verify` as a separate invocation.
5. Commit with descriptive message
6. Update plan document (check off Task, add to Progress Log)
7. **HARD STOP after this Task.** Do NOT start the next Task in the same response turn. Report: completed Task number/title, verification evidence, changed files, next recommended Task. Then wait for the user's next message.
   - Exception: proceed to the next Task in the same turn ONLY IF the user explicitly named multiple Tasks for continuous execution in their request (e.g. "do Tasks 1 through 3"). An ambiguous "continue" / "진행하자" is NOT such permission.

### Phase 4: Final Verification

After all Tasks are committed:

| Timing | Command | Purpose |
|--------|---------|---------|
| Implementation done | `/verify standard` | Compile + unit + build |
| Before push / PR | `/verify full` | Includes integration tests |

Then use `/test` if integration tests need to be written.

## Endpoint / Command Design

URL / command shape, HTTP method mapping, flag conventions — all type-specific. See `references/types/{type}.md`.

## Package / Module Structure

Layer-to-folder mapping is type-specific (web-api differs from cli) and language-specific (Java package vs Python module vs Go internal/). See:
- `references/types/{type}.md` for layer breakdown
- `references/languages/{language}.md` for the language's idiomatic folder layout

## Polyglot Mode (monorepo with multiple type/language modules)

When the project is a polyglot monorepo (e.g., Python API + Go CLI + Java batch coexisting in one repo), a single `[type] [language]` argument is insufficient. Switch to **Polyglot Mode** when `/scan-conventions` reports more than one detected type or language.

### Step 1: Build the module matrix during Phase 0

| Path | Type | Language | Verify command | References to load |
|------|------|----------|----------------|---------------------|
| `services/api/` | web-api | python | `pytest -m unit` | types/web-api.md + languages/python.md + testing/python.md + verify/python.md |
| `tools/cli/` | cli | go | `go test ./...` | types/cli.md + languages/go.md + testing/go.md + verify/go.md |
| `batch/` | batch | java-spring | `./gradlew unit_test` | types/batch.md + languages/java-spring.md + testing/java.md + verify/java-gradle.md |

Record the matrix in the plan document's `Impact Scope` section.

### Step 2: Per-Task module declaration

For each Task in `/plan`, declare which module(s) it touches. **A Task must not cross module boundaries** — each module has different references and verify commands. If a feature requires changes in two modules:

- Split into **two Tasks** (one per module), or
- Add a third Task for the cross-module contract (e.g., shared protobuf / OpenAPI / event schema)

Cross-module Tasks are an L-size red flag and must be decomposed.

### Step 3: Per-Slice reference loading

During Slice execution, load ONLY the references for the current Slice's module. Do not mix idioms from two modules in one Slice (e.g., do not apply Python's `Depends()` idiom inside a Go handler Slice).

### Step 4: Verification fan-out

`/verify` runs the **union** of all touched modules' verify commands. If only `services/api/` was modified in the last Task, only `pytest -m unit` runs; if both `services/api/` and `batch/` were modified, both `pytest` and `./gradlew unit_test` run.

### Red Flags specific to Polyglot Mode

- A single Task touches files in two different modules (split it)
- A Slice mixes idioms from two language references
- `/scan-conventions` reports polyglot but the matrix is missing from the plan document
- Verify fan-out skipped because "I only changed Python, the Go module is unchanged" — verify still must run on every Task-touched module

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "I'll test it all at the end" | Bugs compound. A bug in Slice 1 makes Slices 2-5 wrong. Compile-check each Slice. |
| "It's faster to do it all at once" | It feels faster until something breaks and you cannot find which of 500 changed lines caused it. |
| "I don't need a facade / seam for this" | If you are accessing another module's internals directly, you need a seam. Module boundaries exist for a reason. |
| "This refactor is small enough to include" | Refactors mixed with features make both harder to review and debug. Separate them. |
| "The controller / handler can hold this logic" | Surface layers are thin. Business logic belongs in the service / use case. Always. |
| "I'll skip self-review, the code is straightforward" | Straightforward code still needs architecture and security checks. Run `/self-review`. |

## Red Flags

- More than 100 lines written without a compile / type-check
- Cross-module internals accessed directly (bypassing the documented seam)
- Surface layer (controller / handler / CLI) containing business logic instead of delegating
- Interface changed but test doubles (InMemory/Fake implementations) not updated
- No error handling for module-specific error types
- Skipping Phase 0 (Explore) and jumping straight to coding
- Multiple unrelated changes in a single Task
- Task completed without running `/self-review`
- Starting Task N+1 after completing Task N without explicit user permission for continuous execution
- Treating an ambiguous "continue" as permission to finish all remaining Tasks
- Running `/test`, `/verify`, or `/self-review` as full skill bodies inside `/implement` instead of stopping at that skill boundary

## Verification

After completing all Tasks for a feature:

- [ ] Each Task was individually reviewed (`/self-review`) and committed
- [ ] Module boundaries respected (no cross-module direct access)
- [ ] Layer order from `references/types/{type}.md` followed
- [ ] Language idioms from `references/languages/{language}.md` respected
- [ ] Unit tests pass: `/verify standard`
- [ ] Architecture / lint rules pass: included in `/verify standard`
- [ ] Build / package succeeds
- [ ] Plan document updated (all Tasks checked, Progress Log filled)

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
