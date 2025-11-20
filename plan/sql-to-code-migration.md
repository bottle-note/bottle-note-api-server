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
  └─ TestFactory 설계 및 구현

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

## 5. 개선된 설계: TestFactory 확장 (기존 위치 유지)

### 5.1 설계 철학

**핵심 원칙:**
- ✅ **기존 TestFactory는 이동하지 않음** (bottlenote-product-api에 유지)
- ✅ **기존 테스트를 깨뜨리지 않음** (import 경로 유지)
- ✅ **점진적 마이그레이션** (한 번에 하나씩)

**채택하지 않은 방안과 이유:**

**❌ 방안 1: bottlenote-mono/testFixtures에 TestFactory 배치**
- `java-test-fixtures`는 **순수 Java 라이브러리**
- `@Component`, `@Autowired`, `@Transactional` **사용 불가**
- EntityManager 주입 불가 → 실현 불가능

**❌ 방안 2: bottlenote-test-support 모듈 생성**
- 새로운 Spring Boot 모듈 추가
- 구조 복잡도 증가
- 유지보수 비용 증가

**✅ 채택한 방안: 기존 TestFactory 확장 (bottlenote-product-api 유지)**
- 기존 위치 그대로 유지
- SQL 대체 메서드만 추가
- 기존 테스트 호환성 100% 유지

### 5.2 디렉토리 구조 (변경 없음)

**기존 구조 유지:**
```
bottlenote-product-api/
└── src/test/java/
    └── app/bottlenote/
        ├── IntegrationTestSupport.java
        └── {domain}/fixture/                 # TestFactory 위치 (유지)
            ├── UserTestFactory.java         ✅ 기존 위치 유지
            ├── AlcoholTestFactory.java      ✅ 기존 위치 유지
            ├── ReviewTestFactory.java       ✅ 기존 위치 유지
            └── ...
```

**변경사항: 없음**
- TestFactory를 이동하지 않음
- import 경로 변경 없음
- 모든 기존 테스트가 그대로 작동

**선택적 개선: bottlenote-mono/testFixtures (나중에 고려)**

현재는 구현하지 않지만, 향후 필요 시 다음과 같이 구성 가능:

```
bottlenote-mono/
└── src/testFixtures/java/              # 선택적
    └── app/bottlenote/fixture/
        ├── UserFixture.java            # 순수 빌더만 (Spring 없음)
        ├── AlcoholFixture.java         # 순수 빌더만
        └── support/
            ├── StandardUsers.java      # DTO
            └── StandardAlcohols.java   # DTO
```

**⚠️ 주의: testFixtures는 Spring Bean 사용 불가**
- `@Component`, `@Autowired`, `@Transactional` 사용 불가
- EntityManager 주입 불가
- **순수 빌더 패턴만 가능**

**현재 계획: testFixtures 사용 안 함**
- Phase 0~5에서 testFixtures 구성 작업 제외
- 기존 TestFactory만 확장

### 5.3 UserTestFactory 확장 (SQL 대체 메서드 추가)

**위치:** `bottlenote-product-api/src/test/java/app/bottlenote/user/fixture/UserTestFactory.java`

**기존 파일 수정 (이동 없음)**

```java
@Component
@RequiredArgsConstructor
public class UserTestFactory {

    private final Random random = new SecureRandom();
    @Autowired private EntityManager em;

    // ========== 기존 메서드들 (유지) ==========

    /** 기본 User 생성 */
    @Transactional
    public User persistUser() { /* 기존 코드 */ }

    /** 이메일과 닉네임으로 User 생성 */
    @Transactional
    public User persistUser(String email, String nickName) { /* 기존 코드 */ }

    /** 빌더를 통한 User 생성 */
    @Transactional
    public User persistUser(User.UserBuilder builder) { /* 기존 코드 */ }

    // ========== 새로 추가: SQL 대체 메서드들 ==========

    /**
     * init-user.sql을 대체하는 표준 사용자 8명 생성
     * 기존 SQL과 동일한 데이터 구조 제공
     */
    @Transactional
    public StandardUsers persistStandardUsers() {
        User user1 = persistUser(User.builder()
            .email("hyejj19@naver.com")
            .nickName("WOzU6J8541")
            .gender(GenderType.FEMALE)
            .socialType(List.of(SocialType.KAKAO)));

        User user2 = persistUser(User.builder()
            .email("chadongmin@naver.com")
            .nickName("xIFo6J8726")
            .gender(GenderType.MALE)
            .socialType(List.of(SocialType.KAKAO)));

        User user3 = persistUser(User.builder()
            .email("dev.bottle-note@gmail.com")
            .nickName("PARC6J8814")
            .age(25)
            .gender(GenderType.MALE)
            .socialType(List.of(SocialType.GOOGLE)));

        // ... 나머지 5명

        return new StandardUsers(user1, user2, user3, user4, user5, user6, user7, user8);
    }

    /**
     * 사용자 N명 일괄 생성
     */
    @Transactional
    public List<User> persistUsers(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> persistUser())
            .toList();
    }
}
```

