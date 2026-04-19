# Plan: 둘러보기 API 메인 검색 확장

## Overview

### 배경

기존에 위스키 "둘러보기"(`/api/v1/alcohols/explore/standard`)와 "검색"(`/api/v1/alcohols/search`)이 UX상 별도 페이지로 존재했으나, 개념이 겹치고 기존 검색의 성능이 부족했다. 최근 둘러보기에 2단계 쿼리 기반 성능 개선을 적용하여 대폭 빨라진 상황(`/위스키 둘러보기 API 성능개선.md` 참조)이며, 이를 계기로 **둘러보기를 메인 검색 엔드포인트로 확장**하고 검색의 필터·정렬·응답값을 흡수하기로 결정했다.

### 무엇을 만드는가

`/api/v1/alcohols/explore/standard` 엔드포인트에 검색의 핵심 옵션(필터, 정렬, 응답값)을 추가한다.

- 필터: `category`, `regionIds`(복수·OR), `distilleryIds`(복수·OR), `curationId`
- 정렬: `sortType`(RANDOM/POPULAR/RATING/REVIEW/PICK), `sortOrder`(ASC/DESC)
- 응답값 보강: `reviewCount`, `pickCount`
- 반환 타입 단순화: `Pair<Long, CursorResponse<...>>` → `CursorResponse<AlcoholDetailItem>`

### 왜 만드는가

1. **UX 통합**: 둘러보기·검색의 개념적 중복 제거, 하나의 메인 탐색 엔드포인트로 수렴
2. **성능 이점 계승**: 기존 검색의 단일 쿼리 방식에서 벗어나 2단계 쿼리 구조 위에 기능 확장 → 성능 이득 유지
3. **기능 보강**: 검색에 없던 `regionIds` 복수 OR, `distilleryIds` 신규 필터 등 탐색성 강화

### Assumptions

1. **엔드포인트 경로 유지**: `/api/v1/alcohols/explore/standard` 그대로 사용 (이름 변경 없음)
2. **기존 검색 엔드포인트 유지**: `/api/v1/alcohols/search`는 이번 이슈에서 변경하지 않음. deprecate 여부는 별도 결정
3. **인증**: 선택적 인증 유지 (미인증 시 `userId = -1L`)
4. **DB 스키마 변경 없음**: QueryDSL 레이어만 수정
5. **반환 타입 단순화**: `Pair<Long, CursorResponse<AlcoholDetailItem>>` → `CursorResponse<AlcoholDetailItem>` (total=0 고정값 제거)
6. **큐레이션 자동 매핑 비지원**: 기존 검색 컨트롤러의 `"비 오는 날 추천 위스키"` 등 키워드→curationId 매핑 로직은 **이식하지 않음**. `curationId`는 순수 요청 파라미터로만 동작
7. **필터 파라미터 명명 및 논리**:
   - `keywords`: `List<String>`, 각 키워드 간 **AND**, 각 키워드는 `korName/engName/korCategory/engCategory/region.korName/region.engName + 테이스팅 태그` 중 하나 이상에 LIKE 매칭
   - `category`: `AlcoholCategoryGroup` 단수
   - `regionIds`: `List<Long>`, 복수값 간 **OR** (IN 절). 각 regionId에 대해 자식 region 확장 후 합집합 → 최종 `IN` (기존 `eqRegion`의 자식 확장 로직 유지)
   - `distilleryIds`: `List<Long>`, 복수값 간 **OR** (단순 `IN`, 계층 확장 없음)
   - `curationId`: `Long` 단수, 검색의 `eqCurationId` 서브쿼리 재사용
   - 서로 다른 필터 간(keywords vs category vs regionIds 등): **AND** 결합
8. **정렬 파라미터**:
   - `sortType`: RANDOM(기본), POPULAR, RATING, REVIEW, PICK
   - `sortOrder`: ASC/DESC (RANDOM은 무시)
   - 비 RANDOM 정렬은 `alcohol.id ASC` 보조 정렬로 페이지 흔들림 방지
