```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-07-27

** Core Achievements **
- curation_extension.feed_payload JSON nullable 컬럼 추가 (서브모듈 V4, 환경변수 저장소 b1eeffb)
- CurationFeedProjector.extractFeedPayload: x-feed 투영 + 숨은 입력값(argFrom) 병합 추출
- 어드민 큐레이션 생성·수정 시 원본 payload와 feed_payload 이중 쓰기
- /verify full(L3) 전 단계 PASS (unit 34건, product 통합, admin 통합 17건 포함)

** Key Components **
- git.environment-variables/storage/db/migration/V4__add_feed_payload_to_curation_extension.sql
- bottlenote-mono curation/domain/CurationExtension.java (feedPayload 필드)
- bottlenote-mono curation/service/CurationFeedProjector.java (extractFeedPayload)
- bottlenote-mono curation/service/AdminSpecBasedCurationService.java (이중 쓰기)

** Deferred Items **
- Product 피드 읽기 경로 feed_payload 전환 (+NULL fallback): 후속 과제
- 기존 데이터 backfill: 어드민 재저장으로 자연 반영, 일괄 작업 없음
- 스펙 버전 관리·x-feed 변경 시 재생성 정책: 후속 과제
- Admin 피드 프리뷰 N+1, GraphQL 배치 보강: 후속 과제
================================================================================
```

# Plan: 큐레이션 feed_payload 컬럼 추가 및 쓰기 경로 저장

## Overview

이슈 bottle-note/workspace#322 (큐레이션 피드 Read Model 분리 ADR)의 쓰기 경로를
구현한다. `curation_extension` 테이블에 피드 전용 파생 Read Model 컬럼
`feed_payload`를 추가하고, 어드민 큐레이션 생성·수정 시 원본 `payload`에서
피드용 값만 추출해 같은 트랜잭션으로 함께 저장한다.

원본 `payload`는 건당 최대 128KB로, 피드 조회 시 전체를 전송·투영하는 현행
방식의 최적화 기반이 되는 데이터다. 이번 범위는 어디까지나 쓰기 경로
확보이며, 피드 조회가 `feed_payload`를 사용하도록 전환하는 것은 후속
과제로 남긴다.

### Assumptions

- `curation_extension`에 `feed_payload` JSON nullable 컬럼을 추가한다.
  별도 테이블은 만들지 않는다.
- 마이그레이션은 서브모듈 `git.environment-variables/storage/db/migration/`의
  `V4__` 신규 파일로 작성한다. 서브모듈 저장소에 먼저 main으로 커밋·푸시한
  뒤 본 저장소에서 서브모듈 포인터를 갱신한다. 현재 PR이 본 저장소 main에
  배포되기 전이므로 V4 파일 선점 충돌은 없다.
- JSON nullable 컬럼은 조회 조건에 쓰이지 않으므로 인덱스를 추가하지
  않는다. (MySQL은 JSON 직접 인덱스 불가)
- 어드민 큐레이션 생성·수정 시 원본 payload 검증 후 피드 값을 추출해
  원본과 같은 트랜잭션으로 저장한다.
- 추출 내용은 Response Spec의 `x-feed.enabled=true` 필드와, 피드 경로와
  교차하는 `x-graphql`의 `argFrom` 값(숨은 입력값, 예: `alcoholId`)이다.
- 추출 로직은 신규 클래스를 만들지 않고 기존 클래스에 메서드로 추가한다.
  x-feed 투영을 소유하는 `CurationFeedProjector`를 우선 후보로 하며,
  자잘한 함수 단위로 과도하게 분해하지 않는다.
- 추출 시 GraphQL을 실행하지 않는다. 통계값(`stats.rating`,
  `stats.reviewCount` 등 조회 시점 보강 값)은 저장하지 않고 입력값만
  저장한다.
- Product 피드 API의 읽기 경로 전환(`feed_payload` 사용 및 NULL fallback)은
  이번 범위에서 제외하고 후속 과제로 미룬다.
