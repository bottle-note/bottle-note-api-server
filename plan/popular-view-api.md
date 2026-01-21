# 조회수 기반 인기 위스키 API 구현 계획

## 개요
이번 주/월 조회수 기반 인기 위스키 조회 API 추가. 조회 기록이 부족하면 평점 높은 주류로 채워서 항상 20개를 반환.

## 엔드포인트
- `GET /api/v1/popular/view/week` - 주간 인기 위스키
- `GET /api/v1/popular/view/monthly` - 월간 인기 위스키

## 쿼리 파라미터
| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `top` | Integer | 20 | 조회할 개수 |

## 응답 형식
기존 `PopularItem` 재사용:
```java
public record PopularItem(
    Long alcoholId,
    String korName,
    String engName,
    Double rating,
    Long ratingCount,
    String korCategory,
    String engCategory,
    String imageUrl,
    Boolean isPicked,
    Double popularScore  // 조회수 기반이므로 viewCount로 대체 고려
)
```

## 비즈니스 로직
1. 기간 내 `alcohols_view_histories` 조회수 집계
2. 조회수 높은 순 정렬
3. 결과가 `top`개 미만이면 평점 높은 주류로 채움 (중복 제외)
4. 최종 `top`개 반환

## 레포지토리 구조

```
PopularQueryRepository (도메인 인터페이스) - 메서드 시그니처 추가
    ↑ 구현
JpaPopularQueryRepository (JPA + Custom 상속)
    ↑ 상속
CustomPopularQueryRepository (QueryDSL 인터페이스) - 신규
    ↑ 구현
CustomPopularQueryRepositoryImpl (QueryDSL 구현체) - 신규
```

## 파일 구조

### bottlenote-mono (핵심 로직)
```
app.bottlenote.alcohols/
├── domain/
│   └── PopularQueryRepository.java          # 메서드 추가 (기존)
├── repository/
│   ├── CustomPopularQueryRepository.java    # 신규 - QueryDSL 인터페이스
│   ├── CustomPopularQueryRepositoryImpl.java # 신규 - QueryDSL 구현
│   └── JpaPopularQueryRepository.java       # Custom 상속 추가 (기존)
└── service/
    └── AlcoholPopularService.java           # 메서드 추가 (기존)
```

### bottlenote-product-api (컨트롤러 + 테스트)
```
# 메인
app.bottlenote.alcohols/
└── controller/
    └── AlcoholPopularQueryController.java   # 엔드포인트 추가 (기존)

# 테스트
app.bottlenote.alcohols/
├── integration/
│   └── PopularViewIntegrationTest.java      # 신규 - 통합 테스트
└── fixture/
    └── PopularsObjectFixture.java           # 필요시 수정 (기존)

app.docs.alcohols/
└── RestPopularViewControllerTest.java       # 신규 - REST Docs 테스트
```

## 구현 순서 (Bottom-Up)

> 컴파일 에러 없이 단계별로 진행. 각 Phase 완료 후 `./gradlew compileJava` 확인.

### Phase 1: QueryDSL Custom Repository 인터페이스 (신규)
**파일**: `bottlenote-mono/.../repository/CustomPopularQueryRepository.java`
```java
public interface CustomPopularQueryRepository {
  List<PopularItem> getPopularByViewsWeekly(Long userId, int limit);
  List<PopularItem> getPopularByViewsMonthly(Long userId, int limit);
}
```
- 컴파일 확인: `./gradlew :bottlenote-mono:compileJava`

### Phase 2: QueryDSL Custom Repository 구현체 (신규)
**파일**: `bottlenote-mono/.../repository/CustomPopularQueryRepositoryImpl.java`
- `JPAQueryFactory` 주입
- 주간/월간 조회수 집계 쿼리 구현
- 부족분 평점 기반 채우기 로직 구현
- 컴파일 확인: `./gradlew :bottlenote-mono:compileJava`

### Phase 3: JPA Repository 수정 (기존)
**파일**: `bottlenote-mono/.../repository/JpaPopularQueryRepository.java`
- `CustomPopularQueryRepository` 상속 추가
```java
public interface JpaPopularQueryRepository
    extends PopularQueryRepository,
            CustomPopularQueryRepository,  // 추가
            JpaRepository<Alcohol, Long> { ... }
```
- 컴파일 확인: `./gradlew :bottlenote-mono:compileJava`