9. **성능 목표**: `/위스키 둘러보기 API 성능개선.md`의 측정값 수준을 유지 — dev 환경에서 신규 필터·정렬 조합 시 **p50 ≤ 130ms, p95 ≤ 400ms, 가장 무거운 케이스 avg ≤ 200ms**를 유지해야 함
10. **2단계 쿼리 구조 유지 필수**:
    - 1단계: `alcohol.id` 만 추출하는 경량 쿼리 (heavy 상관 서브쿼리 금지)
    - 2단계: 후보 ID에 대한 본문·집계·상관 서브쿼리 수행
    - `keywordsMatch`의 EXISTS→IN 패턴 유지 (성능개선 이슈에서 확정된 기법)
11. **RestDocs 문서화**: 신규 파라미터·응답 필드를 모두 반영하여 `explore.standard.adoc` 갱신

### Success Criteria

1. `GET /api/v1/alcohols/explore/standard`가 신규 파라미터 `category`, `regionIds`(복수), `distilleryIds`(복수), `curationId`, `sortType`, `sortOrder`를 수신하고 각각의 필터·정렬이 결과에 반영된다
2. `sortType` 미지정 시 RANDOM으로 동작하며, POPULAR/RATING/REVIEW/PICK 정렬은 `sortOrder`에 따라 올바른 순서로 반환된다 (POPULAR = avgRating + reviewCount 합산 기준)
3. `regionIds`는 복수값 OR 조건으로 동작하고, 각 regionId에 대해 자식 region까지 포함된다 (기존 `eqRegion` 자식 확장 동작 계승)
4. `distilleryIds`는 복수값 OR 조건(단순 IN)으로 동작한다
5. 응답 `AlcoholDetailItem`에 `reviewCount`, `pickCount`가 포함된다
6. 반환 타입이 `CursorResponse<AlcoholDetailItem>`로 단순화되고, 응답 meta에는 `keywords` 외 적용된 필터·정렬 정보가 포함된다
7. **쿼리 구조 검증 테스트가 존재한다**: 1단계 쿼리에 `myRating`/`averageReviewRating`/`isPickedSubquery`/`getTastingTags` 등 heavy 상관 서브쿼리가 포함되지 않음을 검증
8. **성능 회귀 없음**: dev k6 측정 기준 아래 임계값 충족
   - 키워드 없는 첫 페이지: avg ≤ 160ms, p95 ≤ 300ms
   - 키워드 많은 케이스(예: "위스키"): avg ≤ 200ms, p95 ≤ 400ms
   - 다중 키워드(탈라모어+사과): avg ≤ 160ms, p95 ≤ 250ms
   - 신규 필터 조합(keywords + category + regionIds + distilleryIds): avg ≤ 200ms, p95 ≤ 400ms
9. 단위 테스트(필터·정렬별 케이스), 통합 테스트(필터 조합 시나리오), RestDocs 테스트 모두 통과
10. RestDocs 문서(`bottlenote-product-api/src/docs/asciidoc/api/alcohols/explore.standard.adoc`)에 신규 파라미터·응답 필드 설명이 모두 기재된다
11. HTTP 샘플(`http/product/02_위스키탐색/위스키정보/둘러보기.http`)에 신규 파라미터 조합 예시가 추가된다

### Impact Scope

#### 모듈
- `bottlenote-mono`: 리포지토리/서비스/DTO/Supporter 수정
- `bottlenote-product-api`: 컨트롤러·테스트·RestDocs 수정

#### 도메인
- alcohols (단일 도메인, Facade 불필요)

#### 파일 변경 목록

**신규 또는 대폭 수정**
- `bottlenote-mono/.../alcohols/dto/request/ExploreStandardRequest.java` (신규 — 요청 DTO)
- `bottlenote-mono/.../alcohols/dto/dsl/ExploreStandardCriteria.java` (신규 — 서비스→리포 전달용)
- `bottlenote-mono/.../alcohols/repository/CustomAlcoholQueryRepositoryImpl.java`
  - `getStandardExplore`: 1단계 정렬 분기, 필터 합류, 2단계 LEFT JOIN(review/picks) 추가, 반환 타입 `CursorResponse`로 단순화
