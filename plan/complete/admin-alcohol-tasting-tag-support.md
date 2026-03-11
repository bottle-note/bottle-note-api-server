# 어드민 위스키 등록/수정 API - 테이스팅 태그 지원 추가

```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-02-24

** Core Achievements **
- 어드민 위스키 등록/수정 API에 테이스팅 태그 동기화 기능 추가
- null vs 빈 리스트 의미 분리로 하위 호환성 보장 (null=유지, []=전부삭제, [1,2]=교체)
- mono 모듈 4개, admin-api 모듈 3개 총 7개 파일 수정
- 통합 테스트 5개 케이스 추가, 전체 빌드 및 테스트 통과 확인

** Key Components **
- `AdminAlcoholCommandService.saveTastingTags()`: replace 전략 기반 태그 저장/교체 로직
- `AlcoholsTastingTagsRepository.deleteByAlcoholId()`: alcoholId 기준 일괄 삭제
- `AdminAlcoholUpsertRequest.tastingTagIds`: nullable 태그 ID 목록 필드
================================================================================
```

## 이슈

어드민 위스키 정보 관리 시 바로 태그 동기화도 추가

---

## 수정 파일 목록

| # | 모듈        | 파일                                      | 변경 내용                                       |
|---|-----------|-----------------------------------------|---------------------------------------------|
| 1 | mono      | `AlcoholsTastingTagsRepository.java`    | `deleteByAlcoholId()` 메서드 추가                |
| 2 | mono      | `JpaAlcoholsTastingTagsRepository.java` | 위 메서드 JPQL 구현                               |
| 3 | mono      | `AdminAlcoholUpsertRequest.java`        | `tastingTagIds` 필드 추가                       |
| 4 | mono      | `AdminAlcoholCommandService.java`       | 태그 저장/교체 로직 추가                              |
| 5 | admin-api | `AlcoholsHelper.kt`                     | `createAlcoholUpsertRequestMap`에 태그 파라미터 추가 |
| 6 | admin-api | `AdminAlcoholsControllerDocsTest.kt`    | RestDocs 요청 필드 문서화                          |
| 7 | admin-api | `AdminAlcoholsIntegrationTest.kt`       | 태그 관련 통합 테스트 추가                             |

---

## 구현 순서

### Step 1. 도메인 레포지토리 - alcoholId 기반 삭제 메서드 추가

**파일**: `bottlenote-mono/.../alcohols/domain/AlcoholsTastingTagsRepository.java`

```java
void deleteByAlcoholId(Long alcoholId);
```

현재는 `deleteByTastingTagIdAndAlcoholIdIn`(태그 기준 삭제)만 존재.
수정 시 "해당 위스키의 기존 태그 전부 삭제 후 신규 태그 등록"(replace 전략)을 위해 alcoholId 기준 삭제가 필요.

### Step 2. JPA 구현체 - JPQL 쿼리 추가

**파일**: `bottlenote-mono/.../alcohols/repository/JpaAlcoholsTastingTagsRepository.java`

```java

@Override
@Modifying
@Query("delete from alcohol_tasting_tags att where att.alcohol.id = :alcoholId")
void deleteByAlcoholId(@Param("alcoholId") Long alcoholId);
```

기존 `deleteByTastingTagIdAndAlcoholIdIn`과 동일한 `@Modifying` JPQL 패턴 사용.

### Step 3. 요청 DTO - tastingTagIds 필드 추가

**파일**: `bottlenote-mono/.../alcohols/dto/request/AdminAlcoholUpsertRequest.java`

기존 14개 필드 뒤에 추가:

```java
List<Long> tastingTagIds  // validation 어노테이션 없음 (nullable)
```

**null vs 빈 리스트 의미 분리**:

- `null` (필드 미포함) = 수정 시 기존 태그 유지 (하위 호환성)
- `[]` (빈 배열) = 태그 전부 제거
- `[1, 2, 3]` = 해당 태그로 교체

### Step 4. 서비스 로직 - 태그 처리 추가

**파일**: `bottlenote-mono/.../alcohols/service/AdminAlcoholCommandService.java`

**4-1. 의존성 추가** (생성자 주입, `@RequiredArgsConstructor`):

- `AlcoholsTastingTagsRepository`
- `TastingTagRepository`

**4-2. `createAlcohol()` 수정** - `save()` 후, `tastingTagIds`가 null이 아니고 비어있지 않으면 태그 연결:

```java
Alcohol saved = alcoholQueryRepository.save(alcohol);
if(request.

tastingTagIds() !=null&&!request.

tastingTagIds().

isEmpty()){

saveTastingTags(saved, request.tastingTagIds());
		}

publishImageActivatedEvent(request.imageUrl(),saved.

getId());
```

