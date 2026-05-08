```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-05-08

** Core Achievements **
- product/admin API 의 Distillery·Region 응답 DTO 에 sortOrder 필드 노출
- JpaDistilleryRepository / JpaRegionQueryRepository JPQL select 절에 sortOrder 추가
- RestDocs 응답 필드 문서화 갱신
- 데이터베이스/엔티티 변경 없이 응답 직렬화만 보강

** Key Components **
- AdminDistilleryItem, AdminRegionItem, RegionsItem DTO sortOrder 필드 추가
- JpaDistilleryRepository.findAllDistilleries / JpaRegionQueryRepository JPQL 변경

** Related **
- main commits: 2257a428 feat: Add default sort order field..., 8b6da61c feat: Expose sortOrder in Region and Distillery API responses
================================================================================
```

# Plan: Distillery/Region 응답에 sortOrder 노출

## Overview

이미 DB 컬럼 및 엔티티 필드까지 추가가 완료된 `Distillery.sortOrder`, `Region.sortOrder` 값을
**조회 API 응답**에 노출시킨다. 본 작업은 **응답값 확장만** 다루며,
어드민의 sortOrder 수정 기능은 별도 PR로 분리한다.

### 선행 완료 사항 (참고)
- Liquibase changeset `hgkim:20260506-1`, `hgkim:20260506-2` 적용 완료 (DEV/PROD)
- `Distillery.sortOrder: Integer` (default 9999, NOT NULL) 추가 완료
- `Region.sortOrder: Integer` (default 9999, NOT NULL) 추가 완료

### Assumptions

1. **응답 노출 대상은 "distillery/region 자체 조회 엔드포인트"에 한정한다.**
   - product-api: `GET /api/v1/regions` (`RegionsItem`)
   - admin-api: `GET /distilleries` (`AdminDistilleryItem`)
   - admin-api: `GET /regions` (`AdminRegionItem`)
   - **product-api의 `Alcohol` 조회 응답 내 중첩된 distillery/region 정보(korName/engName만 노출 중)에는 추가하지 않는다.**
     -> 별도 요구가 있을 때 확장
2. product-api에는 distillery 자체 조회 엔드포인트가 존재하지 않으므로 distillery는 admin-api에서만 노출된다.
3. `RegionsItem`은 product-api 응답에 한 번만 사용되므로 필드 추가 시 다른 호출처 영향이 없다 (확인 완료).
4. 캐시 키 `local_cache_alcohol_region_information`(`AlcoholReferenceService.findAllRegion`)은
   응답 구조가 바뀌므로 **무효화/구버전 직렬화 충돌 방지 필요**
   -> 로컬(Caffeine) 캐시이므로 애플리케이션 재시작 시 자동 갱신 가정. 별도 조치 불필요.
5. 정렬 순서를 응답에 추가할 뿐이며, **DB 정렬(ORDER BY sort_order) 적용은 본 PR 범위 외**.
   -> 정렬 적용은 후속 PR (또는 별도 의사결정) 대상.
6. 필드명은 응답에서 **`sortOrder`** (camelCase, JSON 직렬화 동일).

-> 위 가정들 중 의도와 어긋나는 항목이 있으면 알려주세요.
   특히 **5번(정렬을 적용까지 할지 vs 노출만 할지)** 확인이 필요합니다.

### Success Criteria

- [ ] `GET /api/v1/regions` 응답 각 항목에 `sortOrder: number` 필드가 포함된다 (default 9999).
- [ ] 어드민 `GET /distilleries` 응답 각 항목에 `sortOrder: number` 필드가 포함된다.
- [ ] 어드민 `GET /regions` 응답 각 항목에 `sortOrder: number` 필드가 포함된다.
- [ ] 기존 응답 필드는 모두 유지된다 (BC 보장).
- [ ] 단위 테스트 / RestDocs 통합 테스트가 신규 필드를 검증한다.
- [ ] `./gradlew compileJava :bottlenote-admin-api:compileKotlin` 통과.
- [ ] `./gradlew test`(기본 태그) 통과.

### Impact Scope

**모듈**
- `bottlenote-mono` (DTO + Repository JPQL 수정)
- `bottlenote-product-api` (RestDocs 테스트 수정)
- `bottlenote-admin-api` (RestDocs 테스트 수정)

**파일 (수정 예정)**
| 파일 | 변경 내용 |
|------|----------|
| `mono/.../alcohols/dto/response/RegionsItem.java` | `sortOrder` 필드 추가 |
| `mono/.../alcohols/dto/response/AdminRegionItem.java` | `sortOrder` 필드 추가 |
| `mono/.../alcohols/dto/response/AdminDistilleryItem.java` | `sortOrder` 필드 추가 |
| `mono/.../alcohols/repository/JpaRegionQueryRepository.java` | `findAllRegionsResponse` JPQL select 절에 `r.sortOrder` 추가 / `findAllRegions` 도 동일 |
| `mono/.../alcohols/repository/JpaDistilleryRepository.java` | `findAllDistilleries` JPQL select 절에 `d.sortOrder` 추가 |
| `product-api/.../alcohols/controller/AlcoholReferenceControllerTest.java` (또는 RestDocs 테스트) | 신규 필드 문서화 + 검증 |
| `admin-api/.../alcohols/presentation/AdminDistilleryControllerTest.kt` | 신규 필드 문서화 + 검증 |
| `admin-api/.../alcohols/presentation/AdminRegionControllerTest.kt` | 신규 필드 문서화 + 검증 |

**도메인 / 이벤트 / 캐시**
- 도메인: `alcohols`
- 이벤트: 영향 없음
- 캐시: `local_cache_alcohol_region_information` (Caffeine 로컬 캐시) — 앱 재기동 후 자동 무효화

**테스트 종류**
- 통합 테스트(RestDocs) 보강: 응답 필드 문서화 + 값 검증
- 단위 테스트: DTO 생성자/필드 추가에 따른 기존 테스트가 깨지지 않는지 확인

**고려하지 않는 것 (Out of Scope)**
- 어드민의 sortOrder **수정 엔드포인트** (별도 PR)
- 응답을 sort_order **기준으로 ORDER BY 적용**하는 정렬 동작 (Assumption #5 참조)
- product-api의 Alcohol 조회 응답에 중첩된 distillery/region 정보로 sortOrder 전파 (Assumption #1 참조)
