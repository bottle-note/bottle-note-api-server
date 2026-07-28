# Plan: 큐레이션 피드 읽기 경로 전환 및 스펙 변경 시 재생성

## Overview

이슈 bottle-note/workspace#322 (큐레이션 피드 Read Model 분리 ADR)의 **읽기 경로**를
구현하고, 스펙이 바뀌었을 때 저장된 Read Model이 낡지 않도록 재생성 장치를 넣는다.

PR #677로 `curation_extension.feed_payload` 쓰기 경로는 확보됐다. 다만 지금은
아무도 그 값을 읽지 않고, 스펙 JSON의 `x-feed`가 바뀌면 저장된 값이 조용히
낡는 상태다. 이 둘을 함께 해결한다.

세 덩어리다.

1. **변경 감지** — `CurationSpecResourceSyncService.sync()`가 지금은 기존 스펙을
   비교 없이 매 기동 무조건 `update()`한다. `responseSpec`이 실제로 바뀌었는지
   판별할 수단을 넣는다.
2. **재생성** — `responseSpec`이 바뀐 스펙에 한해, 그 스펙으로 저장된 큐레이션의
   `feed_payload`를 다시 만든다. `feed_payload`가 NULL인 레거시 행도 대상에
   포함하므로 backfill을 겸한다.
3. **읽기 경로 전환** — Product 피드와 Admin 피드 프리뷰가 원본 `payload` 대신
   `feed_payload`를 소스로 쓴다. `feed_payload`가 NULL이면 원본에서 파싱한다.

### Assumptions