**4-3. `updateAlcohol()` 수정** - `alcohol.update()` 후, `tastingTagIds`가 null이 아닐 때만 태그 교체:

```java
alcohol.update(...);

if(request.

tastingTagIds() !=null){
		alcoholsTastingTagsRepository.

deleteByAlcoholId(alcoholId);
    if(!request.

tastingTagIds().

isEmpty()){

saveTastingTags(alcohol, request.tastingTagIds());
		}
		}

handleImageChange(oldImageUrl, request.imageUrl(),alcoholId);
```

**4-4. private 헬퍼 메서드 추가**:

```java
private void saveTastingTags(Alcohol alcohol, List<Long> tagIds) {
	List<AlcoholsTastingTags> mappings = tagIds.stream()
			.map(tagId -> tastingTagRepository.findById(tagId)
					.orElseThrow(() -> new AlcoholException(TASTING_TAG_NOT_FOUND)))
			.map(tag -> AlcoholsTastingTags.of(alcohol, tag))
			.toList();
	alcoholsTastingTagsRepository.saveAll(mappings);
}
```

기존 `TastingTagService.addAlcoholsToTag()`의 개별 조회 패턴과 동일.

### Step 5. 테스트 헬퍼 수정

**파일**: `bottlenote-admin-api/.../helper/alcohols/AlcoholsHelper.kt`

`createAlcoholUpsertRequestMap`에 `tastingTagIds` 파라미터 추가:

```kotlin
fun createAlcoholUpsertRequestMap(
	// ... 기존 파라미터 유지 ...
	tastingTagIds: List<Long>? = null  // 기본값 null로 기존 테스트 호환
): Map<String, Any> = buildMap {
	// 기존 필드 put
	put("korName", korName)
	// ...
	tastingTagIds?.let { put("tastingTagIds", it) }
}
```

반환 타입 `Map<String, Any>` 유지 (null인 경우 map에 미포함).

### Step 6. RestDocs 테스트 수정

**파일**: `bottlenote-admin-api/.../docs/alcohols/AdminAlcoholsControllerDocsTest.kt`

`createAlcohol()`, `updateAlcohol()` 테스트의 `requestFields`에 추가:

```kotlin
fieldWithPath("tastingTagIds").type(JsonFieldType.ARRAY)
	.optional().description("테이스팅 태그 ID 목록")
```

헬퍼 호출 시 `tastingTagIds = listOf(1L, 2L)` 전달하여 스니펫에 포함.

### Step 7. 통합 테스트 추가

**파일**: `bottlenote-admin-api/.../integration/alcohols/AdminAlcoholsIntegrationTest.kt`

`TastingTagTestFactory` 의존성 추가 후, 기존 `CreateAlcohol`, `UpdateAlcohol` inner class에 테스트 케이스 추가:

**CreateAlcohol 추가**:

- 테이스팅 태그와 함께 위스키를 생성할 수 있다
- 존재하지 않는 태그 ID로 생성 시 실패한다

**UpdateAlcohol 추가**:

- 수정 시 태그를 교체할 수 있다 (기존 태그 제거 + 새 태그 연결)
- `tastingTagIds` 미포함(null) 시 기존 태그가 유지된다
- `tastingTagIds=[]` 시 태그가 전부 삭제된다

검증 방식: 등록/수정 후 상세 조회 API(`GET /alcohols/{id}`)로 `tastingTags` 배열 확인.

---

## 검증 체크리스트

```bash
# 1. 코드 포맷팅 (mono Java만 대상)
./gradlew :bottlenote-mono:spotlessApply

# 2. Java 컴파일
./gradlew :bottlenote-mono:compileJava

# 3. Kotlin 컴파일
./gradlew :bottlenote-admin-api:compileKotlin
./gradlew :bottlenote-admin-api:compileTestKotlin

# 4. 단위 테스트
./gradlew unit_test

# 5. 아키텍처 규칙 테스트
./gradlew check_rule_test

# 6. 통합 테스트
./gradlew admin_integration_test
./gradlew integration_test

# 7. RestDocs 문서 생성
./gradlew :bottlenote-admin-api:asciidoctor

# 8. 전체 빌드
./gradlew build
```

---

## 주의사항

- `@Modifying` JPQL `deleteByAlcoholId` 실행 시 1차 캐시 정합성: 동일 트랜잭션에서 삭제 후 `saveAll`은 신규 엔티티 생성이므로 문제 없음
- `AdminAlcoholUpsertRequest`는 Jackson이 JSON 역직렬화 시 누락 필드를 `null`로 처리하므로 하위 호환성 보장됨
- `AlcoholsHelper.createAlcoholUpsertRequestMap` 반환 타입 `Map<String, Any>` 유지: `tastingTagIds`가 null이면 map에 미포함
