# Plan: 복수 코드 기반 큐레이션 피드 조회 개선

## Overview

Product API의 `GET /api/v2/curations/feed`가 하나 이상의 큐레이션 스펙 코드를
받아 필요한 후보만 DB에서 먼저 선별하도록 개선한다. 피드 응답 계약과 기존
`x-feed` 기반 GraphQL hydration 규칙은 유지하면서, 전체 노출 큐레이션 및
payload를 메모리에 적재하던 방식과 Extension N+1 조회를 제거한다.

### Assumptions

- 변경 대상은 `bottlenote-product-api`의 공개 피드 API와
  `bottlenote-mono`의 큐레이션 조회 경로다.
- 쿼리 파라미터 이름은 `code`를 유지하고 배열로 받는다.
  복수 값은 `?code=PROGRAM&code=WHISKY_TASTING_EVENT` 형태로 전달한다.
- `code`는 한 개 이상 필수다. 누락, 빈 배열, 공백 값은 유효하지 않은
  요청으로 처리한다.
- 여러 `code` 값은 OR 조건으로 조회하며, `keyword`가 함께 있으면 코드
  조건과 AND로 결합한다.
- 존재하지 않는 코드만 전달된 경우 오류가 아니라 빈 피드 결과를 반환한다.
- 노출 여부는 기존과 동일하게 활성 상태와 노출 시작일·종료일을 기준으로
  판단한다.
- 정렬은 기존 `displayOrder ASC, id ASC`를 유지한다.
- 현재 `cursor`는 마지막 ID가 아닌 offset으로 사용되고 있으므로 이번
  변경에서도 그 의미를 유지한다.
- `size`의 기본값 10과 최대값 10을 유지한다.
- 후보 조회는 조건에 맞는 큐레이션 ID를 `size + 1`개까지만 먼저 조회하고,
  응답에 필요한 큐레이션·스펙·Extension은 후보 ID 기준으로 일괄 조회한다.
- 피드 응답의 공통 필드와 `payload` 구조는 변경하지 않는다.
- GraphQL 조회는 기존처럼 `x-feed.enabled=true`인 응답 필드와 교차하는
  쿼리만 실행한다.
- `feed_source_payload` Read Model, DB 스키마, 캐시, Admin API는 이번 PR의
  범위에 포함하지 않는다.

### Success Criteria

- `GET /api/v2/curations/feed?code=PROGRAM` 요청이 단일 코드 피드를 반환한다.
- `GET /api/v2/curations/feed?code=PROGRAM&code=WHISKY_TASTING_EVENT` 요청이
  두 코드 중 하나에 해당하는 피드를 반환한다.
- `keyword`와 복수 `code`를 함께 전달하면 두 조건을 모두 만족하는
  큐레이션만 반환한다.
- `code` 누락, 공백 값 또는 빈 값은 HTTP 400으로 거부된다.
- 존재하지 않는 코드만 전달하면 HTTP 200과 빈 `items`를 반환한다.
- 활성·노출 기간 조건, 정렬, `cursor`, `size`, `hasNext` 동작이 기존 계약과
  동일하다.
- 응답의 필드명, 타입, 중첩 구조와 `x-feed` projection 결과가 변경 전과
  동일하다.
- 피드 조회 경로에서 전체 노출 큐레이션을 조회하는
  `findAllVisibleOn`을 사용하지 않는다.
- 첫 후보 조회 결과는 최대 `size + 1`, 즉 현재 제한에서 최대 11개 ID다.
- 후보 큐레이션의 스펙과 Extension은 항목별 조회가 아닌 각각 일괄 조회한다.
- 단위 테스트는 단일 코드, 복수 코드, keyword 결합, 필수값 검증, 미존재
  코드, 페이지 경계를 검증한다.
- Product 통합 테스트와 REST Docs 테스트가 요청 파라미터 및 기존 응답
  호환성을 검증한다.

### Impact Scope

- **Product API**:
  `ProductSpecBasedCurationController`의 `code` 파라미터 타입과 필수값 검증,
  REST Docs 요청 계약이 변경된다.
- **Mono service**:
  `ProductSpecBasedCurationService`의 메모리 필터링·페이지 분할을 후보 ID
  기반 조회와 일괄 조립으로 교체한다.
- **Persistence**:
  큐레이션 후보 ID 조회와 후보 엔티티 일괄 조회를 위한 도메인 포트 및
  JPA/QueryDSL 구현이 필요하다. Extension 저장소에도 후보 ID 기반 일괄
  조회가 필요하다.
- **GraphQL**:
  SDL과 executor 계약은 변경하지 않는다. 후보 수가 제한된 이후 기존
  `materializeFeed` 흐름을 사용한다.
- **Schema / migration**:
  DB 컬럼, 테이블, 인덱스 변경은 없다.
- **Admin / batch / events / cache**:
  변경하지 않는다.
- **Tests / docs**:
  Mono 단위 테스트, Product 통합 테스트, Product REST Docs가 영향을 받는다.
- **외부 계약**:
  응답은 호환되지만 요청의 `code`가 선택값에서 한 개 이상 필수인 배열로
  변경된다. 호출자는 동일한 파라미터를 반복해 복수 코드를 전달해야 한다.

## Tasks

### Task 1: 피드 대상 스펙 일괄 해석