- 기존 데이터 backfill은 하지 않는다. 기존 큐레이션은 어드민 대시보드에서
  재저장 시 자연스럽게 `feed_payload`가 채워진다.
- 원본 `payload`는 SSOT로 유지하며, 어드민 상세 조회와 상세 API는 계속
  원본을 사용한다.
- 스펙 버전 관리와 `x-feed` 변경 시 Read Model 재생성 정책은 후속 과제로
  남긴다.
- Admin 피드 프리뷰(`AdminSpecBasedCurationService.searchFeed`)는 기존
  온디맨드 방식을 유지한다.
- 피드 응답 구조·필드·정렬·커서 동작을 변경하지 않는다.

### Success Criteria

- V4 마이그레이션으로 `curation_extension.feed_payload` (JSON, NULL)
  컬럼이 생성된다.
- 어드민 큐레이션 생성 시 `payload`와 `feed_payload`가 함께 저장된다.
- 어드민 큐레이션 수정 시 `feed_payload`도 함께 갱신된다.
- 저장된 `feed_payload`에는 x-feed 필드와 숨은 입력값만 있고 GraphQL
  통계값은 포함되지 않는다.
- 추출 로직은 4개 스펙(`RECOMMENDED_WHISKY`, `WHISKY_PAIRING`,
  `WHISKY_TASTING_EVENT`, `PROGRAM`)의 대표 payload에 대해 단위 테스트로
  검증된다.
- 기존 피드·상세 API의 응답이 변경 전과 동일하다 (회귀).

### Impact Scope

- **서브모듈 (git.environment-variables)**: `V4__` 마이그레이션 SQL 신규.
  본 저장소는 서브모듈 포인터 갱신.
- **mono**: `CurationExtension` 엔티티 `feedPayload` 필드 추가,
  `CurationFeedProjector`에 피드 값 추출 메서드 추가 (신규 클래스 없음),
  `AdminSpecBasedCurationService` 생성·수정에 추출·저장 로직 추가.
- **test-support**: `InMemoryCurationExtensionRepository`,
  `CurationFixtureFactory` 갱신.
- **product-api / admin-api**: 코드 변경 없음 (읽기 경로·API 계약 불변).

## Execution Mode

- mode: delegated
- scope: plan, implement, test, verify, commit, push, pr
- 서브모듈(git.environment-variables) main 직접 커밋·푸시도 위임에 포함한다.
  V4 SQL을 서브모듈 main에 먼저 푸시한 뒤 본 저장소에서 포인터를 갱신한다.
- stop-conditions: 기본 3종 (① 가정 붕괴 시 재개봉 프로토콜, ② /verify 3회
  실패 시 /debug 보고 후 정지, ③ scope 밖 되돌리기 어려운 행동 전 확인)

## Tasks

### Task 1: V4 마이그레이션 및 CurationExtension feedPayload 필드 추가
- Acceptance: 서브모듈에 `V4__` 마이그레이션으로 `curation_extension.feed_payload`
  (JSON, NULL) 컬럼이 추가되고, 서브모듈 main에 푸시된다. 본 저장소는 서브모듈
  포인터가 갱신되고, `CurationExtension`에 `feedPayload` 필드가 매핑되어
  Flyway validate를 통과한다.
- Verification: `./gradlew :bottlenote-mono:compileJava` 및 TestContainers
  기반 테스트 1건 이상이 스키마 검증과 함께 통과
- Files (advisory): `git.environment-variables/storage/db/migration/V4__*.sql`,
  서브모듈 포인터, `CurationExtension.java`
- Depends: 없음
- Size: S
- Status: [x] done

### Task 2: CurationFeedProjector에 feed payload 추출 메서드 추가
- Acceptance: 원본 payload와 response spec으로부터 x-feed.enabled=true 필드와
  피드 경로 교차 x-graphql의 argFrom 값(숨은 입력값)만 추출하는 메서드가
  `CurationFeedProjector`에 추가된다. GraphQL은 실행하지 않는다. 신규 클래스
  없이 메서드로 추가하며, 4개 스펙 리소스 기준 단위 테스트가 통과한다.
