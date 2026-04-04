```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-04-05

** Core Achievements **
- Region 엔티티에 parent_id Self-Reference 추가 (FK 기반 부모-자식 관계)
- 부모 지역(스코틀랜드/전체) 검색 시 하위 6개 지역 위스키 포함 조회
- 별도 클래스 생성 없이 QuerySupporter 내부에서 하위 지역 해석 처리

** Key Components **
- Region.java: parent 필드 (ManyToOne self-reference)
- AlcoholQuerySupporter / RatingQuerySupporter / UserQuerySupporter: eqRegion 내부에서 RegionRepository.findChildRegionIds 호출
- RegionsItem / AdminRegionItem: parentId 필드 추가

** Deferred Items **
- id=20 "스코트랜드" 오타 수정: 별도 작업으로 처리
- 초기 데이터(04-data-region.sql) 미수정: Liquibase changelog + 수동 쿼리로 대체
================================================================================
```

# Region 계층 구조 도입

## Context

현재 Region(지역)은 플랫 구조로, 스코틀랜드 하위 지역(캠벨타운, 아일라, 스페이사이드 등)이 독립 레코드로 존재한다.
"스코틀랜드"(id=19)를 "스코틀랜드/전체"로 변경하고, 이를 선택하면 모든 스코틀랜드 하위 지역의 위스키가 함께 조회되도록 한다.
향후 다른 국가(미국, 일본 등)에도 동일 패턴을 적용할 수 있어야 한다.

## 접근 방식

FK 기반 부모-자식 관계로 데이터 무결성을 보장하는 방식.
별도 Resolver 클래스 없이, QuerySupporter 내부에서 RegionRepository를 직접 주입받아 하위 지역을 해석한다.

## 조회 동작

"스코틀랜드/전체"(id=19) 선택 시: id=19 자체 + 하위 지역(14,15,16,17,18,20) 위스키 모두 조회

---

## 수정 파일 목록

### DB 스키마 (Liquibase)

| 파일 | 변경 |
|------|------|
| `git.environment-variables/storage/mysql/changelog/schema.mysql.sql` | changeset 2개 추가 |

```sql
-- changeset rlagu:20260404-1 splitStatements:false
-- comment: regions 테이블에 parent_id 컬럼 추가
ALTER TABLE regions ADD COLUMN parent_id BIGINT NULL COMMENT '상위 지역 ID';

-- changeset rlagu:20260404-2 splitStatements:false
-- comment: regions parent_id 인덱스 추가
CREATE INDEX idx_regions_parent_id ON regions(parent_id);
```

수동 반영 (양쪽 DB에 직접 실행):
```sql
UPDATE regions SET kor_name = '스코틀랜드/전체', eng_name = 'Scotland' WHERE id = 19;
UPDATE regions SET parent_id = 19 WHERE id IN (14, 15, 16, 17, 18, 20);
```

### 엔티티 + Repository

| 파일 | 변경 |
|------|------|
| `bottlenote-mono/.../alcohols/domain/Region.java` | `parent` 필드 추가 (`@ManyToOne`, `@JoinColumn(name = "parent_id")`) |
| `bottlenote-mono/.../alcohols/domain/RegionRepository.java` | `findChildRegionIds(Long parentId)` 메서드 추가 |
| `bottlenote-mono/.../alcohols/repository/JpaRegionQueryRepository.java` | JPQL에 `r.parent.id` 추가, `findChildRegionIds` 구현 |

### QuerySupporter (하위 지역 해석 내재화)

| 파일 | 변경 |
|------|------|
| `bottlenote-mono/.../alcohols/repository/AlcoholQuerySupporter.java` | RegionRepository 주입, `eqRegion` 내부에서 하위 지역 IN 절 처리 |
| `bottlenote-mono/.../rating/repository/RatingQuerySupporter.java` | RegionRepository 주입, `eqAlcoholRegion` 내부에서 하위 지역 IN 절 처리 |
| `bottlenote-mono/.../user/repository/UserQuerySupporter.java` | RegionRepository 주입, `eqRegion` 내부에서 하위 지역 IN 절 처리 |

변경 패턴 (3곳 동일, 시그니처 변경 없음):
```java
public BooleanExpression eqRegion(Long regionId) {
    if (regionId == null) return null;
    List<Long> childIds = regionRepository.findChildRegionIds(regionId);
    if (childIds.isEmpty()) return alcohol.region.id.eq(regionId);
    List<Long> regionIds = new ArrayList<>(childIds.size() + 1);
    regionIds.add(regionId);
    regionIds.addAll(childIds);
    return alcohol.region.id.in(regionIds);
}
```

### API 응답 DTO

| 파일 | 변경 |
|------|------|
| `bottlenote-mono/.../alcohols/dto/response/RegionsItem.java` | `parentId` 필드 추가 |
| `bottlenote-mono/.../alcohols/dto/response/AdminRegionItem.java` | `parentId` 필드 추가 |

### 테스트 수정

| 파일 | 변경 |
|------|------|
| `bottlenote-mono/.../config/ModuleConfig.java` | QuerySupporter 빈 생성 시 RegionRepository 주입 |
| `bottlenote-product-api/.../docs/alcohols/RestReferenceControllerTest.java` | RegionsItem에 parentId 추가, RestDocs에 parentId 필드 문서화 |
| `bottlenote-product-api/.../alcohols/service/RegionServiceTest.java` | RegionsItem에 parentId 추가 |
| `bottlenote-product-api/.../alcohols/integration/AlcoholQueryIntegrationTest.java` | 부모/하위 지역 검색 통합 테스트 2개 추가 |
| `bottlenote-admin-api/.../helper/alcohols/AlcoholsHelper.kt` | AdminRegionItem에 parentId 추가 |
| `bottlenote-admin-api/.../docs/alcohols/AdminRegionControllerDocsTest.kt` | RestDocs에 parentId 필드 문서화 |

---

## 영향받는 API 목록

| API | 엔드포인트 |
|------|----------|
| 알코올 검색 | `GET /api/v1/alcohols/search?regionId=19` |
| 별점 평가 목록 | `GET /api/v1/rating?regionId=19` |
| MyBottle (Review) | `GET /api/v1/my-page/{userId}/my-bottle/reviews?regionId=19` |
| MyBottle (Rating) | `GET /api/v1/my-page/{userId}/my-bottle/ratings?regionId=19` |
| MyBottle (Picks) | `GET /api/v1/my-page/{userId}/my-bottle/picks?regionId=19` |
| Region 목록 | `GET /api/v1/regions` (parentId 필드 추가) |
| Admin Region 목록 | `GET /admin/api/v1/regions` (parentId 필드 추가) |
| Admin 알코올 검색 | Admin API 내 알코올 검색 |

---

## 검증 결과

| 검증 항목 | 결과 |
|-----------|------|
| `./gradlew compileJava compileTestJava` | 통과 |
| `./gradlew spotlessCheck` | 통과 |
| `./gradlew unit_test` | 통과 |
| `./gradlew check_rule_test` | 통과 |
| `./gradlew restDocsTest` | 통과 (product + admin) |
| `./gradlew integration_test` | 통과 |
| `./gradlew admin_integration_test` | 통과 |
