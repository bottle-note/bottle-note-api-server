---
name: scan-conventions
description: |
  Scans a project ONCE to discover its real conventions (directory layout, naming, build system, test patterns, error model) and writes a single artifact for downstream GSL steps to consult.
  Trigger: "/scan-conventions", or when the user says "관습 파악", "프로젝트 스캔", "scan conventions", "convention discovery", "preflight".
  Use on project entry, or when conventions have drifted significantly from the existing artifact.
  Other GSL skills (`/define`, `/implement`, `/test`) consult the artifact at their Phase 0 — they do not re-scan.
argument-hint: "[project-root path, default = current working dir]"
---

# Scan Conventions — Preflight Project Discovery

## Overview

GSL's `references/` provide generic patterns per project type (web-api / cli / batch / library) and per language (java-spring / python / go). But every real project has its own conventions that may agree with, refine, or contradict those references. This skill runs **once per project** (not per feature) to discover the actual conventions and writes a single artifact at `plan/conventions.md`. All other GSL skills then read that artifact at their Phase 0 instead of re-scanning, which keeps per-step cost low and conventions consistent across the session.

If `plan/conventions.md` already exists and is recent, **do not re-run** — let the user invoke explicitly if they suspect drift.

## When to Use

- First time using GSL on a project
- After significant restructuring (modules added/removed, framework upgrade)
- User explicitly asks ("관습 다시 파악해줘", "rescan")
- Any GSL skill's Phase 0 reports missing or stale `plan/conventions.md`

## When NOT to Use

- `plan/conventions.md` already exists and project hasn't significantly changed
- Mid-feature work (run `/scan-conventions` only on entry, not between Tasks)
- Single-file edits to a known project

## Process

### Step 1: Confirm Run Conditions

- Locate the project root (argument, or current working dir + walk up to first `.git` / lockfile)
- Check whether `plan/conventions.md` already exists
  - Exists and < 30 days old → ask user "Rescan, or use existing? (use existing recommended)"
  - Exists and > 30 days OR user explicitly asked → proceed with rescan
  - Does not exist → proceed

### Step 2: Discover Directory Layout

Identify:
- Top-level structure (monorepo / single module / multi-module)
- Source root convention (`src/`, `src/main/...`, `internal/`, `lib/`, ...)
- Test root convention
- Configuration / build files location

### Step 3: Identify Build System & Language

Detect via lockfiles / manifests:
- `build.gradle` / `pom.xml` / `pyproject.toml` / `package.json` / `go.mod` / `Cargo.toml` / etc.
- Language version (from manifest)
- Test runner (Gradle test, pytest, go test, jest, etc.)
- Linter / formatter (spotless, ruff, eslint, gofmt, ...)

### Step 4: Sample Naming Conventions

Read 5+ recent source files (favor non-test, non-generated). Extract:
- File naming pattern (`{Domain}Controller.java`, `{snake_case}.py`, `{name}_handler.go`)
- Type / class / function naming patterns
- Test class / function naming convention
- Display name convention (Korean `@DisplayName` "~할 때 ~한다" / English describe blocks / etc.)

**Bootstrap fallback (insufficient samples).** If the project has fewer than 5 sampleable source files (a freshly bootstrapped repo with only `main.py` / `cmd/main.go` / `App.java` / similar), **do NOT block downstream skills**. Instead:

1. Mark this section as `INSUFFICIENT_SAMPLE` in the artifact (`plan/conventions.md`).
2. Adopt the matching `references/languages/{language}.md` defaults as the working conventions for naming / layering / test patterns.
3. Record in the artifact that this is a bootstrap convention and **flag for mandatory re-scan after the first 5+ non-test source files are committed**.
4. The artifact still gets written so that `/define`·`/plan`·`/implement` can proceed; downstream skills MUST consult the `INSUFFICIENT_SAMPLE` marker and warn the user that the convention is provisional.

This unblocks green-field projects without forcing fabricated conventions. The Red Flag "Sampling fewer than 5 files" applies ONLY to projects that have 5+ files but the skill skipped them — not to projects that genuinely have fewer than 5.

### Step 5: Identify Test Patterns

- Test categorization mechanism (`@Tag("unit")`, `@pytest.mark.unit`, build tags `//go:build integration`, ...)
- Fake / InMemory pattern present? (search for `InMemory*`, `Fake*`, fixture directories)
- Integration test base class / fixture
- Test data factory pattern
- Mocking library presence (note as informational — GSL policy is Fake-first regardless)

### Step 6: Detect Architecture Markers

- Layer markers (custom annotations like `@FacadeService`, `@DomainRepository`, internal package boundaries)
- Cross-module seams (facade / port / interface boundaries)
- Error model (custom exception hierarchies, error code enums)
- Persistence pattern (Repository tiers, Active Record, etc.)

### Step 7: Compare Against GSL References

