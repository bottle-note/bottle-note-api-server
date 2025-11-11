# @Sql 어노테이션을 코드 베이스 방식으로 마이그레이션 계획

## 1. 현재 상황 분석

### 1.1 @Sql 사용 현황

**통계:**
- **총 12개 테스트 파일**에서 **34개 @Sql 어노테이션** 사용 중
- **9개의 SQL 스크립트 파일** 참조
- 대부분 **메서드 레벨**에서 사용 (약 32개)
- 일부 **클래스/Nested 레벨**에서 사용 (약 3개)

**사용 중인 SQL 스크립트 파일:**

| 파일명 | 데이터 개수 | 주요 내용 | 사용 빈도 |
|--------|------------|----------|----------|
| `init-user.sql` | 8명 | 테스트 사용자 데이터 | 매우 높음 |
| `init-alcohol.sql` | 227개 | 지역(27), 증류소(179), 주류(21) | 매우 높음 |
| `init-review.sql` | 8개 | 리뷰 데이터 | 높음 |
| `init-review-reply.sql` | 7개 | 리뷰 댓글/대댓글 | 중간 |
| `init-help.sql` | 1개 | 도움말/문의 | 중간 |
| `init-user-mypage-query.sql` | 복합 | 마이페이지용 전체 데이터 | 중간 |
| `init-user-mybottle-query.sql` | 복합 | 마이보틀용 전체 데이터 | 중간 |
| `init-user-history.sql` | 5개 | 사용자 히스토리 | 낮음 |
| `init-popular_alcohol.sql` | 26개 | 인기 주류 통계 | 매우 낮음 |

### 1.2 @Sql 사용 패턴

**패턴 1: 단일 SQL 파일 (간단한 테스트)**
```java
@Sql(scripts = {"/init-script/init-user.sql"})
@Test
void 회원탈퇴에_성공한다() { ... }
```

**패턴 2: 다중 SQL 파일 (복합 테스트)**
```java
@Sql(scripts = {
    "/init-script/init-user.sql",
    "/init-script/init-alcohol.sql",
    "/init-script/init-review.sql"
})
@Test
void 리뷰_목록을_조회할_수_있다() { ... }
```

**패턴 3: ExecutionPhase 명시**
```java
@Sql(
    scripts = {"/init-script/init-user.sql"},
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Test
void test_1() { ... }
```

**패턴 4: Nested 클래스 레벨 적용**
```java
@Sql(scripts = {"/init-script/init-user.sql", "/init-script/init-help.sql"})
@Nested
class HelpReadIntegrationTest { ... }
```

### 1.3 현재 프로젝트의 Fixture 인프라

**이미 구축된 인프라:**
- ✅ **Fixture 클래스**: `UserObjectFixture`, `ReviewObjectFixture`, `AlcoholQueryFixture` 등
- ✅ **TestFactory 클래스**: `UserTestFactory`, `AlcoholTestFactory`, `RatingTestFactory` 등
- ✅ **고급 기능**: 빌더 패턴, 누락 필드 자동 채우기, 연관 엔티티 자동 생성

**TestFactory 특징:**
```java
@Component
public class UserTestFactory {
    @Autowired private EntityManager em;

    // 1. 기본 생성
    @Transactional
    public User persistUser() { ... }

    // 2. 커스텀 생성
    @Transactional
    public User persistUser(String email, String nickname) { ... }

    // 3. 빌더 패턴 (누락 필드 자동 채우기)
    @Transactional
    public User persistUser(User.UserBuilder builder) {
        User.UserBuilder filledBuilder = fillMissingUserFields(builder);
        User user = filledBuilder.build();
        em.persist(user);
        return user;
    }
}
```

---

## 2. 문제점 및 개선 필요성

### 2.1 @Sql 방식의 문제점

#### 문제 1: 데이터 의존성 불명확
```java
@Sql(scripts = {"/init-script/init-user.sql", "/init-script/init-review.sql"})
@Test
void 리뷰_수정_테스트() {
    // SQL 파일을 열어보기 전까지 어떤 데이터가 있는지 알 수 없음
    // User ID가 1인지 2인지? Review ID는 무엇인지?
    mockMvc.perform(patch("/api/v1/reviews/1")  // ← 매직 넘버
        .contentType(MediaType.APPLICATION_JSON)
        .content(...))
}
```

**문제:**
- 테스트 코드만 보고 데이터 구조를 파악할 수 없음
- SQL 파일을 열어봐야 ID, 관계, 상태를 확인 가능
- 매직 넘버(1, 2, 3) 남발로 가독성 저하

#### 문제 2: 데이터 변경 영향 범위 파악 어려움
```sql
-- init-user.sql 수정
-- 기존: 8명 → 변경: 7명 (한 명 삭제)
```

**영향:**
- 어떤 테스트가 영향을 받는지 파악하기 어려움
- SQL 파일 하나 수정 시 여러 테스트가 동시에 깨질 수 있음
- 의존하는 모든 테스트를 일일이 찾아야 함

#### 문제 3: 테스트 격리 및 독립성 위반
```java
// TestA.java
@Sql(scripts = {"/init-script/init-user.sql"})
@Test
void testA() {
    // User ID 1번 사용
}

// TestB.java
@Sql(scripts = {"/init-script/init-user.sql"})
@Test
void testB() {
    // User ID 1번 사용 (같은 데이터)
}
```