- `bottlenote-mono/.../alcohols/repository/AlcoholQuerySupporter.java`
  - 1단계용 정렬 판별 + RANDOM 외 정렬 지원
  - `inRegionIds(List<Long>)` 추가 (자식 확장 + IN)
  - `inDistilleryIds(List<Long>)` 추가 (단순 IN)
- `bottlenote-mono/.../alcohols/dto/response/AlcoholDetailItem.java`
  - `reviewCount`, `pickCount` 필드 추가 — **주의**: 다른 사용처(`findAlcoholDetailById` 등)에도 영향, 해당 쿼리들도 카운트 반영 필요 여부 결정
- `bottlenote-mono/.../alcohols/domain/AlcoholQueryRepository.java` + `CustomAlcoholQueryRepository.java`
  - 시그니처 교체
- `bottlenote-mono/.../alcohols/service/AlcoholQueryService.java`
  - `getStandardExplore` 시그니처 교체 (`ExploreStandardRequest` 기반)
- `bottlenote-product-api/.../alcohols/controller/AlcoholExploreController.java`
  - 파라미터 확장, `@ModelAttribute @Valid ExploreStandardRequest` 수신

**테스트**
- `bottlenote-product-api/src/test/.../alcohols/fixture/InMemoryAlcoholQueryRepository.java` (신규 메서드 대응)
- `bottlenote-mono/src/test/.../alcohols/fixture/InMemoryAlcoholQueryRepository.java`
- `bottlenote-product-api/src/test/.../alcohols/repository/CustomJpaAlcoholQueryRepositoryImplTest.java` (필터·정렬 케이스)
- `bottlenote-product-api/src/test/.../alcohols/service/AlcoholQueryServiceTest.java`
- `bottlenote-product-api/src/test/.../alcohols/integration/AlcoholQueryIntegrationTest.java`
- `bottlenote-product-api/src/test/.../docs/alcohols/RestAlcoholExploreControllerTest.java` (RestDocs)
- (선택) 2단계 쿼리 구조 검증 테스트 — 1단계 쿼리에 heavy 서브쿼리 미포함 확인

**문서**
- `bottlenote-product-api/src/docs/asciidoc/api/alcohols/explore.standard.adoc` — 파라미터·응답 필드 전면 갱신
- `bottlenote-product-api/src/docs/asciidoc/product-api.adoc` — include 확인
- `http/product/02_위스키탐색/위스키정보/둘러보기.http` — 신규 파라미터 샘플 추가

#### 변경 없음
- DB 스키마 / Liquibase
- 이벤트, 캐시
- 기존 `/api/v1/alcohols/search` 엔드포인트 및 관련 DTO
- `AlcoholSearchRequest`, `AlcoholSearchCriteria`, `AlcoholsSearchItem`

#### 리스크 / 주의
- `AlcoholDetailItem`에 필드를 추가하면 알코올 상세 조회(`findAlcoholDetailById`) 등 다른 사용처의 생성자 호출 지점도 함께 갱신해야 함 → `/plan` 단계에서 호출부 전수조사 필요
- `regionIds`에 자식 확장을 적용하면 `regionRepository.findChildRegionIds`가 원소 수만큼 호출될 수 있음 → 일괄 조회(`findChildRegionIdsIn`) 도입 고려
- 1단계 쿼리에 비 RANDOM 정렬을 수용할 때 `GROUP BY`와 집계가 추가되면 성능 regression 가능 → 정렬별 쿼리 분기 또는 필요한 컬럼만 최소 집계로 제한
- `Pair`→`CursorResponse` 변경은 컨트롤러 응답 구조가 바뀌므로 **클라이언트 계약 변경**에 해당. 프론트와 응답 스키마 확인 필요

## Tasks

작업 순서는 위험도/의존성 기준: **반환 타입 단순화 → DTO 필드 보강 → Supporter 헬퍼 → 요청 DTO → 필터 확장 → 정렬 확장 → 문서/샘플/성능 검증**. 각 태스크는 독립적으로 컴파일·테스트 통과하는 커밋 단위로 구성.