### Phase 4: 도메인 Repository 수정 (기존)
**파일**: `bottlenote-mono/.../domain/PopularQueryRepository.java`
- 메서드 시그니처 추가
```java
List<PopularItem> getPopularByViewsWeekly(Long userId, int limit);
List<PopularItem> getPopularByViewsMonthly(Long userId, int limit);
```
- 컴파일 확인: `./gradlew :bottlenote-mono:compileJava`

### Phase 5: Service 계층 수정 (기존)
**파일**: `bottlenote-mono/.../service/AlcoholPopularService.java`
- 메서드 추가
```java
public List<PopularItem> getPopularByViewsWeekly(Integer top, Long userId) { ... }
public List<PopularItem> getPopularByViewsMonthly(Integer top, Long userId) { ... }
```
- 컴파일 확인: `./gradlew :bottlenote-mono:compileJava`

### Phase 6: Controller 계층 수정 (기존)
**파일**: `bottlenote-product-api/.../controller/AlcoholPopularQueryController.java`
- 엔드포인트 추가
```java
@GetMapping("/popular/view/week")
public ResponseEntity<?> getPopularViewWeek(@RequestParam(defaultValue = "20") Integer top) { ... }

@GetMapping("/popular/view/monthly")
public ResponseEntity<?> getPopularViewMonthly(@RequestParam(defaultValue = "20") Integer top) { ... }
```
- 컴파일 확인: `./gradlew :bottlenote-product-api:compileJava`

### Phase 7: 테스트 데이터 준비
**파일**: `bottlenote-product-api/src/test/resources/init-script/init-alcohols_view_history.sql`
- 테스트용 조회 기록 데이터 INSERT

### Phase 8: 통합 테스트 작성
**파일**: `bottlenote-product-api/.../integration/PopularViewIntegrationTest.java`
- `IntegrationTestSupport` 상속
- `@Tag("integration")`
- 테스트 실행: `./gradlew :bottlenote-product-api:integration_test --tests "PopularViewIntegrationTest"`

### Phase 9: REST Docs 테스트 작성
**파일**: `bottlenote-product-api/.../docs/alcohols/RestPopularViewControllerTest.java`
- `AbstractRestDocs` 상속
- API 문서 생성 확인: `./gradlew :bottlenote-product-api:asciidoctor`

## 테스트 계획

### 1. 통합 테스트 (`PopularViewIntegrationTest.java`)
- 위치: `bottlenote-product-api/src/test/java/app/bottlenote/alcohols/integration/`
- 베이스 클래스: `IntegrationTestSupport` 상속
- 태그: `@Tag("integration")`

**테스트 케이스:**
```java
@Tag("integration")
@DisplayName("[integration] [controller] Popular View")
class PopularViewIntegrationTest extends IntegrationTestSupport {

  @Test
  @DisplayName("주간 조회수 기반 인기 위스키를 조회할 수 있다")
  @Sql(scripts = {
    "/init-script/init-alcohol.sql",
    "/init-script/init-user.sql",
    "/init-script/init-alcohols_view_history.sql",  // 신규 필요
    "/init-script/init-rating.sql"
  })
  void test_getPopularViewWeekly() { ... }

  @Test
  @DisplayName("조회 기록이 부족하면 평점 높은 주류로 채워서 반환한다")
  void test_getPopularViewWeekly_fillWithRating() { ... }

  @Test
  @DisplayName("월간 조회수 기반 인기 위스키를 조회할 수 있다")
  void test_getPopularViewMonthly() { ... }
}
```

**필요한 테스트 데이터:**
- `init-alcohols_view_history.sql` - 조회 기록 테스트 데이터 (신규 작성)

### 2. REST Docs 테스트 (`RestPopularViewControllerTest.java`)
- 위치: `bottlenote-product-api/src/test/java/app/docs/alcohols/`
- 베이스 클래스: `AbstractRestDocs` 상속
- Mock 사용: `AlcoholPopularService` mock

**테스트 케이스:**
```java
@DisplayName("조회수 기반 인기 위스키 RestDocs 테스트")
class RestPopularViewControllerTest extends AbstractRestDocs {

  @Test
  @DisplayName("주간 조회수 기반 인기 위스키를 조회할 수 있다")
  void docs_getPopularViewWeekly() {
    // document: "alcohols/populars/view/week"
  }

  @Test
  @DisplayName("월간 조회수 기반 인기 위스키를 조회할 수 있다")
  void docs_getPopularViewMonthly() {
    // document: "alcohols/populars/view/monthly"
  }
}
```

