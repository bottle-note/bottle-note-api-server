# Plan: Admin Bulk Reorder API

## Overview

어드민에서 드래그로 정렬한 항목을 한 번에 저장할 수 있도록 bulk reorder API를 추가한다.
기존 단건 정렬 API는 유지하고, bulk API는 모든 도메인에서 `/bulk/reorder` 경로로 통일한다.

공통 요청 형식은 다음과 같다.

```json
{
  "ids": [3, 1, 6, 5]
}
```

`ids`는 전체 목록이 아니라 해당 scope의 맨 앞 구간으로 올릴 항목들의 최종 순서다.
요청 ID는 최소 1개, 최대 100개까지만 허용한다.

정렬 예시는 다음과 같다.

```text
변경 전

정렬 슬롯:   1    10    20    30    40
          +----+-----+-----+-----+-----+
항목 ID:  | 11 |  1  |  3  |  5  |  6  |
          +----+-----+-----+-----+-----+

요청

{ "ids": [3, 1, 6, 5] }

변경 후

정렬 슬롯:   1    10    20    30    40
          +----+-----+-----+-----+-----+
항목 ID:  |  3 |  1  |  6  |  5  | 11  |
          +----+-----+-----+-----+-----+
```

요청에 포함된 항목은 scope의 맨 앞 정렬 슬롯부터 배열 순서대로 배정된다.
기존 앞쪽에 있던 미포함 항목은 뒤로 밀리고, 나머지 미포함 항목의 상대 순서는 유지된다.

## Assumptions

1. 대상 API는 admin-api이며, 비즈니스 로직은 기존 패턴대로 bottlenote-mono 서비스에 둔다.
2. 인증/인가 정책은 기존 어드민 API 정책을 그대로 따른다.
3. DB schema 변경은 하지 않는다. 기존 `sortOrder` 또는 `displayOrder` 필드를 재사용한다.
4. 기존 단건 API 동작은 변경하지 않는다.
5. bulk reorder는 임의 위치 삽입이 아니라 scope의 앞쪽으로 올리는 동작이다.
6. `ids`에 없는 항목은 삭제/비활성/숨김 처리하지 않고 정렬 위치만 필요 시 뒤로 밀린다.
7. 요청 ID가 100개를 초과하면 validation 실패로 처리한다.
8. 중복 ID, 존재하지 않는 ID, scope가 다른 ID는 실패 처리한다.
9. 지역 자식 정렬 API의 path variable은 부모 지역 ID인 `parentId`다.
10. 배너와 큐레이션은 비활성 또는 미노출 항목도 정렬값을 가질 수 있다.

## Success Criteria

1. `PATCH /admin/api/v1/banners/bulk/reorder`가 전체 배너 scope에서 `sortOrder`를 재배치한다.
2. `PATCH /admin/api/v1/curations/bulk/reorder`가 전체 큐레이션 scope에서 `displayOrder`를 재배치한다.
3. `PATCH /admin/api/v1/distilleries/bulk/reorder`가 전체 증류소 scope에서 `sortOrder`를 재배치한다.
4. `PATCH /admin/api/v1/regions/bulk/reorder`가 전체 지역 scope에서 `sortOrder`를 재배치한다.
5. `PATCH /admin/api/v1/regions/{parentId}/children/bulk/reorder`가 해당 `parentId`의 직접 자식 지역 scope에서만 `sortOrder`를 재배치한다.
6. 모든 bulk reorder 요청은 하나의 트랜잭션에서 처리된다.
7. 요청 ID 순서대로 scope의 맨 앞 정렬 슬롯에 배정된다.
8. 기존 앞쪽 미포함 항목은 뒤로 밀리고, 나머지 미포함 항목의 상대 순서는 유지된다.
9. 요청 ID가 비어 있거나 100개를 초과하면 validation 실패가 발생한다.
10. 요청 ID가 중복되면 validation 실패가 발생한다.
11. 존재하지 않는 ID가 포함되면 도메인 예외가 발생한다.
12. 지역 자식 정렬에서 다른 부모의 지역 ID가 포함되면 scope 검증 실패가 발생한다.
13. 단건 정렬 API와 bulk reorder API의 차이를 admin API adoc 문서에 설명한다.
14. adoc 문서에 ASCII 정렬 예시를 포함한다.
15. 배너/큐레이션 문서에 비활성 또는 미노출 항목이 정렬값을 가지지만 조회 필터에 따라 화면에 보이지 않을 수 있음을 예시와 함께 설명한다.
16. 관련 단위 테스트, RestDocs 테스트, full verify가 통과한다.