### Task 1: 반환 타입 단순화 (`Pair<Long, CursorResponse>` → `CursorResponse`) ✓
- 수용 기준:
  - `CustomAlcoholQueryRepository.getStandardExplore` 반환 타입 `CursorResponse<AlcoholDetailItem>`
  - `AlcoholQueryService.getStandardExplore` 반환 타입 동일하게 변경
  - `AlcoholExploreController`에서 `Pair` 해체 코드 제거, 응답 body 구조는 기존 `data` 필드 구조 유지
  - 기존 단위/통합 테스트 모두 통과 (total 값을 참조하던 테스트 수정 포함)
  - InMemory fixture 2곳(`mono`, `product-api`) 시그니처 맞춤
- 검증: `./gradlew :bottlenote-product-api:test :bottlenote-mono:test`
- 파일:
  - `bottlenote-mono/.../alcohols/domain/AlcoholQueryRepository.java`
  - `bottlenote-mono/.../alcohols/repository/CustomAlcoholQueryRepository.java`
  - `bottlenote-mono/.../alcohols/repository/CustomAlcoholQueryRepositoryImpl.java`
  - `bottlenote-mono/.../alcohols/service/AlcoholQueryService.java`
  - `bottlenote-product-api/.../alcohols/controller/AlcoholExploreController.java`
  - `bottlenote-mono/src/test/.../alcohols/fixture/InMemoryAlcoholQueryRepository.java`
  - `bottlenote-product-api/src/test/.../alcohols/fixture/InMemoryAlcoholQueryRepository.java`
- 크기: M
- 상태: [ ] 미완료

### Task 2: `AlcoholDetailItem`에 `reviewCount`/`pickCount` 추가 + 2단계 쿼리 집계 보강 ✓
- 수용 기준:
  - `AlcoholDetailItem`에 `Long reviewCount`, `Long pickCount` 필드 추가
  - `getStandardExplore` 2단계 쿼리에 `LEFT JOIN review`, `LEFT JOIN picks` 및 `countDistinct` projection 추가, `GROUP BY` 갱신
  - **다른 Projections.constructor 호출부 동기화**: `findAlcoholDetailById`(알코올 상세) 등 `AlcoholDetailItem`을 생성자로 만드는 모든 QueryDSL 지점에 새 필드 projection 반영 (값이 필요 없으면 0L 상수 또는 동일 집계 적용)
  - `AlcoholViewHistoryService` 영향 확인 및 필요 시 업데이트
  - InMemory fixture와 `AlcoholQueryFixture`에도 필드 반영
- 검증: `./gradlew :bottlenote-product-api:test :bottlenote-mono:test`, 기존 알코올 상세 조회 RestDocs 깨지지 않는지 확인
- 파일:
  - `bottlenote-mono/.../alcohols/dto/response/AlcoholDetailItem.java`
  - `bottlenote-mono/.../alcohols/repository/CustomAlcoholQueryRepositoryImpl.java` (`getStandardExplore`, `findAlcoholDetailById`)
  - `bottlenote-mono/.../history/service/AlcoholViewHistoryService.java` (필요 시)
  - `bottlenote-product-api/src/test/.../alcohols/fixture/AlcoholQueryFixture.java`
  - InMemory fixture 2곳
- 크기: M
- 상태: [ ] 미완료

### Checkpoint: Task 1~2 완료 후
- [ ] 전체 컴파일 통과
- [ ] 기존 단위/통합 테스트 전부 그린
- [ ] `AlcoholDetailItem` 호출부 누락 없음 (rg로 생성자 호출 지점 스캔)
- [ ] 알코올 상세 조회(`GET /api/v1/alcohols/{id}`) 응답 필드 정상