**문제:**
- 여러 테스트가 동일한 SQL 스크립트에 의존
- SQL 스크립트 수정 시 모든 테스트에 영향
- 각 테스트가 필요한 데이터를 명시적으로 선언하지 않음

#### 문제 4: 복잡한 테스트 시나리오 표현 제한
```sql
-- init-user.sql
-- 단순 INSERT 문만 가능
INSERT INTO users (email, nick_name, ...) VALUES (...);
```

**한계:**
- 조건부 데이터 생성 불가능
- 동적 데이터 생성 불가능 (예: 현재 시간, 랜덤 값)
- 복잡한 관계 설정 어려움

#### 문제 5: 유지보수 비용 증가
```
bottlenote-product-api/src/test/resources/init-script/
├── init-user.sql                    # 41줄
├── init-alcohol.sql                 # 231줄  ← 매우 큼
├── init-review.sql                  # 41줄
├── init-review-reply.sql            # 별도 관리
├── init-help.sql                    # 별도 관리
├── init-user-mypage-query.sql       # 복합 데이터
├── init-user-mybottle-query.sql     # 복합 데이터
├── init-user-history.sql            # 별도 관리
└── init-popular_alcohol.sql         # 별도 관리
```

**문제:**
- SQL 파일과 테스트 코드가 물리적으로 분리
- SQL 스크립트 중복 관리 (유사한 데이터를 여러 파일에서 관리)
- 버전 관리 어려움 (엔티티 변경 시 SQL도 함께 수정 필요)

### 2.2 개선 필요성

**핵심 요구사항:**
1. ✅ **명확성**: 테스트 코드만 보고 어떤 데이터가 사용되는지 명확히 파악
2. ✅ **독립성**: 각 테스트가 필요한 데이터를 스스로 생성
3. ✅ **유지보수성**: 엔티티 변경 시 컴파일 에러로 즉시 감지
4. ✅ **재사용성**: 공통 데이터 생성 로직을 재사용
5. ✅ **표현력**: 복잡한 시나리오를 코드로 유연하게 표현

---

## 3. 목표 및 기대 효과

### 3.1 마이그레이션 목표

**주요 목표:**
- @Sql 어노테이션을 제거하고 TestFactory/Fixture 패턴으로 전환
- 테스트 데이터를 코드로 명시적으로 관리
- 기존 Fixture 인프라를 최대한 활용

**마이그레이션 후 모습:**
```java
// Before (AS-IS)
@Sql(scripts = {"/init-script/init-user.sql", "/init-script/init-review.sql"})
@Test
void 리뷰_수정_테스트() {
    // 데이터 불명확
    mockMvc.perform(patch("/api/v1/reviews/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(...))
}

// After (TO-BE)
@Test
void 리뷰_수정_테스트() {
    // Given: 명확한 데이터 준비
    User user = userTestFactory.persistUser("test@example.com", "테스터");
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    Review review = reviewTestFactory.persistReview(
        Review.builder()
            .user(user)
            .alcohol(alcohol)
            .content("테스트 리뷰 내용")
    );

    // When & Then
    mockMvc.perform(patch("/api/v1/reviews/" + review.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(...))
}
```

### 3.2 기대 효과

#### 효과 1: 가독성 및 명확성 향상
```java
// 테스트 코드만 보고 즉시 이해 가능
User reviewer = userTestFactory.persistUser("reviewer@test.com", "리뷰어");
Alcohol whisky = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY);
Review review = reviewTestFactory.persistReview(reviewer, whisky, "좋은 위스키!");
```

#### 효과 2: 테스트 독립성 보장
```java
// 각 테스트가 자신만의 데이터를 생성
@Test
void testA() {
    User userA = userTestFactory.persistUser(); // 독립적인 사용자
}

@Test
void testB() {
    User userB = userTestFactory.persistUser(); // 다른 사용자
}
```

#### 효과 3: 컴파일 타임 안정성
```java
// 엔티티 필드 변경 시 컴파일 에러로 즉시 감지
User user = userTestFactory.persistUser(
    User.builder()
        .email("test@test.com")
        .nickName("테스터")
        .newField("새로운 필드")  // ← 엔티티에 필드 추가 시 컴파일 에러
);
```

#### 효과 4: 유연한 시나리오 표현
```java
// 조건부, 동적 데이터 생성 가능
List<User> users = IntStream.range(0, 5)
    .mapToObj(i -> userTestFactory.persistUser())
    .toList();

User inactiveUser = userTestFactory.persistUser(
    User.builder()
        .status(UserStatus.INACTIVE)  // 특정 상태 설정
);
```

#### 효과 5: 유지보수 비용 절감
- SQL 파일 별도 관리 불필요
- 엔티티와 테스트 데이터가 같은 코드베이스에 존재
- IDE 리팩토링 도구 활용 가능 (Rename, Find Usages 등)

---

## 4. 마이그레이션 전략

### 4.1 단계별 전환 전략 (Big Picture)

```
Phase 0: 준비
  └─ TestDataLoader 설계 및 구현

Phase 1: 고빈도 파일 마이그레이션
  ├─ init-user.sql
  ├─ init-alcohol.sql (대용량)
  └─ init-review.sql

Phase 2: 중빈도 파일 마이그레이션
  ├─ init-review-reply.sql
  ├─ init-help.sql
  └─ init-user-history.sql

Phase 3: 복합 파일 마이그레이션
  ├─ init-user-mypage-query.sql
  └─ init-user-mybottle-query.sql

Phase 4: 저빈도 파일 마이그레이션
  └─ init-popular_alcohol.sql

Phase 5: 정리 및 검증
  ├─ SQL 파일 삭제
  ├─ 문서화
  └─ 최종 검증
```

