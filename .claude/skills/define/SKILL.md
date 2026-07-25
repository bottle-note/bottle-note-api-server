---
name: define
description: |
  코드를 쓰기 전에 WHAT(무엇을·왜)을 확정하고 plan 문서와 Execution Mode 계약을 만든다.
  Trigger: "/define", 또는 사용자가 "이거 구현해줘", "기능 추가", "요구사항 정리", "define", "spec this"라고 할 때.
  상태 기반: 다중 파일 변경이 필요한 요청인데 plan/에 해당 기능 문서가 없을 때, 요청의 범위·성공 조건이 불명확할 때.
  이 스킬 안에서는 코드를 쓰지 않는다 — 산출물은 plan 문서다.
argument-hint: "[feature description]"
---

# Define — WHAT 확정과 위임 계약

## 철학: WHAT과 HOW를 섞지 않는다

이 스킬은 **무엇을·왜**만 다룬다. 어떻게(파일 구조, 태스크 순서, 구현 방식)는 `/plan`과 `/implement`의 몫이다.

이유는 재생성 비용의 비대칭이다. HOW는 틀리면 에이전트가 다시 짜면 되지만(싸다), WHAT은 사람의 의도를 끌어내야 해서 다시 만들기 비싸다. 그래서 사람의 협의는 여기에 집중하고, WHAT이 승인되면 이후 실패는 HOW의 실패로 격리된다.

- 허용: 타당성 조사 (스키마가 이미 있는지, 어떤 모듈이 영향받는지 살펴보기)
- 금지: HOW 결정 (태스크 분해, 파일 목록 확정, 구현 방식 선택)

## 전제

프로젝트 지침(CLAUDE.md/AGENTS.md)이 관습의 유일한 소유자다. 레이어 표준 15, 네이밍, 어노테이션, Flyway 규칙은 지침에 있으므로 여기서 다시 조사하지 않는다.

## When NOT to Use

- 재현 가능한 버그 수정 → `/debug`
- 범위가 자명한 단일 파일 수정 → 바로 `/implement`
- 이미 plan 문서가 있는 기능 → `/plan` 또는 `/implement`
- 테스트만 추가 → `/test`

## Process

### Step 1: 요청 파싱

범위를 추측하지 않는다. 어떤 도메인인가, 어떤 모듈(product-api/admin-api/batch/mono)이 관련되나, 외부에서 관찰 가능한 기대 행동은 무엇인가. 불명확하면 진행 전에 묻는다.

### Step 2: 가정 표면화

모든 가정을 명시하고 사용자 확인을 받는다. 가정 하나하나가 "틀릴 수 있는 것"이다.

```
ASSUMPTIONS:
1. 이 기능은 product-api 대상이다 (admin-api 아님)
2. 인증 필요 (공개 엔드포인트 아님)
3. {엔티티}는 이미 존재하고 스키마 변경 불필요
→ 확인 또는 수정해 주세요.
```

**확인 없이 진행 금지.** 가정을 조용히 채워 넣는 것이 재작업의 근원이다.

### Step 3: 성공 기준

각 기준은 구체적이고 검증 가능해야 한다. "더 좋게", "성능 개선" 같은 검증 불가 기준은 거부하고 구체화를 요청한다.

```
SUCCESS CRITERIA:
- GET {경로}가 평균·건수·분포를 반환한다
- 미인증 호출은 401
- 응답에 {필드1}, {필드2} 포함
→ 이 목표가 맞습니까?
```

성공 기준도 가정과 같다 — 확인 없이 다음 Step으로 가지 않는다.

### Step 4: 영향 범위 조사

결정이 아니라 조사다: 관련 모듈, cross-domain 결합(Facade 신설 필요?), 스키마 마이그레이션 여부(Flyway — 엔티티 변경 시 필수), 이벤트 발행/수신, 캐시, 필요한 테스트 계층, 외부 API 계약.

### Step 5: plan 문서 작성

`plan/{feature-name}.md` 생성. 기능 하나 = 문서 하나.

```markdown
# Plan: [기능명]

## Overview
[무엇을, 왜]

### Assumptions
### Success Criteria
### Impact Scope

## Execution Mode
- mode: (승인 게이트에서 확정)

## Tasks
(/plan에서 작성)

## Progress Log
```

### Step 6: 승인 게이트 = 계약 서명

Overview 승인과 **Execution Mode 선언**을 함께 받는다. 이것이 이 스킬의 핵심 산출물이다.

```
plan/{feature-name}.md 작성 완료

- 가정 [N]건 / 성공 기준 [N]건 / 영향: [모듈]

Execution Mode를 선택해 주세요:
1. step-by-step (기본) — 단계마다 승인
2. delegated — 이후 자율 진행. scope를 함께 선언해 주세요
   (plan, implement, test, verify, commit [, push, pr])
```

선택 결과를 plan 문서의 `## Execution Mode` 섹션에 기록한다. delegated면 scope와 stop-conditions를 함께 기록한다 — 기본 3종: ① 가정 붕괴(재개봉 프로토콜) ② `/verify` 3회 실패 ③ scope 밖 행동. **문서에 기록된 것만이 유효한 계약이다.** scope에 push/pr을 포함하면 그 행위가 별도 재확인 없이 수행된다는 뜻임을 게이트에서 함께 고지한다.

## 재개봉 프로토콜

이후 어느 단계(`/plan`, `/implement`, `/test`)에서든 **가정을 깨는 발견**이 나오면:

1. 즉시 정지한다 (delegated 모드여도 — stop-condition 1번).
2. 이 문서의 Assumptions를 수정한다.
3. WHAT(성공 기준·범위)이 바뀌었으면 재승인을 받는다. 표현만 정밀해진 것이면 기록만 남기고 계속한다.

조용히 적응해서 계속 진행하는 것이 최악의 선택이다.

## Common Rationalizations

| 합리화 | 현실 |
|--------|------|
| "간단해서 스펙 필요 없다" | 간단한 작업도 수용 기준은 필요하다. 2줄짜리 스펙이면 된다. |
| "코딩하면서 알아가면 된다" | 그게 재작업이 생기는 방식이다. 스펙 15분이 잘못된 구현 3시간을 아낀다. |
| "사용자가 뭘 원하는지 안다" | 명확한 요청에도 암묵적 가정이 있다. 스펙이 그걸 드러낸다. |
| "사용자가 건너뛰라고 했다" | 속도 요청은 형식을 압축하라는 뜻이다(가정을 묶어 한 번에 제시). 확인 자체는 생략하지 않는다. |

## Red Flags

- 가정 확인 없이 다음 단계로 진행
- 검증 불가능한 성공 기준을 그대로 수용
- Execution Mode 선언 없이 게이트 통과
- 기능 하나에 plan 문서 여러 개 생성
- 이 스킬 안에서 코드 작성 또는 태스크 분해

## 종료

지침의 **GSL Execution Mode** 규칙을 따른다. 산출물(문서 경로, 가정·기준 개수, 확정된 모드)을 보고한다. step-by-step이면 `Next: /plan`을 제안한 뒤 턴을 끝내고, delegated(scope에 plan 포함)면 `/plan`으로 계속한다.