## Impact Scope

### Modules

- `bottlenote-admin-api`: admin controller endpoint 및 RestDocs 테스트
- `bottlenote-mono`: request DTO, service reorder logic, repository query, exception/result code, unit test fixture

### API Surface

추가 API:

```text
PATCH /admin/api/v1/banners/bulk/reorder
PATCH /admin/api/v1/curations/bulk/reorder
PATCH /admin/api/v1/distilleries/bulk/reorder
PATCH /admin/api/v1/regions/bulk/reorder
PATCH /admin/api/v1/regions/{parentId}/children/bulk/reorder
```

유지 API:

```text
PATCH /admin/api/v1/banners/{bannerId}/sort-order
PATCH /admin/api/v1/curations/{curationId}/display-order
PATCH /admin/api/v1/distilleries/{distilleryId}/sort-order
PATCH /admin/api/v1/regions/{regionId}/sort-order
```

### Persistence

- schema 변경 없음
- 기존 `sortOrder` / `displayOrder` 컬럼 업데이트만 수행
- 대량 ID는 최대 100개로 제한

### Domain Rules

- 배너: 전체 배너 기준 `sortOrder`
- 큐레이션: 전체 큐레이션 기준 `displayOrder`
- 증류소: 전체 증류소 기준 `sortOrder`
- 지역 전체: 전체 지역 기준 `sortOrder`
- 지역 자식: `parentId`의 직접 자식 지역 기준 `sortOrder`

### Tests

- service unit test: 재배치 성공, 중복 ID, 빈 요청, 100개 초과, 존재하지 않는 ID, scope mismatch
- fake/in-memory repository: bulk 조회와 정렬 검증 지원
- RestDocs test: 5개 endpoint 문서 snippet 생성
- verification: GSL full verify 전 `./gradlew test` baseline은 이미 통과한 상태에서 시작

### Documentation

- `bottlenote-admin-api/src/docs/asciidoc/api/admin-banners/banners.adoc`
- `bottlenote-admin-api/src/docs/asciidoc/api/admin-curations/curations.adoc`
- `bottlenote-admin-api/src/docs/asciidoc/api/admin-reference/reference.adoc`

문서에는 FE가 이해하기 쉽도록 단건 API와 bulk API의 차이, 최대 100개 제한, 앞쪽 이동 방식, 비활성/미노출 항목 주의사항, 지역 자식 scope 검증을 명확히 기록한다.

## Tasks

### Task 1: 공통 bulk reorder 요청 계약

- Acceptance: 공통 request DTO가 `ids` 최소 1개, 최대 100개, null/0 이하 ID를 Bean Validation으로 막는다.
- Verification: `./gradlew :bottlenote-mono:compileJava :bottlenote-admin-api:compileKotlin`
- Files:
  - Create: `bottlenote-mono/src/main/java/app/bottlenote/global/dto/request/AdminBulkReorderRequest.java`
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/global/exception/custom/code/ValidExceptionCode.java`
- Size: S
- Status: [x] done

### Task 2: 배너 bulk reorder core

- Acceptance: 전체 배너 scope에서 요청 ID가 맨 앞 슬롯으로 이동하고, 미포함 항목의 상대 순서가 유지된다.
- Verification: `./gradlew :bottlenote-mono:test --tests app.bottlenote.banner.service.AdminBannerServiceTest`
- Files:
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/banner/domain/BannerRepository.java`
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/banner/repository/JpaBannerRepository.java`
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/banner/service/AdminBannerService.java`
  - Modify: `bottlenote-mono/src/test/java/app/bottlenote/banner/fixture/InMemoryBannerRepository.java`
  - Create: `bottlenote-mono/src/test/java/app/bottlenote/banner/service/AdminBannerServiceTest.java`
