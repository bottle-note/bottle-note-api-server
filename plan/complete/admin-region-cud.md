```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-05-08

** Core Achievements **
- 어드민 Region 5개 엔드포인트(상세·POST·PUT·DELETE·PATCH /sort-order) 추가
- Banner 표준 일치: 자동 reorder + korName ASC 보조 정렬, 계층 2단계 제한, 자식·연결 위스키 보유 시 삭제 가드
- ValidExceptionCode 4건 + AlcoholExceptionCode 7건 + ResultCode 4건 추가
- 단위 15 + 통합 8 PASSED, admin_integration_test 159/159 회귀 0

** Key Components **
- bottlenote-admin-api/.../alcohols/presentation/AdminRegionController.kt
- bottlenote-mono/.../alcohols/service/AdminRegionService.java
- Region 도메인 메서드(update/updateSortOrder/changeParent), RegionRepository CUD/exists/reorder 확장
- DTO: AdminRegionCreateRequest/UpdateRequest/SortOrderRequest/DetailResponse
- Test fixture: InMemoryRegionRepository, RegionTestFactory

** Related **
- PR #586 → main merge: f670c47b, 4ba6c659
================================================================================
```

# Plan: 지역 정보 관리(Region) 어드민 CUD

## Overview

어드민이 위스키의 원산지(`Region`)를 관리할 수 있도록 생성/수정/삭제/단건 조회/정렬 변경 기능을 추가한다. 현재는 목록 조회(`GET /regions`)만 노출되어 있어, 어드민에서 신규 지역을 등록하거나 기존 지역의 정보·정렬 순서를 변경할 수 없다. 다른 어드민 도메인(Banner, Curation)과 동일한 표준(Controller=admin-api·Kotlin / Service=mono·Java, `AdminResultResponse` 반환, 필드별 PATCH 분리, offset 페이징)을 따른다.

### Assumptions

운영 비즈니스 룰은 사용자 확인을 통해 다음과 같이 확정되었다.

1. **모듈 구조**: admin-api(Kotlin)에는 컨트롤러만, 비즈니스 로직과 DTO는 mono(Java)에 작성한다.
2. **이름 중복 정책**: `korName`, `engName` 각각 **전역 유니크**. 같은 부모 하 유니크가 아니다.
3. **계층 깊이 제한**: `Region.parent` self-reference는 **최대 2단계**까지 허용 (루트 + 1단계 자식, 손자 금지).
4. **삭제 정책**: 자식 지역이 존재하거나(`REGION_HAS_CHILDREN`), 해당 지역을 참조하는 위스키(`Alcohol.region_id`)가 존재하면(`REGION_HAS_ALCOHOLS`) **삭제 금지**.
5. **sortOrder 정책**: Banner와 동일한 **자동 reorder(밀어내기)** 방식을 적용한다. 같은 `sortOrder` 값이 잔존하는 경우 보조 정렬키로 **`korName ASC`** 를 적용한다 (기존 default `9999` 다수 케이스 대비).
6. **`continent` 타입**: 자유 문자열 유지. enum화하지 않는다.
7. **PATCH 분리 범위**: `sort-order` 한 가지만 별도 PATCH로 분리. `parent` 변경은 PUT(전체 수정)에 흡수한다.
8. **부모 변경 검증**: 자기 자신 또는 자신의 하위 트리를 부모로 지정하는 사이클을 금지한다 (`REGION_PARENT_CYCLE`). 2단계 제한과 결합해 실질적으로 "이미 자식이 있는 지역은 다른 지역의 자식이 될 수 없다".
9. **인증/권한**: 기존 어드민 컨트롤러와 동일하게 admin JWT 기반. 별도의 RBAC 차등은 두지 않는다 (관리 페이지 진입 가능자면 모두 허용).
10. **API 문서화**: 통합 테스트에서 RestDocs 스니펫을 생성한다(`asciidoctor`).

### Success Criteria