### 4.2 점진적 마이그레이션 원칙

**1) 기존 테스트 깨지지 않기**
- 한 번에 하나의 테스트만 마이그레이션
- 각 테스트 마이그레이션 후 즉시 실행 및 검증
- 문제 발생 시 즉시 롤백 가능한 구조

**2) 하위 호환성 유지**
- SQL 파일은 마지막에 삭제
- 미처 마이그레이션하지 못한 테스트는 계속 @Sql 사용 가능
- 점진적 전환 지원

**3) 공통 패턴 우선 처리**
- 가장 많이 사용되는 SQL 파일부터 마이그레이션
- 재사용 가능한 Loader 패턴 먼저 구축
- 반복 작업 최소화

### 4.3 마이그레이션 우선순위

**우선순위 기준:**
1. **사용 빈도**: 가장 많이 참조되는 SQL 파일 우선
2. **복잡도**: 단순한 파일부터 시작
3. **영향 범위**: 영향받는 테스트 수 고려

**마이그레이션 순서:**
```
1순위: init-user.sql              (빈도: 매우 높음, 복잡도: 낮음)
2순위: init-review.sql            (빈도: 높음, 복잡도: 낮음)
3순위: init-alcohol.sql           (빈도: 매우 높음, 복잡도: 높음) ⚠️
4순위: init-review-reply.sql      (빈도: 중간, 복잡도: 낮음)
5순위: init-help.sql              (빈도: 중간, 복잡도: 낮음)
6순위: init-user-history.sql      (빈도: 낮음, 복잡도: 낮음)
7순위: init-user-mypage-query.sql (빈도: 중간, 복잡도: 높음)
8순위: init-user-mybottle-query.sql (빈도: 중간, 복잡도: 높음)
9순위: init-popular_alcohol.sql   (빈도: 매우 낮음, 복잡도: 중간)
```

---

## 5. 신규 컴포넌트 설계: TestDataLoader

### 5.1 TestDataLoader 개념

**목적:**
- SQL 스크립트의 "사전 정의된 데이터 세트" 개념을 코드로 전환
- 복잡한 데이터 조합을 재사용 가능한 형태로 제공

**위치:**
```
bottlenote-product-api/src/test/java/app/bottlenote/
└── operation/
    └── loader/
        ├── UserDataLoader.java
        ├── AlcoholDataLoader.java
        ├── ReviewDataLoader.java
        └── CompositeDataLoader.java
```

### 5.2 TestDataLoader 설계

#### 기본 구조
```java
@Component
@RequiredArgsConstructor
public class UserDataLoader {

    private final UserTestFactory userTestFactory;

    /**
     * init-user.sql을 대체하는 표준 사용자 8명 생성
     * 기존 SQL과 동일한 데이터 구조 제공
     */
    @Transactional
    public StandardUsers loadStandardUsers() {
        User user1 = userTestFactory.persistUser(
            User.builder()
                .email("hyejj19@naver.com")
                .nickName("WOzU6J8541")
                .gender(GenderType.FEMALE)
                .socialType(List.of(SocialType.KAKAO))
        );

        User user2 = userTestFactory.persistUser(
            User.builder()
                .email("chadongmin@naver.com")
                .nickName("xIFo6J8726")
                .gender(GenderType.MALE)
                .socialType(List.of(SocialType.KAKAO))
        );

        // ... 나머지 6명

        return new StandardUsers(user1, user2, user3, user4, user5, user6, user7, user8);
    }

    /**
     * 단순 사용자 1명만 필요한 경우
     */
    @Transactional
    public User loadSingleUser() {
        return userTestFactory.persistUser(
            User.builder()
                .email("test@example.com")
                .nickName("테스터")
                .gender(GenderType.MALE)
                .socialType(List.of(SocialType.KAKAO))
        );
    }

    /**
     * 커스텀 사용자 N명 생성
     */
    @Transactional
    public List<User> loadUsers(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> userTestFactory.persistUser())
            .toList();
    }
}
```

#### StandardUsers DTO
```java
/**
 * init-user.sql의 8명 사용자를 표현하는 DTO
 * 기존 SQL 의존 코드를 쉽게 마이그레이션하기 위한 구조
 */
public record StandardUsers(
    User user1,  // hyejj19@naver.com
    User user2,  // chadongmin@naver.com
    User user3,  // dev.bottle-note@gmail.com
    User user4,  // eva.park@oysterable.com
    User user5,  // rlagusrl928@gmail.com
    User user6,  // ytest@gmail.com
    User user7,  // juye@gmail.com
    User user8   // rkdtkfma@naver.com
) {
    public User getFirst() { return user1; }
    public User getById(int index) {
        return switch(index) {
            case 1 -> user1;
            case 2 -> user2;
            case 3 -> user3;
            case 4 -> user4;
            case 5 -> user5;
            case 6 -> user6;
            case 7 -> user7;
            case 8 -> user8;
            default -> throw new IllegalArgumentException("Invalid index: " + index);
        };
    }
    public List<User> toList() {
        return List.of(user1, user2, user3, user4, user5, user6, user7, user8);
    }
}
```

### 5.3 AlcoholDataLoader 설계 (대용량 데이터 처리)

**특수성:**
- init-alcohol.sql은 231줄, 227개 데이터 (지역 27 + 증류소 179 + 주류 21)
- 대용량이므로 **배치 처리** 및 **선택적 로딩** 필요