- Size: M
- Status: [x] done

### Task 3: 배너 bulk reorder API와 문서

- Acceptance: `PATCH /banners/bulk/reorder`가 RestDocs snippet을 생성하고 adoc에 단건 API와 bulk API 차이, 비활성 항목 예시가 포함된다.
- Verification: `./gradlew :bottlenote-admin-api:test --tests app.docs.banner.AdminBannerControllerDocsTest`
- Files:
  - Modify: `bottlenote-admin-api/src/main/kotlin/app/bottlenote/banner/presentation/AdminBannerController.kt`
  - Modify: `bottlenote-admin-api/src/test/kotlin/app/docs/banner/AdminBannerControllerDocsTest.kt`
  - Modify: `bottlenote-admin-api/src/docs/asciidoc/api/admin-banners/banners.adoc`
- Size: S
- Status: [x] done

### Checkpoint: after Tasks 1-3

- [x] Compile passes
- [x] Banner unit test passes
- [x] Banner RestDocs test passes

### Task 4: 큐레이션 bulk reorder core

- Acceptance: 전체 큐레이션 scope에서 요청 ID가 맨 앞 `displayOrder` 슬롯으로 이동하고, 미포함 항목의 상대 순서가 유지된다.
- Verification: `./gradlew :bottlenote-mono:test --tests app.bottlenote.alcohols.service.AdminCurationServiceTest`
- Files:
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/domain/CurationKeywordRepository.java`
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/JpaCurationKeywordRepository.java`
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/service/AdminCurationService.java`
  - Create: `bottlenote-mono/src/test/java/app/bottlenote/alcohols/fixture/InMemoryCurationKeywordRepository.java`
  - Create: `bottlenote-mono/src/test/java/app/bottlenote/alcohols/service/AdminCurationServiceTest.java`
- Size: M
- Status: [x] done

### Task 5: 증류소 bulk reorder core

- Acceptance: 전체 증류소 scope에서 요청 ID가 맨 앞 `sortOrder` 슬롯으로 이동하고, 미포함 항목의 상대 순서가 유지된다.
- Verification: `./gradlew :bottlenote-mono:test --tests app.bottlenote.alcohols.service.DistilleryServiceTest`
- Files:
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/domain/DistilleryRepository.java`
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/JpaDistilleryRepository.java`
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/service/DistilleryService.java`
  - Modify: `bottlenote-mono/src/test/java/app/bottlenote/alcohols/fixture/InMemoryDistilleryRepository.java`
  - Modify: `bottlenote-mono/src/test/java/app/bottlenote/alcohols/service/DistilleryServiceTest.java`
- Size: M
- Status: [x] done

### Task 6: 큐레이션/증류소 API와 문서

