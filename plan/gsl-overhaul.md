# Plan: GSL 스킬셋 개편

## Overview

GSL(Guided Software Lifecycle) 9개 스킬을 이 저장소 전용으로 대폭 개편한다. 범용 프레임워크의 무게를 걷어내고, 두 가지 설계 철학을 중심축으로 재작성한다.

**철학 1 — WHAT/HOW 분리.** `/define`(무엇을)과 `/plan`(어떻게)은 합치지 않는다. WHAT은 사람의 의도를 끌어내야 해서 재생성이 비싸고, HOW는 에이전트가 다시 짜면 되므로 재생성이 싸다. 사람의 협의 비용은 WHAT에 집중 투자한다. WHAT이 명확히 협의되면 이후 실패는 HOW의 실패로 격리되어 회복 가능해진다.

**철학 2 — Task/Slice 이원화 (정육 모델).** Task는 문제의 자연 관절(의존성·모듈 경계·리뷰 단위)을 따라 자르는 부위 분해로, 올바른 답이 적고 틀리면 비싸므로 사용자 승인 대상이다. Slice는 그 부위를 어떻게 썰지에 대한 실행 선택으로, 정답이 여럿이고 틀려도 싸므로 에이전트 전권이다. 순서는 분해 → 의존성 정렬 → 크기 검증이며, 크기는 절단 기준이 아니라 검산이다.

**신규 축 — Execution Mode (define 후 위임 계약).** define 승인 게이트를 계약 서명으로 격상한다. WHAT 승인 시 이후 단계의 위임 수준을 함께 선언하고 plan 문서에 기록한다. 기본값은 step-by-step(현행 단계별 승인)이며, delegated 선언 시 plan→implement→test→verify→commit(→push→PR·체인지로그, scope에 따라)을 자율 진행하되 stop-conditions(가정 붕괴, verify 반복 실패, 선언 범위 밖 파괴적 행동)에서는 무조건 멈춘다.

### Assumptions

1. GSL은 이 저장소 전용이다. 범용성(타 언어 references, Polyglot Mode, Superpowers 연동)은 제거한다. 다른 프로젝트에 쓰려면 그때 복사해 변형한다. — 사용자 확정
2. Execution Mode 기본값은 step-by-step이다. delegated는 define 승인 시 명시적으로 선언한 경우에만 적용된다. — 사용자 확정
3. `/next-flow`는 삭제한다. 위임 모드가 마찰 문제를 근본 해결하고, 다음 명령 제안은 각 스킬 종료 보고에 이미 있다. — 사용자 확정
4. `/scan-conventions`는 완전 은퇴한다. 스킬 삭제, `plan/conventions.md` 정리. 관습의 유일 소유자는 CLAUDE.md/AGENTS.md다. `/define`의 하드 게이트는 conventions.md 존재 확인 대신 지침 존재로 대체한다. — 사용자 확정
5. `.claude/skills` ↔ `.agents/skills` 바이트 동일 미러 규칙은 유지한다. 모든 변경은 양쪽에 짝으로 적용한다.
6. 지침(CLAUDE.md/AGENTS.md)의 "GSL Runtime Boundary Rules" 섹션은 개편 후 Execution Mode 규칙으로 대체된다. HARD STOP 보일러플레이트(15줄×9회)는 지침의 단일 섹션으로 통합하고 스킬에는 한 줄 참조만 남긴다.
7. plan 문서 생명주기(`plan/` → `complete/`/`stale/`)와 "기능 하나 = 문서 하나" 원칙은 유지한다.
8. 유지 대상: Common Rationalizations 표, Fake/InMemory-first 정책, verify L1/L2/L3, 수직 슬라이스 원칙.
9. 이 개편 작업 자체는 현 브랜치(`chore/sync-agent`)가 아닌 별도 브랜치·별도 PR로 진행한다. PR #672는 이미 11커밋 규모라 더 얹지 않는다. — 승인 시 확정 필요
10. 이 개편 작업 자체의 Execution Mode는 step-by-step이다. — 승인 시 확정 필요

