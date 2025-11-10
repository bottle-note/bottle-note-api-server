# Integration Test Refactoring Plan

## 1. 현재 구조 분석

### 1.1 IntegrationTestSupport의 문제점

현재 `IntegrationTestSupport` 클래스는 너무 많은 책임을 가지고 있어 **단일 책임 원칙(SRP)**을 위반하고 있습니다.

**현재 책임 목록:**
1. TestContainers 설정 및 관리 (MySQL, Redis)
2. 인증 토큰 생성 및 관리
3. 테스트 데이터 초기화 및 정리
4. HTTP 응답 파싱 헬퍼
5. Spring Boot 테스트 환경 설정

```java
// 현재 IntegrationTestSupport.java의 구조
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {
    // 1. TestContainers 설정
    @Container static MySQLContainer<?> MY_SQL_CONTAINER = ...
    @Container static GenericContainer<?> REDIS_CONTAINER = ...

    // 2. 인증 관련
    protected String getToken() { ... }
    protected String getRandomToken() { ... }
    protected Long getTokenUserId() { ... }

    // 3. 데이터 초기화
    @AfterEach void deleteAll() { ... }

    // 4. 응답 파싱
    protected <T> T extractData(...) { ... }
    protected GlobalResponse parseResponse(...) { ... }
}
```

### 1.2 현재 테스트 패턴

**1) @Sql 어노테이션 기반 초기 데이터 구성**
```java
@Sql(scripts = {
    "/init-script/init-alcohol.sql",
    "/init-script/init-user.sql"
})
@Test
void test_1() { ... }
```

**2) TestFactory 패턴 활용**
```java
@Autowired private AlcoholTestFactory alcoholTestFactory;
@Autowired private UserTestFactory userTestFactory;

@Test
void test_2() {
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    User user = userTestFactory.persistUser(...);
}
```

**3) DataInitializer를 통한 데이터 정리**
```java
@AfterEach
void deleteAll() {
    dataInitializer.deleteAll(); // TRUNCATE all tables
}
```

---

## 2. 개선 목표

### 2.1 핵심 목표

**IntegrationTestSupport를 순수 게이트웨이로 전환**
- 각 책임을 독립적인 컴포넌트로 분리
- IntegrationTestSupport는 컴포넌트 조합만 담당
- 테스트 코드의 가독성 및 유지보수성 향상

### 2.2 SOLID 원칙 적용

1. **단일 책임 원칙 (SRP)**: 각 클래스는 하나의 책임만 가짐
2. **개방-폐쇄 원칙 (OCP)**: 확장에는 열려있고 변경에는 닫혀있음
3. **의존성 역전 원칙 (DIP)**: 구체적인 구현이 아닌 추상화에 의존

---

## 3. 리팩토링 설계

### 3.1 새로운 컴포넌트 구조

```
bottlenote-product-api/src/test/java/app/bottlenote/support/
├── containers/
│   ├── TestContainersConfiguration.java        # TestContainers 설정 전담
│   └── DatabaseContainer.java                   # DB 컨테이너 래퍼 (옵션)
├── auth/
│   ├── TestAuthenticationSupport.java          # 인증/토큰 관리 전담
│   └── TestTokenGenerator.java                 # 토큰 생성 로직 (옵션)
├── data/
│   ├── TestDataCleaner.java                    # 데이터 초기화 전담
│   └── DataInitializer.java                    # 기존 유지, 개선
└── http/
    └── TestResponseHelper.java                 # HTTP 응답 파싱 전담

IntegrationTestSupport.java                      # 게이트웨이 역할만 수행
```

### 3.2 각 컴포넌트의 책임

#### 3.2.1 TestContainersConfiguration

**책임:**
- MySQL, Redis 컨테이너 생성 및 관리
- 컨테이너 재사용 설정
- DynamicPropertySource 설정

**주요 메서드:**
```java
public class TestContainersConfiguration {
    private static final MySQLContainer<?> MYSQL;
    private static final GenericContainer<?> REDIS;

    static {
        // 컨테이너 초기화 및 병렬 시작
    }

    public static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        // Redis, MySQL 연결 정보 설정
    }

    public static MySQLContainer<?> getMysqlContainer() { ... }
    public static GenericContainer<?> getRedisContainer() { ... }
}
```

**개선 포인트:**
- 컨테이너 설정을 외부로 분리하여 재사용성 향상
- 다른 테스트 클래스에서도 동일한 컨테이너 설정 사용 가능
- 컨테이너별 설정 변경이 용이

#### 3.2.2 TestAuthenticationSupport