### 3. 단위 테스트 (선택)
- `CustomPopularQueryRepositoryImpl` QueryDSL 로직 검증
- InMemory Repository 패턴 또는 `@DataJpaTest` 사용

## QueryDSL 쿼리 설계

### 주간 조회수 기반 인기 주류
```java
// 1. 이번 주 조회수 집계
QAlcoholsViewHistory h = QAlcoholsViewHistory.alcoholsViewHistory;
QAlcohol a = QAlcohol.alcohol;
QRating r = QRating.rating;
QPicks p = QPicks.picks;

LocalDateTime weekStart = LocalDate.now()
    .with(DayOfWeek.MONDAY)
    .atStartOfDay();

// 조회수 기반 결과
List<PopularItem> viewBasedResults = queryFactory
    .select(new QPopularItem(...))
    .from(h)
    .join(a).on(h.id.alcoholId.eq(a.id))
    .leftJoin(r).on(a.id.eq(r.id.alcoholId))
    .where(h.viewAt.goe(weekStart))
    .groupBy(h.id.alcoholId, a.korName, ...)
    .orderBy(h.id.alcoholId.count().desc())
    .limit(top)
    .fetch();

// 2. 부족분 평점 기반 채우기
if (viewBasedResults.size() < top) {
    List<Long> excludeIds = viewBasedResults.stream()
        .map(PopularItem::alcoholId)
        .toList();

    int remaining = top - viewBasedResults.size();

    List<PopularItem> ratingBasedResults = queryFactory
        .select(new QPopularItem(...))
        .from(a)
        .join(r).on(a.id.eq(r.id.alcoholId))
        .where(a.id.notIn(excludeIds))
        .groupBy(a.id, ...)
        .orderBy(r.ratingPoint.rating.avg().desc())
        .limit(remaining)
        .fetch();

    viewBasedResults.addAll(ratingBasedResults);
}
```

## 참고 테이블

### alcohols_view_histories
| 컬럼 | 타입 | 설명 |
|------|------|------|
| user_id | BIGINT | 사용자 ID (PK) |
| alcohol_id | BIGINT | 주류 ID (PK) |
| view_at | DATETIME | 조회 시점 |

### ratings
| 컬럼 | 타입 | 설명 |
|------|------|------|
| user_id | BIGINT | 사용자 ID (PK) |
| alcohol_id | BIGINT | 주류 ID (PK) |
| rating | DOUBLE | 평점 (0.0~5.0) |

---

## 구현 시 주의사항 (CLAUDE.md 기반)

### 아키텍처 패턴
- **계층 구조 준수**: Controller → Service → Repository → Domain
- **도메인 레포지토리**: `PopularQueryRepository`는 Spring/JPA에 의존하지 않는 순수 인터페이스
- **서비스 계층**: 도메인 레포지토리 인터페이스에만 의존

### 네이밍 컨벤션
- Custom 인터페이스: `Custom{도메인명}Repository` → `CustomPopularQueryRepository`
- 구현체: `Custom{도메인명}RepositoryImpl` → `CustomPopularQueryRepositoryImpl`
- 메서드: 조회는 `get/find/search` 사용

### QueryDSL 레포지토리 규칙
- **Custom 인터페이스**: 어노테이션 불필요 (순수 인터페이스)
- **구현체**: 어노테이션 불필요 (Spring Data JPA가 `Impl` 접미사로 자동 감지)
- **위치**: `app.bottlenote.alcohols.repository` 패키지

### 코드 스타일
- Lombok: `@Getter`, `@Builder`, `@RequiredArgsConstructor` 사용
- DTO: `record` 사용 (불변성)
- 주석: 한 줄로 간략하게만

### 테스트 규칙
- `@Tag("integration")`: 통합 테스트 태그 필수
- `@DisplayName`: 한글로 테스트 목적 명시
- 베이스 클래스: `IntegrationTestSupport` (통합), `AbstractRestDocs` (문서)
- 테스트 데이터: `src/test/resources/init-script/` 디렉토리

### 응답 형식
- `GlobalResponse` 사용하여 응답 통일
- 기존 `PopularsOfWeekResponse` 패턴 따르기