For each discovered convention, compare to the matching GSL references:
- `implement/references/types/{detected-type}.md`
- `implement/references/languages/{detected-language}.md`
- `test/references/testing/{detected-language}.md`
- `verify/references/verify/{detected-language}.md`

Classify each finding as:
- **MATCH** — project convention matches references (downstream skills can use references as-is)
- **REFINEMENT** — project has a more specific convention (note in artifact; downstream skills should follow project's)
- **CONFLICT** — project convention contradicts references (escalate; user decides which wins in Step 8)

### Step 8: Resolve Conflicts (user decision)

For each CONFLICT, present:
```
CONFLICT: <area>
  references: <pattern A>
  project:    <pattern B>
  Which should downstream GSL skills follow?
  1) Project (recommended — match existing code)
  2) References (only if you intend to refactor the project)
```

Record the user's decision in the artifact.

### Step 8.5: Virtual / Dry-run Mode (read-only environments)

If running in a **read-only sandbox** (`codex exec`, MCP sandbox, CI dry-run) or the user explicitly requested a **simulation / dry-run**:

- Do NOT attempt to write `plan/conventions.md` to disk
- Instead, output the same artifact content to conversation / stdout with header `[VIRTUAL ARTIFACT — NOT WRITTEN]`
- Downstream skills in the same session can read this content from conversation context as if it were the on-disk file
- This does NOT satisfy `/define`'s hard gate for production work — virtual mode is for analysis / dry-run only

For real project entry, proceed to Step 9.

### Step 9: Write the Artifact

Write to `plan/conventions.md`:

```markdown
# Project Conventions

> Generated by /scan-conventions on YYYY-MM-DD. Re-run if the project structure changes significantly.

## Detected Project Type
[web-api / cli / batch / library / hybrid]

## Build System & Language
- Language: [...]
- Build: [...]
- Test runner: [...]
- Linter / formatter: [...]

## Directory Layout
[summary tree]

## Naming Conventions
- File: [...]
- Type / class: [...]
- Test: [...]
- Display name: [...]

## Test Patterns
- Categorization: [tag / marker / build-tag / decorator]
- Fake / InMemory present: [yes / no — list if yes]
- Integration base: [...]
- Mocking library present: [name or "none"]

## Architecture Markers
- Layering: [...]
- Cross-module seams: [...]
- Error model: [...]
- Persistence: [...]

## Comparison with GSL References
| Area | references | project | classification | follow |
|------|-----------|---------|----------------|--------|
| ... | ... | ... | MATCH/REFINEMENT/CONFLICT | references/project |

## Conflict Resolutions
- [conflict A]: follow [project / references] — reason: [...]
```

### Step 10: Report

Print a short summary:
```
Scan complete. plan/conventions.md written.
- Detected type: <type>
- Language: <language>
- N findings: M MATCH, K REFINEMENT, L CONFLICT (resolved)
- Other GSL skills will consult this artifact in their Phase 0.
```

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "I'll just scan again each feature, faster than reading the artifact" | Per-feature scanning is expensive and produces drift across sessions. One artifact, one source of truth. |
| "References are best practice — override project's convention" | No. Match existing code unless the user explicitly wants to refactor. Conflict resolution belongs to the user, not the skill. |
| "User didn't ask about conventions, skip this skill" | Other GSL skills depend on this artifact existing. Run on project entry. |
| "Skip the comparison step, just record findings" | The comparison is the whole point — downstream skills need to know match vs conflict. |
| "Just glance at one file, generalize from there" | Sample 5+ files. One file can mislead. |

## Red Flags

- Running this skill mid-feature (after `/define`) — should be preflight only
- Writing anywhere besides `plan/conventions.md`
- Modifying source files (this skill is discovery-only)
- Resolving CONFLICT without asking the user
- Generating the artifact without the comparison table populated
- Sampling fewer than 5 files for naming conventions

## Verification

After running:

- [ ] `plan/conventions.md` exists at project root
- [ ] All 7 sections (Project Type, Build, Layout, Naming, Tests, Architecture, Comparison) are populated
- [ ] At least 5 source files were sampled for naming
- [ ] Comparison table has classification for every meaningful area
- [ ] All CONFLICTs have a recorded user resolution
- [ ] No source files were modified
- [ ] Report printed to user with counts (MATCH / REFINEMENT / CONFLICT)

## Runtime Boundary — HARD STOP

This skill ENDS after the Verification checklist and final report are completed.

For codex and any runtime without an enforced skill-return boundary:
- MUST stop the assistant turn here.
- MUST NOT invoke, load, or execute any next GSL skill in the same response turn.
- MUST NOT continue into `/next-flow`, `/define`, `/plan`, `/implement`, `/test`, `/verify`, `/debug`, or `/self-review`.
- MAY print exactly one suggested next command as plain text.
- MUST wait for the user's next message before running any next skill.

If the user says only "continue", treat that as permission to report the next recommended command, not permission to execute it.