**책임:**
- 테스트용 사용자 생성
- JWT 토큰 생성 및 관리
- 인증된 요청을 위한 토큰 제공

**주요 메서드:**
```java
@Component
public class TestAuthenticationSupport {
    private final OauthRepository oauthRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 기본 토큰 생성 (첫 번째 유저 또는 새로 생성)
    public String getToken() { ... }

    // 랜덤 유저 토큰 생성
    public String getRandomToken() { ... }

    // 특정 유저 토큰 생성
    public TokenItem getToken(User user) { ... }
    public TokenItem getToken(OauthRequest request) { ... }

    // 토큰 유저 ID 조회
    public Long getTokenUserId() { ... }
    public Long getTokenUserId(String email) { ... }

    // 테스트용 유저 생성
    public User createTestUser() { ... }
    public User createTestUser(String email, String nickname) { ... }
}
```

**개선 포인트:**
- 인증 로직을 독립적인 컴포넌트로 분리
- 토큰 생성 전략 확장 가능
- 테스트 데이터(유저) 생성과 토큰 생성의 명확한 분리

#### 3.2.3 TestDataCleaner

**책임:**
- 테스트 후 데이터 정리
- 선택적 데이터 정리 옵션 제공
- 데이터 초기화 전략 관리

**주요 메서드:**
```java
@Component
public class TestDataCleaner {
    private final DataInitializer dataInitializer;

    // 전체 데이터 삭제
    public void cleanAll() {
        dataInitializer.deleteAll();
    }

    // 특정 테이블만 삭제
    public void cleanTables(String... tableNames) { ... }

    // 특정 도메인 데이터만 삭제
    public void cleanDomain(Class<?> entityClass) { ... }
}
```

**개선 포인트:**
- 데이터 정리 전략을 유연하게 변경 가능
- 필요한 경우 부분 삭제 지원
- DataInitializer와의 역할 분리 명확화

#### 3.2.4 TestResponseHelper

**책임:**
- HTTP 응답 파싱
- GlobalResponse 데이터 추출
- 응답 검증 헬퍼

**주요 메서드:**
```java
@Component
public class TestResponseHelper {
    private final ObjectMapper objectMapper;

    // GlobalResponse 파싱 및 data 추출 (MvcTestResult)
    public <T> T extractData(MvcTestResult result, Class<T> dataType) { ... }

    // GlobalResponse 파싱 및 data 추출 (MvcResult)
    public <T> T extractData(MvcResult result, Class<T> dataType) { ... }

    // GlobalResponse만 파싱
    public GlobalResponse parseResponse(MvcTestResult result) { ... }
    public GlobalResponse parseResponse(MvcResult result) { ... }

    // 에러 응답 파싱
    public List<Error> extractErrors(MvcResult result) { ... }
}
```

**개선 포인트:**
- 응답 파싱 로직을 독립적으로 관리
- JSON 변환 로직 중앙화
- 다양한 응답 형식에 대한 유연한 처리

#### 3.2.5 IntegrationTestSupport (리팩토링 후)

**책임:**
- 각 컴포넌트를 조합하는 게이트웨이 역할
- 공통 테스트 설정 (@SpringBootTest, @AutoConfigureMockMvc 등)
- 편의 메서드 제공 (위임 패턴)

**리팩토링 후 구조:**
```java
@Testcontainers
@ActiveProfiles({"test", "batch"})
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class IntegrationTestSupport {

    // 1. 컨테이너 설정 위임
    @Container
    protected static MySQLContainer<?> MY_SQL_CONTAINER =
        TestContainersConfiguration.getMysqlContainer();

    @Container
    protected static GenericContainer<?> REDIS_CONTAINER =
        TestContainersConfiguration.getRedisContainer();

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        TestContainersConfiguration.configureDynamicProperties(registry);
    }

    // 2. 컴포넌트 주입
    @Autowired protected TestAuthenticationSupport authSupport;
    @Autowired protected TestDataCleaner dataCleaner;
    @Autowired protected TestResponseHelper responseHelper;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected MockMvc mockMvc;
    @Autowired protected MockMvcTester mockMvcTester;

    // 3. 데이터 정리
    @AfterEach
    void cleanUpAfterEach() {
        dataCleaner.cleanAll();
    }

    // 4. 편의 메서드 (위임)
    protected String getToken() {
        return authSupport.getToken();
    }

    protected String getRandomToken() {
        return authSupport.getRandomToken();
    }

    protected Long getTokenUserId() {
        return authSupport.getTokenUserId();
    }

    protected <T> T extractData(MvcTestResult result, Class<T> dataType) throws Exception {
        return responseHelper.extractData(result, dataType);
    }

    protected GlobalResponse parseResponse(MvcTestResult result) throws Exception {
        return responseHelper.parseResponse(result);
    }
}
```