```java
@Component
@RequiredArgsConstructor
public class AlcoholDataLoader {

    private final AlcoholTestFactory alcoholTestFactory;

    /**
     * 전체 표준 데이터 로드 (init-alcohol.sql 전체)
     * ⚠️ 성능 고려: 필요한 경우에만 사용
     */
    @Transactional
    public StandardAlcohols loadFullStandardData() {
        // 1. 지역 27개 생성
        List<Region> regions = loadStandardRegions();

        // 2. 증류소 179개 생성 (배치 처리)
        List<Distillery> distilleries = loadStandardDistilleries();

        // 3. 주류 21개 생성
        List<Alcohol> alcohols = loadStandardAlcohols(regions, distilleries);

        return new StandardAlcohols(regions, distilleries, alcohols);
    }

    /**
     * 경량 데이터: 주요 지역 + 증류소 + 주류 각 3개씩만 생성
     * 대부분의 테스트에서 사용 권장
     */
    @Transactional
    public LightweightAlcohols loadLightweightData() {
        Region region1 = alcoholTestFactory.persistRegion("스코틀랜드", "Scotland");
        Region region2 = alcoholTestFactory.persistRegion("일본", "Japan");
        Region region3 = alcoholTestFactory.persistRegion("미국", "United States");

        Distillery distillery1 = alcoholTestFactory.persistDistillery("맥캘란", "Macallan");
        Distillery distillery2 = alcoholTestFactory.persistDistillery("글렌피딕", "Glenfiddich");
        Distillery distillery3 = alcoholTestFactory.persistDistillery("야마자키", "Yamazaki");

        Alcohol alcohol1 = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, region1, distillery1);
        Alcohol alcohol2 = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, region2, distillery2);
        Alcohol alcohol3 = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY, region3, distillery3);

        return new LightweightAlcohols(
            List.of(region1, region2, region3),
            List.of(distillery1, distillery2, distillery3),
            List.of(alcohol1, alcohol2, alcohol3)
        );
    }

    /**
     * 주류 데이터만 필요한 경우 (최소 데이터)
     */
    @Transactional
    public Alcohol loadSingleAlcohol() {
        return alcoholTestFactory.persistAlcohol();  // 연관 엔티티 자동 생성
    }

    /**
     * 표준 지역 27개 생성 (init-alcohol.sql 기준)
     */
    private List<Region> loadStandardRegions() {
        return List.of(
            alcoholTestFactory.persistRegion("호주", "Australia"),
            alcoholTestFactory.persistRegion("핀란드", "Finland"),
            alcoholTestFactory.persistRegion("프랑스", "France"),
            // ... 나머지 24개
        );
    }

    /**
     * 표준 증류소 179개 생성 (배치 처리)
     * ⚠️ 성능 최적화: JDBC Batch Insert 고려
     */
    private List<Distillery> loadStandardDistilleries() {
        // TODO: JDBC Batch Insert로 성능 최적화 가능
        return List.of(
            alcoholTestFactory.persistDistillery("글래스고", "The Glasgow Distillery Co."),
            alcoholTestFactory.persistDistillery("글렌 그란트", "Glen Grant"),
            // ... 나머지 177개
        );
    }
}
```

#### StandardAlcohols / LightweightAlcohols DTO
```java
public record StandardAlcohols(
    List<Region> regions,        // 27개
    List<Distillery> distilleries, // 179개
    List<Alcohol> alcohols       // 21개
) {
    public Region getRegionByName(String korName) {
        return regions.stream()
            .filter(r -> r.getKorName().contains(korName))
            .findFirst()
            .orElseThrow();
    }
}

public record LightweightAlcohols(
    List<Region> regions,        // 3개
    List<Distillery> distilleries, // 3개
    List<Alcohol> alcohols       // 3개
) {
    public Region getFirstRegion() { return regions.get(0); }
    public Distillery getFirstDistillery() { return distilleries.get(0); }
    public Alcohol getFirstAlcohol() { return alcohols.get(0); }
}
```

### 5.4 CompositeDataLoader 설계

**목적:**
- 여러 도메인의 데이터를 조합해서 한 번에 로드
- init-user-mypage-query.sql, init-user-mybottle-query.sql 등 복합 SQL 대체

```java
@Component
@RequiredArgsConstructor
public class CompositeDataLoader {

    private final UserDataLoader userDataLoader;
    private final AlcoholDataLoader alcoholDataLoader;
    private final ReviewDataLoader reviewDataLoader;

    /**
     * 마이페이지 테스트용 전체 데이터 로드
     * (init-user-mypage-query.sql 대체)
     */
    @Transactional
    public MyPageTestData loadMyPageTestData() {
        // 1. 사용자 로드
        StandardUsers users = userDataLoader.loadStandardUsers();

        // 2. 경량 주류 데이터 로드
        LightweightAlcohols alcohols = alcoholDataLoader.loadLightweightData();

        // 3. 리뷰 데이터 로드
        List<Review> reviews = reviewDataLoader.loadReviews(users.user1(), alcohols.getFirstAlcohol(), 5);

        // 4. 팔로우 관계 설정
        // ...

        return new MyPageTestData(users, alcohols, reviews);
    }

    /**
     * 리뷰 전체 기능 테스트용 데이터
     * (init-user.sql + init-alcohol.sql + init-review.sql 조합 대체)
     */
    @Transactional
    public ReviewFeatureTestData loadReviewFeatureTestData() {
        User reviewer = userDataLoader.loadSingleUser();
        Alcohol alcohol = alcoholDataLoader.loadSingleAlcohol();
        Review review = reviewDataLoader.loadSingleReview(reviewer, alcohol);

        return new ReviewFeatureTestData(reviewer, alcohol, review);
    }
}
```

