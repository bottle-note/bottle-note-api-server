# Plan: 어드민 카테고리 레퍼런스 응답 구조 grouped Map 변경 (issue #221)

## Overview

어드민 위스키 등록/수정(`POST/PUT /admin/api/v1/alcohols`)이 필수로 요구하는 `categoryGroup`(SINGLE_MALT, BLEND, BLENDED_MALT, BOURBON, RYE, OTHER)을 카테고리 레퍼런스 API에서 함께 내려주도록 응답 구조를 변경한다.

현재 `GET /admin/api/v1/alcohols/categories/reference` 응답은 `[{korCategory, engCategory}, ...]` 형태이며, 프론트엔드에서 `korCategory ↔ categoryGroup` 매핑을 하드코딩하고 있어 카테고리 추가/변경 시 동기화 누락 문제가 발생한다 (#220 위스키 등록 오류 원인).

응답 구조는 1안(flat array) / 2안(grouped map) 중 **2안**으로 결정되었다. `korCategory`/`engCategory`가 서버 입장에서 자유 입력이라 같은 그룹에 여러 표기가 들어올 수 있으므로, `categoryGroup` 1:N 관계를 자료구조로 명확히 표현한다.

### 결정된 응답 형식

```json
{
  "SINGLE_MALT": [
    { "korCategory": "싱글 몰트", "engCategory": "Single Malt" }
  ],
  "BLEND": [
    { "korCategory": "블렌디드", "engCategory": "Blend" }
  ],
  "BLENDED_MALT": [
    { "korCategory": "블렌디드 몰트", "engCategory": "Blended Malt" }
  ],
  "BOURBON": [
    { "korCategory": "버번", "engCategory": "Bourbon" }
  ],
  "RYE": [],
  "OTHER": [
    { "korCategory": "테네시", "engCategory": "Tennessee" },
    { "korCategory": "콘", "engCategory": "Corn" }
  ]
}
```

### Assumptions

1. **응답 타입**: `Map<AlcoholCategoryGroup, List<CategoryPairItem>>` 형태로 변경한다 (CategoryPairItem은 `{korCategory, engCategory}` record).
2. **빈 그룹 포함**: 데이터가 0건인 그룹도 응답에 빈 배열 `[]`로 포함하여 6개 enum 키 모두 항상 응답에 존재한다.
3. **키 순서 보장**: 응답 키 순서는 `AlcoholCategoryGroup` enum 선언 순서(SINGLE_MALT → BLEND → BLENDED_MALT → BOURBON → RYE → OTHER)를 따른다. `LinkedHashMap` + enum 순회로 구현한다.
4. **Breaking Change 허용**: 기존 List 응답 클라이언트는 프론트엔드 어드민 페이지 단일이며, 이슈 본문에 따라 백엔드 변경 후 프론트도 동시에 마이그레이션되므로 응답 형식 변경(breaking change)을 허용한다. v2 신규 엔드포인트를 만들지 않는다.
5. **type 파라미터 없음**: 기존 엔드포인트는 type 필터 없이 전체 카테고리를 반환한다. 본 변경에서도 이를 유지한다.
6. **데이터 소스**: 기존 QueryDSL 메서드(`findAllCategoryPairs`)를 확장하여 `alcohol.categoryGroup` 컬럼을 함께 select한다. 별도 마스터 테이블이나 schema 변경은 없다.
7. **DTO 신규 정의**: 응답 내부 항목은 기존 `CategoryItem(korCategory, engCategory, categoryGroup)`이 아닌, `categoryGroup`이 빠진 새 record(`CategoryPairItem` 또는 유사 명칭)를 정의한다 — 그룹핑 후 항목 안에 `categoryGroup`을 또 넣을 필요가 없기 때문.
8. **캐싱**: 본 엔드포인트는 현재 캐싱되지 않는다(어드민 전용). 캐시 정책 변경 없음.
9. **인증/인가**: 기존 admin-api 보안 설정(`/admin/api/v1` context-path, 어드민 RBAC)을 유지한다.

→ 위 가정 9가지 확인 완료 (사용자 승인).

### 결정 사항 (2026-05-06 확정)

| # | 결정 | 적용 |
|---|------|------|
| 1 | 기존 `findAllCategoryPairs()` 시그니처 변경 허용 | 신규 메서드 추가 X. 다만 영향받는 테스트(`AdminAlcoholsControllerDocsTest`, `InMemoryAlcoholQueryRepository` 2곳)를 빠짐없이 동기화한다. |
| 2 | grouping 로직 위치 | **Service 레이어**(`AlcoholQueryService`)에서 `Map<AlcoholCategoryGroup, List<CategoryPairItem>>` 형태로 반환. 컨트롤러는 단순 위임. |
| 3 | 응답 래퍼 클래스 | 도입하지 않음. 컨트롤러에서 `Map`을 `GlobalResponse.ok()`로 그대로 감싸 반환. **최종 JSON 응답 key가 enum 선언 순서로 정확히 직렬화되는 것만 보장**한다. |

### Success Criteria

1. `GET /admin/api/v1/alcohols/categories/reference` 응답이 `Map<String, List<{korCategory, engCategory}>>` 형태의 JSON 객체로 반환된다.
2. 응답에 6개 키(SINGLE_MALT, BLEND, BLENDED_MALT, BOURBON, RYE, OTHER)가 enum 선언 순서대로 항상 포함된다.
3. DB에 데이터가 0건인 그룹은 빈 배열 `[]`로 응답된다 (키 자체는 누락되지 않음).
4. 응답 항목은 `korCategory` 오름차순으로 정렬된다 (그룹 내 순서 안정성).
5. `JpaAlcoholQueryRepository.findAllCategories(AlcoholType)`(상품 API용)는 기존 동작과 시그니처를 유지한다.
6. `AdminAlcoholsControllerDocsTest`가 새 응답 구조 기준으로 통과한다.
7. `InMemoryAlcoholQueryRepository`(admin/product 양쪽) Fake 구현이 새 메서드 시그니처에 맞춰 동기화된다.
8. RestDocs 문서가 새 응답 구조를 반영한다.
9. `./gradlew :bottlenote-admin-api:test`와 mono 단위 테스트가 모두 통과한다.

### Impact Scope

#### 모듈
- **bottlenote-admin-api** (Kotlin, presentation)
- **bottlenote-mono** (Java, domain/service/repository/dto)
- **bottlenote-product-api** (테스트 픽스처 영향만, 컨트롤러 변경 없음)

#### 변경 파일

**bottlenote-mono (main)**
- `dto/response/CategoryItem.java` — 유지 (product-api `findAllCategories(type)`에서 계속 사용)
- `dto/response/CategoryPairItem.java` (신규) — `{korCategory, engCategory}` record
- `dto/response/CategoryReferenceResponse.java` (신규, 선택) — Map 응답 래퍼 또는 typealias
- `domain/AlcoholQueryRepository.java` — `findAllCategoryPairs()` 시그니처 변경 또는 신규 메서드 추가
- `repository/CustomAlcoholQueryRepository.java` — 동일
- `repository/CustomAlcoholQueryRepositoryImpl.java` — QueryDSL select에 `categoryGroup` 추가, 반환 타입 변경
- `service/AlcoholQueryService.java` — `findAllCategoryPairs()` 반환 타입 변경, grouping 로직 추가 (또는 컨트롤러 위임)

**bottlenote-admin-api (main)**
- `presentation/AdminAlcoholsController.kt` — `getCategoryReference()` 응답 변환 로직 변경 (`mapOf` 제거, Map 직접 반환)

**bottlenote-mono (test)**
- `fixture/InMemoryAlcoholQueryRepository.java` — 시그니처 동기화

**bottlenote-product-api (test)**
- `fixture/InMemoryAlcoholQueryRepository.java` — 시그니처 동기화

**bottlenote-admin-api (test)**
- `app/docs/alcohols/AdminAlcoholsControllerDocsTest.kt` — 응답 구조 변경, RestDocs descriptor 갱신

#### 비변경 영역
- 엔티티 / 스키마 / Liquibase 마이그레이션: 변경 없음 (`alcohol.category_group` 컬럼 이미 존재)
- 도메인 이벤트: 영향 없음
- 캐시: 영향 없음 (해당 엔드포인트 미캐싱)
- 보안 설정: 영향 없음

#### 테스트 종류
- 단위 테스트: `CustomAlcoholQueryRepositoryImpl` grouping 로직 (필요 시)
- 통합 테스트: `AdminAlcoholsControllerDocsTest`에서 응답 구조 검증 + RestDocs
- 아키텍처 규칙 테스트: 영향 없음

---

### 외부 영향 (참고)
- 프론트엔드 어드민(`alcohol.api.ts`)의 `GROUP_TO_CATEGORY`, `CATEGORY_TO_GROUP_MAP` 하드코딩 제거 후속 작업 예정 (별도 PR/이슈)
- #220 위스키 등록 오류는 프론트가 기존 하드코딩으로 우선 수정한 상태이며, 본 변경 후 단일 소스화로 재발 방지

---

## Tasks

### Task 1: 응답 항목 DTO 추가
- **목적**: 그룹 내부 항목용 record 정의
- **파일**: `bottlenote-mono/src/main/java/app/bottlenote/alcohols/dto/response/CategoryPairItem.java` (신규)
- **내용**: `public record CategoryPairItem(String korCategory, String engCategory) {}`
- **검증**: 컴파일 통과

### Task 2: 도메인/QueryDSL 레포지토리 시그니처 변경
- **목적**: 레포지토리는 grouping 책임 없이 `categoryGroup` 포함한 raw 데이터만 반환
- **결정**: 메서드명 `findAllCategoryPairs` → `findAllCategoryItems`로 의도 명확화. 반환 타입 `List<Pair<String,String>>` → `List<CategoryItem>` (기존 `CategoryItem(korCategory, engCategory, categoryGroup)` 재사용 — type 파라미터 없는 전체 조회 용도)
- **파일**:
  - `bottlenote-mono/.../domain/AlcoholQueryRepository.java` — 시그니처 변경
  - `bottlenote-mono/.../repository/CustomAlcoholQueryRepository.java` — 시그니처 변경
  - `bottlenote-mono/.../repository/CustomAlcoholQueryRepositoryImpl.java` — QueryDSL select에 `alcohol.categoryGroup` 추가, `Projections.constructor(CategoryItem.class, ...)` 또는 tuple → CategoryItem 매핑, group by에 categoryGroup 추가, order by `korCategory.asc()` 유지
- **검증**: 단위 테스트로 grouping 입력 데이터 형태 확인 가능

### Task 3: Service 계층 grouping 로직 추가
- **목적**: enum 선언 순서 + 빈 그룹 `[]` 보장하는 `LinkedHashMap` 구성
- **파일**: `bottlenote-mono/.../service/AlcoholQueryService.java`
- **메서드 변경**: `findAllCategoryPairs()` → `findAllCategoryReferenceMap()` (또는 적절한 명칭). 반환 타입 `Map<AlcoholCategoryGroup, List<CategoryPairItem>>`
- **구현**:
  1. `repository.findAllCategoryItems()` 호출
  2. `LinkedHashMap<AlcoholCategoryGroup, List<CategoryPairItem>>` 생성
  3. `AlcoholCategoryGroup.values()` 순회하며 빈 `ArrayList` 초기화 (키 순서 + 빈 그룹 `[]` 보장)
  4. 조회 결과를 `categoryGroup` 기준으로 해당 리스트에 `new CategoryPairItem(korCategory, engCategory)` 추가
  5. 그룹 내 `korCategory` 오름차순 정렬 (DB order by가 이미 보장하지만 방어적으로)
- **검증**: 단위 테스트 (Fake 레포지토리로 grouping 결과 검증)

### Task 4: Admin 컨트롤러 응답 변경
- **파일**: `bottlenote-admin-api/.../presentation/AdminAlcoholsController.kt`
- **변경**:
  - `getCategoryReference()` 내부의 `mapOf` 변환 제거
  - `alcoholQueryService.findAllCategoryReferenceMap()` 결과를 `GlobalResponse.ok()`에 그대로 전달
- **검증**: Jackson 직렬화 시 `LinkedHashMap` 순서 유지 확인 (enum 선언 순)

### Task 5: 테스트 Fake 구현 동기화
- **목적**: 시그니처 변경된 메서드를 InMemory 구현체에 반영
- **파일**:
  - `bottlenote-mono/src/test/java/app/bottlenote/alcohols/fixture/InMemoryAlcoholQueryRepository.java`
  - `bottlenote-product-api/src/test/java/app/bottlenote/alcohols/fixture/InMemoryAlcoholQueryRepository.java`
- **구현**: 기존 `findAllCategoryPairs` 메서드를 새 시그니처로 교체. `alcohols.values().stream().map(a -> new CategoryItem(a.getKorCategory(), a.getEngCategory(), a.getCategoryGroup())).distinct().toList()`

### Task 6: AdminAlcoholsControllerDocsTest 갱신
- **파일**: `bottlenote-admin-api/src/test/kotlin/app/docs/alcohols/AdminAlcoholsControllerDocsTest.kt`
- **변경**:
  - `given(alcoholQueryService.findAllCategoryReferenceMap())` stub을 `LinkedHashMap` 형태로 구성 (모든 enum 키 포함, 일부는 빈 리스트)
  - RestDocs `responseFields` 갱신: `data[].korCategory` 형태 → `data.SINGLE_MALT[].korCategory` / `data.BLEND[].korCategory` 형태로 각 enum 키 6개에 대해 documenting (또는 동적 패턴)
  - 응답 키 순서 검증 추가 가능 시 추가
- **검증**: `./gradlew :bottlenote-admin-api:asciidoctor` 통과

### Task 7: 검증 (/verify full)
- **명령**:
  - `./gradlew :bottlenote-mono:test` (단위/통합)
  - `./gradlew :bottlenote-admin-api:test`
  - `./gradlew :bottlenote-product-api:test` (Fake 동기화 영향 확인)
  - `./gradlew :bottlenote-admin-api:asciidoctor`
- **성공 기준**: 모든 테스트 통과, RestDocs 빌드 성공

## Implementation Order
Task 1 → Task 2 → Task 3 → Task 5 (Fake 먼저 컴파일 깨짐 방지) → Task 4 → Task 6 → Task 7

> 주의: Task 2에서 시그니처를 바꾸면 Task 3, 4, 5가 모두 컴파일 에러 상태가 됨. Task 5(Fake 동기화)를 먼저 끝내고 Task 4(컨트롤러)로 넘어가야 admin-api 모듈 컴파일이 복구됨.

## Progress Log

### 2026-05-06
- Task 1 완료: `CategoryPairItem` record 신규 (`bottlenote-mono/.../dto/response/CategoryPairItem.java`)
- Task 2 완료: 레포지토리 시그니처 변경
  - `AlcoholQueryRepository`, `CustomAlcoholQueryRepository`: `findAllCategoryPairs(): List<Pair<String, String>>` → `findAllCategoryItems(): List<CategoryItem>`
  - `CustomAlcoholQueryRepositoryImpl`: QueryDSL select에 `alcohol.categoryGroup` 추가, `Projections.constructor(CategoryItem.class, ...)`로 매핑
- Task 3 완료: `AlcoholQueryService.findAllCategoryReferenceMap()` 추가
  - `EnumMap<AlcoholCategoryGroup, List<CategoryPairItem>>`로 enum 선언 순서 + 빈 그룹 `[]` 보장
- Task 5 완료: Fake 동기화 (`bottlenote-mono/.../fixture/InMemoryAlcoholQueryRepository.java`, `bottlenote-product-api/.../fixture/InMemoryAlcoholQueryRepository.java`)
- Task 4 완료: `AdminAlcoholsController.kt`의 `getCategoryReference()`를 단순 위임으로 변경 (`mapOf` 제거)
- Task 6 완료: `AdminAlcoholsControllerDocsTest`의 stub과 RestDocs `responseFields` 갱신
- Task 7 완료: 검증
  - `:bottlenote-mono:compileJava`, `:bottlenote-admin-api:compileKotlin` BUILD SUCCESSFUL
  - `:bottlenote-admin-api:test` 전체 통과 (카테고리 레퍼런스 테스트 포함)
  - `:bottlenote-mono:unit_test`, `:bottlenote-product-api:unit_test` BUILD SUCCESSFUL
  - 실제 RestDocs 응답 결과(`build/generated-snippets/admin/alcohols/category-reference/response-body.adoc`)가 의도한 grouped 구조와 정확히 일치 (enum 선언 순서, `RYE: []` 포함)
  - `:bottlenote-admin-api:asciidoctor` BUILD SUCCESSFUL
  - `:bottlenote-mono:integration_test` BUILD SUCCESSFUL
  - `:bottlenote-product-api:integration_test` BUILD SUCCESSFUL (4m 6s)
