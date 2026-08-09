# Plan: Notification Action MS1-2 (이슈 #381)

## Overview

`Notification` DB SSOT와 기존 알림함 API envelope를 유지하면서, 알림을 누른 앱·웹이 같은 의미 기반 Action 계약으로 이동 대상을 결정할 수 있게 한다. 이번 저장소 범위는 백엔드 저장·조회·읽음·필터·댓글 이벤트와 공통 Action API 계약이며, 실제 앱·웹 라우팅 코드는 별도 저장소 작업으로 남긴다.

- Milestone: `Notification Platform v1`
- Issue: `bottle-note/workspace#381`
- Base: PR `bottle-note/bottle-note-api-server#701`, commit `08e903dfb224075b3c544c1d5dbb3a524aeca998`
- API shape: 기존 `GlobalResponse` + `data.totalCount/items` + `meta.pageable` 유지
- Primary action: `OPEN_REVIEW`

### Assumptions

1. 읽음 여부의 호환 SSOT는 `is_read`이고 `read_at`은 최초 읽음 시각이다. `is_read=true, read_at=NULL`인 기존 행은 과거 읽음 시각 미상 데이터로 유지한다.
2. 신규 단건·전체 읽음 처리는 `status`를 `READ`로 바꾸지 않고 기존 `PENDING/SENT/FAILED` 값을 유지한다. 레거시 `READ` 값은 조회 호환을 위해 enum에서 즉시 제거하지 않는다.
3. 별도 Action 실행 endpoint는 만들지 않는다. 목록 응답의 의미 기반 Action을 앱·웹이 각자의 내부 route로 변환한다.
4. 서버와 DB에는 임의 URL, Universal Link, 앱 route, 웹 route 문자열을 저장하지 않는다.
5. 댓글 알림은 `source_type=REVIEW_REPLY`, `source_id=replyId`, `action_type=OPEN_REVIEW`, `action_target_id=reviewId`, `action_payload.replyId=replyId`, `action_version=1`이다.
6. `OPEN_REVIEW` v1 payload는 `replyId` 하나만 허용하는 typed DTO이며, targetId/replyId는 양수여야 한다. 직렬화 payload 상한은 1 KiB로 둔다.
7. 알 수 없는 action type/version, 누락·과다 key, 잘못된 타입·범위, 크기 초과 payload는 해당 item의 `action=null`로 강등한다. 다른 알림 item과 목록 전체는 정상 반환한다.
8. `fallbackType`은 저장값이 아니라 공통 계약 상수 `OPEN_NOTIFICATION_CENTER`로 응답한다.
9. 리뷰 상세 API가 이동 시점의 존재·공개·접근 권한을 다시 검증한다. 댓글만 삭제된 경우 클라이언트는 리뷰 상세를 열고 강조를 생략하며, 리뷰 삭제·접근 불가는 알림함으로 fallback한다. 이를 위해 알림 목록에서 리뷰·댓글 도메인을 N+1 조회하지 않는다.
10. 목록 필터는 `types`, `categories`, `readStatus`, `[createdFrom, createdTo)`이며 정렬은 계속 `id DESC`다. 빈 type/category 목록은 전체 조건으로 해석한다.
11. DB/JVM 기본 시간대는 `Asia/Seoul`이고 저장 시각은 `LocalDateTime`으로 유지한다. API 입력은 `OffsetDateTime`을 동일 instant의 KST로 정규화하고, `createAt/readAt` 응답은 `+09:00` offset을 포함한다.
12. 동일 `(source_type, source_id, user_id)`는 DB UNIQUE가 최종 중복 방지선이다. 애플리케이션은 순차 재전달을 멱등 처리하되 경쟁 상황의 무결성은 DB가 보장한다.
13. 기존 nullable source/action 컬럼 행은 그대로 호환하며 `action=null`로 응답한다.
14. 필터 후보 인덱스는 실제 운영 데이터 분포와 `EXPLAIN ANALYZE` 증거 없이 추가하지 않는다. 이번 마이그레이션은 필수 UNIQUE와 컬럼만 대상으로 한다.
15. 향후 Outbox/SSE/FCM은 같은 canonical notification/action 의미를 채널별 envelope로 매핑한다. API 목록 envelope를 SSE/FCM에 바이트 단위로 복제하는 것은 이번 범위가 아니다.