**핵심 변경:**
- ✅ TestFactory 제거 → UserTestFactory에 통합
- ✅ `persistStandardUsers()` → `persistStandardUsers()`로 일관성 유지
- ✅ 기존 메서드와 신규 메서드가 한 곳에 공존

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

### 5.4 AlcoholTestFactory 확장 (대용량 데이터 처리)

**위치:** `bottlenote-product-api/src/test/java/app/bottlenote/alcohols/fixture/AlcoholTestFactory.java`

**기존 파일 수정 (이동 없음)**

**특수성:**
- init-alcohol.sql은 231줄, 227개 데이터 (지역 27 + 증류소 179 + 주류 21)
- 대용량이므로 **배치 처리** 및 **선택적 로딩** 필요

```java
@Component
@RequiredArgsConstructor
public class AlcoholTestFactory {

    private final Random random = new SecureRandom();
    @Autowired private EntityManager em;

    // ========== 기존 메서드들 (유지) ==========

    /** 기본 Alcohol 생성 */
    @Transactional
    public Alcohol persistAlcohol() { /* 기존 코드 */ }

    // ========== 새로 추가: SQL 대체 메서드들 ==========

    /**
     * init-alcohol.sql 전체 대체: 표준 데이터 생성
     * ⚠️ 성능 고려: 필요한 경우에만 사용
     */
    @Transactional
    public StandardAlcohols persistStandardAlcohols() {
        // 1. 지역 27개 생성
        List<Region> regions = persistStandardRegions();

        // 2. 증류소 179개 생성 (배치 처리)
        List<Distillery> distilleries = persistStandardDistilleries();

        // 3. 주류 21개 생성
        List<Alcohol> alcohols = persistStandardAlcoholList(regions, distilleries);

        return new StandardAlcohols(regions, distilleries, alcohols);
    }

    /**
     * 경량 데이터: 주요 지역 + 증류소 + 주류 각 3개씩만 생성
     * 대부분의 테스트에서 사용 권장
     */
    @Transactional
    public LightweightAlcohols persistLightweightAlcohols() {
        Region region1 = persistRegion("스코틀랜드", "Scotland");
        Region region2 = persistRegion("일본", "Japan");
        Region region3 = persistRegion("미국", "United States");

        Distillery distillery1 = persistDistillery("맥캘란", "Macallan");
        Distillery distillery2 = persistDistillery("글렌피딕", "Glenfiddich");
        Distillery distillery3 = persistDistillery("야마자키", "Yamazaki");

        Alcohol alcohol1 = persistAlcohol(AlcoholType.WHISKY, region1, distillery1);
        Alcohol alcohol2 = persistAlcohol(AlcoholType.WHISKY, region2, distillery2);
        Alcohol alcohol3 = persistAlcohol(AlcoholType.WHISKY, region3, distillery3);

        return new LightweightAlcohols(
            List.of(region1, region2, region3),
            List.of(distillery1, distillery2, distillery3),
            List.of(alcohol1, alcohol2, alcohol3)
        );
    }

    /**
     * 표준 지역 27개 생성 (init-alcohol.sql 기준)
     */
    private List<Region> persistStandardRegions() {
        return List.of(
            persistRegion("호주", "Australia"),
            persistRegion("핀란드", "Finland"),
            persistRegion("프랑스", "France"),
            // ... 나머지 24개
        );
    }

    /**
     * 표준 증류소 179개 생성 (배치 처리)
     * ⚠️ 성능 최적화: JDBC Batch Insert 고려
     */
    private List<Distillery> persistStandardDistilleries() {
        // TODO: JDBC Batch Insert로 성능 최적화 가능
        return List.of(
            persistDistillery("글래스고", "The Glasgow Distillery Co."),
            persistDistillery("글렌 그란트", "Glen Grant"),
            // ... 나머지 177개
        );
    }
}
```