| # | 기준 | 검증 방법 |
|---|------|-----------|
| SC1 | `GET /admin/api/v1/regions/{regionId}` 가 단건 상세(부모 id, 자식 여부, 위스키 연결 카운트 포함)를 반환한다 | RestDocs 통합 테스트 200 OK |
| SC2 | `POST /admin/api/v1/regions` 로 신규 지역을 생성하면 `AdminResultResponse(REGION_CREATED, savedId)` 와 201/200 응답을 받는다 | 통합 테스트 + DB 저장 검증 |
| SC3 | `korName` 또는 `engName` 중복 시 `409 CONFLICT` (`REGION_DUPLICATE_KOR_NAME` / `REGION_DUPLICATE_ENG_NAME`) | 통합 테스트 |
| SC4 | `parent` 가 이미 자식을 가진 지역을 가리키거나(2단계 초과) 자기/하위 트리를 가리킬 때 각각 `REGION_MAX_DEPTH_EXCEEDED`, `REGION_PARENT_CYCLE` 발생 | 단위 + 통합 테스트 |
| SC5 | `PUT /admin/api/v1/regions/{id}` 로 전체 수정 시 도메인 메서드(`region.update(...)`)로 상태 변경, `AdminResultResponse(REGION_UPDATED, id)` 반환 | 통합 테스트 + Reflection-free 도메인 단위 테스트 |
| SC6 | `DELETE /admin/api/v1/regions/{id}` 시 자식 또는 연결 위스키 존재하면 `409`, 없으면 `200` + `REGION_DELETED` | 통합 테스트 (3개 시나리오) |
| SC7 | `PATCH /admin/api/v1/regions/{id}/sort-order` 로 정렬 변경 시 동일 `sortOrder` 이상 모든 지역이 +1 밀려나고, 변경 대상의 새 값이 적용된다 | 단위 + 통합 테스트 |
| SC8 | 목록(`GET /regions`)이 `sortOrder ASC, korName ASC` 정렬을 반환한다 | JPQL 변경 후 통합 테스트 검증 |
| SC9 | 모든 응답이 `GlobalResponse` 표준을 따른다(목록은 `fromPage`, 그 외는 `GlobalResponse.ok`) | 응답 JSON shape 단언 |
| SC10 | `/verify full` (compile + unit + integration + rule) 통과 | 로컬 CI 결과 |

### Impact Scope

**모듈/컴포넌트 변경 범위**

- **모듈**: `bottlenote-mono` (도메인/서비스/DTO/예외/리포지토리), `bottlenote-admin-api` (컨트롤러)
- **도메인**: `alcohols` (Region 단일 도메인). 크로스 도메인 Facade 불필요.
- **엔티티**: `Region` 스키마 변경 없음 — 컬럼·테이블 수정 없음, **Liquibase 마이그레이션 불요**. 다만 도메인 메서드(`update/updateSortOrder/changeParent`) 추가.
- **이벤트**: 신규 도메인 이벤트 없음.
- **캐시**: Region을 캐시하는 기존 항목 없음(필요 시 추후). 이번 범위에선 캐시 작업 없음.

**파일 변경/생성 목록 (요약)**

mono:
- `domain/Region.java` (도메인 메서드 추가)
- `domain/RegionRepository.java` (CUD/exists/reorder용 IF 추가)
- `repository/JpaRegionQueryRepository.java` (메서드 추가, 정렬 ORDER BY 보정)
- `dto/request/AdminRegionCreateRequest.java` (신규)
- `dto/request/AdminRegionUpdateRequest.java` (신규)
- `dto/request/AdminRegionSortOrderRequest.java` (신규)
- `dto/response/AdminRegionDetailResponse.java` (신규)
- `service/AdminRegionService.java` (신규)
- `exception/AlcoholExceptionCode.java` (코드 추가: DUPLICATE_KOR/ENG_NAME, HAS_CHILDREN, HAS_ALCOHOLS, PARENT_NOT_FOUND, PARENT_CYCLE, MAX_DEPTH_EXCEEDED)
- `global/dto/response/AdminResultResponse.java` (ResultCode: REGION_CREATED/UPDATED/DELETED/SORT_ORDER_UPDATED 추가)

admin-api:
- `presentation/AdminRegionController.kt` (단건/POST/PUT/DELETE/PATCH 추가, `AdminRegionService` 의존성 주입)

테스트:
- `bottlenote-mono/src/test/java/.../alcohols/service/AdminRegionServiceTest.java` (단위, Fake `RegionRepository`)
- `bottlenote-admin-api/src/test/kotlin/.../integration/region/AdminRegionIntegrationTest.kt` (통합 + RestDocs)

**테스트 종류**: 단위(`@Tag("unit")`), 통합(`@Tag("integration")`), 아키텍처 규칙(`@Tag("rule")`) 자동 검증 통과.

**보안/권한**: 기존 admin JWT 필터 그대로 적용. 추가 작업 없음.

---

## Tasks

각 Task 종료 시 `/self-review` 게이트 통과 후 다음 Task로 이동한다.