### Task 3: `AlcoholQuerySupporter`에 필터/정렬 헬퍼 추가 ✓
- 수용 기준:
  - `inRegionIds(List<Long>)`: null/empty일 때 조건 무시, 각 id에 대해 자식 region 확장 후 `alcohol.region.id.in(...)` 생성. `findChildRegionIds` 반복 호출을 피하려면 일괄 조회 메서드(`findChildRegionIdsIn(Collection<Long>)`) 신설하거나, N개 id에 대해 stream으로 한번에 병합
  - `inDistilleryIds(List<Long>)`: null/empty일 때 무시, 단순 `alcohol.distillery.id.in(...)`
  - 1단계 ID 추출용 정렬 판별 메서드 (RANDOM / POPULAR / RATING / REVIEW / PICK 모두 지원, `alcohol.id ASC` 보조 정렬 포함). 기존 `sortBy`는 2단계 projection 환경 전용이므로 필요 시 ID-only 경로용 별도 오버로드 추가
  - 단위 테스트 (null 입력, 빈 리스트, 다중 값 케이스)
- 검증: `./gradlew :bottlenote-mono:unit_test`
- 파일:
  - `bottlenote-mono/.../alcohols/repository/AlcoholQuerySupporter.java`
  - `bottlenote-mono/.../alcohols/domain/RegionRepository.java` (+ JPA 구현) — 일괄 조회 메서드 신설 시
  - 단위 테스트 파일 (신규 또는 기존 supporter 테스트 확장)
- 크기: S
- 상태: [ ] 미완료

### Task 4: 요청 DTO 신설 (`ExploreStandardRequest` + `ExploreStandardCriteria`) ✓
- 수용 기준:
  - `ExploreStandardRequest` record: `keywords(List<String>)`, `category`, `regionIds(List<Long>)`, `distilleryIds(List<Long>)`, `curationId`, `sortType`, `sortOrder`, `cursor(Long)`, `size(Integer)` + 컴팩트 생성자 기본값 (`sortType=RANDOM`, `sortOrder=DESC`, `cursor=0`, `size=20`, 컬렉션 null→empty)
  - `ExploreStandardCriteria`: 서비스→리포 전달용, `of(request, userId)` 팩토리
  - 검증 어노테이션 최소 적용 (`@Positive`, `@Size` 등 — 프로젝트 컨벤션 확인)
- 검증: `./gradlew :bottlenote-mono:unit_test`
- 파일:
  - `bottlenote-mono/.../alcohols/dto/request/ExploreStandardRequest.java` (신규)
  - `bottlenote-mono/.../alcohols/dto/dsl/ExploreStandardCriteria.java` (신규)
  - (선택) 검증 단위 테스트
- 크기: S
- 상태: [ ] 미완료

### Task 5: 필터 확장 — 요청/서비스/리포 연결 (`category`/`regionIds`/`distilleryIds`/`curationId`) ✓
- 수용 기준:
  - `AlcoholExploreController`가 `@ModelAttribute @Valid ExploreStandardRequest` 수신, 기존 `keywords/size/cursor`는 동일하게 바인딩
  - `AlcoholQueryService.getStandardExplore` 시그니처 `(userId, request)` 또는 `(criteria)` 기반으로 교체
  - `CustomAlcoholQueryRepositoryImpl.getStandardExplore` 1단계 where에 `eqCategory` + `inRegionIds` + `inDistilleryIds` + `eqCurationId` 합류
  - 2단계 쿼리 구조 유지 (heavy 서브쿼리는 2단계에만 존재)
  - 단위 테스트: 필터별 ON/OFF, 복수 값 케이스, null 처리
  - 응답 meta에 적용된 필터 정보 포함 (`keywords` 외 `searchParameters` 형태 권장)
- 검증: `./gradlew :bottlenote-product-api:test`
- 파일:
  - `bottlenote-product-api/.../alcohols/controller/AlcoholExploreController.java`
  - `bottlenote-mono/.../alcohols/service/AlcoholQueryService.java`
  - `bottlenote-mono/.../alcohols/repository/CustomAlcoholQueryRepositoryImpl.java`
  - `bottlenote-mono/.../alcohols/domain/AlcoholQueryRepository.java`
  - InMemory fixture 2곳
  - 단위/통합 테스트
- 크기: M
- 상태: [ ] 미완료