### 5.5 TestDataLoader 사용 예시

#### Before (AS-IS)
```java
@Sql(scripts = {"/init-script/init-user.sql", "/init-script/init-alcohol.sql", "/init-script/init-review.sql"})
@Test
void 리뷰_목록을_조회할_수_있다() throws Exception {
    // 데이터 불명확, SQL 파일 열어봐야 함
    mockMvc.perform(get("/api/v1/reviews")
        .param("alcoholId", "1"))  // 매직 넘버
        .andExpect(status().isOk());
}
```

#### After (TO-BE) - 옵션 1: 경량 데이터
```java
@Autowired private CompositeDataLoader compositeDataLoader;

@Test
void 리뷰_목록을_조회할_수_있다() throws Exception {
    // Given: 명확한 데이터 준비 (경량)
    ReviewFeatureTestData testData = compositeDataLoader.loadReviewFeatureTestData();

    // When & Then
    mockMvc.perform(get("/api/v1/reviews")
        .param("alcoholId", String.valueOf(testData.alcohol().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].userId").value(testData.reviewer().getId()));
}
```

#### After (TO-BE) - 옵션 2: 완전 커스텀
```java
@Autowired private UserTestFactory userTestFactory;
@Autowired private AlcoholTestFactory alcoholTestFactory;
@Autowired private ReviewTestFactory reviewTestFactory;

@Test
void 특정_상태의_리뷰만_조회된다() throws Exception {
    // Given: 필요한 데이터만 정확히 생성
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();

    Review publicReview = reviewTestFactory.persistReview(
        Review.builder()
            .user(user)
            .alcohol(alcohol)
            .status(ReviewStatus.PUBLIC)  // 명시적 상태 설정
            .content("공개 리뷰")
    );

    Review privateReview = reviewTestFactory.persistReview(
        Review.builder()
            .user(user)
            .alcohol(alcohol)
            .status(ReviewStatus.PRIVATE)  // 명시적 상태 설정
            .content("비공개 리뷰")
    );

    // When & Then: PUBLIC 리뷰만 조회되어야 함
    mockMvc.perform(get("/api/v1/reviews")
        .param("alcoholId", String.valueOf(alcohol.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].reviewId").value(publicReview.getId()));
}
```

---

## 6. 구현 계획 (단계별)

### Phase 0: 준비 단계

#### 목표
- TestDataLoader 인프라 구축
- 기존 TestFactory 검증 및 보완

#### 작업 내용

**1) TestDataLoader 디렉토리 구조 생성**
```bash
mkdir -p bottlenote-product-api/src/test/java/app/bottlenote/operation/loader
```

**2) UserDataLoader 구현**
- `loadStandardUsers()`: 표준 8명 사용자 생성
- `loadSingleUser()`: 단일 사용자 생성
- `StandardUsers` DTO 정의

**3) 기존 TestFactory 검증**
- UserTestFactory의 메서드 검토
- 누락된 기능 추가 (예: 특정 상태 사용자 생성)
- 문서화 주석 추가

**4) 파일럿 테스트**
- UserCommandIntegrationTest 중 1개 메서드만 마이그레이션
- 기존 @Sql 방식과 새 방식 비교
- 성능 측정 (데이터 로딩 시간)

**검증 기준:**
- ✅ UserDataLoader로 생성한 데이터가 init-user.sql과 동일한 구조
- ✅ 파일럿 테스트가 성공적으로 통과
- ✅ 성능 차이 10% 이내

---

### Phase 1: 고빈도 파일 마이그레이션

#### Phase 1-1: init-user.sql 마이그레이션

**영향받는 테스트 파일 (7개):**
1. `UserCommandIntegrationTest` (7개 메서드)
2. `UserQueryIntegrationTest` (5개 메서드)
3. `ReviewIntegrationTest` (일부)
4. `RatingIntegrationTest` (일부)
5. `PicksIntegrationTest` (일부)
6. `LikesIntegrationTest` (일부)
7. `UserHistoryIntegrationTest` (일부)

**마이그레이션 순서:**
```
1. UserCommandIntegrationTest (7개 메서드)
  ├─ test_1: 회원탈퇴
  ├─ test_2: 탈퇴한 회원 재탈퇴
  ├─ test_3: 닉네임 변경
  ├─ test_4: 중복 닉네임
  ├─ test_5: 유효하지 않은 닉네임
  ├─ test_6: 프로필 이미지 변경
  └─ test_7: 로그인 테스트

2. UserQueryIntegrationTest (5개 메서드)
  ├─ test_1: 사용자 정보 조회
  ├─ test_2: 마이페이지 조회
  ├─ test_3: 사용자 검색
  ├─ test_4: 팔로워 목록 조회
  └─ test_5: 팔로잉 목록 조회

3. 나머지 테스트 파일 (5개) + 검증
```