- 신규 PR은 `main` 기준 새 브랜치로 낸다. PR #677 위에 쌓지 않는다.
  (#677이 먼저 머지되는 것을 전제한다)
- 스키마 변경이 없다. 변경 감지는 `update()` 직전에 DB의 기존 `responseSpec`을
  읽어 그 자리에서 해시를 내고 비교하므로 해시 저장용 컬럼이 필요 없다.
  따라서 Flyway 마이그레이션도 없다.
- 변경 감지 기준은 `responseSpec`만이다. `name`, `description`, `requestSpec`,
  `hydratorKey`, `version`만 바뀐 경우는 재생성하지 않는다. `feed_payload`
  추출에 영향을 주는 것은 `responseSpec`뿐이기 때문이다.
- 비교는 정규화 후 해시로 한다. 단순 문자열 비교는 쓸 수 없다 — MySQL JSON
  컬럼이 객체 키를 자체 정렬하고, Jackson 직렬화 공백·수치 표기가 경로마다
  달라 매번 "변경됨"으로 오판한다. 키를 재귀 정렬하고 공백을 제거한 canonical
  JSON을 SHA-256으로 비교한다.
- 재생성은 admin-api 기동 시점(`CurationSpecResourceSyncRunner`)에서만 돈다.
  product-api에는 러너를 추가하지 않는다.
- 재생성 대상은 변경된 스펙에 속한 **모든** 큐레이션이다. `feed_payload`가
  NULL인 레거시 행도 포함한다. 즉 backfill을 겸한다.
- 다중 인스턴스 동시 기동 시 Redis 분산 락으로 한 인스턴스만 재생성한다.
  락을 얻지 못한 인스턴스는 건너뛰고 정상 기동한다. JVM 로컬 상태는 쓰지 않는다.
- 재생성 중 예외가 나면 경고 로그만 남기고 기동을 계속한다. `feed_payload`는
  파생 데이터이고 NULL fallback이 있으므로 서비스 전체를 막지 않는다.
- 읽기 경로 전환 대상은 Product 피드와 Admin 피드 프리뷰 **둘 다**다.
  프리뷰의 목적이 사용자가 볼 화면을 미리 확인하는 것이므로 같은 소스를 봐야 한다.
- Product **상세** API는 전환하지 않는다. 상세는 원본 `payload`에 전체 GraphQL
  보강을 적용하는 별도 경로이며 `x-feed`와 무관하다.
- 전환 후에도 파이프라인 형태는 유지한다: `소스 → materializeFeed →
  projectPayload`. `projectPayload`를 계속 통과시켜야 `feed_payload`에 섞인
  숨은 입력값(x-feed가 아닌 argFrom 값)이 응답에 노출되지 않는다.
- `x-feed` 스펙 보정은 이번 범위에서 **제외**한다. 피드 응답 `payload`는 API
  계약이라 좁히면 breaking change이며 프론트 협의가 선행되어야 한다. 이번에
  3번이 들어가면, 협의 후 스펙 JSON만 고쳐도 재생성이 자동으로 반영한다.

### Success Criteria

- `responseSpec`이 바뀌지 않은 채 재기동하면 재생성이 **0건** 수행된다.
  (현재는 판별 자체가 불가능하다)
- 스펙 JSON의 `x-feed`를 바꾸고 재기동하면 해당 스펙의 큐레이션만
  `feed_payload`가 갱신되고, 다른 스펙의 행은 그대로다.
- 재생성 후 `feed_payload`가 NULL인 행이 남지 않는다 (해당 스펙 범위 내).
- 피드 응답이 전환 전후로 동일하다. 4개 스펙 각각에 대해 원본 경로 결과와
  `feed_payload` 경로 결과가 같음을 테스트로 보인다.
- `feed_payload`가 NULL인 큐레이션도 피드에 정상 노출된다 (원본 fallback).
- 피드 응답에 숨은 입력값(x-feed가 아닌 argFrom 값)이 노출되지 않는다.
- 재생성이 실패해도 애플리케이션이 정상 기동한다.
- 락을 얻지 못한 인스턴스는 재생성을 건너뛰고 정상 기동한다.

### Impact Scope

- **mono**: `CurationSpecResourceSyncService`(변경 감지),
  `CurationSpecSyncResponse`(변경된 스펙 식별자 전달), 재생성 서비스 신규,
  `ProductSpecBasedCurationService`·`AdminSpecBasedCurationService`(읽기 경로 전환).
  정규화·해시 유틸 신규.
- **admin-api**: `CurationSpecResourceSyncRunner`가 동기화 후 재생성을 호출.
- **product-api**: 코드 변경 없음. 피드 응답 동작만 바뀐다 (계약은 불변).
- **test-support**: `CurationFixtureFactory`가 NULL fallback 케이스를 만들 수
  있어야 한다 (feed_payload 없는 픽스처).
- **스키마**: 변경 없음. Flyway 마이그레이션 없음.
- **Redis**: 재생성 락 키 추가. 기존 `RedisConfig` 사용.

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- 구현·리뷰에 codex CLI(`codex exec`, `codex exec review`)를 적극 활용한다.
  특히 작성자와 리뷰어가 분리되도록 Task 구현 후 독립 리뷰를 codex에 맡긴다.
- stop-conditions: 기본 3종 (① 가정 붕괴 시 재개봉 프로토콜, ② /verify 3회
  실패 시 /debug 보고 후 정지, ③ scope 밖 되돌리기 어려운 행동 전 확인)

## Tasks

### Task 1: responseSpec 변경 감지
- Acceptance: `sync()`가 기존 스펙의 `responseSpec`과 리소스의 `responseSpec`을
  canonical JSON(키 재귀 정렬 + 공백 제거) SHA-256으로 비교해, 실제로 바뀐
  스펙만 식별한다. 키 순서·공백·수치 표기 차이는 "동일"로 판정한다.
  `CurationSpecSyncResponse`가 변경된 스펙 식별자를 함께 반환한다.
  신규 생성 스펙은 변경으로 취급하지 않는다 (큐레이션이 아직 없다).
- Verification: `./gradlew :bottlenote-mono:test --tests '*CurationSpecResourceSync*'`
- Files (advisory): canonical/해시 유틸 신규, `CurationSpecResourceSyncService.java`,
  `CurationSpecSyncResponse.java`, 관련 테스트
- Depends: 없음
- Size: M
- Status: [x] done

### Task 2: 스펙 단위 큐레이션 조회와 feed_payload 재생성
- Acceptance: `CurationExtensionRepository`에 specId 기준 조회가 추가되고,
  주어진 스펙의 모든 큐레이션 `feed_payload`를 `extractFeedPayload`로 다시
  만들어 저장하는 서비스가 생긴다. `feed_payload`가 NULL인 레거시 행도
  대상에 포함한다(backfill 겸용). 다른 스펙의 행은 건드리지 않는다.
- Verification: `./gradlew :bottlenote-mono:test --tests '*Regenerat*'`
- Files (advisory): `CurationExtensionRepository.java`,
  `JpaCurationExtensionRepository.java`, 재생성 서비스 신규,
  `InMemoryCurationExtensionRepository.java`, 관련 테스트
- Depends: 없음 (Task 1과 병렬 가능 — 서비스는 specId 목록만 입력받는다)
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-2
- [ ] 컴파일 통과 / 단위 테스트 통과 / ArchUnit 룰 통과

### Task 3: 기동 시 재생성 실행과 중복 억제
- Acceptance: admin-api 러너가 동기화 직후, 변경된 스펙이 있을 때만 재생성을
  호출한다. Redis 분산 락으로 한 인스턴스만 실행하며, 락을 얻지 못한
  인스턴스는 건너뛰고 정상 기동한다. 재생성 중 예외가 나면 경고 로그를 남기고
  기동을 계속한다.
- Verification: `./gradlew :bottlenote-admin-api:test`
- Files (advisory): 락 지원 신규, `CurationSpecResourceSyncRunner.kt`, 관련 테스트
- Depends: 1, 2
- Size: M
- Status: [x] done

### Task 4: 피드 읽기 경로를 feed_payload로 전환
- Acceptance: Product 피드와 Admin 피드 프리뷰가 `feed_payload`를 소스로 쓰고,
  NULL이면 원본 `payload`로 fallback한다. `소스 → materializeFeed →
  projectPayload` 파이프라인은 유지해 숨은 입력값이 응답에 노출되지 않는다.
  Product 상세 API는 전환하지 않는다.
- Verification: `./gradlew :bottlenote-mono:test --tests '*SpecBasedCuration*'`
- Files (advisory): `ProductSpecBasedCurationService.java`,
  `AdminSpecBasedCurationService.java`, `CurationExtension.java`, 관련 테스트
- Depends: 없음 (1-3과 병렬 가능)
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 3-4
- [ ] 컴파일 통과 / 단위 테스트 통과 / ArchUnit 룰 통과

### Task 5: Product 피드 전환 통합 검증
- Acceptance: 4개 스펙 각각에 대해 `원본 payload → materializeFeed →
  projectPayload` 결과와 `feed_payload → 동일 파이프라인` 결과가 같음을
  보인다(두 값을 직접 계산해 비교하며, 전환 전 API를 호출하지 않는다).
  `feed_payload`가 NULL인 큐레이션도 피드에 정상 노출되고, 응답에 숨은
  입력값이 없다. 픽스처가 NULL/비NULL 두 상태를 모두 만들 수 있다.
- Verification: `./gradlew integration_test`
- Files (advisory): `ProductSpecBasedCurationIntegrationTest.java`,
  `CurationFixtureFactory.java`
- Depends: 4
- Size: M
- Status: [ ] not done

### Task 6: Admin 프리뷰 전환과 재생성 통합 검증
- Acceptance: Admin 피드 프리뷰가 `feed_payload` 기준으로 응답하고 NULL이면
  원본으로 fallback한다. 스펙 `responseSpec`을 바꾼 뒤 동기화를 돌리면 해당
  스펙의 큐레이션만 `feed_payload`가 갱신되고 NULL 행이 채워지며, 다른 스펙은
  불변임을 실제 DB로 확인한다.
- Verification: `./gradlew :bottlenote-admin-api:admin_integration_test`
- Files (advisory): `AdminSpecBasedCurationIntegrationTest.kt`, 동기화·재생성
  통합 테스트 신규
- Depends: 3, 4
- Size: M
- Status: [ ] not done

## Progress Log

- /plan 완료: Task 6개(M 5, M 1), 의존 순서 1·2·4 병렬 → 3 → 5·6.
  codex(`codex exec --sandbox read-only`)로 분해 검토를 받아 4건 중 3건 반영:
  Task 5의 불필요한 Depends 2 제거, "원본 경로 응답" 문구를 직접 계산 비교로
  구체화(전환 후엔 호출 가능한 원본 경로가 없음), Task 3 크기 S→M.
  Task 4·5 분리 지적은 Task 5만 모듈 경계(product-api/admin-api)로 분리하고
  Task 4는 mono 단일 모듈의 동일 메커니즘이라 유지.
- Task 1 완료: `CurationSpecFingerprint` 신규(canonical JSON + SHA-256),
  `sync()`가 `update()` 덮어쓰기 전에 비교해 `changedSpecIds`를 반환.
  신규 생성 스펙은 큐레이션이 없어 변경으로 잡지 않는다.
  codex 리뷰 3건 전부 반영: ① 실수 trailing zero 미제거로 `1.20`/`1.2`
  오탐 → 항상 `stripTrailingZeros` ② `changedSpecIds` null을 빈 목록으로
  뭉개 재생성 누락을 "변경 없음"으로 위장 → `List.copyOf`로 NPE 노출
  ③ 정규화 엣지 미검증 → `CurationSpecFingerprintTest` 9건 신규.
  단위 테스트 14건 통과.
- Task 2 완료: `CurationFeedPayloadRegenerationService` 신규,
  `CurationExtensionRepository.findAllBySpecIdIn`과
  `CurationExtension.updateFeedPayload` 추가. NULL 레거시 행 포함(backfill 겸용).
  codex 리뷰에서 **경합 버그**를 잡아 `@DynamicUpdate` 적용: 전체 컬럼 UPDATE면
  재생성이 로드한 stale `payload`가 어드민 저장분을 덮어써 SSOT가 유실된다.
  나머지 지적은 설계 판단으로 유지 — `save()` 명시 호출은 더티체킹 의존을
  피해 포트 계약을 지키려는 것이고, 행 단위 예외의 전체 롤백은 "재생성 안 됨
  + fallback 동작"이라 안전한 쪽이며, `specIds` null 원소는 Task 1과 같은
  이유로 NPE 노출을 택했다. 단위 테스트 6건 통과.
- Task 4 완료: `CurationExtension.feedSource()`(NULL이면 원본 fallback)를 두 서비스가
  공유한다. Product 피드·Admin 프리뷰 전환, 상세는 원본 유지.
  codex 리뷰가 **동등성 논증의 허점**을 잡았다: 최초 테스트가 `materializeFeed`를
  건너뛰고 "두 경로 모두 동일 적용"이라고 가정했는데, 인자가 비면 materializer가
  no-op이 아니라 `writeTo`에 null/[]을 써서 배열 원소를 살린다. 원본 경로는 그
  원소가 남고 feed_payload 경로는 추출 단계에서 버려 응답이 갈릴 수 있다.
  → 테스트를 실제 파이프라인(소스 → materializeFeed → projectPayload) 비교로
  바꾸고, GraphQL 실행 시 실패하는 executor를 물렸다. 인자가 빈 경우는 실행 없이
  갈리므로 "현행 스펙에 피드 교차 x-graphql 없음"을 명시적 트립와이어로 고정했다.
  현재 4개 스펙 모두 교차 0건이라 동등성이 성립하며, 새 스펙이 생기면 테스트가
  깨져 재검토를 강제한다. 단위 테스트 8건 통과.
- Task 3 완료: 러너가 동기화 직후 `changedSpecIds`가 있을 때만 재생성한다.
  락은 `CurationFeedRegenerationLock`(도메인 인터페이스) +
  `RedisCurationFeedRegenerationLock`(구현)으로 분리했다 — 처음엔 서비스에
  Redis를 직접 물렸으나 admin-api 테스트 클래스패스에 `RedisTemplate`이 없어
  컴파일이 깨졌고, 레이어 표준 4(기술 세부사항은 구현에 격리)에도 이 형태가 맞다.
  재생성 예외는 경고 로그로 삼키고 락은 finally에서 해제한다. 러너 단위 4건 통과.
- 알려진 한계: 재생성이 대상 전체를 한 트랜잭션에 적재한다. 현재 운영 28건
  기준 문제없으나 큐레이션이 수천 건이 되면 chunk 처리가 필요하다.