### T1. 예외 코드 + ResultCode enum 추가
- **파일**: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/exception/AlcoholExceptionCode.java`, `bottlenote-mono/src/main/java/app/bottlenote/global/dto/response/AdminResultResponse.java`
- **내용**:
  - `AlcoholExceptionCode`: `REGION_DUPLICATE_KOR_NAME(409)`, `REGION_DUPLICATE_ENG_NAME(409)`, `REGION_HAS_CHILDREN(409)`, `REGION_HAS_ALCOHOLS(409)`, `REGION_PARENT_NOT_FOUND(404)`, `REGION_PARENT_CYCLE(400)`, `REGION_MAX_DEPTH_EXCEEDED(400)`
  - `AdminResultResponse.ResultCode`: `REGION_CREATED`, `REGION_UPDATED`, `REGION_DELETED`, `REGION_SORT_ORDER_UPDATED` 추가 (한국어 메시지 포함)
- **검증**: 컴파일 통과 (`./gradlew :bottlenote-mono:compileJava`)

### T2. Region 도메인 메서드 추가
- **파일**: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/domain/Region.java`
- **내용**:
  - `update(String korName, String engName, String continent, String description, Integer sortOrder, Region parent)` — 모든 필드 일괄 갱신
  - `updateSortOrder(int sortOrder)` — 정렬값만 갱신
  - `changeParent(Region parent)` — 부모 교체 (null 허용)
  - 검증 헬퍼: 자기/하위 사이클 검증은 Service에서 수행 (도메인은 단일 엔티티 책임만)
- **검증**: 도메인 단위 테스트 (T7에 포함)

### T3. RegionRepository / JpaRegionQueryRepository 확장
- **파일**: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/domain/RegionRepository.java`, `bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/JpaRegionQueryRepository.java`
- **내용**:
  - IF 추가: `Region save(Region)`, `void delete(Region)`, `boolean existsByKorName(String)`, `boolean existsByEngName(String)`, `boolean existsByKorNameAndIdNot(String, Long)`, `boolean existsByEngNameAndIdNot(String, Long)`, `List<Region> findAllBySortOrderGreaterThanEqual(int)`, `boolean existsAlcoholByRegionId(Long)`
  - 기존 `findAllRegions(keyword, pageable)` JPQL ORDER BY 추가: `r.sortOrder ASC, r.korName ASC`
  - `existsAlcoholByRegionId`: `Alcohol` 엔티티 존재 여부 확인 — JPQL `select count(a)>0 from alcohol a where a.region.id = :regionId`
- **검증**: 컴파일 통과 + 기존 통합 테스트 회귀 없음

### T4. Request/Response DTO 추가
- **파일** (신규):
  - `bottlenote-mono/.../alcohols/dto/request/AdminRegionCreateRequest.java`
  - `bottlenote-mono/.../alcohols/dto/request/AdminRegionUpdateRequest.java`
  - `bottlenote-mono/.../alcohols/dto/request/AdminRegionSortOrderRequest.java`
  - `bottlenote-mono/.../alcohols/dto/response/AdminRegionDetailResponse.java`
- **내용**:
  - Create: `@NotBlank korName, engName`, optional `continent, description, parentId`, `@Min(0) sortOrder` (default 9999)
  - Update: Create와 동일 필드 (전체 수정)
  - SortOrder: `@Min(0) sortOrder`
  - Detail: `id, korName, engName, continent, description, sortOrder, parentId, parentKorName, hasChildren(boolean), alcoholCount(long), createAt, lastModifyAt`

### T5. AdminRegionService 신설
- **파일**: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/service/AdminRegionService.java`
- **메서드**:
  - `getDetail(Long regionId)` → `AdminRegionDetailResponse` (`@Transactional(readOnly=true)`)
  - `create(AdminRegionCreateRequest)` → `AdminResultResponse(REGION_CREATED, id)`
  - `update(Long, AdminRegionUpdateRequest)` → `AdminResultResponse(REGION_UPDATED, id)`
  - `delete(Long)` → 자식/위스키 검증 후 `AdminResultResponse(REGION_DELETED, id)`
  - `updateSortOrder(Long, AdminRegionSortOrderRequest)` → reorder 후 `AdminResultResponse(REGION_SORT_ORDER_UPDATED, id)`
- **검증 로직 (private)**:
  - `validateUniqueNames(korName, engName, excludeId=null)`
  - `resolveParent(Long parentId, Long selfId)` — null 허용, 자기 자신 금지, 부모 존재, **부모가 이미 자식인 경우(2단계 초과) 거부**, 사이클 방지(자기 하위 트리 = `findChildRegionIds(selfId)`에 포함되면 거부)
  - `reorderSortOrders(int newSortOrder, Long excludeId)` — Banner 패턴 동일

### T6. AdminRegionController CUD 엔드포인트 추가
- **파일**: `bottlenote-admin-api/src/main/kotlin/app/bottlenote/alcohols/presentation/AdminRegionController.kt`
- **내용**:
  - `AdminRegionService` 의존성 주입 추가
  - 기존 `GET /regions` 유지 (`alcoholReferenceService` 위임)
  - 신규 endpoint: `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`, `PATCH /{id}/sort-order`
  - 응답 래핑: 단순 위임 + `GlobalResponse.ok(...)`