**작업 방법:**
```java
// 1단계: @Sql 제거
- @Sql(scripts = {"/init-script/init-user.sql"})
+ // @Sql(scripts = {"/init-script/init-user.sql"})  // 주석 처리 (롤백 대비)

// 2단계: TestDataLoader 주입
@Autowired private UserDataLoader userDataLoader;

// 3단계: Given 절에 데이터 로딩 추가
@Test
void 회원탈퇴에_성공한다() throws Exception {
    // Given
+   User user = userDataLoader.loadSingleUser();

    // When & Then
    mockMvc.perform(delete("/api/v1/users")
        .contentType(MediaType.APPLICATION_JSON)
-       .header("Authorization", "Bearer " + getToken())
+       .header("Authorization", "Bearer " + getToken(user))  // 명시적 사용자 지정
        .with(csrf()))
        .andExpect(status().isOk());
}
```

**검증:**
- 각 메서드 변경 후 즉시 테스트 실행
- 실패 시 즉시 롤백 (주석 해제)

---

#### Phase 1-2: init-review.sql 마이그레이션

**영향받는 테스트 파일:**
- `ReviewIntegrationTest` (2개 메서드)
- `ReviewReplyIntegrationTest` (2개 메서드)

**작업 내용:**
1. `ReviewDataLoader` 구현
2. `loadStandardReviews()` 메서드 (8개 리뷰)
3. `loadSingleReview()` 메서드

**예시:**
```java
@Component
@RequiredArgsConstructor
public class ReviewDataLoader {

    private final ReviewTestFactory reviewTestFactory;

    @Transactional
    public Review loadSingleReview(User user, Alcohol alcohol) {
        return reviewTestFactory.persistReview(
            Review.builder()
                .user(user)
                .alcohol(alcohol)
                .content("테스트 리뷰 내용")
                .status(ReviewStatus.PUBLIC)
                .sizeType(SizeType.BOTTLE)
                .price(65000L)
        );
    }
}
```

---

#### Phase 1-3: init-alcohol.sql 마이그레이션 ⚠️

**난이도: 높음**
- 227개 데이터 (지역 27 + 증류소 179 + 주류 21)
- 대용량 데이터 처리 전략 필요

**작업 내용:**
1. `AlcoholDataLoader` 구현
2. 경량 데이터 제공: `loadLightweightData()` (각 3개씩)
3. 전체 데이터 제공: `loadFullStandardData()` (227개 전체)
4. 성능 최적화: JDBC Batch Insert 적용 고려

**마이그레이션 전략:**
```java
// 대부분의 테스트: 경량 데이터 사용
@Test
void 주류_목록_조회() {
    LightweightAlcohols alcohols = alcoholDataLoader.loadLightweightData();
    // 3개 주류만 사용
}

// 대용량 데이터가 필요한 테스트만: 전체 데이터
@Test
void 전체_주류_페이징_조회() {
    StandardAlcohols alcohols = alcoholDataLoader.loadFullStandardData();
    // 227개 전체 사용
}
```

**성능 측정:**
- 경량 데이터 로딩 시간: 목표 < 100ms
- 전체 데이터 로딩 시간: 목표 < 1000ms
- 필요시 캐싱 전략 도입

---

### Phase 2: 중빈도 파일 마이그레이션

#### Phase 2-1: init-review-reply.sql
- `ReviewReplyDataLoader` 구현
- 댓글/대댓글 계층 구조 지원

#### Phase 2-2: init-help.sql
- `HelpDataLoader` 구현
- 단순 구조이므로 빠른 마이그레이션 가능

#### Phase 2-3: init-user-history.sql
- `UserHistoryDataLoader` 구현
- 5개 히스토리 레코드 생성

---

### Phase 3: 복합 파일 마이그레이션

#### Phase 3-1: init-user-mypage-query.sql
- `CompositeDataLoader.loadMyPageTestData()` 구현
- User + Alcohol + Review + Follow + Rating 조합

#### Phase 3-2: init-user-mybottle-query.sql
- `CompositeDataLoader.loadMyBottleTestData()` 구현
- User + Alcohol + Review + Pick 조합

---

### Phase 4: 저빈도 파일 마이그레이션

#### init-popular_alcohol.sql
- `PopularAlcoholDataLoader` 구현
- 26개 인기 주류 통계 데이터

---

### Phase 5: 정리 및 검증

#### 작업 내용:
1. **SQL 파일 삭제**
   - `/init-script/` 디렉토리 전체 삭제
   - 사용하지 않는 @Sql import 제거

2. **문서화**
   - TestDataLoader 사용 가이드 작성
   - 마이그레이션 전후 비교 문서
   - Best Practices 문서

3. **최종 검증**
   - 전체 통합 테스트 실행
   - 성능 측정 및 비교
   - 커버리지 확인

4. **팀 공유**
   - 마이그레이션 결과 발표
   - Q&A 세션

---

## 7. 마이그레이션 예시 (Before & After)

### 예시 1: 단순 사용자 테스트

#### Before
```java
@Sql(scripts = {"/init-script/init-user.sql"})
@Test
void 회원탈퇴에_성공한다() throws Exception {
    mockMvc.perform(delete("/api/v1/users")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + getToken())  // 어떤 사용자인지 불명확
        .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}
```

#### After
```java
@Autowired private UserDataLoader userDataLoader;

@Test
void 회원탈퇴에_성공한다() throws Exception {
    // Given: 명확한 사용자 생성
    User user = userDataLoader.loadSingleUser();
    String token = authSupport.getToken(user);

    // When & Then
    mockMvc.perform(delete("/api/v1/users")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value(user.getId()));
}
```

**개선점:**
- ✅ 어떤 사용자가 탈퇴하는지 명확
- ✅ 사용자 ID를 검증에 활용 가능
- ✅ 필요한 데이터만 생성 (8명 → 1명)