### Success Criteria

| # | 기준 | 검증 방향 |
|---|---|---|
| SC1 | 신규 Flyway가 nullable `read_at/source/action` 컬럼과 source UNIQUE를 추가하고 기존 행을 보존한다 | schema diff + GitHub CI integration |
| SC2 | 최초 읽음에만 `read_at`이 기록되고 단건 재호출·전체 읽음 재호출에서 최초 시각이 유지된다 | unit + integration |
| SC3 | 읽음 처리 후에도 기존 전달 `status`가 유지되며 신규 `READ`가 기록되지 않는다 | unit + integration |
| SC4 | 댓글 알림이 합의된 REVIEW_REPLY/OPEN_REVIEW 계약으로 저장·응답된다 | unit + integration |
| SC5 | 동일 source 이벤트가 재전달돼도 수신자별 알림은 1건만 존재한다 | unit + DB integration |
| SC6 | allowlist·v1 typed payload·1 KiB 상한을 위반한 행은 item `action=null`로 강등된다 | unit + integration |
| SC7 | 목록 응답은 기존 envelope를 유지하고 `readAt`·`action`을 additive field로 제공한다 | controller integration + OpenAPI |
| SC8 | 단건 읽음 응답은 `notificationId/isRead/readAt/changed/unreadCount`를 반환하고 멱등하다 | unit + controller integration |
| SC9 | type/category/readStatus/time-range 필터가 단독·조합으로 동작하고 totalCount도 같은 필터를 반영한다 | unit + DB integration |
| SC10 | 동일 필터를 유지한 cursor pagination에서 중복·누락 없이 `id DESC`를 유지한다 | unit + DB integration |
| SC11 | `createAt/readAt`은 항상 `+09:00` offset을 포함한다 | JSON contract integration |
| SC12 | 타 사용자 알림 조회·읽음 차단과 기존 미읽음 API가 회귀하지 않는다 | integration |
| SC13 | Product/Admin Flyway 리소스 복사와 Batch 마이그레이션 제외 가드가 유지된다 | GitHub CI build/rule |
| SC14 | 전체 GitHub Actions `ci pipeline`이 최종 backend head SHA에서 성공한다 | workflow run jobs |

### Impact Scope

**Backend**

- `bottlenote-mono`: Notification entity/port/JPA/query/service, Action contract, 댓글 listener
- `bottlenote-product-api`: 목록·읽음 API DTO/OpenAPI와 controller integration test
- `bottlenote-test-support`: InMemory Notification repository
- `git.environment-variables`: 별도 Orca worktree의 신규 Flyway SQL
- backend repo: 서브모듈 gitlink와 이 계획 문서

**Out of Scope**

- FCM/APNs, Device Token, 채널별 Delivery: #382
- 보관·삭제·아카이브·수신 정책: #383
- Outbox·재처리·이벤트 파이프라인 공동화: #373
- SSE, Web Push
- 앱·웹 저장소의 실제 route/화면 코드와 Universal Link/App Link 설정
- 새 Action 실행 endpoint
- 실제 데이터 접근 권한이 없는 상태에서의 운영 `EXPLAIN ANALYZE`
- PR 생성과 development/production 배포

## Execution Mode

- mode: delegated
- scope: plan, implement, test, self-review, commit, schema-push, backend-push, GitHub-CI-verify, final-opus-review
- verification-policy:
  - 로컬 Gradle compile/test/build, `unit_test`, `integration_test`, `check_rule_test`, Spotless 실행 금지
  - 최종 검증 SSOT는 `.github/workflows/ci_pipeline.yml`의 `workflow_dispatch` run, head SHA, job 로그다
  - CI 실패는 로그 기반 Codex 수정으로 최대 3회 시도한다