**핵심 변경:**
- ✅ 기존 위치 유지 (bottlenote-product-api)
- ✅ SQL 대체 메서드 추가 (persistStandardAlcohols, persistLightweightAlcohols)
- ✅ 기존 메서드 재사용 (persistRegion, persistDistillery, persistAlcohol)

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

### 5.5 복합 데이터 처리 방식

**복합 데이터 없이 TestFactory 직접 조합:**

복합 SQL 파일(init-user-mypage-query.sql 등)은 별도의 Composite 클래스 없이 각 TestFactory를 테스트 메서드에서 직접 조합하여 사용합니다.

**장점:**
- ✅ 추가 계층 없이 명확하고 간단
- ✅ 테스트 코드에서 필요한 데이터만 정확히 생성
- ✅ 불필요한 중간 DTO(MyPageTestData 등) 제거

**예시:**
```java
@Test
void 마이페이지_조회_테스트() {
    // Given: 각 TestFactory를 직접 조합
    StandardUsers users = userTestFactory.persistStandardUsers();
    LightweightAlcohols alcohols = alcoholTestFactory.persistLightweightAlcohols();
    Review review1 = reviewTestFactory.persistReview(users.user1(), alcohols.getFirstAlcohol());
    Review review2 = reviewTestFactory.persistReview(users.user1(), alcohols.alcohols().get(1));

    // When & Then
    mockMvc.perform(get("/api/v1/users/" + users.user1().getId() + "/mypage"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviewCount").value(2));
}
```

### 5.6 TestFactory 사용 예시

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

#### After (TO-BE) - TestFactory 직접 사용
```java
@Autowired private UserTestFactory userTestFactory;
@Autowired private AlcoholTestFactory alcoholTestFactory;
@Autowired private ReviewTestFactory reviewTestFactory;

@Test
void 리뷰_목록을_조회할_수_있다() throws Exception {
    // Given: 명확한 데이터 준비
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    Review review = reviewTestFactory.persistReview(user, alcohol);

    // When & Then
    mockMvc.perform(get("/api/v1/reviews")
        .param("alcoholId", String.valueOf(alcohol.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].userId").value(user.getId()));
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
- 기존 TestFactory에 SQL 대체 메서드 추가
- 파일럿 테스트를 통한 마이그레이션 방식 검증

#### 작업 내용

**1) UserTestFactory 보강** (bottlenote-product-api/src/test/java/app/bottlenote/user/fixture/UserTestFactory.java)
- `persistStandardUsers()`: init-user.sql 대체 (표준 8명 사용자 생성)
- `persistUsers(int count)`: N명 사용자 생성
- `StandardUsers` record 정의 (생성된 8명의 사용자 참조)

**예시:**
```java
@Component
@RequiredArgsConstructor
public class UserTestFactory {

    @Autowired private EntityManager em;

    // 기존 메서드들...

    /**
     * init-user.sql과 동일한 8명의 표준 사용자 생성
     */
    @Transactional
    public StandardUsers persistStandardUsers() {
        User user1 = persistUser("test1@test.com", "테스터1");
        User user2 = persistUser("test2@test.com", "테스터2");
        // ... 8명 생성
        return new StandardUsers(user1, user2, ...);
    }

    public record StandardUsers(
        User user1, User user2, User user3, User user4,
        User user5, User user6, User user7, User user8
    ) {}
}
```

**2) 파일럿 테스트**
- UserCommandIntegrationTest 중 1개 메서드만 마이그레이션
- 기존 @Sql 방식과 새 방식 성능 비교

**검증 기준:**
- ✅ UserTestFactory로 생성한 데이터가 init-user.sql과 동일한 구조
- ✅ 파일럿 테스트가 성공적으로 통과
- ✅ 성능 차이 10% 이내
- ✅ 기존 다른 테스트들은 영향받지 않음 (TestFactory를 이동하지 않았으므로)

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

// 2단계: TestFactory 주입
@Autowired private UserTestFactory userTestFactory;

// 3단계: Given 절에 데이터 로딩 추가
@Test
void 회원탈퇴에_성공한다() throws Exception {
    // Given
+   User user = userTestFactory.persistUser();

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

#### Phase 1-2: init-alcohol.sql 마이그레이션 ⚠️

**난이도: 높음**
- 227개 데이터 (지역 27 + 증류소 179 + 주류 21)
- 대용량 데이터 처리 전략 필요

**작업 내용:**
1. `AlcoholTestFactory` 구현
2. 경량 데이터 제공: `persistLightweightAlcohols()` (각 3개씩)
3. 전체 데이터 제공: `persistStandardAlcohols()` (227개 전체)
4. 성능 최적화: JDBC Batch Insert 적용 고려

**마이그레이션 전략:**
```java
// 대부분의 테스트: 경량 데이터 사용
@Test
void 주류_목록_조회() {
    LightweightAlcohols alcohols = alcoholTestFactory.persistLightweightAlcohols();
    // 3개 주류만 사용
}