**개선 포인트:**
- 순수 조합/게이트웨이 역할만 수행
- 각 컴포넌트로 위임하여 결합도 감소
- 테스트 코드 작성자는 기존과 동일한 방식으로 사용 가능 (하위 호환)

---

## 4. 구현 단계

### Phase 1: 컴포넌트 분리 (기존 기능 유지)

**목표:** 기존 IntegrationTestSupport의 기능을 유지하면서 컴포넌트 분리

1. **TestContainersConfiguration 생성**
   - MySQL, Redis 컨테이너 설정 이동
   - DynamicPropertySource 로직 이동
   - 정적 초기화 블록 유지

2. **TestAuthenticationSupport 생성**
   - 토큰 생성 메서드 이동 (getToken, getRandomToken 등)
   - OauthRepository, JwtTokenProvider 의존성 주입
   - @Component로 등록하여 스프링 빈으로 관리

3. **TestDataCleaner 생성**
   - DataInitializer 래핑
   - cleanAll() 메서드로 deleteAll() 위임
   - 향후 확장을 위한 인터페이스 준비

4. **TestResponseHelper 생성**
   - 응답 파싱 메서드 이동 (extractData, parseResponse)
   - ObjectMapper 의존성 주입

5. **IntegrationTestSupport 리팩토링**
   - 각 컴포넌트로 위임하도록 수정
   - 편의 메서드는 그대로 유지 (하위 호환)
   - @Autowired로 컴포넌트 주입

**검증:**
- 기존 통합 테스트가 모두 통과하는지 확인
- 테스트 동작 방식 변경 없음 (리팩토링만)

### Phase 2: 컴포넌트 개선 및 확장

**목표:** 각 컴포넌트의 기능 개선 및 확장성 향상

1. **TestAuthenticationSupport 개선**
   - 다양한 토큰 생성 전략 추가
   - 테스트 유저 생성 메서드 확장
   - 인증 컨텍스트 관리 기능 추가

2. **TestDataCleaner 개선**
   - 선택적 테이블 삭제 기능 추가
   - 도메인별 데이터 삭제 기능 추가
   - 데이터 초기화 전략 인터페이스 정의

3. **TestResponseHelper 개선**
   - 에러 응답 파싱 기능 추가
   - 페이징 응답 파싱 헬퍼 추가
   - 커스텀 응답 검증 메서드 추가

4. **DataInitializer 개선**
   - 성능 최적화 (캐싱 등)
   - 로깅 개선
   - 에러 처리 강화

**검증:**
- 신규 기능이 기존 테스트에 영향 없는지 확인
- 새로운 기능을 활용한 테스트 작성

### Phase 3: 문서화 및 마이그레이션 가이드

**목표:** 팀 전체가 새로운 구조를 이해하고 활용할 수 있도록 문서화

1. **문서 작성**
   - 각 컴포넌트 사용 가이드
   - 마이그레이션 가이드 (기존 → 새 구조)
   - Best Practices 문서

2. **예제 코드 작성**
   - 각 컴포넌트를 직접 사용하는 예제
   - 복잡한 시나리오에 대한 예제

3. **팀 공유**
   - 리팩토링 목적 및 이점 공유
   - Q&A 세션

---

## 5. 예상 효과

### 5.1 코드 품질 개선

**1) 단일 책임 원칙 준수**
- 각 클래스가 명확한 하나의 책임만 가짐
- 변경의 이유가 하나로 명확해짐

**2) 결합도 감소**
- IntegrationTestSupport와 각 기능 간의 결합도 감소
- 컴포넌트 독립적 테스트 가능

**3) 응집도 증가**
- 관련된 기능끼리 그룹화
- 코드 이해 및 유지보수 용이

### 5.2 테스트 작성 편의성 향상

**1) 명확한 의도 표현**
```java
// Before
String token = getToken();

// After (필요시 직접 사용 가능)
String token = authSupport.getToken();
User testUser = authSupport.createTestUser();
```

**2) 유연한 데이터 정리**
```java
// Before
@AfterEach
void deleteAll() {
    dataInitializer.deleteAll(); // 항상 전체 삭제
}

// After
@AfterEach
void cleanup() {
    dataCleaner.cleanTables("users", "reviews"); // 필요한 것만
}
```

**3) 확장 가능한 응답 검증**
```java
// Before
GlobalResponse response = parseResponse(result);
ReviewResponse data = mapper.convertValue(response.getData(), ReviewResponse.class);

// After
ReviewResponse data = responseHelper.extractData(result, ReviewResponse.class);
List<Error> errors = responseHelper.extractErrors(errorResult);
```