---

### 예시 2: 복합 데이터 테스트

#### Before
```java
@Sql(scripts = {
    "/init-script/init-user.sql",
    "/init-script/init-alcohol.sql",
    "/init-script/init-review.sql"
})
@Test
void 리뷰_수정에_성공한다() throws Exception {
    // 어떤 리뷰를 수정하는지 불명확
    mockMvc.perform(patch("/api/v1/reviews/1")  // 매직 넘버
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"content\":\"수정된 내용\"}"))
        .andExpect(status().isOk());
}
```

#### After (옵션 1: CompositeDataLoader 사용)
```java
@Autowired private CompositeDataLoader compositeDataLoader;

@Test
void 리뷰_수정에_성공한다() throws Exception {
    // Given: 필요한 데이터 조합 로드
    ReviewFeatureTestData testData = compositeDataLoader.loadReviewFeatureTestData();
    String token = authSupport.getToken(testData.reviewer());

    // When & Then
    mockMvc.perform(patch("/api/v1/reviews/" + testData.review().getId())
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .content("{\"content\":\"수정된 내용\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviewId").value(testData.review().getId()));
}
```

#### After (옵션 2: 완전 커스텀)
```java
@Autowired private UserTestFactory userTestFactory;
@Autowired private AlcoholTestFactory alcoholTestFactory;
@Autowired private ReviewTestFactory reviewTestFactory;

@Test
void 리뷰_수정에_성공한다() throws Exception {
    // Given: 정확히 필요한 데이터만 생성
    User reviewer = userTestFactory.persistUser("reviewer@test.com", "리뷰어");
    Alcohol whisky = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY);
    Review review = reviewTestFactory.persistReview(
        Review.builder()
            .user(reviewer)
            .alcohol(whisky)
            .content("원본 리뷰 내용")
            .status(ReviewStatus.PUBLIC)
    );
    String token = authSupport.getToken(reviewer);

    // When
    String updatedContent = "수정된 내용";
    mockMvc.perform(patch("/api/v1/reviews/" + review.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .content("{\"content\":\"" + updatedContent + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").value(updatedContent));

    // Then: DB에서 확인
    Review updated = reviewRepository.findById(review.getId()).orElseThrow();
    assertEquals(updatedContent, updated.getContent());
}
```

**개선점:**
- ✅ 테스트 의도가 명확 (어떤 리뷰를 수정하는지)
- ✅ 필요한 데이터만 생성 (8+227+8 → 1+1+1)
- ✅ 매직 넘버 제거
- ✅ 검증 강화 (DB 조회까지 확인)

---

### 예시 3: 특정 상태 테스트

#### Before (불가능)
```java
@Sql(scripts = {"/init-script/init-user.sql"})
@Test
void 비활성_사용자는_로그인_불가() throws Exception {
    // ❌ init-user.sql에는 모두 ACTIVE 사용자만 존재
    // ❌ 비활성 사용자를 테스트할 수 없음
}
```

#### After (가능)
```java
@Autowired private UserTestFactory userTestFactory;

@Test
void 비활성_사용자는_로그인_불가() throws Exception {
    // Given: 명시적으로 비활성 사용자 생성
    User inactiveUser = userTestFactory.persistUser(
        User.builder()
            .email("inactive@test.com")
            .nickName("비활성유저")
            .status(UserStatus.INACTIVE)  // ✅ 명시적 상태 설정
    );

    // When & Then
    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + inactiveUser.getEmail() + "\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("비활성 사용자입니다"));
}
```

**개선점:**
- ✅ SQL로 불가능했던 시나리오를 코드로 구현 가능
- ✅ 다양한 상태 조합 테스트 가능
- ✅ 경계값 테스트 용이

---

### 예시 4: 대용량 데이터 테스트

#### Before
```java
@Sql(scripts = {"/init-script/init-alcohol.sql"})  // 227개 전체 로드
@Test
void 주류_3개만_조회_테스트() throws Exception {
    mockMvc.perform(get("/api/v1/alcohols")
        .param("size", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(3));
}
```

#### After
```java
@Autowired private AlcoholDataLoader alcoholDataLoader;

@Test
void 주류_3개만_조회_테스트() throws Exception {
    // Given: 필요한 만큼만 생성 (227개 → 3개)
    LightweightAlcohols alcohols = alcoholDataLoader.loadLightweightData();

    // When & Then
    mockMvc.perform(get("/api/v1/alcohols")
        .param("size", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(3))
        .andExpect(jsonPath("$.data.content[0].alcoholId").value(alcohols.getFirstAlcohol().getId()));
}
```

**개선점:**
- ✅ 불필요한 224개 데이터 로딩 제거
- ✅ 테스트 실행 속도 향상
- ✅ 메모리 사용량 감소

---

## 8. 주의사항 및 체크리스트

### 8.1 마이그레이션 시 주의사항

#### 주의 1: 트랜잭션 경계
```java
// ❌ 잘못된 예: 트랜잭션 외부에서 Lazy Loading
@Test
void test() {
    User user = userTestFactory.persistUser();  // @Transactional

    // 트랜잭션 종료됨

    List<Review> reviews = user.getReviews();  // ❌ LazyInitializationException
}

// ✅ 올바른 예: 필요한 데이터는 즉시 로딩
@Test
void test() {
    User user = userTestFactory.persistUser();
    List<Review> reviews = reviewTestFactory.loadReviews(user, 5);  // ✅ 명시적 로딩
}
```

