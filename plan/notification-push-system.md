# Plan: Notification / Push 시스템 (이슈 #311)

## Overview

앱/웹 알림의 기준 데이터를 `Notification`으로 두고(SSOT), 사용자가 알림함에서 조회·읽음 처리할 수 있게 한다.  
장기 목표는 웹 SSE · 모바일 Push 독립 전달이지만, **1차 범위는 Notification 저장(SSOT)과 조회/읽음 API**에 한정한다.

관련 이슈: `bottle-note/workspace#311`  
설계 원문: 로컬 `bottlenote-notification-push-design.md` (ChatGPT 공유 정리본)

### 장기 방향 (1차 구현 아님, 아키텍처 가이던스)

- Notification = 무엇을 누구에게 알릴 것인가 (SSOT)
- Delivery = SSE / Mobile Push 등 채널별 전달 (Notification과 책임 분리)
- Web Push 사용 안 함
- SSE와 Mobile Push는 채널별 독립 판정
- 모바일 Foreground 억제는 Flutter 책임
- 초기 SSE Registry는 Spring 내부 메모리; 멀티 인스턴스는 이후 Redis Pub/Sub 등

### Assumptions

#### 확정 가정 (사용자 확인 반영)

1. 이번 작업의 **1차 구현 범위**는 Notification SSOT + 알림함 API다. **SSE · Device Token · Mobile Push · FCM 재도입은 1차 제외**다.
2. **admin-api 관리자 알림 발송 API는 1차 제외**다.
3. 기존 `notifications` 테이블과 `app.external.notification` 패키지를 **재사용·진화**한다. (전면 drop 후 재생성 아님)
4. 대상 모듈은 **product-api + mono** 중심이다. batch 변경은 1차 불필요를 전제로 한다.
5. 알림 조회·읽음 API는 **인증 사용자 본인 데이터만** 대상으로 한다.
6. 최소 생성 트리거는 **리뷰 댓글 알림 1종**이다. (설계 9.1 — 서비스 활동 알림)
7. Delivery 채널이 없어도 Notification은 DB에 남아 알림함 조회로 확인 가능하다.
8. Web Push는 사용하지 않는다. (장기·1차 공통)
9. 마케팅 캠페인 대량 발송, 야간/방해금지, NotificationDelivery 이력, Outbox, 멀티 인스턴스 SSE 라우팅은 1차 제외다.
10. Flutter 앱 코드 변경은 서버 범위 밖이다.
11. 스키마가 엔티티/요구와 어긋나면 Flyway 마이그레이션으로 진화한다. (서브모듈 `git.environment-variables`)
12. 과거 푸시 제거(plan `remove-push-feature`)로 FCM 코드는 없으며, 1차에서는 재도입하지 않는다. `user_device_tokens` / `user_push_configs` 테이블 drop도 1차에서 하지 않는다.

#### 후속 페이즈로 넘기는 가정 (참고)

- Phase 2+: SSE Connection Registry, Mobile Push(Device Token + FCM), 채널 독립 Delivery, 마케팅 동의 Policy 연동 발송, admin 발송, Delivery 이력/재시도

### Success Criteria

| # | 기준 | 검증 방향 |
|---|------|-----------|
| SC1 | 리뷰 댓글 생성 시 리뷰 작성자 대상 Notification이 DB에 저장된다 | unit/integration |
| SC2 | 인증 사용자가 본인 알림 목록을 조회할 수 있다 | API + integration |
| SC3 | 인증 사용자가 미읽음 개수를 조회할 수 있다 | API + integration |
| SC4 | 인증 사용자가 단건 읽음 처리할 수 있다 | API + integration |
| SC5 | 인증 사용자가 전체 읽음 처리할 수 있다 | API + integration |
| SC6 | 미인증 호출은 거부된다 (401 등 기존 보안 정책) | API test |
| SC7 | 타 사용자 알림 조회·읽음은 불가하다 | unit/integration |
| SC8 | Delivery(SSE/Push) 없이도 알림함 API만으로 SC1~SC5가 성립한다 | 수동/테스트 |
| SC9 | 관련 compile 및 unit/integration 테스트가 통과한다 | `/verify` |

### Impact Scope

**조사 결과 (현재 코드)**

- mono: `app.external.notification` — 엔티티/저장 골격, `UserNotificationService`는 DB 저장만 수행
- product-api: `NotificationController` 빈 껍데기 (`/api/v1/external/notification`)
- DB: `notifications`, `user_push_configs`, `user_device_tokens` 잔존
- FCM / Push 코드: 제거됨 (재도입 1차 제외)
- 마케팅 동의 `MARKETING`: 완료됨 — 1차 캠페인 발송 없음으로 Policy 연동은 후속

**예상 영향 (WHAT 수준, 파일 목록 확정은 `/plan`)**

- mono notification 도메인 진화 (타입/카테고리, 읽음, 목록 조회 포트)
- product-api 알림 목록·미읽음·읽음 API
- 리뷰 댓글 → Notification 생성 연동 (이벤트 또는 Facade, HOW는 `/plan`)
- 필요 시 Flyway 마이그레이션
- unit/integration 테스트, OpenAPI 계약

**1차 비영향**