- Verification: `./gradlew :bottlenote-mono:test --tests '*CurationFeedProjector*'`
- Files (advisory): `CurationFeedProjector.java`, 신규 단위 테스트
- Depends: 없음 (Task 1과 병렬 가능)
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 1-2
- [ ] 컴파일 통과 / 단위 테스트 통과 / ArchUnit 룰 통과

### Task 3: 큐레이션 생성·수정 시 feed_payload 이중 쓰기
- Acceptance: `AdminSpecBasedCurationService`의 create/update가 원본 payload
  검증 후 추출 메서드로 만든 feed_payload를 같은 트랜잭션으로 저장·갱신한다.
  test-support의 InMemory 레포지토리·픽스처가 갱신되고, 생성·수정 시
  feed_payload 저장을 검증하는 테스트가 통과한다.
- Verification: `./gradlew :bottlenote-mono:test --tests '*AdminSpecBasedCuration*'`
  및 관련 통합 테스트
- Files (advisory): `AdminSpecBasedCurationService.java`,
  `InMemoryCurationExtensionRepository.java`, `CurationFixtureFactory.java`,
  관련 테스트
- Depends: 1, 2
- Size: M
- Status: [x] done

## Progress Log

- Task 1 완료 (8c3ce6d4): 서브모듈 main에 V4 마이그레이션 푸시(환경변수 저장소
  b1eeffb), 본 저장소 포인터 갱신 + `CurationExtension.feedPayload` 매핑.
  ProductSpecBasedCurationIntegrationTest 14건 통과로 Flyway V4 적용 및
  Hibernate validate 확인.
- Task 2 완료 (20fbb541): `CurationFeedProjector.extractFeedPayload` 추가.
  x-feed 투영 + 피드 경로 교차 x-graphql의 argFrom 숨은 입력값 병합,
  GraphQL 미실행. 피드 경로 수집은 items 재귀 포함(materializer와 동일 의미).
  단위 테스트 5건 통과(4개 스펙 리소스 + 숨은 입력값 시나리오).
- Checkpoint(1-2) 통과: unit_test, check_rule_test 전체 그린.
- Task 3 완료 (6dc5bea4): create/update 이중 쓰기. 레포지토리 인터페이스
  변경이 없어 InMemory 레포지토리·픽스처 갱신은 불필요로 판명(acceptance
  advisory 조정). mono 단위 34건, admin 통합 17건 통과(실제 DB JSON
  라운드트립 포함).
- PR #677 리뷰 반영 (925ddfeb): `argFrom`을 루트 기준으로 탐색해
  `payloadPath` 하위(`whisky_tasting_event`)의 숨은 입력값이 누락되던 결함
  수정. 남길 스키마 경로를 미리 모아 투영 재귀에 넘기는 방식으로 재작성해
  배열 원소에서도 경로가 어긋나지 않게 했고, `setAtPath`·`extractObject`가
  제거됐다. 남길 값이 없으면 null 대신 빈 컨테이너를 저장해 backfill 이전
  레거시 행과 구분한다. 피드 경로 해석 규칙은 `CurationFeedPaths`로 추출해
  materializer와 공유한다. 신규 단위 테스트 2건은 수정 전 코드에서 실패를
  확인했다.
- PR #677 리뷰 반영 (1ef1a777): Task 3의 "픽스처 갱신 불필요" 판단을
  뒤집는다. `CurationFixtureFactory`가 feed_payload를 비워두면 후속 읽기
  경로 테스트가 NULL fallback 분기만 타게 되므로 운영과 같은 추출 결과를
  남기도록 했다.
- 리뷰 반영 후 검증: unit_test 459건(mono 233 + product 205 +
  observability 21), integration_test 254건, admin_integration_test 206건
  전부 통과. check_rule_test·spotlessCheck 그린.
- 서브모듈 포인터 6611431로 최신화. 개발 환경 이미지 태그 범프 2건이며
  `storage/` 변경은 없다(V4 이후 신규 마이그레이션 없음).