### Task 6: 정렬 확장 — `sortType`/`sortOrder` 지원 + 2단계 구조 검증 테스트 ✓
- 수용 기준:
  - 1단계 ID 추출 쿼리에 정렬 분기 적용:
    - RANDOM: 현 랜덤 정렬 경로 유지 (조인/집계 최소)
    - POPULAR: `AVG(rating) + COUNT(DISTINCT review.id)` 기반 정렬 + 필요한 LEFT JOIN / GROUP BY
    - RATING: `AVG(rating)` 기반 정렬 + rating LEFT JOIN
    - REVIEW: `COUNT(DISTINCT review.id)` 기반 정렬 + review LEFT JOIN
    - PICK: `COUNT(DISTINCT picks.id)` 기반 정렬 + picks LEFT JOIN
    - 모든 비 RANDOM 정렬은 `alcohol.id ASC` 보조 정렬로 타이브레이크
  - 2단계 본문 쿼리는 정렬 로직 보유하지 않음 (`IN` 결과를 앱에서 1단계 순서로 재정렬)
  - **1단계 쿼리 구조 검증 테스트**: 1단계에서 만들어지는 QueryDSL 표현에 `myRating`/`averageReviewRating`/`isPickedSubquery`/`getTastingTags`가 포함되지 않음을 검증 (필요 시 해당 로직을 별도 메서드로 추출 후 메서드 호출 격리 테스트, 또는 SQL 캡처 테스트)
  - 정렬별 정수 데이터 기반 단위 테스트
- 검증: `./gradlew :bottlenote-product-api:test :bottlenote-mono:test`
- 파일:
  - `bottlenote-mono/.../alcohols/repository/CustomAlcoholQueryRepositoryImpl.java`
  - `bottlenote-mono/.../alcohols/repository/AlcoholQuerySupporter.java`
  - 단위/통합 테스트
- 크기: M
- 상태: [ ] 미완료

### Checkpoint: Task 3~6 완료 후
- [ ] 컴파일 + 단위 테스트 통과
- [ ] 통합 테스트 통과 (`./gradlew integration_test`)
- [ ] 아키텍처 규칙 통과 (`./gradlew check_rule_test`)
- [ ] 2단계 쿼리 구조 검증 테스트 존재 및 그린

### Task 7: RestDocs 갱신 + 통합 테스트 시나리오 추가 ✓
- 수용 기준:
  - `RestAlcoholExploreControllerTest`에 신규 파라미터(`category`, `regionIds`, `distilleryIds`, `curationId`, `sortType`, `sortOrder`) + 응답 필드(`reviewCount`, `pickCount`)의 RestDocs 스니펫 정의
  - `explore.standard.adoc` 문서에서 요청 파라미터 표, 응답 필드 표, 메타 필드 표를 신규 내용으로 갱신 — 누락 없이 모든 필드 기재
  - `AlcoholQueryIntegrationTest`에 복합 필터 시나리오(keywords + category + regionIds + distilleryIds) 추가
  - `./gradlew asciidoctor` 성공 및 렌더링 결과에 신규 섹션 포함
- 검증: `./gradlew :bottlenote-product-api:test :bottlenote-product-api:asciidoctor`
- 파일:
  - `bottlenote-product-api/src/test/.../docs/alcohols/RestAlcoholExploreControllerTest.java`
  - `bottlenote-product-api/src/docs/asciidoc/api/alcohols/explore.standard.adoc`
  - `bottlenote-product-api/src/test/.../alcohols/integration/AlcoholQueryIntegrationTest.java`
- 크기: M
- 상태: [ ] 미완료

### Task 8: HTTP 샘플 갱신 + 성능 회귀 확인
- 수용 기준:
  - `http/product/02_위스키탐색/위스키정보/둘러보기.http`에 신규 파라미터 조합 호출 예시 추가 (정렬별 1건, 필터 조합 1건, regionIds 복수/distilleryIds 복수 1건 이상)
  - 기존 README와 앵커 링크 정합성 확인 (`http/product/README.md`)
  - 로컬 또는 dev 환경에서 성능 회귀 확인 — 키워드 없는 첫 페이지 / D 케이스("위스키") / 다중 키워드 / 신규 필터 조합 각각 간이 측정. 수용 임계값(`Success Criteria #8`) 충족
  - 측정 결과를 Progress Log에 기록