- orchestration:
  - coordinator: 현재 Orca Run `run_27b9d340d752`
  - implementation/test workers: Codex only
  - final reviewer: 전체 코드와 GitHub CI 성공 뒤 Opus를 정확히 1회만 사용
  - PR과 배포는 실행하지 않는다
- stop-conditions:
  1. #381과 충돌하거나 비즈니스 결정을 새로 요구하는 발견
  2. schema 최신 번호·브랜치·pointer 불일치
  3. GitHub CI 실패를 3회 안에 해결하지 못함
  4. PR·배포·인프라 변경 등 scope 밖 행동 필요
  5. Action trust boundary나 기존 데이터 호환을 보장할 수 없는 설계 발견

## Tasks

### Task 1: Notification Action Flyway 스키마
- Acceptance:
  - `git.environment-variables`의 원격 기본 브랜치와 최신 마이그레이션 번호를 재확인한다.
  - 독립 Orca worktree `notification-action-schema`, 브랜치 `Whale0928/feat-issues-381-schema`에서 nullable 컬럼 7개와 UNIQUE `(source_type, source_id, user_id)`를 추가한다.
  - 기존 행 backfill이나 후보 조회 인덱스를 근거 없이 추가하지 않고 schema commit을 먼저 push한다.
- Verification: 원격 schema branch/commit 확인, SQL 정적 검토, 이후 backend GitHub CI의 Flyway/Testcontainers 결과
- Files (advisory): `storage/db/migration/V{latest+1}__add_notification_action.sql`
- Depends: 없음
- Size: S
- Status: [ ] not done

### Task 2: 읽음 시각과 전달 상태 분리
- Acceptance:
  - backend gitlink가 Task 1 schema commit을 가리키고 Notification이 신규 nullable 컬럼을 매핑한다.
  - 단건·전체 읽음이 원자적으로 `is_read=true`, `read_at=COALESCE(read_at, now)`를 적용하며 전달 `status`를 보존한다.
  - InMemory와 unit 시나리오가 최초 시각·반복 호출·레거시 읽음·타 사용자 격리를 같은 의미로 구현한다.
- Verification: 정적 self-review 후 commit, 최종 GitHub CI unit/integration
- Files (advisory): `git.environment-variables`, `Notification`, `NotificationRepository`, `JpaNotificationRepository`, `InMemoryNotificationRepository`, `UserNotificationServiceTest`
- Depends: Task 1
- Size: M
- Status: [ ] not done

### Task 3: 읽음 API 멱등 응답
- Acceptance:
  - 단건 읽음 service 결과가 `readAt/changed/unreadCount`를 반환하고 controller가 합의된 additive 응답을 제공한다.
  - 재요청은 같은 `readAt`, `changed=false`를 반환하며 전체 읽음은 기존 최초 시각을 보존한다.
  - 기존 인증·404 소유권·미읽음 개수 계약을 유지한다.
- Verification: 정적 self-review 후 commit, 최종 GitHub CI unit/controller integration
- Files (advisory): `NotificationService`, `UserNotificationService`, 읽음 result DTO, `NotificationController`, `NotificationMarkReadResponse`, `UserNotificationServiceTest`, `NotificationControllerIntegrationTest`
- Depends: Task 2
- Size: M
- Status: [ ] not done

### Checkpoint: after Tasks 1-3
- [ ] schema commit/push와 backend gitlink SHA 일치
- [ ] 신규 읽음 경로에서 `status=READ` 대입 0건
- [ ] 로컬 실행 없이 정적 self-review Critical/Important 0건

### Task 4: OPEN_REVIEW Action trust boundary
- Acceptance:
  - allowlist, typed `OpenReviewActionPayload`, v1, 양수 ID, 정확한 key, 1 KiB 상한을 저장과 응답 경계에서 검증한다.
  - 유효한 Action은 `OPEN_REVIEW/targetId/replyId/version=1/fallbackType`으로 응답하고 기존·미지원·불완전 행은 `action=null`로 강등한다.
  - 임의 URL·플랫폼 route 필드를 도메인·DB·응답에 도입하지 않는다.