### Success Criteria

- 스킬 수 9 → 7 (`next-flow`, `scan-conventions` 삭제, 양쪽 미러)
- references에서 `python.md`, `go.md`, `cli.md`, `library.md`, `worker-daemon.md` 제거. 잔존은 `java-spring.md`, `bottlenote-patterns.md`, `web-api.md`, `batch.md`, 테스트 `java.md`, verify `java-gradle.md`
- 전체 스킬에서 "Superpowers" 언급 0건, "Polyglot" 언급 0건
- HARD STOP 보일러플레이트가 스킬 본문에서 제거되고 지침의 단일 섹션 참조로 대체됨
- `/define`에 Execution Mode 계약 정의: 선언 형식(mode/scope/push/pr/stop-conditions), plan 문서 기록 위치, 승인 게이트 문구
- `/define`에 재개봉 프로토콜 명시: 구현 중 가정이 깨지면 STOP → define 수정 → 재승인 (WHAT 변경 시)
- `/plan`이 분해 → 의존성 정렬 → 크기 검증 순서로 재작성됨. Task 계약은 수용 기준이며 `Files:`는 참고 추정(advisory)으로 강등
- `/implement`에 delegated 모드 분기: HARD STOP 대신 체크포인트 보고(Progress Log 기록 + 진행 보고), stop-conditions 준수
- PR 오픈·체인지로그 작성이 생명주기 공식 단계로 정의됨 (이 저장소의 "PR 본문 = 체인지로그" 관례 명시, delegated scope 항목)
- 각 스킬 description에 발화 트리거 외 산출물·상태 기반 트리거 추가
- 검증: `diff -rq .agents/skills .claude/skills` 일치, 지침 정규화 diff 일치, 지침 Skills 표가 7개 스킬과 정합
- 스킬 전체 용량이 현행 약 197KB에서 절반 이하로 감소

### Impact Scope

- `.claude/skills/` + `.agents/skills/` (미러 양쪽): 스킬 삭제 2종, 재작성 7종(define/plan/implement/test/verify/debug/self-review), references 삭제 약 10파일
- `CLAUDE.md` / `AGENTS.md`: GSL Runtime Boundary Rules 섹션 → Execution Mode 규칙으로 대체, Skills 표 9→7 갱신, `/define` 게이트 서술 변경
- `plan/conventions.md`: 삭제 또는 `stale/` 이동
- 운영 코드(Java/Kotlin) 변경 없음. Gradle 빌드·테스트 영향 없음
- 브랜치: 별도 브랜치·별도 PR (Assumption 9)

## Execution Mode

- mode: step-by-step (Assumption 10, 확정)
- 검증 방법론: 서브에이전트 시나리오 검증 (사용자 지시로 추가)

## 검증 방법론 — 서브에이전트 시나리오 검증

재작성된 스킬 텍스트가 실제로 의도한 행동을 유도하는지, 합성 시나리오를 만들어 서브에이전트로 롤플레이 검증한다.

- **방식**: 서브에이전트에게 재작성된 SKILL.md 전문과 가상의 기능 요청(예: "시음 노트에 온도 기록 추가")을 주고, 그 스킬을 따르는 에이전트로서 어떻게 행동할지 서술하게 한다. 지시가 모호한 곳, 상충하는 곳, 폭주(승인 없이 진행)를 허용하는 구멍을 보고받는다.
- **함정 시나리오 포함**: 정상 경로만이 아니라 위반 유도 시나리오를 섞는다 — 수평 분해를 유도하는 요청, 애매한 "계속해줘", 가정이 깨지는 중간 발견, delegated 모드에서 stop-condition 상황.
- **수용 기준**: 치명 결함(승인 게이트 우회 가능, stop-condition 무시 가능, 지시 상충) 0건. 경미한 모호함은 기록 후 수정 여부 판단.
- 서브에이전트는 스킬 텍스트만 보고 판단한다 (이 대화의 맥락 없이) — 신규 세션이 읽었을 때의 실효성을 검증하는 것이 목적이다.

