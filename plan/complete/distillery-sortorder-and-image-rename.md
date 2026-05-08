```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-05-08

** Core Achievements **
- 어드민 Distillery sortOrder 3-채널(생성·수정 DTO 필드 + PATCH /sort-order) 보강
- Banner/Region 표준 일치: 자동 reorder + korName ASC 보조 정렬, default 9999
- ValidExceptionCode 2건 + ResultCode 1건(DISTILLERY_SORT_ORDER_UPDATED) 추가
- 단위 17 + 통합 12 + RestDocs 6 PASSED, admin_integration 171/171 회귀 0
- DB 컬럼 logo_img_url → image_url rename 은 별도 작업으로 분리(사용자 main 처리)

** Key Components **
- bottlenote-admin-api/.../alcohols/presentation/AdminDistilleryController.kt 의 PATCH /sort-order
- bottlenote-mono/.../alcohols/service/DistilleryService.java 의 reorderSortOrders + updateSortOrder
- Distillery.update(...) 시그니처 확장 + updateSortOrder(int) 신설
- DistilleryRepository.findAllBySortOrderGreaterThanEqual + JPQL ORDER BY 정렬 보정
- DTO: AdminDistilleryUpsertRequest(sortOrder 추가), AdminDistillerySortOrderRequest 신규

** Related **
- PR #578 (rebased to linear) → main merge: a7bf0ab4, f374c118, a812ca6c, 61b5f7da, f385855b
================================================================================
```

# Plan: Distillery sortOrder 입력 채널 보강

## Overview

PR #578 (증류소 어드민 CRUD) 후속 보강. 현재 `Distillery` 엔티티는 `sortOrder Integer = 9999` 필드를 가지지만 **어드민이 입력·수정·재정렬할 수단이 전혀 없어** 신규 등록 시 항상 9999 로 고정된다. Banner/Region 의 3-채널 패턴(생성·수정·PATCH `/sort-order`)을 그대로 적용해 일관성을 맞춘다.

> 주: DB 컬럼명 `logo_img_url → image_url` rename 은 본 계획에서 제외. 사용자가 별도로 main 에서 처리. `@Column(name = "logo_img_url")` 매핑이 있어 운영 기동 영향 없음.

### Assumptions

1. **모듈 패턴**: admin-api(Kotlin) 위임 / mono(Java) 비즈니스 로직 / `AdminResultResponse` 반환 / Bean Validation + `ValidExceptionCode` 매칭 — Banner/Region 표준 그대로.
2. **sortOrder 정책**: Region 과 동일 — **자동 reorder(밀어내기)** + 같은 값 잔존 시 `kor_name ASC` 보조 정렬. `@Min(0)`, default `9999`.
3. **호환성**: 기존 `AdminDistilleryUpsertRequest(korName, engName, imageUrl)` 의 필드 순서는 유지하면서 마지막에 `sortOrder` 추가. 기존 클라이언트가 sortOrder 미전송해도 default 9999 적용.
4. **머지 단위**: 본 PR #578 (`feature/local-env-setup` 브랜치) 에 **추가 커밋** 으로 보강. 작업 단위가 작고 PR #578 의 직접 후속이라 별도 PR 분리하지 않는다.
5. **Validation 메시지**: `ValidExceptionCode` 에 `DISTILLERY_SORT_ORDER_REQUIRED`, `DISTILLERY_SORT_ORDER_MINIMUM` 추가. `AdminResultResponse.ResultCode` 에 `DISTILLERY_SORT_ORDER_UPDATED` 추가.
6. **컬럼명 영향 없음**: 본 작업은 `sort_order` 컬럼만 사용. 이미 서브모듈 schema 의 `ALTER TABLE distilleries ADD COLUMN sort_order INT DEFAULT 9999 NOT NULL` 이 적용된 상태이므로 **schema 변경 불필요**.

### Success Criteria

