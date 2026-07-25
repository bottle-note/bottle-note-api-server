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

- mode: step-by-step (Assumption 10, 승인 시 확정)

## Tasks

(/plan에서 작성)

## Progress Log

(비어 있음 — /implement에서 기록)
