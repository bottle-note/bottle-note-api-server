---
name: plan
description: |
  승인된 define 문서를 받아 일을 자연 관절대로 분해하고 의존성 순서로 정렬해 Tasks를 만든다.
  Trigger: "/plan", 또는 사용자가 "계획 세워줘", "태스크 분해", "plan this", "break it down"이라고 할 때.
  상태 기반: plan/{기능}.md에 Overview는 있으나 Tasks 섹션이 비어 있을 때.
  Execution Mode가 delegated(scope에 plan 포함)면 승인 게이트 대신 체크포인트 보고로 진행한다.
argument-hint: "[feature-name or plan file path]"
---

# Plan — 분해와 정렬

## 철학: Task는 관절, Slice는 썰기

정육 모델로 구분한다. 소를 등심·안심으로 나누는 부위 분해가 **Task**다 — 문제의 자연 관절(의존성, 모듈 경계, 리뷰 단위)을 따라 자르며, 올바른 답이 적고 틀리면 커밋 이력과 리뷰 단위가 오염되므로 **승인 대상**이다. 그 안심을 어떻게 썰어 요리할지가 **Slice**다 — 정답이 여럿이고 틀려도 다시 썰면 되므로 **에이전트 전권**이며 `/implement`에서 결정한다.

따라서 이 스킬의 순서는 **분해 → 정렬 → 크기 검증**이다. 크기는 절단 기준이 아니라 관절을 제대로 찾았는지의 검산이다.

## 전제

`plan/{feature-name}.md`에 승인된 Overview(Assumptions·Success Criteria·Impact Scope)와 Execution Mode 선언이 있어야 한다. 없으면 `/define`을 안내하고 종료한다.

## 가정 붕괴 시 즉시 중단 (전역 규칙)

어느 Step에서든 define의 Assumption을 깨는 발견이 나오면 **그 자리에서 중단한다**. Tasks를 아직 기록하지 않았으면 기록하지 않고, 이미 기록했다면 해당 기록을 되돌린다 (아직 승인되지 않은 기록에 한함 — 승인·커밋된 Task의 처리는 `/implement`의 정지 보고 규칙을 따른다). 이것이 stop-condition 1번(가정 붕괴)이며 delegated 위임보다 우선한다.

중단 후 절차(재개봉 프로토콜): ① 발견 내용과 깨진 가정을 보고한다 ② define 문서의 Assumptions 수정안을 제시하고 반영한다 ③ WHAT(성공 기준·범위)이 바뀌었으면 재승인을 받고, 표현만 정밀해진 것이면 기록만 남기고 계속한다.

## When NOT to Use

- plan 문서가 없다 → `/define` 먼저
- 범위가 자명한 단일 파일 수정 → 바로 `/implement`
- 버그 수정 → `/debug`

## Process

### Step 1: define 산출물 읽기

Overview·Assumptions·Success Criteria·Impact Scope·Execution Mode를 읽는다. 여기 없는 요구사항을 이 단계에서 새로 만들지 않는다 — 부족하면 위 전역 규칙의 재개봉 프로토콜로 돌아간다.

### Step 2: 분해 — 자연 관절 찾기

Impact Scope를 보고 일의 관절을 찾는다. 관절의 기준:

- **의존성 경계**: 먼저 존재해야 다음이 성립하는 지점. entity → repository → service → controller는 **순서**의 기준이지, 레이어마다 Task를 만들라는 뜻이 아니다
- **모듈 경계**: product-api / admin-api / mono / batch — 한 Task는 한 모듈에 집중
- **리뷰 단위**: 리뷰어가 한 번에 이해하고 승인할 수 있는 덩어리 = 커밋 하나

**수직 슬라이스 원칙**: 레이어별 수평 분해(모든 DTO → 모든 Repository → 모든 Service)는 금지다. 스택을 관통하는 완결된 경로 하나씩 자른다.

```
나쁨(수평): Task 1: DTO 전부 / Task 2: Repository 전부 / Task 3: Service 전부
좋음(수직): Task 1: 통계 조회 경로 (DTO+Repository+Service) / Task 2: Controller+문서 / Task 3: 통합 테스트
```

수평 금지의 기준은 "한 레이어를 여러 경로에 걸쳐 모았는가"다. 한 경로의 표면(Controller)을 뒤따르는 Task로 두는 것은 수직 경로의 순차 절단이라 무방하다.

