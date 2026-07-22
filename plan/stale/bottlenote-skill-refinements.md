# Plan: bottle-note 특화 스킬 보강 (GSL 이식 후 잔여 3 항목)

## Overview

`plan/stale/claude-skill-improvements.md` 에서 분리된 bottle-note 특화 잔여 항목을 별도 plan 으로 재정의한다.

배경: 2026-05-13~14 에 `dev-cycle-skills` GSL 9 스킬셋 추출·이식 작업 (커밋 `e40d1c98`) 이 완료되면서 원본 plan 의 P1(스킬 간 자동 연결) / P2(plan 마무리 자동화) 는 `/next-flow` 스킬 신설 및 `/implement` Phase 4 stamp 절차로 흡수됨. 그러나 P3 / P4 / P5 는 bottle-note 의 Spring Boot + RestDocs + JPA + QueryDSL 스택에 강하게 결합된 항목으로, GSL 범용화 의도와 맞지 않아 의도적으로 제외됨.

본 plan 은 이 3 항목을 bottle-note `.claude/skills/` 내 **프로젝트 특화 보강** 단위로 다룬다. GSL 골격은 건드리지 않고 references 추가/수정 + (선택적) `/docs` 스킬 신설로 처리.

### Assumptions

1. **GSL 골격 유지**: `.claude/skills/{define,plan,implement,test,verify,debug,self-review,scan-conventions,next-flow}/SKILL.md` 9 개는 GSL 표준이므로 직접 수정하지 않음 (수정 시 `dev-cycle-skills` 원본과 drift 발생, 다음 sync 에서 손실됨).
2. **references 는 프로젝트별 보강 허용**: `.claude/skills/implement/references/languages/java-spring.md` 는 GSL 표준이지만 bottle-note 특화 보강은 별도 파일 (`bottlenote-patterns.md`) 로 분리하면 sync 안전.
3. **`/docs` 는 신규 스킬**: GSL 표준 9 개와 충돌 없음. `.claude/skills/docs/SKILL.md` 신설.
4. 변경 범위는 `.claude/skills/` 와 `.agents/skills/` 두 곳 (mirror 유지).

### Success Criteria

| # | 기준 | 검증 방법 |
|---|------|-----------|
| SC1 | `/docs` 스킬 신설 — SKILL.md + product/admin 분기 + asciidoctor 빌드 검증 명령 포함 | `.claude/skills/docs/SKILL.md` 존재, `gh skill list` 류 검증 |
| SC2 | `.claude/skills/implement/references/languages/` 또는 별도 `bottlenote-patterns.md` 에 InMemory 갱신 시 확인할 **구체 경로 2개** 명시: `bottlenote-mono/src/test/.../alcohols/fixture/InMemory*Repository.java`, `bottlenote-product-api/src/test/.../alcohols/fixture/InMemory*Repository.java` | grep |
| SC3 | 동일 references 또는 `bottlenote-patterns.md` 에 "QueryDSL `Projections.constructor()` 에 사용하는 record 는 클래스/인터페이스 레벨에 정의" 한 줄 추가 + 예시 1건 | grep |
| SC4 | `.agents/skills/` 도 동일 변경 mirror (.claude/skills/ ↔ .agents/skills/ 완전 일치) | `diff -r --brief` |
| SC5 | (선택) 풀 사이클 실전 검증: `/define → /plan → /implement → /test → /docs → /verify full → /next-flow` 가 사용자 추가 지시 없이 끝까지 진행 가능 | 검증 로그 |

### Impact Scope

**변경 대상**: `.claude/skills/` + `.agents/skills/` 디렉토리만 (코드 변경 0, DB 변경 0, 빌드 영향 0).

**파일 변경/생성 목록**

신설:
- `.claude/skills/docs/SKILL.md` (P3) — RestDocs + asciidoctor 워크플로우 가이드
- `.claude/skills/implement/references/languages/bottlenote-patterns.md` (P4 + P5) — bottle-note 특화 Spring Boot/JPA/QueryDSL 패턴 (InMemory 경로, local record 주의 등)
- `.agents/skills/...` 동일 미러

수정 없음 (GSL 표준 SKILL.md 는 건드리지 않는다는 원칙).

**비영향**: 도메인 코드, DB, 운영 환경, CI 파이프라인, Liquibase, 서브모듈, gh API 호출, GSL 원본 (`/Users/hgkim/Documents/sync/AI/claude/dev-cycle-skills/`) — 모두 영향 없음.