### T7. 단위 테스트 (Service)
- **파일**: `bottlenote-mono/src/test/java/.../alcohols/service/AdminRegionServiceTest.java` (`@Tag("unit")`)
- **테스트 더블**: `InMemoryRegionRepository` Fake (`bottlenote-mono/src/test/.../alcohols/fixture/InMemoryRegionRepository.java`)
- **시나리오**:
  - 생성: 정상 / korName 중복 / engName 중복 / parent 없음 / 깊이 초과 / 사이클
  - 수정: 정상 / 자기 자신 부모 / 다른 지역 이름 중복
  - 삭제: 정상 / 자식 존재 / 위스키 존재
  - sortOrder: 정상 reorder / 같은 값 변경시 no-op / 더 큰 값으로 이동 시 다른 항목 영향 없음

### T8. 통합 테스트 (Controller + RestDocs)
- **파일**: `bottlenote-admin-api/src/test/kotlin/.../integration/region/AdminRegionIntegrationTest.kt` (`@Tag("integration")`)
- **베이스**: `IntegrationTestSupport`, `MockMvcTester`
- **시나리오**: 6개 엔드포인트별 happy path + 주요 실패 케이스 (RestDocs 스니펫 생성)
- **데이터**: `RegionTestFactory` 신설 또는 기존 fixture 활용, `init-script/regions.sql` 확장 (필요 시)

### T9. /verify full 검증
- `./gradlew clean compileJava compileTestJava unit_test integration_test check_rule_test asciidoctor`
- 모든 그린 확인 후 커밋

## Progress Log

- **T1 ✅** 예외 코드 7건(`REGION_DUPLICATE_KOR_NAME`, `REGION_DUPLICATE_ENG_NAME`, `REGION_HAS_CHILDREN`, `REGION_HAS_ALCOHOLS`, `REGION_PARENT_NOT_FOUND`, `REGION_PARENT_CYCLE`, `REGION_MAX_DEPTH_EXCEEDED`) + ResultCode 4건(`REGION_CREATED/UPDATED/DELETED/SORT_ORDER_UPDATED`) 추가. 컴파일 통과.
- **T2 ✅** `Region.update / updateSortOrder / changeParent` 도메인 메서드 추가. setter 미사용, 캡슐화 유지.
- **T3 ✅** `RegionRepository` IF + `JpaRegionQueryRepository` 확장. `existsByKor/EngName(+IdNot)`, `findAllBySortOrderGreaterThanEqual`, `existsAlcoholByRegionId`, `countAlcoholsByRegionId` 추가. JPQL ORDER BY를 `sortOrder ASC, korName ASC` 로 보정.
- **T4 ✅** `AdminRegionCreateRequest`, `AdminRegionUpdateRequest`, `AdminRegionSortOrderRequest`, `AdminRegionDetailResponse` 신설.
- **T5 ✅** `AdminRegionService` 신설. 검증 로직(`validateUniqueNames`, `resolveParent`, `reorderSortOrders`) 캡슐화. 부모 확인 시 2단계 제한 + 사이클 방지(자기 하위 트리 + 자식 보유 검사).
- **T6 ✅** `AdminRegionController.kt` 에 단건/POST/PUT/DELETE/PATCH(sort-order) 추가. 기존 GET 유지.
- **T7 ✅** `InMemoryRegionRepository` Fake + `AdminRegionServiceTest` 단위 테스트 15개 작성 — `./gradlew :bottlenote-mono:test --tests AdminRegionServiceTest` 15/15 PASSED.
- **T8 ✅** `RegionTestFactory` + `AdminRegionIntegrationTest` 통합 테스트 8개 작성 (Detail 2 / Create 2 / Update 1 / Delete 2 / SortOrder 1).
- **T9 ✅** 검증 결과
  - `./gradlew unit_test` BUILD SUCCESSFUL
  - `./gradlew check_rule_test` BUILD SUCCESSFUL
  - `./gradlew integration_test` BUILD SUCCESSFUL
  - `./gradlew :bottlenote-admin-api:admin_integration_test` 159/159 PASSED, 실패 0, 회귀 없음

스키마 변경 없음 → Liquibase 마이그레이션 불요.


Summary:
- Assumptions: 10개 항목 명시(사용자 결정사항 반영)
- Success criteria: 10개 검증 가능 조건
- Impact: mono(8 파일 수정/신설) + admin-api(1 파일 수정) + 테스트 2종

다음 단계 `/plan` 으로 Task 분해 진행 가능 여부 확인 부탁드립니다.