- Verification: 정적 self-review 후 commit, 최종 GitHub CI unit/integration/OpenAPI
- Files (advisory): `notification/action/*`, `NotificationListResponse`, `UserNotificationService`, `UserNotificationServiceTest`
- Depends: Task 2
- Size: M
- Status: [ ] not done

### Task 5: 필터 결합 cursor 조회
- Acceptance:
  - 요청·criteria가 types/categories/readStatus/createdFrom/createdTo를 검증하고 `[from,to)` KST 조건으로 전달한다.
  - JPA와 InMemory가 같은 동적 조건, `id < cursor`, `id DESC`, `pageSize+1`, 필터 totalCount를 구현한다.
  - 단독·조합 필터와 다중 페이지에서 중복·누락이 없고 빈 목록 필터는 전체로 처리한다.
- Verification: 정적 self-review 후 commit, 최종 GitHub CI unit/DB integration
- Files (advisory): `NotificationPageableRequest`, `NotificationListCriteria`, custom repository, `UserNotificationService`, `InMemoryNotificationRepository`, `UserNotificationServiceTest`, `NotificationControllerIntegrationTest`
- Depends: Task 3
- Size: M
- Status: [ ] not done

### Task 6: 댓글 source/action과 중복 방지
- Acceptance:
  - 댓글 이벤트가 합의된 REVIEW_REPLY/OPEN_REVIEW source/action을 저장한다.
  - 순차 동일 이벤트는 멱등하게 생략하고 경쟁 상황은 DB UNIQUE로 중복 저장을 막는다.
  - 본인 댓글 생략, AFTER_COMMIT + Async + REQUIRES_NEW, 원본 댓글 트랜잭션 비영향 경계를 유지한다.
- Verification: 정적 self-review 후 commit, 최종 GitHub CI unit/ReviewReply integration
- Files (advisory): `NotificationMessage`, `ReviewReplyNotificationListener`, `NotificationRepository`, `JpaNotificationRepository`, `UserNotificationService`, listener unit test, reply integration test
- Depends: Task 4
- Size: M
- Status: [ ] not done

### Checkpoint: after Tasks 4-6
- [ ] raw URL/route 저장·응답 0건
- [ ] invalid Action 한 건이 목록 전체를 실패시키지 않는 테스트 존재
- [ ] source UNIQUE와 애플리케이션 멱등 경로 정합

### Task 7: 공통 API 계약 통합 검증과 문서화
- Acceptance:
  - 목록 additive field, +09:00, Action fallback, 필터+cursor, 읽음 멱등, 타 사용자 차단을 실제 Spring context/Testcontainers 시나리오로 완성한다.
  - Product OpenAPI 설명이 실제 응답·query key와 일치하고 Product/Admin 복사·Batch 제외 가드가 회귀하지 않는다.
  - 앱·웹 별도 작업에 필요한 Action 매핑과 미래 채널별 envelope 원칙을 이 계획/완료 보고에 명시한다.
- Verification: backend push 후 `ci_pipeline.yml` workflow_dispatch, head SHA 기준 모든 job 성공
- Files (advisory): `NotificationControllerIntegrationTest`, `ReviewReplyNotificationIntegrationTest`, `NotificationApiDocs`, plan Progress Log
- Depends: Tasks 3, 5, 6
- Size: M
- Status: [ ] not done

## Progress Log

- 2026-08-09: #381 최신 본문, #384 개발 검증 댓글, PR #701, 기존 MS1-1 계획, Notification 코드/테스트, Flyway와 아키텍처 문서를 재확인했다.
- 2026-08-09: 사용자 합의 반영 — 기존 응답 envelope 유지, 읽음은 `isRead/readAt`, `status`는 전달 상태, 별도 Action endpoint 없음.
- 2026-08-09: Execution Mode `delegated` 확정. Codex 구현자, GitHub CI-only 검증, 최종 Opus 리뷰 정확히 1회, PR/배포 제외.
- 2026-08-09: Orca Run `run_27b9d340d752` 생성. 계획 전용 Codex worker 2회는 파일 변경 전 응답 정지로 중단했고 coordinator가 동일 근거로 define/plan 문서를 작성했다.