**사용자가 수평 분해를 명시적으로 요청하면**: 근거(마지막 Task까지 아무것도 동작하지 않고 중간 커밋이 검증 불가능해짐)를 한 번 제시하고 수직 대안을 권한다. 사용자가 재차 수평을 지시하면 그것이 결정이다 — 따르되, plan 문서에 "수평 분해: 사용자 결정"을 기록한다.

### Step 3: 의존성 정렬

기반이 되는 Task를 앞에, 독립적인 Task는 병렬 가능으로 표시한다. cross-domain 결합(Facade 신설 등)이 있으면 그 계약을 별도 Task로 앞세운다.

### Step 4: 크기 검증 (검산)

- 8개 이상 파일을 건드리는 Task → 관절을 잘못 찾은 신호. 재분해한다
- 제목에 "and"/"및"이 들어가는 Task → 아마 두 개다
- 수용 기준을 3불릿 이내로 못 쓰는 Task → 너무 넓다

크기 라벨: S(1-3파일) / M(4-7파일). L은 존재해선 안 된다.

### Step 5: Tasks 기록

plan 문서에 Tasks 섹션을 채운다.

```markdown
### Task N: [제목]
- Acceptance: [관찰 가능하고 검증 가능한 조건 — 이것이 계약이다]
- Verification: [실행할 명령 또는 확인 방법]
- Files (advisory): [예상 파일 — 참고 추정일 뿐, 계약이 아니다]
- Depends: [선행 Task 번호 | 없음 — '없음'끼리는 병렬 가능]
- Size: [S | M]
- Status: [ ] not done
```

2-3개 Task마다 Checkpoint를 삽입한다:

```markdown
### Checkpoint: after Tasks N-M
- [ ] 컴파일 통과 / 단위 테스트 통과 / ArchUnit 룰 통과
```

`Files:`는 advisory다 — 계획 시점의 파일 예측은 낡는다. Task의 계약은 Acceptance뿐이다.

마지막 Task 뒤에는 Checkpoint를 두지 않는다 — `/implement` Phase 4의 `/verify`가 그 역할을 한다. 총 Task가 2개 이하면 Checkpoint를 생략해도 된다.

### Step 6: 게이트 (모드 분기)

- **step-by-step**: 태스크 목록 요약(개수·크기 분포·의존 순서)을 제시하고 턴을 끝낸다. 사용자의 승인 메시지가 오면 그것이 곧 `/implement` 진행 허가다 — 재차 제안만 하고 멈추지 않는다. 승인 전 `/implement` 진행 금지. **승인이 거부되면**: 거부 사유를 받아 Step 2부터 재분해한다. 사유가 분해 방식이 아니라 요구사항 자체라면 재개봉 프로토콜로 `/define`에 돌린다.
- **delegated (scope에 plan 포함)**: 승인 권한은 define 게이트에서 이미 위임받았다. 목록을 체크포인트 보고로 남기고 다음 단계로 계속한다. 단, 분해 중 가정을 깨는 발견이 있었다면 stop-condition 1번(재개봉)이 우선한다.

## Common Rationalizations

| 합리화 | 현실 |
|--------|------|
| "태스크가 뻔하다" | 적어라. 명시된 태스크가 숨은 의존성과 빠뜨린 엣지를 드러낸다. |
| "머릿속에 다 있다" | 컨텍스트는 유한하다. 문서만이 세션과 컴팩션을 넘어 살아남는다. |
| "레이어별로 나누는 게 깔끔하다" | 수평 분해는 마지막 Task까지 아무것도 동작하지 않는 분해다. 수직으로 잘라라. |

## Red Flags

- Acceptance 없이 "기능 구현"이라고만 쓴 Task
- Verification 명령이 없는 Task
- 8개 이상 파일이 예상되는 Task를 재분해 없이 둠 (S/M 어느 라벨도 붙일 수 없는 상태)
- 파일 목록을 계약처럼 취급 (advisory임을 잊음)
- define에 없는 요구사항을 분해 중에 창조

## 종료

지침의 **GSL Execution Mode** 규칙을 따른다. step-by-step이면 목록 제시 후 턴을 끝내고, 이후 도착한 승인 메시지가 곧 `/implement` 진행 허가다 (Step 6과 동일 규칙). delegated면 보고를 남기고 계속한다.