// 대용량 데이터가 필요한 테스트만: 전체 데이터
@Test
void 전체_주류_페이징_조회() {
    StandardAlcohols alcohols = alcoholTestFactory.persistStandardAlcohols();
    // 227개 전체 사용
}
```

**성능 측정:**
- 경량 데이터 로딩 시간: 목표 < 100ms
- 전체 데이터 로딩 시간: 목표 < 1000ms
- 필요시 캐싱 전략 도입

---

#### Phase 1-3: init-review.sql 마이그레이션

**영향받는 테스트 파일:**
- `ReviewIntegrationTest` (2개 메서드)
- `ReviewReplyIntegrationTest` (2개 메서드)

**작업 내용:**
1. `ReviewTestFactory` 구현
2. `persistStandardReviews()` 메서드 (8개 리뷰)
3. `persistReview()` 메서드

**예시:**
```java
@Component
@RequiredArgsConstructor
public class ReviewTestFactory {

    @Autowired private EntityManager em;

    @Transactional
    public Review persistReview(User user, Alcohol alcohol) {
        Review review = Review.builder()
            .user(user)
            .alcohol(alcohol)
            .content("테스트 리뷰 내용")
            .status(ReviewStatus.PUBLIC)
            .sizeType(SizeType.BOTTLE)
            .price(65000L)
            .build();
        em.persist(review);
        return review;
    }
}
```

---

### Phase 2: 중빈도 파일 마이그레이션

#### Phase 2-1: init-review-reply.sql
- `ReviewReplyTestFactory`에 `persistStandardReplies()` 메서드 추가
- 댓글/대댓글 계층 구조 지원

#### Phase 2-2: init-help.sql
- `HelpTestFactory`에 `persistStandardHelps()` 메서드 추가
- 단순 구조이므로 빠른 마이그레이션 가능

#### Phase 2-3: init-user-history.sql
- `UserHistoryTestFactory`에 `persistStandardHistories()` 메서드 추가
- 5개 히스토리 레코드 생성

---

### Phase 3: 복합 파일 마이그레이션

#### Phase 3-1: init-user-mypage-query.sql
- 여러 TestFactory를 조합하여 데이터 생성
- User + Alcohol + Review + Follow + Rating 조합
- 예: `userTestFactory`, `alcoholTestFactory`, `reviewTestFactory` 함께 사용

#### Phase 3-2: init-user-mybottle-query.sql
- 여러 TestFactory를 조합하여 데이터 생성
- User + Alcohol + Review + Pick 조합
- 예: `userTestFactory`, `alcoholTestFactory`, `pickTestFactory` 함께 사용

---

### Phase 4: 저빈도 파일 마이그레이션

#### init-popular_alcohol.sql
- `AlcoholTestFactory`에 `persistPopularAlcohols()` 메서드 추가
- 26개 인기 주류 통계 데이터

---

### Phase 5: 정리 및 검증

#### 작업 내용:
1. **SQL 파일 삭제**
   - `/init-script/` 디렉토리 전체 삭제
   - 사용하지 않는 @Sql import 제거

2. **문서화**
   - TestFactory 사용 가이드 작성
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
@Autowired private UserTestFactory userTestFactory;

@Test
void 회원탈퇴에_성공한다() throws Exception {
    // Given: 명확한 사용자 생성
    User user = userTestFactory.persistUser();
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

#### After
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
@Autowired private AlcoholTestFactory alcoholTestFactory;

@Test
void 주류_3개만_조회_테스트() throws Exception {
    // Given: 필요한 만큼만 생성 (227개 → 3개)
    LightweightAlcohols alcohols = alcoholTestFactory.persistLightweightAlcohols();

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
    List<Review> reviews = reviewTestFactory.persistReviews(user, 5);  // ✅ 명시적 로딩
}
```

#### 주의 2: ID 의존성
```java
// ❌ 잘못된 예: 하드코딩된 ID
@Test
void test() {
    userTestFactory.persistStandardUsers();

    mockMvc.perform(get("/api/v1/users/1"))  // ❌ ID=1이라는 보장 없음
}

// ✅ 올바른 예: 생성된 객체의 ID 사용
@Test
void test() {
    User user = userTestFactory.persistUser();

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
    StandardAlcohols alcohols = alcoholTestFactory.persistStandardAlcohols();  // 227개
    // 실제로는 1개만 필요
}

// ✅ 필요한 만큼만 생성
@Test
void 단순_조회_테스트() {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();  // 1개
}
```