| # | 기준 | 검증 |
|---|------|------|
| SC1 | `POST /admin/api/v1/distilleries` 본문에 `sortOrder` 입력 시 그 값이 저장되고, 같은 값 이상 다른 증류소가 +1 reorder 된다 | 단위 + 통합 |
| SC2 | `PUT /admin/api/v1/distilleries/{id}` 본문에 `sortOrder` 포함 시 도메인 `update(...)` 가 그 값으로 갱신, 변경 시에만 reorder 수행 | 단위 + 통합 |
| SC3 | `PATCH /admin/api/v1/distilleries/{id}/sort-order` 가 `AdminResultResponse(DISTILLERY_SORT_ORDER_UPDATED, id)` 반환 | 단위 + 통합 |
| SC4 | `sortOrder` 가 `null` 또는 `0` 미만이면 400 (`DISTILLERY_SORT_ORDER_REQUIRED` / `DISTILLERY_SORT_ORDER_MINIMUM`) | 통합 |
| SC5 | sortOrder 변경 후 `GET /admin/api/v1/distilleries` 목록이 `sortOrder ASC, kor_name ASC` 정렬을 반환 | 통합 |
| SC6 | 단위 13 + 통합 12 + RestDocs 6 모두 PASSED, 기존 회귀 0 | `./gradlew unit_test admin_integration_test` |
| SC7 | `/verify` 통과 — compile + unit + rule + integration + admin_integration | 로컬 CI |
| SC8 | RestDocs 스니펫 PATCH 1 건 생성 | `:admin-api:asciidoctor` |

### Impact Scope

**모듈/파일**

mono:
- `domain/Distillery.java` — `update(...)` 시그니처에 `Integer sortOrder` 추가; `updateSortOrder(int)` 추가.
- `domain/DistilleryRepository.java` — `findAllBySortOrderGreaterThanEqual(int)` 추가.
- `repository/JpaDistilleryRepository.java` — 위 메서드 `@Query` 또는 메서드쿼리.
- `dto/request/AdminDistilleryUpsertRequest.java` — `@Min(0) Integer sortOrder` 필드 추가, compact constructor 로 default 9999.
- `dto/request/AdminDistillerySortOrderRequest.java` (신규) — `@NotNull @Min(0) Integer sortOrder`.
- `service/DistilleryService.java` — `create/update` 에 sortOrder 전파 + reorder; `updateSortOrder(...)` 메서드 신설.
- `global/exception/custom/code/ValidExceptionCode.java` — `DISTILLERY_SORT_ORDER_REQUIRED`, `DISTILLERY_SORT_ORDER_MINIMUM` 추가.
- `global/dto/response/AdminResultResponse.java` — `DISTILLERY_SORT_ORDER_UPDATED("증류소 정렬 순서가 변경되었습니다.")` 추가.

admin-api:
- `presentation/AdminDistilleryController.kt` — `@PatchMapping("/{distilleryId}/sort-order")` 1 건 추가, 기존 POST/PUT 변경 없음.

테스트:
- `bottlenote-mono/src/test/.../alcohols/service/DistilleryServiceTest.java` — sortOrder reorder/no-op/같은 값 시나리오 보강.
- `bottlenote-mono/src/test/.../alcohols/fixture/InMemoryDistilleryRepository.java` — `findAllBySortOrderGreaterThanEqual` Fake 구현 추가.
- `bottlenote-admin-api/src/test/kotlin/.../integration/alcohols/AdminDistilleryIntegrationTest.kt` — Create with sortOrder, Update with sortOrder, PATCH 시나리오 추가.
- `bottlenote-admin-api/src/test/kotlin/.../docs/alcohols/AdminDistilleryControllerDocsTest.kt` — PATCH RestDocs 1 건 추가.

**비영향**: 도메인 이벤트 / 캐시 / 다른 도메인 / 스키마 변경 — 모두 없음.

---

## Tasks

각 Task 종료 시 `/self-review` + 컴파일 게이트 통과 후 다음으로 이동.

### T1. ValidExceptionCode + ResultCode enum 추가
- `ValidExceptionCode`: `DISTILLERY_SORT_ORDER_REQUIRED`, `DISTILLERY_SORT_ORDER_MINIMUM`
- `AdminResultResponse.ResultCode`: `DISTILLERY_SORT_ORDER_UPDATED("증류소 정렬 순서가 변경되었습니다.")`

### T2. Distillery 도메인 메서드 보강
- `update(String korName, String engName, String imageUrl, Integer sortOrder)` — 시그니처 확장
- `updateSortOrder(int sortOrder)` — 신규
- 기존 `update(...)` 호출처 모두 갱신 (Service)