## Tasks

### Task 1: 삭제 — 스킬 2종·미사용 references·conventions 은퇴
- Acceptance: `next-flow`/`scan-conventions` 스킬이 미러 양쪽에서 삭제됨. references에서 python/go/cli/library/worker-daemon 삭제. `plan/conventions.md` 은퇴(stale 이동). 잔존 스킬 7종은 아직 수정 전이어도 무방
- Verification: `diff -rq .agents/skills .claude/skills` 일치, 삭제 대상 파일 0건 확인, 잔존 스킬에서 삭제된 스킬 참조가 깨지는 지점 목록화(다음 Task 입력)
- Files (advisory): 미러 양쪽 약 24파일 삭제 + plan/conventions.md 이동. 브랜치 분리(`chore/gsl-overhaul` 생성, define 커밋 이관) 포함
- Size: M
- Status: [x] done

### Task 2: Execution Mode 계약 + 지침 갱신
- Acceptance: 지침(CLAUDE.md/AGENTS.md)의 "GSL Runtime Boundary Rules" 섹션이 Execution Mode 규칙(기본 step-by-step, delegated 선언 형식, stop-conditions, HARD STOP 통합 규정)으로 대체됨. Skills 표 9→7. PR 본문=체인지로그 관례가 delegated scope 정의에 포함됨
- Verification: 지침 정규화 diff 일치, 표의 7개 커맨드가 실제 스킬 디렉터리와 정합
- Files (advisory): CLAUDE.md, AGENTS.md
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 1-2
- [x] 미러 diff 일치, 지침 diff 일치
- [x] 잔존 스킬의 깨진 참조 목록 확보

### Task 3: /define 재작성
- Acceptance: WHAT/HOW 분리 철학 명문화, 하드 게이트를 conventions.md → 지침 존재로 교체, 승인 게이트에 Execution Mode 선언 절차 추가, 재개봉 프로토콜(가정 붕괴 시 STOP→수정→재승인) 명시, HARD STOP 보일러플레이트 제거(지침 참조 한 줄), 트리거에 상태 기반 조건 추가
- Verification: 서브에이전트 시나리오 2건 — (a) 정상: 신규 기능 요청 → 가정·성공기준 도출 및 모드 선언까지, (b) 함정: 모호한 요청에 가정 확인 없이 진행하도록 유도 → 게이트가 막는지
- Files (advisory): define/SKILL.md × 미러 2
- Size: S
- Status: [x] done

### Task 4: /plan 재작성
- Acceptance: 분해(자연 관절)→의존성 정렬→크기 검증 순서로 재구성, Task=승인 대상/Slice=에이전트 전권 구분 명문화, `Files:`를 advisory로 강등, Superpowers 블록 제거, HARD STOP 제거
- Verification: 서브에이전트 시나리오 2건 — (a) 정상: Task 3 시나리오의 define 산출물로 분해, (b) 함정: 수평 분해가 자연스러워 보이는 요청 → 수직 강제가 작동하는지
- Files (advisory): plan/SKILL.md × 미러 2
- Size: S
- Status: [x] done

### Task 5: /implement 재작성
- Acceptance: Execution Mode 분기 구현 — step-by-step은 Task별 HARD STOP 유지, delegated는 체크포인트 보고(Progress Log 기록+진행 보고)로 대체하되 stop-conditions 준수. Polyglot Mode·언어 fallback 제거, java-spring 고정. PR·체인지로그 단계를 Phase 4 이후 공식 꼬리로 정의
- Verification: 서브에이전트 시나리오 3건 — (a) step 모드 Task 완료 후 정지 확인, (b) delegated 모드 연속 진행+보고 확인, (c) 함정: delegated 중 define 가정이 깨지는 발견 → stop-condition 발동 확인
- Files (advisory): implement/SKILL.md × 미러 2
- Size: M
- Status: [ ] not done