### 8.2 마이그레이션 체크리스트

**작업 전 준비**
- [ ] 제거할 @Sql 파일 위치 확인 및 데이터 구조 파악
- [ ] 필요한 TestFactory가 이미 존재하는지 확인 (없으면 생성 필요)
  - 확인 경로: `bottlenote-product-api/src/test/java/app/bottlenote/{domain}/fixture/`
  - 또는: `bottlenote-mono/src/test/java/app/bottlenote/{domain}/fixture/`
- [ ] 기존 TestFactory들의 사용 패턴 확인 (특히 email, 필드 주입 방식)

**Factory 생성/사용 시**
- [ ] EntityManager는 `@Autowired` 필드 주입 사용 (생성자 주입 X)
- [ ] UserTestFactory.persistUser()는 로컬 파트만 전달 (`"user1"` ⭕ / `"user1@test.com"` ❌)
- [ ] 5가지 팩토리 원칙 준수 확인 (단일책임, 격리, 순수성, 명시성, 응집성)

**토큰 생성 시**
- [ ] 이미 생성한 User 객체가 있다면 `getToken(user)` 사용
- [ ] `getToken(OauthRequest)` 사용 금지 (유저 중복 생성 위험)
- [ ] 파라미터 없는 `getToken()`은 기본 유저 사용 (역할이 명확하지 않을 때만)

**ID 관리**
- [ ] 하드코딩된 userId 사용 금지 (1, 2, 3, 4 등)
- [ ] 모든 유저는 Factory로 실제 생성 후 `.getId()` 사용
- [ ] @Sql의 `INSERT ... VALUES (1, ...)` 패턴을 Factory로 변환 시 특히 주의

**@Nested 클래스 처리**
- [ ] @Nested 클래스는 `extends` 없이 선언 (자동 상속됨)
- [ ] @BeforeEach는 @Nested 클래스 내부에 작성 (필요 시)
- [ ] 외부 클래스의 @Autowired 필드는 @Nested에서 자동 접근 가능

**작업 후 검증**
- [ ] `grep "@Sql" {파일명}` 으로 모든 @Sql 제거 확인
- [ ] `grep "getToken(oauthRequest)" {파일명}` 으로 잘못된 토큰 생성 확인
- [ ] `grep -E "userId.*\([0-9]+L?\)" {파일명}` 으로 하드코딩된 ID 확인
- [ ] 불필요한 import 제거 (OauthRequest, SocialType 등)
- [ ] 로컬에서 해당 테스트 파일 실행하여 통과 확인

**핵심 원칙**
- [ ] **기존 패턴 먼저 확인**: 프로젝트의 다른 테스트들이 어떻게 하는지 보기
- [ ] **한 곳 수정 시 전체 적용**: 동일한 실수가 여러 곳에 있을 수 있음
- [ ] **내부 구현 파악**: 사용하는 메서드가 무엇을 하는지 확인
- [ ] **@Sql vs Factory 차이 인식**: ID 관리 방식이 다름

**Phase별 체크리스트:**
- [ ] 모든 테스트 파일 마이그레이션 완료
- [ ] SQL 파일 삭제 완료
- [ ] 문서화 완료
- [ ] 전체 테스트 실행 성공
- [ ] 성능 측정 및 비교 완료
- [ ] 팀 공유 완료

---

### 8.3 실제 마이그레이션 경험 (HelpIntegrationTest & ReportIntegrationTest)

이 섹션은 실제 마이그레이션 과정에서 발생한 버그와 학습 내용을 정리한 것입니다.

#### 버그 1. HelpTestFactory EntityManager 주입 방식 오류
- **문제**: `@RequiredArgsConstructor`로 생성자 주입을 시도했지만 EntityManager가 주입되지 않음
- **원인**: 기존 TestFactory 패턴을 확인하지 않고 일반적인 생성자 주입 방식 사용
- **해결**: `@Autowired private EntityManager em` 필드 주입으로 변경
- **교훈**: 새로운 Factory 생성 시 반드시 기존 패턴 확인

