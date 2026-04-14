---
name: define
description: |
  Clarifies requirements before any code is written. Creates a plan document with assumptions, success criteria, and impact scope.
  Trigger: "/define", or when the user says "이거 구현해줘", "기능 추가", "요구사항 정리", "define requirements".
  Use when starting a new feature, when requirements are vague, or when the scope of a change is unclear.
  Do NOT write code during this skill — the output is a plan document, not implementation.
argument-hint: "[feature description]"
---

# Define Requirements

## Overview

Write a structured specification before writing any code. The plan document is the shared source of truth — it defines what we are building, why, and how we will know it is done. Code without a spec is guessing.

This skill creates `plan/{feature-name}.md` with an Overview section. The `/plan` skill later adds Tasks to the same document.

## When to Use

- Starting a new feature or significant change
- Requirements are ambiguous or incomplete
- The change touches 3+ files or multiple modules
- The user gives a vague request ("이거 구현해줘", "추가해줘")

## When NOT to Use

- Bug fixes with clear reproduction (use `/debug`)
- Single-file changes with obvious scope
- Requirements are already documented in a plan file
- Test-only work (use `/test`)

## Process

### Step 1: Parse Request

Identify what the user wants. Do NOT assume scope.

- What domain is involved? (alcohols, rating, review, support, etc.)
- Which module? (product-api, admin-api, or both?)
- What is the expected user-facing behavior?
- Are there related features already implemented?

If anything is unclear, ask before proceeding. Do NOT fill in ambiguous requirements silently.

### Step 2: Surface Assumptions

List every assumption explicitly. Each assumption is something that could be wrong.

```
ASSUMPTIONS:
1. This feature is for product-api (not admin-api)
2. Authentication is required (not a public endpoint)
3. The alcohol entity already exists and does not need schema changes
4. Pagination uses cursor-based approach (project default for product)
-> Confirm or correct these before I proceed.
```

Do NOT proceed without user confirmation on assumptions.

### Step 3: Define Success Criteria

Each criterion must be specific and testable. Translate vague requirements into concrete conditions.

```
REQUIREMENT: "평점 통계 기능 추가"

SUCCESS CRITERIA:
- GET /api/v1/ratings/statistics/{alcoholId} returns average rating, count, and distribution
- Response includes rating distribution as a map (e.g., {FIVE: 12, FOUR: 8, ...})
- Unauthenticated users can access (read-only endpoint)
- Response time < 500ms for alcohols with 1000+ ratings
-> Are these the right targets?
```

### Step 4: Analyze Impact Scope

Check which modules and components are affected:

- **Modules**: Which of mono, product-api, admin-api are involved?
- **Domains**: Does this touch multiple domains? (If yes, Facade needed)
- **Entities**: Any schema changes? (Liquibase migration needed)
- **Events**: New domain events? Existing event listeners affected?
- **Cache**: Does this data need caching? Existing cache invalidation affected?
- **Tests**: Which test types will be needed? (unit, integration, RestDocs)

### Step 5: Create Plan Document

Create `plan/{feature-name}.md` in Korean with the following structure:

```markdown
# Plan: [기능명]

## Overview
[무엇을 왜 만드는지]

### Assumptions
- [가정 1]
- [가정 2]

### Success Criteria
- [성공 기준 1 - 구체적, 테스트 가능]
- [성공 기준 2]

### Impact Scope
- [영향받는 모듈/파일 목록]
```

This document will be extended by `/plan` (Tasks section) and `/implement` (Progress Log).

### Step 6: User Approval Gate

Present the complete Overview to the user. Do NOT proceed to `/plan` or `/implement` without explicit approval.

```
Plan document created: plan/{feature-name}.md

Summary:
- Assumptions: [count] items listed
- Success criteria: [count] conditions defined
- Impact: [modules affected]

Approve to proceed to /plan for task breakdown?
```

## Common Rationalizations

| Rationalization | Reality |
|-----------------|---------|
| "This is simple, I don't need a spec" | Simple tasks still need acceptance criteria. A 2-line spec is fine. |
| "I'll figure it out while coding" | That is how you end up with rework. 15 minutes of spec saves 3 hours of wrong implementation. |
| "Requirements will change anyway" | That is why the spec is a living document. Having one that changes is better than having none. |
| "The user knows what they want" | Even clear requests have implicit assumptions. The spec surfaces those. |
| "I can just start with /implement" | Without defined success criteria, how will you know when you are done? |

## Red Flags

- Jumping to code without user approval on assumptions
- Assumptions not listed explicitly
- Success criteria that are not testable ("make it better", "improve performance")
- Missing impact analysis (especially cross-domain Facade needs)
- Proceeding to `/plan` without user approval on the Overview
- Creating multiple plan documents for a single feature

## Verification

Before proceeding to `/plan`:

- [ ] Plan document exists at `plan/{feature-name}.md`
- [ ] Assumptions are listed and confirmed by user
- [ ] Success criteria are specific and testable
- [ ] Impact scope identifies affected modules, domains, and test types
- [ ] User has explicitly approved the Overview
- [ ] Document is written in Korean (plan documents use Korean)