- admin-api 발송 API, batch, FCM, SSE, device token API, Web Push

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit
- stop-conditions:
  1. 가정 붕괴 (재개봉 프로토콜)
  2. `/verify` 3회 실패
  3. scope 밖 행동 (push/pr, 대량 삭제, 인프라 변경 등 — 직전 확인)
- orchestration:
  - coordinator: 현재 Grok 세션 (Orca orchestration Run)
  - workers: **Grok만** 사용 (`--agent grok`). Codex/Claude/기타 에이전트 사용 금지
  - schema / Flyway 확장 시:
    1. `git.environment-variables` (origin: `bottle-note/environment-variables`)를 **신규 Orca worktree로 분리**해 마이그레이션 작성·커밋·푸시
    2. 백엔드 worktree에서는 **서브모듈 포인터 갱신 커밋만** 수행 (마이그레이션 파일을 백엔드 트리에 직접 쓰지 않음)
    3. schema worker도 Grok만 사용

## Tasks

### Task 1: Notification 도메인 조회·읽음 경로 (mono)
- Acceptance:
  - 사용자별 알림 목록 조회·미읽음 개수·단건 읽음·전체 읽음이 도메인/서비스 계층에서 동작한다
  - 기존 `notifications` 테이블·`app.external.notification` 패키지를 재사용·진화한다
  - 스키마 변경이 필요하면 구현을 멈추고 오케스트레이션으로 schema 서브모듈 worktree 작업을 분리한다 (이 Task에서 마이그레이션 SQL을 백엔드에 직접 추가하지 않는다)
- Verification: `./gradlew :bottlenote-mono:unit_test --tests '*Notification*'` (또는 동등 unit 필터) 및 compile
- Files (advisory): `app.external.notification.domain.*`, `repository/*`, `application/*`, test-support InMemory fixture
- Depends: 없음
- Size: M
- Status: [x] done

### Checkpoint: after Task 1
- [x] mono compile 통과
- [x] Notification 관련 unit test 통과 (9 tests, 0 failures)
- [x] 스키마 변경 필요 여부 판정 기록: **불필요** (`notifications.user_id`, `is_read`, `status` 등 기존 컬럼으로 inbox 경로 충족. Task S 미기동)

### Task 2: product-api 알림함 API
- Acceptance:
  - 인증 사용자가 본인 알림 목록·미읽음 개수·단건 읽음·전체 읽음을 API로 수행한다 (SC2–SC5)
  - 미인증은 거부된다 (SC6)
  - 타 사용자 알림 접근은 불가하다 (SC7)
- Verification: product-api 관련 unit/integration 테스트 및 compile
- Files (advisory): `NotificationController`, security 경로, OpenAPI 어노테이션, integration test
- Depends: Task 1
- Size: M
- Status: [ ] not done

### Task 3: 리뷰 댓글 → Notification 생성 연동
- Acceptance:
  - 리뷰 댓글 등록 시 리뷰 작성자 대상 Notification이 DB에 저장된다 (SC1)
  - 본인 리뷰에 본인이 댓글 단 경우 등 불필요 알림은 생성하지 않는다 (합리적 기본 정책)
  - Delivery(SSE/Push) 없이도 알림함 조회로 확인 가능하다 (SC8)
- Verification: mono unit test + 가능하면 integration 경로
- Files (advisory): review reply 생성 경로, notification 생성 호출/리스너, review author 조회
- Depends: Task 1
- Size: M
- Status: [ ] not done

### Checkpoint: after Tasks 2-3
- [ ] product-api + mono compile 통과
- [ ] 관련 unit test 통과
- [ ] SC1–SC8 대응 테스트 존재

> Task 2와 Task 3은 Task 1 완료 후 병렬 가능.  
> 최종 `/verify`는 implement 종료 후 코디네이터가 수행 (마지막 Task 뒤 Checkpoint 생략 규칙 준수 — 위 Checkpoint는 중간 검산용).

### Task S (조건부): environment-variables 스키마 마이그레이션
- Acceptance: 필요한 Flyway SQL이 서브모듈 저장소에 커밋되고, 백엔드는 서브모듈 포인터만 갱신한다
- Verification: 서브모듈 브랜치/커밋 존재 + 백엔드 `git submodule status` 포인터 일치
- Depends: Task 1에서 스키마 필요 판정 시
- Size: S–M
- Status: [ ] not done (조건부)
- Orchestration: **신규 worktree + Grok worker** (`repo: bottle-note/environment-variables` 또는 로컬 서브모듈 remote). 백엔드 포인터 갱신은 별도 짧은 Task/커밋

## Progress Log

- 2026-08-08: `/define` 착수. 설계 문서 반영, 기존 notification/push 잔존 구조 조사.
- 2026-08-08: 1차 범위 확정 — SSOT+알림함 API만 / admin 발송 제외 / 기존 패키지·테이블 재사용·진화.
- 2026-08-08: Overview 초안 작성. Execution Mode 승인 대기.
- 2026-08-08: Execution Mode **delegated** 확정. scope=`plan, implement, test, verify, commit`. 오케스트레이션=Grok only. 스키마=서브모듈 신규 worktree 분리 후 포인터만 갱신.
- 2026-08-08: `/plan` Tasks 1–3 + 조건부 Task S 작성.
- 2026-08-08: Task 1 완료 — mono Notification 도메인 포트/서비스 조회·읽음 경로 + InMemory unit 9건 통과. 스키마 변경 없음.