#### 버그 2. @Nested 클래스가 IntegrationTestSupport 중복 상속
- **문제**: `@Nested class XXX extends IntegrationTestSupport` 로 작성하여 중복 상속 발생
- **원인**: JUnit5 @Nested 클래스는 외부 클래스의 상속을 자동으로 공유한다는 기본 동작 간과
- **해결**: @Nested 클래스에서 `extends` 제거
- **교훈**: @Nested 클래스는 상속 없이 선언

#### 버그 3. getToken(oauthRequest)로 인한 이메일 중복 문제
- **문제**: 이미 testUser를 생성했는데 OauthRequest로 또 다른 유저를 생성하려고 시도하여 unique constraint 위반
- **원인**: `getToken(OauthRequest)`가 내부적으로 `oauthService.login()`을 호출하여 유저를 생성/조회한다는 점을 파악하지 못함
- **해결**: `getToken(testUser)` 사용으로 이미 생성한 User 객체 직접 전달
- **교훈**: 사용하는 메서드의 내부 구현을 반드시 확인

#### 버그 4. UserTestFactory 이메일 형식 불일치
- **문제**: `persistUser("help-read@test.com", ...)`처럼 전체 이메일을 파라미터로 전달하여 "help-read@test.com-12345@example.com" 형식의 잘못된 이메일 생성
- **원인**: UserTestFactory는 로컬 파트만 받고 자동으로 `@example.com`을 붙이는 구현이라는 것을 확인하지 않음
- **해결**: `persistUser("help-read", ...)`처럼 로컬 파트만 전달
- **교훈**: Factory 메서드 사용 전 내부 구현 확인 필수

#### 버그 5. test_3에서 하드코딩된 userId로 인한 ID 충돌
- **문제**: `IntStream.range(1, 5)`로 userId를 1,2,3,4로 하드코딩하여 fifthReporter.getId()와 충돌 발생
- **원인**: @Sql은 ID를 직접 지정하지만 Factory는 auto_increment로 자동 생성되어 충돌 가능하다는 점 간과
- **해결**: 1~4번째 신고자도 실제 User 객체를 userTestFactory로 생성
- **교훈**: @Sql과 Factory의 ID 관리 방식 차이 인식 중요

#### 버그 6. test_4에서 reporter 유저 미생성
- **문제**: `getToken()` 파라미터 없이 호출하여 신고자가 명확하지 않음
- **원인**: 테스트에서 "신고자"와 "피신고자" 역할을 명확히 구분해야 한다는 점 간과
- **해결**: reporter 유저를 명시적으로 생성하고 `getToken(reporter)` 사용
- **교훈**: 테스트에서 역할이 명확해야 할 때는 각 유저를 명시적으로 생성

#### 버그 7. HelpIntegrationTest에서 getToken(oauthRequest) 잔존
- **문제**: @Nested 내부는 수정했지만 외부 클래스와 일부 @Nested에 getToken(oauthRequest) 2곳 남음
- **원인**: 전체 파일을 grep으로 재확인하지 않고 일부만 수정
- **해결**: `grep "getToken(oauthRequest)" {파일명}` 으로 전체 파일 검증 후 모두 수정
- **교훈**: 수정 후 반드시 grep으로 전체 파일 재확인

#### 근본 원인 요약
- **내부 구현 미파악**: 사용하는 메서드의 내부 동작을 확인하지 않음
- **패턴 불일치**: 기존 테스트 코드와 Factory들의 패턴을 충분히 분석하지 않음
- **@Sql과 Factory의 차이 간과**: ID 관리 방식의 근본적 차이를 간과
- **부분 수정**: 한 가지 실수를 수정할 때 프로젝트 전체에서 동일한 패턴 적용하지 않음
- **검증 부족**: 수정 후 grep 등으로 전체 재확인하지 않음

#### 핵심 교훈
1. **프레임워크/라이브러리 메서드 사용 전 내부 구현 확인**
2. **기존 코드 패턴 철저히 분석 후 동일하게 적용**
3. **데이터 생성 방식 변경 시 ID 관리 전략 재검토**
4. **수정 후 grep으로 전체 파일 재확인**
5. **사용자 피드백 받은 실수는 프로젝트 전체에서 동일하게 수정**

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
   - TestFactory 인프라 구축
   - UserTestFactory 구현
   - 파일럿 테스트 (1개 메서드)

### 후속 작업
1. Phase 1-5 순차적 실행
2. 진행 상황 리뷰
3. 이슈 발생 시 즉시 대응

### 장기 계획
1. 다른 모듈(batch 등)에도 적용
2. TestFactory 패턴을 팀 표준으로 확립
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
