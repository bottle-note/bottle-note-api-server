---
name: implement
description: |
  승인된 Tasks를 Slice 단위로 얇게 구현하고 Task 단위로 커밋한다. Execution Mode에 따라 Task별 정지 또는 자율 연속 진행한다.
  Trigger: "/implement", 또는 사용자가 "API 추가", "기능 구현", "기능 개발", "build this"라고 할 때.
  상태 기반: plan/{기능}.md에 미완료 Task가 있을 때. Controller/Service/Repository/Facade 등 다중 파일 구현 작업일 때.
  Java/Spring 패턴은 references/languages/java-spring.md, 프로젝트 특화 패턴은 bottlenote-patterns.md를 따른다.
argument-hint: "[feature-name or task number]"
---

# Implement — Slice 실행과 Task 커밋

References (코딩 전에 해당 항목을 읽는다):
- `references/languages/java-spring.md` — Java/Spring 구현 패턴 (product-api·mono)
- `references/languages/bottlenote-patterns.md` — 프로젝트 특화 패턴 (InMemory 갱신 체크리스트 등)
- `references/types/web-api.md` — API 계층 패턴 / `references/types/batch.md` — batch 작업 시

## 철학: Slice는 에이전트 전권

Task는 `/plan`에서 승인된 계약(관절)이고, 그 안에서 어떻게 썰어 실행할지는 이 스킬의 재량이다. 단 하나의 규칙: **~100줄을 넘기기 전에 컴파일 체크한다.** Slice 1의 버그는 Slice 2~5를 전부 오염시키므로, 얇게 썰고 자주 확인한다.

```
Task  = 커밋 단위 (승인된 계약 — 변경하려면 /plan 재개봉)
Slice = 컴파일 체크 단위 (에이전트 재량 — 어떻게 썰든 자유)
```

## 전제 (하드 게이트)

`plan/{feature-name}.md`에 승인된 Tasks가 있어야 한다. 없으면 **정지**하고 `/define` → `/plan`을 안내한다. 시작 시 같은 문서의 `## Execution Mode` 섹션에서 mode·scope·stop-conditions를 읽는다 — 선언이 없으면 step-by-step이다.

유일한 예외: 자명한 단일 파일 수정(오타, 리네임, 한 줄 수정) — 이 경우 최소 slice + 약식 리뷰 + 컴파일 체크로 바로 처리한다. **다중 파일 작업은 승인된 plan 없이 절대 진행하지 않는다.**

## Process

### Phase 0: 탐색

코드를 쓰기 전에 파악한다: 대상 모듈·패키지 위치, 재사용할 기존 Service/Repository/Facade, 이미 있는 것과 새로 만들 것의 구분. 그리고 영향을 보고한다 — cross-domain 결합(Facade 신설·수정 시 InMemory 테스트 더블 갱신 필요), 스키마 변경(엔티티를 바꾸면 Flyway 마이그레이션 필수 — `ddl-auto: validate`라 누락 시 기동 실패), 이벤트, 캐시.

### Phase 1: 코어

레이어 순서: 엔티티(신규 시) → 도메인 레포지토리(포트) → JPA 구현 → DTO(record) → 예외 → Service → cross-domain 필요 시 Facade 인터페이스+구현. 네이밍·어노테이션은 지침과 references가 정의하며 ArchUnit이 강제한다.

### Phase 2: 표면

Controller — 경로, 인증(`@SecurityPolicy`), 요청 검증(`@Valid`), 응답(`GlobalResponse`), 페이징(`PageResponse`/`CursorPageable`). Controller는 얇게 — 비즈니스 로직은 Service 소유다.

### Phase 3: Task 사이클

Task 하나마다:

1. Slice 구현 → 컴파일 체크 (`./gradlew compileJava -q` 수준) → 통과할 때까지 수정
2. Task의 모든 Slice 완료 → 5축 약식 리뷰(정확성·가독성·아키텍처·보안·성능 — 전면 리뷰가 필요하면 `/self-review`를 별도 호출로 권고)
3. 단위 테스트 실행 (Task 곁에 테스트 코드를 쓰는 것은 허용; `/test`의 풀 워크플로는 별도 경계)
4. 커밋 — 제목은 Task, 본문 불릿은 Slice. 커밋 메시지는 한국어(프로젝트 관례)
5. plan 문서 갱신: Status 체크, Progress Log에 한 단락 기록
6. **모드 분기**:

| Execution Mode | Task 완료 후 행동 |
|---|---|
| **step-by-step** | 정지. 완료 Task·검증 증거·변경 파일·다음 Task를 보고하고 턴을 끝낸다. 예외: 사용자가 연속 실행을 명시한 경우("Task 1~3", "결과까지")에만 계속. 범위 없는 재개 신호("계속", "고")는 **다음 Task 1개만** 허가로 간주한다 |
| **delegated** (scope에 implement 포함) | 체크포인트 보고를 남기고 다음 Task로 계속한다 — Progress Log는 plan 문서에, 진행 요약은 대화에 즉시 출력해 사용자가 실시간 확인할 수 있게 한다 |

**stop-conditions는 모드와 무관하게 우선한다** (지침의 GSL Execution Mode 참조): 가정을 깨는 발견 → 즉시 정지, 재개봉 프로토콜. verify 3회 실패 → `/debug` 보고 후 정지. scope 밖 행동 필요 → 직전 정지. 정지 시 보고 항목: 발동한 조건, 발견 내용, 깨진 가정(해당 시), 제안 조치. 이미 커밋된 Task는 되돌리지 않고 보고에 포함한다 — 재승인 결과에 따라 후속 Task로 수정한다.

### Phase 4: 마감

모든 Task 커밋 후 생명주기 꼬리는 이 순서다: **통합 테스트(`/test`, 필요 시) → `/verify full` → plan 문서 완료 스탬프·`plan/complete/` 이동 → push → PR**. 마감(스탬프·이동)은 verify PASS 뒤에 온다.

- **step-by-step**: 여기서 정지하고 `Next: /test`(통합 테스트가 필요하면) 또는 `Next: /verify full`을 제안한다. `/test`·`/verify`의 풀 워크플로는 별도 호출 경계다 — 이 스킬 안에서 실행하지 않는다. 마감·푸시·PR은 verify PASS 후 사용자 지시로 진행한다.
- **delegated**: scope에 선언된 단계를 위 순서대로 이어 실행한다 (위임 계약의 이행 — 지침의 격리 규칙 예외). scope의 `test`는 `/test`의 시나리오 승인 게이트를 체크포인트 보고로 대체해 수행하고, `verify`는 full까지 돌린다. 마감(스탬프·`plan/complete/` 이동)은 scope 항목이 아니라 verify PASS 후 항상 수행하는 생명주기 꼬리다. `push`/`pr`는 선언 시에만 — **PR 본문이 체인지로그를 겸한다** (변경 사항·배경·검증·제외 범위 구조). 미선언이면 해당 직전에서 정지하고, 남은 단계와 검증 상태(full 통과 여부)를 보고한다.

## Common Rationalizations

| 합리화 | 현실 |
|--------|------|
| "한 번에 다 만들고 마지막에 테스트" | 버그는 복리다. Slice마다 컴파일 체크해라. |
| "Controller에 로직 조금 넣어도" | 표면은 얇게. 로직은 Service 소유다. 항상. |
| "리팩토링도 겸사겸사" | 기능과 리팩토링을 섞으면 둘 다 리뷰·디버그가 어려워진다. 분리해라. |
| "인터페이스만 바꾸고 Fake는 나중에" | 포트를 바꾸면 InMemory 구현도 같은 Task에서 갱신한다 (bottlenote-patterns.md 체크리스트). |

## Red Flags

- 100줄 넘게 쓰고 컴파일 체크 없음
- 타 도메인 Service/Repository 직접 참조 (Facade 경유 위반 — ArchUnit이 잡지만 그 전에 스스로 잡아라)
- 승인된 plan 없이 다중 파일 작업 시작
- 엔티티 변경에 Flyway 마이그레이션 누락
- step-by-step인데 명시 허가 없이 다음 Task 진행
- delegated인데 stop-condition을 무시하고 계속 진행
- Task 커밋 없이 여러 Task를 한 커밋에 뭉침

## 종료

지침의 **GSL Execution Mode** 규칙을 따른다. step-by-step이면 Task 보고 후 턴 종료, 다음은 `Next: /implement (Task N+1)`, 전 Task 완료 시에는 Phase 4의 순서(`/test` → `/verify full` → 마감)를 안내한다. delegated면 Phase 4를 scope대로 이어 실행한다.