- Acceptance: `PATCH /curations/bulk/reorder`, `PATCH /distilleries/bulk/reorder`가 RestDocs snippet을 생성하고 adoc에 bulk reorder 예시와 주의사항이 포함된다.
- Verification: `./gradlew :bottlenote-admin-api:test --tests app.docs.curation.AdminCurationControllerDocsTest --tests app.docs.alcohols.AdminDistilleryControllerDocsTest`
- Files:
  - Modify: `bottlenote-admin-api/src/main/kotlin/app/bottlenote/alcohols/presentation/AdminCurationController.kt`
  - Modify: `bottlenote-admin-api/src/main/kotlin/app/bottlenote/alcohols/presentation/AdminDistilleryController.kt`
  - Modify: `bottlenote-admin-api/src/test/kotlin/app/docs/curation/AdminCurationControllerDocsTest.kt`
  - Modify: `bottlenote-admin-api/src/test/kotlin/app/docs/alcohols/AdminDistilleryControllerDocsTest.kt`
  - Modify: `bottlenote-admin-api/src/docs/asciidoc/api/admin-curations/curations.adoc`
  - Modify: `bottlenote-admin-api/src/docs/asciidoc/api/admin-reference/reference.adoc`
- Size: M
- Status: [x] done

### Checkpoint: after Tasks 4-6

- [x] Curation unit test passes
- [x] Distillery unit test passes
- [x] Curation/Distillery RestDocs tests pass

### Task 7: 지역 전체/자식 bulk reorder core

- Acceptance: 전체 지역 scope와 `parentId` 직접 자식 scope가 분리되고, 자식 API에서 다른 부모 ID가 섞이면 실패한다.
- Verification: `./gradlew :bottlenote-mono:test --tests app.bottlenote.alcohols.service.AdminRegionServiceTest`
- Files:
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/domain/RegionRepository.java`
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/JpaRegionQueryRepository.java`
  - Modify: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/service/AdminRegionService.java`
  - Modify: `bottlenote-mono/src/test/java/app/bottlenote/alcohols/fixture/InMemoryRegionRepository.java`
  - Modify: `bottlenote-mono/src/test/java/app/bottlenote/alcohols/service/AdminRegionServiceTest.java`
- Size: M
- Status: [x] done

### Task 8: 지역 API와 문서

- Acceptance: `PATCH /regions/bulk/reorder`, `PATCH /regions/{parentId}/children/bulk/reorder`가 RestDocs snippet을 생성하고 adoc에 전체 지역과 자식 지역 scope 차이가 포함된다.
- Verification: `./gradlew :bottlenote-admin-api:test --tests app.docs.alcohols.AdminRegionControllerDocsTest`
- Files:
  - Modify: `bottlenote-admin-api/src/main/kotlin/app/bottlenote/alcohols/presentation/AdminRegionController.kt`
  - Modify: `bottlenote-admin-api/src/test/kotlin/app/docs/alcohols/AdminRegionControllerDocsTest.kt`
  - Modify: `bottlenote-admin-api/src/docs/asciidoc/api/admin-reference/reference.adoc`
- Size: S
- Status: [x] done

### Task 9: full verification과 PR

- Acceptance: GSL `/verify full`에 해당하는 전체 검증이 통과하고 PR이 생성된다.
- Verification:
  - `./gradlew check_rule_test`
  - `./gradlew unit_test integration_test`
  - `./gradlew admin_integration_test`
  - `./gradlew :bottlenote-admin-api:test`
  - `./gradlew asciidoctor`
- Files:
  - Modify: `plan/admin-bulk-reorder.md`
- Size: S
- Status: [x] done

## Progress Log

- 2026-05-23: `/define` 완료. baseline `./gradlew test` BUILD SUCCESSFUL in 1m 37s.
- 2026-05-23: `/plan` 태스크 9개로 분해.
- 2026-05-23: `codex/alcohol-lookup` 기반 워크트리 변경분을 `origin/main` 기반 `feat/admin-bulk-reorder-main` 워크트리로 충돌 없이 이식.
- 2026-05-23: `./gradlew integration_test` 최초 실패 원인은 새 워크트리의 `git.environment-variables` 서브모듈 미초기화로 확인. `git submodule update --init --recursive` 후 재실행 통과.
- 2026-05-23: `/verify full` 완료. compile, check_rule_test, unit_test, build, integration_test, admin_integration_test, admin-api test, asciidoctor 모두 BUILD SUCCESSFUL.
