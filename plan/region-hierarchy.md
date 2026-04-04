# Region 계층 구조 도입 계획

## Context

현재 Region(지역)은 플랫 구조로, 스코틀랜드 하위 지역(캠벨타운, 아일라, 스페이사이드 등)이 독립 레코드로 존재한다.
"스코틀랜드"(id=19)를 "스코틀랜드/전체"로 변경하고, 이를 선택하면 모든 스코틀랜드 하위 지역의 위스키가 함께 조회되도록 한다.
향후 다른 국가(미국, 일본 등)에도 동일 패턴을 적용할 수 있어야 한다.

## 접근 방식: Region 엔티티에 parentId Self-Reference 추가

이름 기반 LIKE 검색(id=20 "스코트랜드" 오타 문제로 불안정)이나 groupCode(parentId의 열화판) 대신,
FK 기반 부모-자식 관계로 데이터 무결성을 보장하는 방식을 선택한다.

## 작업 범위

- 코드 변경 + Liquibase changelog 추가
- 초기 데이터(04-data-region.sql) 수정하지 않음
- id=20 오타 수정하지 않음

## 조회 동작

"스코틀랜드/전체"(id=19) 선택 시: id=19 자체 + 하위 지역(14,15,16,17,18,20) 위스키 모두 조회

---

## 수정 파일 목록

### Phase 1: DB 스키마 + 엔티티

> [완료] 2026-04-04: DB 스키마 변경 반영 (production + development)
> - changeset `rlagu:20260404-1`: parent_id 컬럼 추가
> - changeset `rlagu:20260404-2`: idx_regions_parent_id 인덱스 추가
> - 스코틀랜드 데이터 UPDATE: 수동 쿼리로 양쪽 DB에 반영 완료

| 파일 | 변경 |
|------|------|
| `git.environment-variables/storage/mysql/changelog/schema.mysql.sql` | parent_id 컬럼 + 인덱스 changeset 추가, 스코틀랜드 데이터 UPDATE |
| `bottlenote-mono/.../alcohols/domain/Region.java` | `parent` 필드 (ManyToOne self-reference) 추가 |

**changelog 추가 내용:**
```sql
-- changeset rlagu:20260404-1 splitStatements:false
-- comment: regions 테이블에 parent_id 컬럼 추가
ALTER TABLE regions ADD COLUMN parent_id BIGINT NULL COMMENT '상위 지역 ID';

-- changeset rlagu:20260404-2 splitStatements:false
-- comment: regions parent_id 인덱스 추가
CREATE INDEX idx_regions_parent_id ON regions(parent_id);
```

**수동 반영 (양쪽 DB에 직접 실행):**
```sql
UPDATE regions SET kor_name = '스코틀랜드/전체', eng_name = 'Scotland' WHERE id = 19;
UPDATE regions SET parent_id = 19 WHERE id IN (14, 15, 16, 17, 18, 20);
```

**Region.java 변경:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_id")
@Comment("상위 지역")
private Region parent;
```

### Phase 2: Repository 계층

| 파일 | 변경 |
|------|------|
| `bottlenote-mono/.../alcohols/domain/RegionRepository.java` | `findChildRegionIds(Long parentId)` 메서드 추가 |
| `bottlenote-mono/.../alcohols/repository/JpaRegionQueryRepository.java` | JPQL 구현 + findAllRegionsResponse에 parentId 추가 |

### Phase 3: RegionIdResolver (신규)

| 파일 | 변경 |
|------|------|
| `bottlenote-mono/.../alcohols/repository/RegionIdResolver.java` | **신규** - regionId -> List<Long> 변환 (부모면 자신+자식 반환) |
| `bottlenote-mono/.../global/cache/local/LocalCacheType.java` | `REGION_CHILDREN_CACHE` 추가 |

**핵심 로직:**
```java
@Component
public class RegionIdResolver {
    // regionId가 부모 Region이면: [regionId, childId1, childId2, ...] 반환
    // regionId가 자식 Region이면: [regionId] 반환
    // null이면: null 반환
    @Cacheable(value = "region_children_cache")
    public List<Long> resolveRegionIds(Long regionId) { ... }
}
```

### Phase 4: QuerySupporter 시그니처 변경 (3개)

| 파일 (라인) | 변경 |
|------|------|
| `bottlenote-mono/.../alcohols/repository/AlcoholQuerySupporter.java` (201) | `eqRegion(Long)` -> `eqRegion(List<Long>)`, IN 절 사용 |
| `bottlenote-mono/.../rating/repository/RatingQuerySupporter.java` (102) | `eqAlcoholRegion(Long)` -> `eqAlcoholRegion(List<Long>)`, IN 절 사용 |
| `bottlenote-mono/.../user/repository/UserQuerySupporter.java` (119) | `eqRegion(Long)` -> `eqRegion(List<Long>)`, IN 절 사용 |

**변경 패턴 (3곳 동일):**
```java
// Before
public BooleanExpression eqRegion(Long regionId) {
    if (regionId == null) return null;
    return alcohol.region.id.eq(regionId);
}

// After
public BooleanExpression eqRegion(List<Long> regionIds) {
    if (regionIds == null || regionIds.isEmpty()) return null;
    if (regionIds.size() == 1) return alcohol.region.id.eq(regionIds.get(0));
    return alcohol.region.id.in(regionIds);
}
```

### Phase 5: Repository 구현체에서 resolve 호출 (3개)

| 파일 | 변경 |
|------|------|
| `bottlenote-mono/.../alcohols/repository/CustomAlcoholQueryRepositoryImpl.java` | RegionIdResolver 주입, 4곳에서 resolve 호출 (라인 99, 122, 329, 343) |
| `bottlenote-mono/.../rating/repository/CustomRatingQueryRepositoryImpl.java` | RegionIdResolver 주입, 2곳에서 resolve 호출 (라인 63, 85) |
| `bottlenote-mono/.../user/repository/CustomUserRepositoryImpl.java` | RegionIdResolver 주입, 6곳에서 resolve 호출 (라인 113, 185, 227, 253, 292, 323) |

**호출 패턴 변경:**
```java
// Before
supporter.eqRegion(criteria.regionId())

// After
supporter.eqRegion(regionIdResolver.resolveRegionIds(criteria.regionId()))
```

### Phase 6: API 응답 DTO 수정

| 파일 | 변경 |
|------|------|
| `bottlenote-mono/.../alcohols/dto/response/RegionsItem.java` | `parentId` 필드 추가 |
| `bottlenote-mono/.../alcohols/dto/response/AdminRegionItem.java` | `parentId` 필드 추가 |

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

## 검증 방법

1. **컴파일 확인**: `./gradlew compileJava`
2. **단위 테스트**: `./gradlew unit_test` - RegionsItem/AdminRegionItem 생성자 변경 영향 확인
3. **통합 테스트**: `./gradlew integration_test` - Region 필터링이 사용되는 API 테스트 통과 확인
4. **수동 검증**: regionId=19로 검색 시 하위 지역 위스키도 함께 반환되는지 확인