### 결정 필요 사항

진행 전 사용자 확정 부탁:

| # | 결정 | 옵션 | 권장 |
|---|------|------|------|
| D1 | bottle-note 특화 패턴 파일 위치 | (a) `implement/references/languages/bottlenote-patterns.md` 신규 / (b) 기존 `java-spring.md` 직접 수정 (GSL sync 시 충돌 위험) / (c) `.claude/CLAUDE.md` 의 Skills 섹션 확장 | **(a)** — GSL sync 안전 + 명확한 책임 분리 |
| D2 | `/docs` 스킬과 `/test` 의 RestDocs 책임 분리 | (a) `/test` 가 RestDocs 테스트만 작성, `/docs` 가 adoc·인덱스·asciidoctor / (b) 모두 `/docs` 에 통합 / (c) 모두 `/test` 에 통합 | **(a)** — 원본 plan Section 11 의도와 일치 |
| D3 | `/docs` 검증 명령 범위 | (a) product-api 만 / (b) admin-api 만 / (c) 양쪽 (default `:bottlenote-admin-api:test` 포함, PR #578 사례 반영) | **(c)** — 누락 방지 |
| D4 | 머지 단위 | (a) 본 plan 모든 항목 한 PR / (b) `/docs` 신설 + 패턴 보강 분리 PR | **(b)** — `/docs` 가 가장 큼 |

---

## Tasks (D1~D4 결정 후 분해)

### T0. (선결정) D1~D4 사용자 확정
- 위 표 결정 후 본 섹션 갱신, 이후 Task 진행

### T1. `bottlenote-patterns.md` 신설 (SC2, SC3)
- `.claude/skills/implement/references/languages/bottlenote-patterns.md` 작성
- 섹션 구성:
  - InMemory 갱신 체크포인트 (구체 경로 2개 + Repository interface 변경 시 점검 절차)
  - local record + QueryDSL `Projections.constructor()` 주의 + 예시 1건
  - (선택) Facade ↔ Service 경계, @ThirdPartyService 사용 시점 등 bottle-note 고유 패턴 추가
- `.agents/skills/` mirror

### T2. `/docs` 스킬 신설 (SC1)
- `.claude/skills/docs/SKILL.md` 작성
- Phase 0~4 (RestDocs 작성 → asciidoc 인덱스 갱신 → asciidoctor 빌드 → 검증 → plan 마무리)
- product/admin 분기 명시
- 검증 명령: `./gradlew asciidoctor` + `:bottlenote-admin-api:test` (default test, PR #578 사례)
- `.agents/skills/` mirror

### T3. (선택) `bottlenote-patterns.md` 를 GSL `java-spring.md` 에 link reference 로 안내
- `implement/SKILL.md` Phase 0 의 "load language reference" 단계에서 bottle-note 프로젝트 한정 `bottlenote-patterns.md` 도 함께 로드하도록 분기 안내 가능
- 단, GSL 표준 SKILL.md 수정은 D1 (a) 결정 시 피해야 하므로 — bottle-note 의 `CLAUDE.md` 또는 `scan-conventions/SKILL.md` 결과물 (`plan/conventions.md`) 에 안내 한 줄 추가하는 방식이 더 안전

### T4. 풀 사이클 실전 검증 (SC5)
- 작은 도메인(예: 간단한 GET 엔드포인트 1건) 으로 `/define → /next-flow` 끝까지 추가 지시 없이 진행되는지 검증
- 결과를 본 plan Progress Log 에 기록

## Progress Log

(implement 단계에서 채워짐)

## Stale

- Status: STALE
- T1의 `bottlenote-patterns.md`는 `.claude/skills/`와 `.agents/skills/` 양쪽에 반영됐다.
- 전용 `/docs` 스킬은 만들지 않고 RestDocs 테스트는 `/test`, adoc 갱신은 `/implement`, asciidoctor 검증은 `/verify`가 담당하는 방식으로 책임을 흡수했다.
- D1~D4 결정과 T2~T4를 원안대로 진행하지 않으므로 남은 계획은 중단된 것으로 분류한다.
- 현재 개발 워크플로우는 `.agents/skills/implement/references/languages/bottlenote-patterns.md`와 `plan/conventions.md`를 기준으로 한다.