- 검증: k6 또는 간단 curl 스크립트로 임계값 확인, RestDocs 렌더링 재확인
- 파일:
  - `http/product/02_위스키탐색/위스키정보/둘러보기.http`
  - `http/product/README.md` (필요 시)
- 크기: S
- 상태: [ ] 미완료

### Checkpoint: Task 7~8 완료 후 (PR 전)
- [ ] `/verify full` 통과
- [ ] RestDocs HTML 렌더링 결과 확인
- [ ] 성능 임계값 충족 확인 결과가 Progress Log에 기록됨
- [ ] `AlcoholDetailItem` 호환성 확인 (상세 조회 응답에서 `reviewCount`/`pickCount` 노출 여부가 기획과 일치)

## Progress Log

### 2026-04-19 Task 1 완료
- `AlcoholQueryRepository`, `CustomAlcoholQueryRepository`, `CustomAlcoholQueryRepositoryImpl`, `AlcoholQueryService`에서 `Pair<Long, CursorResponse<AlcoholDetailItem>>` → `CursorResponse<AlcoholDetailItem>`로 단순화
- Controller는 `CollectionResponse.of(0L, cursorResponse)`로 래핑하여 기존 응답 JSON 구조(`data.totalCount`, `data.items`, `meta.pageable`, `meta.searchParameters`) 완전 호환 유지
- InMemory fixture 2곳 시그니처 업데이트
- `RestAlcoholExploreControllerTest`의 mock 반환값을 `CursorResponse`로 교체, `Pair` import 제거
- 검증: `:bottlenote-product-api:unit_test` 성공, `RestAlcoholExploreControllerTest` 응답 body 호환 확인
  - (참고) `:bottlenote-mono:unit_test`는 MinIO Docker 초기화로 1건 실패, 본 변경과 무관
- 커밋: `2a0572f1 refactor: simplify explore API return type (remove Pair wrapper)`

### 2026-04-19 Task 7 완료
- `explore.standard.adoc`: 상단 설명 전면 갱신 — 기본값 `RANDOM`, 정렬/필터/페이지네이션 정책, 복수 파라미터 사용법 안내
- `RestAlcoholExploreControllerTest`는 Task 5에서 이미 신규 파라미터/필드 스니펫을 포함하도록 확장됨 → 별도 보강 불필요
- `AlcoholQueryIntegrationTest`에 둘러보기 시나리오 3건 추가:
  - `explore_default`: 기본 호출 시 응답 구조(items/pageable/searchParameters/reviewCount/pickCount) 유지 확인
  - `explore_filter_by_regionIds`: 복수 regionIds OR 필터 동작 확인 (regionC 제외)
  - `explore_sort_popular_desc`: POPULAR/DESC 정렬 파라미터 전달 및 응답 searchParameters 반영 확인
- 검증: 컴파일 통과. **통합 테스트 실제 실행은 로컬 Docker/TestContainers 환경 부재로 확인 불가** — Docker 환경 보유 개발자/CI에서 실행 필요

### 2026-04-19 Task 6 완료
- `CustomAlcoholQueryRepositoryImpl`:
  - `getStandardExplore`의 1단계 쿼리를 `fetchCandidateIds` private 메서드로 추출
  - RANDOM: 기존 경량 경로 유지 (rating/review/picks LEFT JOIN 없음, ORDER BY rand())
  - POPULAR/RATING: `LEFT JOIN rating` + GROUP BY alcohol.id + `sortBy(...)` + `alcohol.id ASC` 보조 정렬
  - REVIEW: `LEFT JOIN review` + GROUP BY
  - PICK: `LEFT JOIN picks` + GROUP BY
  - POPULAR는 `rating` + `review` 모두 필요 → 둘 다 LEFT JOIN
  - 정렬 타입별 필요 테이블 판별용 `needsRatingJoin/needsReviewJoin/needsPicksJoin` 헬퍼 3종