#### 주의 2: ID 의존성
```java
// ❌ 잘못된 예: 하드코딩된 ID
@Test
void test() {
    userDataLoader.loadStandardUsers();

    mockMvc.perform(get("/api/v1/users/1"))  // ❌ ID=1이라는 보장 없음
}

// ✅ 올바른 예: 생성된 객체의 ID 사용
@Test
void test() {
    User user = userDataLoader.loadSingleUser();

    mockMvc.perform(get("/api/v1/users/" + user.getId()))  // ✅ 실제 ID 사용
}
```

#### 주의 3: 데이터 정리
```java
// DataInitializer.deleteAll()은 여전히 @AfterEach에서 실행
// 코드로 생성한 데이터도 자동으로 정리됨
@AfterEach
void cleanUpAfterEach() {
    dataCleaner.cleanAll();  // 모든 데이터 삭제
}
```

#### 주의 4: 성능 고려
```java
// ❌ 불필요한 대용량 데이터 생성
@Test
void 단순_조회_테스트() {
    StandardAlcohols alcohols = alcoholDataLoader.loadFullStandardData();  // 227개
    // 실제로는 1개만 필요
}

// ✅ 필요한 만큼만 생성
@Test
void 단순_조회_테스트() {
    Alcohol alcohol = alcoholDataLoader.loadSingleAlcohol();  // 1개
}
```

### 8.2 마이그레이션 체크리스트

**테스트 파일별 체크리스트:**
- [ ] @Sql 어노테이션 제거됨
- [ ] TestDataLoader 또는 TestFactory 주입됨
- [ ] Given 절에 데이터 생성 코드 추가됨
- [ ] 매직 넘버가 객체 참조로 교체됨
- [ ] 테스트가 성공적으로 통과함
- [ ] 테스트 실행 시간이 기존 대비 10% 이내 차이
- [ ] 코드 리뷰 완료

**Phase별 체크리스트:**
- [ ] 모든 테스트 파일 마이그레이션 완료
- [ ] SQL 파일 삭제 완료
- [ ] 문서화 완료
- [ ] 전체 테스트 실행 성공
- [ ] 성능 측정 및 비교 완료
- [ ] 팀 공유 완료

---

## 9. 예상 리스크 및 대응 방안

### 리스크 1: 성능 저하

**원인:**
- 코드로 데이터 생성 시 SQL INSERT보다 느릴 수 있음
- JPA 영속성 컨텍스트 오버헤드

**대응:**
- 경량 데이터 제공 (필요한 만큼만 생성)
- JDBC Batch Insert 적용 (대용량 데이터)
- 성능 측정 후 최적화

**기준:**
- 개별 테스트 실행 시간 10% 이내 증가 허용
- 전체 테스트 Suite 실행 시간 5% 이내 증가 허용

### 리스크 2: 테스트 깨짐

**원인:**
- 데이터 생성 순서 변경
- ID 의존성 문제

**대응:**
- 한 번에 하나씩 마이그레이션
- 각 단계마다 즉시 검증
- 롤백 가능한 구조 유지 (주석 처리)

### 리스크 3: 팀원 혼란

**원인:**
- 새로운 패턴에 익숙하지 않음
- 문서 부족

**대응:**
- 충분한 문서화
- 예제 코드 제공
- Q&A 세션 진행

---

## 10. 성공 지표

### 정량적 지표
1. **마이그레이션 완료율**: 100% (34개 @Sql → 0개)
2. **테스트 통과율**: 100% 유지
3. **성능**: 개별 테스트 +10% 이내, 전체 Suite +5% 이내
4. **코드 라인**: SQL 라인 수 → 0줄

### 정성적 지표
1. **가독성**: 테스트 코드만 보고 데이터 구조 파악 가능
2. **유지보수성**: 엔티티 변경 시 컴파일 에러로 즉시 감지
3. **확장성**: 새로운 테스트 시나리오 추가 용이
4. **팀 만족도**: 팀원 피드백 긍정적

---

## 11. 다음 단계

### 즉시 실행
1. **Phase 0 시작**
   - TestDataLoader 인프라 구축
   - UserDataLoader 구현
   - 파일럿 테스트 (1개 메서드)

### 후속 작업
1. Phase 1-5 순차적 실행
2. 진행 상황 리뷰
3. 이슈 발생 시 즉시 대응

### 장기 계획
1. 다른 모듈(batch 등)에도 적용
2. TestDataLoader 패턴을 팀 표준으로 확립
3. 신규 개발자 온보딩 자료에 포함

---

## 12. 결론

이 마이그레이션 계획은 **@Sql 방식의 한계**를 극복하고 **코드 베이스 방식의 장점**을 최대한 활용하는 것을 목표로 합니다.

**핵심 개선사항:**
- ✅ 테스트 데이터를 명시적이고 명확하게 관리
- ✅ SQL 파일 의존성 제거로 유지보수성 향상
- ✅ 컴파일 타임 안정성 확보
- ✅ 유연한 시나리오 표현 가능
- ✅ 기존 TestFactory 인프라 최대한 활용

**기대 효과:**
- 테스트 코드 가독성 대폭 향상
- 유지보수 비용 절감
- 테스트 독립성 보장
- 팀 생산성 향상

점진적이고 체계적인 접근으로 안전하게 마이그레이션을 진행하여, 더 나은 테스트 환경을 구축할 수 있습니다.