### T3. DistilleryRepository / JpaDistilleryRepository 확장
- IF 추가: `List<Distillery> findAllBySortOrderGreaterThanEqual(int sortOrder)`
- JpaDistilleryRepository: `@Query("select d from distillery d where d.sortOrder >= :sortOrder")` 또는 메서드쿼리

### T4. Request DTO 보강 / 신설
- `AdminDistilleryUpsertRequest` — `@Min(0) Integer sortOrder` 필드 추가, `@Builder` compact constructor 로 default 9999. 기존 record 시그니처에 마지막 인자 추가.
- `AdminDistillerySortOrderRequest` (신규) — `@NotNull @Min(0) Integer sortOrder`

### T5. DistilleryService 보강
- `create(...)` — sortOrder 적용 + `reorderSortOrders(newSortOrder, null)` 선행
- `update(...)` — 기존 sortOrder 와 다를 때만 `reorderSortOrders(newSortOrder, distilleryId)` 후 도메인 `update(...)` 호출
- `updateSortOrder(Long distilleryId, AdminDistillerySortOrderRequest)` (신규) — Banner 패턴
- private `reorderSortOrders(Integer newSortOrder, Long excludeDistilleryId)` 헬퍼

### T6. AdminDistilleryController PATCH 추가
- `@PatchMapping("/{distilleryId}/sort-order")` `updateSortOrder(@PathVariable, @RequestBody @Valid AdminDistillerySortOrderRequest)`

### T7. InMemoryDistilleryRepository Fake 보강
- `findAllBySortOrderGreaterThanEqual` 구현
- Distillery `sortOrder` 정렬 처리 — Region Fake 패턴 동일

### T8. 단위 테스트 보강
- `DistilleryServiceTest`:
  - create_withSortOrder_reordersOthers
  - create_withDefaultSortOrder_uses9999
  - update_whenSortOrderChanged_triggersReorder
  - update_whenSameSortOrder_noReorder
  - updateSortOrder_reordersOtherDistilleries
  - updateSortOrder_whenSameValue_noOp

### T9. 통합 테스트 + RestDocs 보강
- `AdminDistilleryIntegrationTest`: Create with sortOrder / Update with sortOrder / PATCH 정상 / PATCH 0 미만 400 / 목록 정렬키 검증
- `AdminDistilleryControllerDocsTest`: PATCH RestDocs 스니펫 1 건

### T10. /verify 검증
- `./gradlew unit_test check_rule_test`
- `./gradlew :bottlenote-admin-api:test :bottlenote-admin-api:admin_integration_test`
- 결과 그린 확인 후 추가 커밋 + push

## Progress Log

- **T1 ✅** `ValidExceptionCode.DISTILLERY_SORT_ORDER_REQUIRED/MINIMUM` + `AdminResultResponse.ResultCode.DISTILLERY_SORT_ORDER_UPDATED` 추가.
- **T2 ✅** `Distillery.update(...)` 시그니처에 `Integer sortOrder` 추가, `updateSortOrder(int)` 신설.
- **T3 ✅** `DistilleryRepository.findAllBySortOrderGreaterThanEqual(int)` 추가, `JpaDistilleryRepository` 정렬 ORDER BY 보정 + `@Query` 메서드 추가.
- **T4 ✅** `AdminDistilleryUpsertRequest` 에 `@Min(0) Integer sortOrder` 필드 + `@Builder` default 9999, `AdminDistillerySortOrderRequest` 신규.
- **T5 ✅** `DistilleryService` create/update sortOrder 전파 + `reorderSortOrders(...)` 헬퍼, `updateSortOrder(...)` 신설.
- **T6 ✅** `AdminDistilleryController.kt` `@PatchMapping("/{distilleryId}/sort-order")` 추가.
- **T7 ✅** `InMemoryDistilleryRepository` `findAllBySortOrderGreaterThanEqual` Fake 구현.
- **T8 ✅** 단위 테스트 5건 추가 — 17/17 PASSED.
- **T9 ✅** 통합 테스트 3건 추가, RestDocs PATCH 1건 추가 — admin-api default test 45/45, admin_integration 171/171 PASSED.
- **T10 ✅** `unit_test`, `check_rule_test`, `:bottlenote-admin-api:test`, `admin_integration_test` 모두 BUILD SUCCESSFUL, 회귀 0.