- 2단계 구조 검증: `ExploreStandardQueryStructureTest` 추가. `fetchCandidateIds` 메서드 본문에 heavy 상관 서브쿼리(`myRating`, `averageReviewRating`, `isPickedSubquery`, `getTastingTags`) 호출이 포함되지 않음을 소스 텍스트 기반으로 검증
- 검증: `:bottlenote-mono:unit_test` 구조 검증 테스트 그린, `:bottlenote-product-api:unit_test` 전체 그린

### 2026-04-19 Task 5 완료
- `AlcoholQueryRepository` / `CustomAlcoholQueryRepository` / `CustomAlcoholQueryRepositoryImpl.getStandardExplore` 시그니처를 `ExploreStandardCriteria` 기반으로 교체
- 1단계 ID 추출 쿼리 where 절에 `keywordsMatch + eqCategory + inRegionIds + inDistilleryIds + eqCurationId` 합류 (heavy 서브쿼리는 여전히 2단계에서만 실행)
- `AlcoholQueryService.getStandardExplore`는 `(ExploreStandardRequest, userId)` 시그니처로 교체, 내부에서 Criteria로 변환
- Controller는 `@ModelAttribute @Valid ExploreStandardRequest` 수신, `meta.searchParameters`에 request DTO 전체 노출
- InMemory fixture 2곳 시그니처 대응
- `RestAlcoholExploreControllerTest`:
  - `when(...)` 매처 2-arg로 축소
  - queryParameters 스니펫에 `category`/`regionIds`/`distilleryIds`/`curationId`/`sortType`/`sortOrder` 추가
  - `meta.searchParameters.*` 신규 필드 7종 스니펫 추가
- 검증: `:bottlenote-product-api:unit_test` 전체 그린

### 2026-04-19 Task 4 완료
- `ExploreStandardRequest` record 신설: keywords/category/regionIds/distilleryIds/curationId/sortType/sortOrder/cursor/size + compact constructor에 기본값(RANDOM/DESC/0/20, 컬렉션 null→empty)
- `ExploreStandardCriteria` record 신설: 서비스→리포 전달용, `of(request, userId)` 팩토리
- 검증: `:bottlenote-mono:compileJava` 그린

### 2026-04-19 Task 3 완료
- `SearchSortType`에 `RANDOM` 값 추가 (5종 총 지원)
- `RegionRepository` + `JpaRegionQueryRepository`에 `findChildRegionIdsIn(Collection<Long>)` 일괄 자식 확장 메서드 신설 → 복수 regionIds에 대해 N+1 호출 방지
- `AlcoholQuerySupporter`:
  - `inRegionIds(List<Long>)`: 복수 부모 지역 입력 시 자식까지 합집합 IN (LinkedHashSet으로 중복 제거)
  - `inDistilleryIds(List<Long>)`: 단순 IN, 계층 확장 없음
  - `sortBy(...)`에 `RANDOM` 케이스 추가 (`sortByRandom()` 위임)
- 검증: compile OK, `:bottlenote-product-api:unit_test` 그린
- 커밋: TBD

### 2026-04-19 Task 2 완료
- `AlcoholDetailItem`에 `reviewCount`, `pickCount` 필드 추가 (isPicked 뒤, alcoholsTastingTags 앞)
- 2단계 쿼리와 `findAlcoholDetailById`에 `LEFT JOIN review`, `LEFT JOIN picks` + `countDistinct` projection 추가. `rating.id.count()`도 일관성을 위해 `countDistinct()`로 변경 (다중 LEFT JOIN 시 rating/review/picks의 조합이 카티시안 곱으로 부풀려지는 것을 방지)
- `AlcoholQueryFixture` builder에 `reviewCount`/`pickCount` 랜덤값 삽입
- 영향 받은 기존 RestDocs 테스트에 신규 필드 스니펫 추가:
  - `RestAlcoholExploreControllerTest` (둘러보기)
  - `RestAlcoholQueryControllerTest` (알코올 상세)
- `AlcoholViewHistoryService`는 getter만 호출하므로 영향 없음
- 검증: `:bottlenote-product-api:unit_test` 및 해당 RestDocs 테스트 그린. 상세 응답 JSON에 `reviewCount: 79`, `pickCount: 255` 노출 확인


