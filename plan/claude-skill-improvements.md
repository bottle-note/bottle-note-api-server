# Plan: Claude Skill 체계 후속 개선 (P1~P5 + /docs + /finalize)

## Overview

`plan/complete/claude-ai-harness-improvement.md` 의 Section 10(동작 검증 피드백) 과 Section 11(`/docs` 스킬 설계) 에서 도출된 후속 개선 항목을 별도 작업 단위로 추출한다. 각 항목을 **현재 상태로 팩트체크** 한 결과를 함께 표기하고, 작업 우선순위·범위·산출물을 명확히 한다.

본 plan 은 운영 중인 7 스킬(define, plan, implement, test, verify, debug, self-review) 의 행위·책임을 보강하고, 라이프사이클 빈 구간(API 문서화 / 마무리 자동화) 을 채우는 것이 목표다.

### Assumptions (확정 + 정정)

#### 확정 가정

1. **운영 중인 스킬 7개**: define, plan, implement, test, verify, debug, self-review (`/Users/hgkim/workspace/bottlenote/bottle-note-api-server/.claude/skills/` 직접 확인). 별도로 `deploy-batch` 도 존재.
2. **라이프사이클**: `/define → /plan → /implement (+ /self-review) → /test → /verify full` 가 CLAUDE.md 에 명시되어 있고 본 세션의 PR #586/#578 작업에서도 동일하게 작동.
3. **스킬 문서 언어**: SKILL.md / references 는 영어, plan 문서·@DisplayName·대화 응답은 한국어 (claude-ai-harness-improvement Section 6 정책).

#### 팩트체크 결과 (출발 plan 의 가정 정정)

4. **P1 "스킬 간 자동 연결"** — 현재 상태:
   - 각 SKILL.md 끝에 "Next: /xxx" 같은 자동 연결 안내 **미반영** (`grep "Next:"` 결과 0건).
   - 게다가 `/plan` 은 **UI 커맨드** 라 Skill 도구로 자동 호출 자체가 불가능 (본 세션에서 실제 발생: "plan is a UI command, not a skill"). 따라서 `/define → /plan` 은 **원천적으로 자동 체이닝 불가** 이며, plan 문서에 Tasks 섹션을 인라인 추가하는 우회만 가능.
   - 자동 연결 가능 구간: `/implement → /test`, `/test → /verify full`, `/verify full → 커밋·plan 마무리`. 이 3 구간만 안내 추가하면 P1 의 80%가 해결됨.

5. **P2 "plan 문서 마무리 프로세스"** — 현재 상태:
   - `/finalize` (또는 plan 마무리 전담) 스킬 **부재**.
   - 본 세션(2026-05-08) 의 plan 정리도 사용자 명시 요청("complete 찍을거 있나") 으로 시작되었으며 Skill 자동화는 0.
   - 추가로 본 세션에서 **STALE 폴더(`plan/stale/`)** 가 신설됨 → 마무리 라이프사이클이 `complete` 단일이 아니라 `complete | stale` 두 갈래로 확장됨. 이 사실이 출발 plan 의 마무리 절차 정의에 반영되어야 함.

6. **P3 "API 문서화 스킬"** — 현재 상태:
   - `/docs` 스킬 **부재** (디렉토리 없음). Section 11 의 설계가 그대로 미구현 상태.
   - 출발 plan 의 설계(Phase 0~4, product/admin 분기, asciidoctor 빌드 검증) 는 그대로 살릴 수 있으나, 본 세션의 PR #578 작업에서 **AdminDistilleryControllerDocsTest 가 admin_integration_test 가 아닌 default `:bottlenote-admin-api:test` 에서 돌아간다**는 점이 추가로 확인됨 → /docs 검증 명령에 default test 도 포함 필요.

7. **P4 "InMemory 구현체 갱신 누락"** — 현재 상태:
   - `.claude/skills/implement/SKILL.md` 의 Red Flags 에 일반 한 줄("Repository interface changed but InMemory test implementation not updated") 있음.
   - **구체적 경로 미명시**. 본 세션의 PR #578 rebase 시에도 `InMemoryDistilleryRepository.toAdminDistilleryItem` 누락 등이 또 발생 → 강화 필요.

8. **P5 "local record + QueryDSL Projections.constructor"** — 현재 상태:
   - implement references / Red Flags 모두 미반영 (`grep "local record"` 결과 0건).

9. **운영 안전**: 본 plan 은 .claude/skills 변경만 다룬다. 코드 변경/운영 DB 변경 없음. 자동 모드에서도 안전.

### Success Criteria