### Checkpoint: after Tasks 3-5
- [ ] 미러 diff 일치
- [ ] 시나리오 치명 결함 0건 (발견 시 해당 Task로 되돌아가 수정)

### Task 6: test/verify/debug/self-review 정리
- Acceptance: 4개 스킬에서 HARD STOP 보일러플레이트 제거→지침 참조, Superpowers·타 언어 분기 제거, java 고정, 트리거에 상태 기반 조건 추가. 워크플로 본문은 유지(이미 유효)
- Verification: 4파일×미러 grep — "Superpowers" 0건, "HARD STOP" 상세 블록 0건, python/go 분기 0건
- Files (advisory): 4 SKILL.md × 미러 2 = 8파일
- Size: M
- Status: [ ] not done

### Task 7: 종합 검증 및 마감
- Acceptance: Success Criteria 13개 전 항목 체크리스트 대조 통과. E2E 시나리오 2건(step-by-step 전체 사이클 1건, delegated 전체 사이클 1건) 치명 결함 0건. 용량 측정 기록
- Verification: `diff -rq` 미러 일치, 지침 정규화 diff 일치, `du -sk` 전후 비교, E2E 서브에이전트 보고서
- Files (advisory): 수정 없음(검증 전용), 필요 시 발견 결함 수정
- Size: S
- Status: [ ] not done

## Progress Log

- Task 1: `chore/gsl-overhaul` 브랜치 분리(sync-agent는 origin으로 원복). next-flow·scan-conventions 스킬과 python/go/cli/library/worker-daemon references를 미러 양쪽에서 삭제(18파일), conventions.md는 stale로 이동. 삭제 전 용량 276KB/미러. 잔존 참조 44건을 9개 SKILL.md + web-api.md·bottlenote-patterns.md에서 확인 — SKILL.md는 Tasks 3-6 재작성에서, references 2개는 Task 6에서 해소 예정. 미러 diff 일치.
- Task 2: 지침 양쪽의 "GSL Runtime Boundary Rules"를 "GSL Execution Mode" 섹션으로 교체. step-by-step 기본·delegated 선언 형식·stop-conditions 3종(가정 붕괴→재개봉, verify 3회 실패, scope 밖 행동)·PR 본문=체인지로그 관례를 명문화. 지침 정규화 diff 일치, Skills 표 7행이 실제 스킬 7종과 정합. Checkpoint 1-2 통과.
- Task 3: /define 재작성 — WHAT/HOW 분리 철학, 지침 기반 하드 게이트, Execution Mode 선언 게이트(계약 서명), 재개봉 프로토콜, 상태 기반 트리거. 시나리오 검증(scenario-define): 정상 경로 완주 확인, 함정("가정 확인 넘어가고") 게이트 우회 불가 판정, 치명 0건·경미 7건. 경미 중 5건 반영(합리화 표에 외부 압박 행 추가, mode 플레이스홀더, stop-conditions 3종 인라인, Step 3 차단 문구, delegated 종료 분기·push/pr 고지). 188행 → 140행.
- Task 4: /plan 재작성 — 정육 모델(Task=관절=승인 대상, Slice=썰기=에이전트 전권), 분해→정렬→크기검산 순서, Files advisory 강등, Depends 필드 신설. 시나리오 검증(scenario-plan): 정상 완주 확인, 치명 3건 검출(재개봉 프로토콜 미정의 / 사용자 수평 요청 충돌·승인 거부 분기 부재 / 중단 지점이 Step 6에만). "가정 붕괴 시 즉시 중단" 전역 규칙 신설 + 수평 요청 특칙(1회 권고→재지시 시 사용자 결정) + 승인 거부 분기로 수정, 재검증 결과 3건 전부 해소·신규 치명 0건. 경미 4·5·6·8도 반영.