- Acceptance:
  - 전달된 단일·복수 code에 해당하는 스펙을 한 번에 조회한다.
  - 존재하지 않는 code가 포함돼도 조회된 스펙만 반환한다.
  - JPA 구현과 InMemory 구현이 동일한 계약을 따른다.
- Verification:
  `./gradlew :bottlenote-mono:compileJava :bottlenote-test-support:compileJava`
- Files:
  `CurationSpecRepository.java`,
  `JpaCurationSpecRepository.java`,
  `InMemoryCurationSpecRepository.java`
- Size: S
- Status: [x] done

### Task 2: 제한된 피드 후보 조회 경로 구축

- Acceptance:
  - 활성 상태, 노출 기간, keyword, 스펙 ID 조건을 DB에서 적용해
    `displayOrder ASC, id ASC` 순서의 큐레이션 ID만 반환한다.
  - offset cursor부터 `size + 1`개까지만 조회하며, 후보 엔티티도 ID로
    일괄 조회한다.
  - 도메인 포트에 `Pageable`이나 QueryDSL 타입을 노출하지 않고 실제 JPA
    구현과 InMemory 구현이 동일한 계약을 따른다.
- Verification:
  `./gradlew :bottlenote-mono:compileJava :bottlenote-test-support:compileJava`
- Files:
  `CurationFeedSearchCriteria.java`,
  `CurationRepository.java`,
  `CustomCurationFeedRepository.java`,
  `CustomCurationFeedRepositoryImpl.java`,
  `JpaCurationRepository.java`,
  `InMemoryCurationRepository.java`
- Size: M
- Status: [x] done

### Task 3: 후보 피드 일괄 조립

- Acceptance:
  - 서비스는 전체 노출 큐레이션을 읽지 않고 Task 1과 Task 2의 결과만
    사용한다.
  - 후보 큐레이션, 스펙, Extension을 각각 일괄 조회하고 기존 항목 순서를
    보존한다.
  - 기존 `materializeFeed`와 응답 projection을 유지하며 최대 크기와
    offset cursor 경계 테스트가 통과한다.
- Verification:
  `./gradlew :bottlenote-mono:unit_test --tests '*ProductSpecBasedCurationServiceTest'`
- Files:
  `CurationExtensionRepository.java`,
  `JpaCurationExtensionRepository.java`,
  `InMemoryCurationExtensionRepository.java`,
  `ProductSpecBasedCurationService.java`,
  `ProductSpecBasedCurationServiceTest.java`
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 1-3

- [ ] Mono와 test-support가 컴파일된다.
- [ ] Product 큐레이션 서비스 단위 테스트가 통과한다.
- [ ] 도메인 저장소 포트에 Spring Data 또는 QueryDSL 타입이 추가되지 않는다.

### Task 4: code 배열 HTTP 계약 반영

- Acceptance:
  - Controller가 동일한 `code` 쿼리 파라미터를 한 개 이상의 문자열 배열로
    받아 서비스에 전달한다.
  - `code` 누락, 빈 값, 공백 값은 HTTP 400으로 거부된다.
  - REST Docs가 단일·복수 code 요청과 변경되지 않은 응답 필드를 문서화한다.
- Verification:
  `./gradlew :bottlenote-product-api:test --tests '*RestProductSpecBasedCurationControllerTest'`
- Files:
  `CurationFeedSearchRequest.java`,
  `ValidExceptionCode.java`,
  `ProductSpecBasedCurationController.java`,
  `RestProductSpecBasedCurationControllerTest.java`
- Size: M
- Status: [x] done

### Task 5: 피드 조회 계약 통합 검증

- Acceptance:
  - 실제 DB에서 단일 code, 복수 code OR, keyword AND, 미존재 code 조건을
    검증한다.
  - 노출 조건, 정렬, offset cursor, `hasNext`, 최대 10개 반환을 검증한다.
  - 기존 응답 구조와 `x-feed` projection이 유지됨을 검증한다.
- Verification:
  `./gradlew :bottlenote-product-api:integration_test --tests '*ProductSpecBasedCurationIntegrationTest'`
- Files:
  `ProductSpecBasedCurationIntegrationTest.java`
- Size: S
- Status: [ ] not done

### Checkpoint: after Tasks 4-5

- [ ] Product REST Docs 테스트가 통과한다.
- [ ] Product 큐레이션 통합 테스트가 통과한다.
- [ ] 요청 파라미터 외 기존 피드 응답 계약에 변경이 없다.

## Progress Log

- Task 1 완료: 복수 code 기반 스펙 일괄 조회 포트를 JPA와 InMemory 구현에
  추가했다. Mono와 test-support 컴파일을 통과했다.
- Task 2 완료: 노출·스펙·keyword 조건과 offset/limit을 적용하는 후보 ID
  조회를 QueryDSL과 InMemory에 추가했다. Mono와 test-support 컴파일을
  통과했다.
- Task 3 완료: 후보 ID의 큐레이션과 Extension만 일괄 조회하고 최대 10건만
  hydration하도록 서비스 흐름을 교체했다. Product 큐레이션 서비스 단위
  테스트를 통과했다.
- Task 4 완료: `code` 반복 파라미터를 필수 배열로 바꾸고 누락·공백 요청을
  HTTP 400으로 검증했다. Product REST Docs 테스트 5개를 통과했다.