| # | 기준 | 검증 방법 |
|---|------|-----------|
| SC1 | `/implement`, `/test`, `/verify`, `/self-review` SKILL.md 끝 Verification 또는 Next-Step 섹션에 다음 스킬 안내 1줄 추가 | grep `Next:` |
| SC2 | `/implement` Phase 4(Final Verification) 직후에 plan 마무리 절차 (스탬프 생성·복사 → `plan/complete/` 이동) 가 명시되거나, 별도 `/finalize` 스킬 신설 | SKILL.md diff |
| SC3 | `/finalize` (또는 동등 절차) 가 **STALE 분류** 도 인식 — 작업이 자연 폐기·후속 plan 분리된 경우 `plan/stale/` 로 이동하는 분기 포함 | SKILL.md / 절차 문서 |
| SC4 | `/docs` 스킬 신설: SKILL.md + Phase 0~4 절차 + product/admin 분기 + asciidoctor 빌드 검증 명령 포함 | `.claude/skills/docs/SKILL.md` 존재 + `gh skill list` 류 검증 |
| SC5 | `/implement` Red Flags 또는 references 에 InMemory 갱신 시 확인할 **구체 경로 2개** 명시: `bottlenote-mono/src/test/.../alcohols/fixture/InMemory*Repository.java`, `bottlenote-product-api/src/test/.../alcohols/fixture/InMemory*Repository.java` | grep |
| SC6 | `/implement` references 또는 Red Flags 에 "QueryDSL `Projections.constructor()` 에 사용하는 record 는 클래스/인터페이스 레벨에 정의" 한 줄 추가 | grep |
| SC7 | 풀 사이클 실전 검증 1회 (간단한 도메인) — `/define → /plan → /implement → /test → /docs → /verify full → /finalize` 가 사용자 추가 지시 없이 끝까지 진행 가능 | 검증 로그 |

### Impact Scope

**변경 대상**: `.claude/skills/` 디렉토리만 (코드 변경 0, DB 변경 0, 빌드 영향 0).

**파일 변경/생성 목록**

신설:
- `.claude/skills/docs/SKILL.md` (P3)
- `.claude/skills/finalize/SKILL.md` (P2 — `/finalize` 또는 implement Phase 4 흡수 중 택1)

수정:
- `.claude/skills/implement/SKILL.md` — Phase 4 plan 마무리 절차 + Next-Step + InMemory 경로 + local record 주의 (SC1, SC2, SC5, SC6)
- `.claude/skills/implement/references/mono-patterns.md` — local record + QueryDSL 주의사항 (SC6)
- `.claude/skills/test/SKILL.md` — Next-Step → /docs 또는 /verify full (SC1)
- `.claude/skills/verify/SKILL.md` — Next-Step → /finalize 또는 plan 마무리 안내 (SC1)
- `.claude/skills/self-review/SKILL.md` — Next-Step → 커밋 (SC1)
- `.claude/skills/define/SKILL.md` — `/plan` 이 UI 커맨드라 Skill 도구로 자동 호출 불가하다는 한계와 우회법(plan 문서 인라인) 명시 (Assumption #4 정정 반영)

**비영향**: 도메인 코드, DB, 운영 환경, CI 파이프라인, Liquibase, 서브모듈, gh API 호출 — 모두 영향 없음.

### 결정 필요 사항

진행 전 사용자 확정 부탁:

| # | 결정 | 옵션 | 권장 |
|---|------|------|------|
| D1 | 마무리 자동화 형태 | (a) `/implement` Phase 4 확장 흡수 / (b) 별도 `/finalize` 스킬 신설 | **(b)** — Phase 분리가 명확하고 STALE 분기 처리에 유리 |
| D2 | `/docs` 와 `/test` 의 RestDocs 책임 분리 | (a) `/test` 가 RestDocs 테스트만 작성, `/docs` 가 adoc·인덱스·asciidoctor / (b) 모두 `/docs` 에 통합 / (c) 모두 `/test` 에 통합 | **(a)** — 현재 출발 plan Section 11 의도와 일치, 책임 깨끗 |
| D3 | Next-Step 안내 형식 | (a) Verification 섹션 마지막 줄 / (b) 별도 Next 섹션 / (c) 양쪽 | **(b)** — 가시성 좋음 |
| D4 | 머지 단위 | (a) 본 plan 모든 항목 한 PR / (b) /docs 신설은 별도 PR / (c) 항목별 PR | **(b)** — /docs 가 가장 큼 |

---

## Tasks (D1~D4 결정 후 분해)

### T0. (선결정) D1~D4 사용자 확정
- 위 표 결정 후 본 섹션 갱신, 이후 Task 진행

### T1. SKILL.md 5건 Next-Step 안내 추가 (SC1)
- 대상: implement, test, verify, self-review, define
- 각 SKILL 끝에 1~3줄 Next 섹션. /plan 이 UI 커맨드라는 한계 명시(define 만)

### T2. /implement InMemory 경로 + local record 주의 추가 (SC5, SC6)
- `.claude/skills/implement/SKILL.md` Red Flags + references/mono-patterns.md 수정

### T3. /finalize 스킬 신설 (SC2, SC3) — D1 (b) 선택 시
- 라이프사이클: /verify full 통과 → 스탬프 생성 → `plan/complete/` 또는 `plan/stale/` 이동 → 커밋 → 사용자 확인
- STALE 분기: 작업이 자연 폐기됐거나 후속 plan 으로 분리된 경우

### T4. /docs 스킬 신설 (SC4)
- 출발 plan Section 11 의 Phase 0~4 그대로 차용
- product/admin 분기 + asciidoctor 빌드 검증 명령 포함
- default `:bottlenote-admin-api:test` 도 검증 단계에 포함 (PR #578 사례 반영)

### T5. 풀 사이클 실전 검증 (SC7)
- 작은 도메인(예: 간단한 GET 엔드포인트 1건) 으로 /define → /finalize 끝까지 추가 지시 없이 진행되는지 검증
- 결과를 본 plan Progress Log 에 기록

### T6. (선택) `/finalize` 가 흡수하지 못하는 잔여 자동화 식별
- 예: PR 코멘트 답글, CI 트래킹 — 본 세션에서 모두 수동 진행됨. 자동화 가치/비용 평가 후 별도 plan 후보화

## Progress Log

(implement 단계에서 채워짐)