### 5.3 유지보수성 향상

**1) 변경의 영향 범위 최소화**
- 토큰 생성 로직 변경 → TestAuthenticationSupport만 수정
- 컨테이너 설정 변경 → TestContainersConfiguration만 수정

**2) 테스트 가능성 향상**
- 각 컴포넌트를 독립적으로 단위 테스트 가능
- Mock 객체로 대체 가능

**3) 재사용성 증가**
- 다른 테스트 클래스에서도 컴포넌트 재사용 가능
- 배치 테스트, 통합 테스트 등에서 공통 활용

---

## 6. 마이그레이션 전략

### 6.1 점진적 적용

**1단계: 새 컴포넌트 도입 (기존 코드 유지)**
- 새로운 컴포넌트 클래스 생성
- IntegrationTestSupport에서 위임 패턴 적용
- 기존 테스트 코드는 변경 없음

**2단계: 신규 테스트에 적용**
- 새로 작성되는 테스트부터 새 구조 활용
- 점진적으로 팀원들에게 익숙해지도록

**3단계: 기존 테스트 개선 (선택적)**
- 필요시 기존 테스트를 새 구조로 마이그레이션
- 우선순위: 자주 변경되는 테스트부터

### 6.2 하위 호환성 유지

**편의 메서드 유지:**
```java
// IntegrationTestSupport에서 여전히 사용 가능
protected String getToken() {
    return authSupport.getToken();
}
```

**점진적 전환:**
```java
// 기존 방식 (계속 사용 가능)
String token = getToken();

// 새로운 방식 (권장)
String token = authSupport.getToken();
User testUser = authSupport.createTestUser();
```

---

## 7. 위험 요소 및 대응

### 7.1 예상 위험

**1) 기존 테스트 깨짐**
- **대응:** Phase 1에서 기능 변경 없이 리팩토링만 수행
- **검증:** 모든 통합 테스트 실행 후 통과 확인

**2) 성능 저하**
- **대응:** 컨테이너 재사용 설정 유지
- **검증:** 테스트 실행 시간 측정 및 비교

**3) 팀원 혼란**
- **대응:** 충분한 문서화 및 예제 제공
- **검증:** 코드 리뷰 시 새 구조 사용 가이드

### 7.2 롤백 계획

**문제 발생 시:**
1. 커밋 히스토리에서 리팩토링 이전 상태로 복구
2. 문제 원인 분석 후 재시도

**부분 롤백:**
- 특정 컴포넌트만 문제 발생 시 해당 컴포넌트만 롤백
- 다른 컴포넌트는 유지

---

## 8. 성공 지표

### 8.1 정량적 지표

1. **테스트 실행 시간**: 기존 대비 10% 이내 차이
2. **테스트 통과율**: 100% 유지
3. **코드 커버리지**: 기존 유지 또는 향상

### 8.2 정성적 지표

1. **코드 가독성**: 팀원 피드백 (긍정적)
2. **유지보수성**: 변경 작업 시간 감소
3. **확장성**: 새로운 테스트 패턴 추가 용이성

---

## 9. 다음 단계

### 9.1 즉시 실행 (Phase 1)

1. `TestContainersConfiguration` 클래스 생성
2. `TestAuthenticationSupport` 클래스 생성
3. `TestDataCleaner` 클래스 생성
4. `TestResponseHelper` 클래스 생성
5. `IntegrationTestSupport` 리팩토링
6. 모든 통합 테스트 실행 및 검증

### 9.2 후속 작업 (Phase 2-3)

1. 각 컴포넌트 기능 확장
2. 문서화 및 예제 작성
3. 팀 공유 및 피드백 수렴
4. 지속적 개선

---

## 10. 결론

이 리팩토링은 **단일 책임 원칙**을 준수하여 IntegrationTestSupport를 순수 게이트웨이로 만드는 것을 목표로 합니다.

**핵심 개선사항:**
- ✅ 각 책임을 독립적인 컴포넌트로 분리
- ✅ IntegrationTestSupport는 조합/위임만 담당
- ✅ 기존 테스트 코드 호환성 유지
- ✅ 확장 가능하고 유지보수하기 쉬운 구조

**기대 효과:**
- 코드 품질 향상
- 테스트 작성 편의성 증대
- 유지보수성 개선
- 팀 생산성 향상

이 계획에 따라 점진적으로 리팩토링을 진행하면, 기존 기능을 유지하면서도 더 나은 테스트 구조를 구축할 수 있습니다.
