# MockMvc to MockMvcTester 마이그레이션 계획

## 1. 개요

### 1.1 목표
product-api 모듈의 통합 테스트에서 레거시 `MockMvc` API를 최신 `MockMvcTester` API로 마이그레이션하여 테스트 코드의 가독성과 유지보수성을 향상시킨다.

### 1.2 현황 분석

| 구분 | MockMvc (레거시) | MockMvcTester (신규) |
|------|-----------------|---------------------|
| 통합 테스트 | 15개 파일 | 2개 파일 |
| RestDocs 테스트 | 27개 파일 | 해당 없음 |

**마이그레이션 대상**: 통합 테스트 15개 파일
**제외 대상**: RestDocs 테스트 27개 파일 (스탠드얼론 MockMvc 설정 유지)

### 1.3 마이그레이션 이점

| 항목 | MockMvc (레거시) | MockMvcTester (신규) |
|------|-----------------|---------------------|
| 빌더 패턴 | X | O |
| HTTP 메서드 명시성 | `.perform(get(...))` | `.get().uri(...)` |
| 응답 처리 | 수동 파싱 필요 | AssertJ 통합 |
| 코드 라인 수 | 많음 | 적음 |
| 가독성 | 중간 | 높음 |

---

## 2. 마이그레이션 대상 파일

### 2.1 통합 테스트 (15개 파일)

```
bottlenote-product-api/src/test/java/app/bottlenote/
├── alcohols/integration/
│   ├── PopularIntegrationTest.java
│   └── TastingTagIntegrationTest.java
├── banner/integration/
│   └── BannerIntegrationTest.java
├── history/integration/
│   └── UserHistoryIntegrationTest.java
├── like/integration/
│   └── LikesIntegrationTest.java
├── picks/integration/
│   └── PicksIntegrationTest.java
├── rating/integration/
│   └── RatingIntegrationTest.java
├── review/integration/
│   ├── ReviewIntegrationTest.java
│   └── ReviewReplyIntegrationTest.java
├── support/
│   ├── business/integration/BusinessSupportIntegrationTest.java
│   ├── help/integration/HelpIntegrationTest.java
│   └── report/integration/
│       ├── DailyDataReportIntegrationTest.java
│       └── ReportIntegrationTest.java
└── user/integration/
    ├── UserCommandIntegrationTest.java
    └── UserQueryIntegrationTest.java
```

### 2.2 마이그레이션 제외 대상

#### RestDocs 테스트 (27개 파일) - 제외 사유
- `AbstractRestDocs` 베이스 클래스가 `MockMvcBuilders.standaloneSetup()` 사용
- RestDocs 문서화 설정이 MockMvc에 의존
- 스탠드얼론 모드로 빠른 테스트 속도 유지 필요

#### 이미 마이그레이션된 파일 (2개 파일)
- `AlcoholQueryIntegrationTest.java`
- `ImageUploadIntegrationTest.java`

---

## 3. 마이그레이션 가이드

### 3.1 코드 변환 패턴

#### Before (MockMvc 레거시)
```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.MvcResult;

MvcResult result = mockMvc
    .perform(
        put("/api/v1/picks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(request))
            .header("Authorization", "Bearer " + getToken())
            .with(csrf()))
    .andDo(print())
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(200))
    .andExpect(jsonPath("$.data").exists())
    .andReturn();

String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
PicksUpdateResponse data = mapper.convertValue(response.getData(), PicksUpdateResponse.class);
```

#### After (MockMvcTester 신규)
```java
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

MvcTestResult result = mockMvcTester
    .put()
    .uri("/api/v1/picks")
    .contentType(APPLICATION_JSON)
    .content(mapper.writeValueAsString(request))
    .header("Authorization", "Bearer " + getToken())
    .with(csrf())
    .exchange();

PicksUpdateResponse data = extractData(result, PicksUpdateResponse.class);
```

### 3.2 HTTP 메서드별 변환

| MockMvc | MockMvcTester |
|---------|---------------|
| `perform(get("/path"))` | `.get().uri("/path")` |
| `perform(post("/path"))` | `.post().uri("/path")` |
| `perform(put("/path"))` | `.put().uri("/path")` |
| `perform(patch("/path"))` | `.patch().uri("/path")` |
| `perform(delete("/path"))` | `.delete().uri("/path")` |

### 3.3 파라미터 및 헤더 설정

| 항목 | MockMvc | MockMvcTester |
|------|---------|---------------|
| Query Param | `.param("key", "value")` | `.param("key", "value")` |
| Path Variable | `get("/api/{id}", 1L)` | `.get().uri("/api/{id}", 1L)` |
| Content Type | `.contentType(MediaType.APPLICATION_JSON)` | `.contentType(APPLICATION_JSON)` |
| Header | `.header("Authorization", token)` | `.header("Authorization", token)` |
| CSRF | `.with(csrf())` | `.with(csrf())` |

### 3.4 응답 처리 변환

#### 단순 상태 검증
```java
// Before
mockMvc.perform(get("/api/v1/test"))
    .andExpect(status().isOk());

// After
mockMvcTester.get().uri("/api/v1/test").exchange()
    .assertThat().hasStatusOk();
```

#### 응답 데이터 추출
```java
// Before
MvcResult result = mockMvc.perform(...).andReturn();
String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
GlobalResponse response = mapper.readValue(json, GlobalResponse.class);
MyDto data = mapper.convertValue(response.getData(), MyDto.class);

// After (IntegrationTestSupport의 헬퍼 메서드 활용)
MvcTestResult result = mockMvcTester.get().uri(...).exchange();
MyDto data = extractData(result, MyDto.class);
```

### 3.5 Import 문 변경

#### 제거할 import
```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.MvcResult;
import java.nio.charset.StandardCharsets;
```

#### 추가할 import
```java
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
```

#### 유지할 import
```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
```

---

## 4. 마이그레이션 단계

### Phase 1: 인프라 확인 (완료)
- [x] `IntegrationTestSupport`에 `mockMvcTester` 주입 확인
- [x] `extractData(MvcTestResult, Class<T>)` 헬퍼 메서드 확인
- [x] `parseResponse(MvcTestResult)` 헬퍼 메서드 확인

### Phase 2: 도메인별 순차 마이그레이션

#### 2-1. picks (1개 파일) - 우선순위 높음 (간단한 CRUD)
- [ ] `PicksIntegrationTest.java`

#### 2-2. like (1개 파일)
- [ ] `LikesIntegrationTest.java`

#### 2-3. rating (1개 파일)
- [ ] `RatingIntegrationTest.java`

#### 2-4. banner (1개 파일)
- [ ] `BannerIntegrationTest.java`

#### 2-5. history (1개 파일)
- [ ] `UserHistoryIntegrationTest.java`

#### 2-6. alcohols (2개 파일)
- [ ] `PopularIntegrationTest.java`
- [ ] `TastingTagIntegrationTest.java`

#### 2-7. review (2개 파일)
- [ ] `ReviewIntegrationTest.java`
- [ ] `ReviewReplyIntegrationTest.java`

#### 2-8. support (4개 파일)
- [ ] `BusinessSupportIntegrationTest.java`
- [ ] `HelpIntegrationTest.java`
- [ ] `ReportIntegrationTest.java`
- [ ] `DailyDataReportIntegrationTest.java`

#### 2-9. user (2개 파일)
- [ ] `UserCommandIntegrationTest.java`
- [ ] `UserQueryIntegrationTest.java`

### Phase 3: 정리
- [ ] 미사용 import 제거
- [ ] `IntegrationTestSupport`에서 MockMvc 필드 제거 검토
- [ ] 테스트 실행 및 검증

---

## 5. 참조 코드

### 5.1 admin-api IntegrationTestSupport (Kotlin)
```kotlin
// 참조: MockMvcTester만 사용하는 깔끔한 구조
@Autowired
protected lateinit var mockMvcTester: MockMvcTester

protected fun <T> extractData(result: MvcTestResult, dataType: Class<T>): T {
    val response = parseResponse(result)
    return mapper.convertValue(response.data, dataType)
}
```

### 5.2 이미 마이그레이션된 테스트 예시
- `ImageUploadIntegrationTest.java`: 복잡한 비동기 시나리오
- `AlcoholQueryIntegrationTest.java`: 페이징 및 검색 쿼리

---

## 6. 예상 작업량

| 파일 | 예상 테스트 메서드 수 | 복잡도 |
|------|---------------------|--------|
| PicksIntegrationTest | 2 | 낮음 |
| LikesIntegrationTest | 2-3 | 낮음 |
| RatingIntegrationTest | 3-5 | 중간 |
| BannerIntegrationTest | 2-3 | 낮음 |
| UserHistoryIntegrationTest | 3-5 | 중간 |
| PopularIntegrationTest | 3-5 | 중간 |
| TastingTagIntegrationTest | 2-3 | 낮음 |
| ReviewIntegrationTest | 5-10 | 높음 |
| ReviewReplyIntegrationTest | 3-5 | 중간 |
| BusinessSupportIntegrationTest | 3-5 | 중간 |
| HelpIntegrationTest | 3-5 | 중간 |
| ReportIntegrationTest | 3-5 | 중간 |
| DailyDataReportIntegrationTest | 2-3 | 낮음 |
| UserCommandIntegrationTest | 5-8 | 높음 |
| UserQueryIntegrationTest | 3-5 | 중간 |

**총 예상**: 약 45-70개 테스트 메서드 마이그레이션

---

## 7. 검증 체크리스트

마이그레이션 완료 후 각 파일별로 확인:

- [ ] 모든 테스트가 성공적으로 실행되는가?
- [ ] `mockMvc` 필드 사용이 완전히 제거되었는가?
- [ ] 불필요한 import가 제거되었는가?
- [ ] `extractData()` 또는 `parseResponse()` 헬퍼 메서드를 활용하고 있는가?
- [ ] HTTP 메서드가 명시적으로 표현되어 있는가? (`.get()`, `.post()` 등)

---

## 8. 최종 정리 작업

모든 파일 마이그레이션 완료 후:

### 8.1 IntegrationTestSupport 정리 (선택적)
```java
// 제거 검토 대상
@Autowired protected MockMvc mockMvc;

// 제거 검토 대상 (레거시 오버로드)
protected <T> T extractData(MvcResult result, Class<T> dataType)
protected GlobalResponse parseResponse(MvcResult result)
```

### 8.2 최종 테스트 실행
```bash
./gradlew :bottlenote-product-api:integration_test
```

---

## 9. 주의사항 및 트러블슈팅

### 9.1 에러 응답 처리 주의

`extractData()` 헬퍼 메서드는 내부적으로 `hasStatusOk()`를 호출하므로, **에러 케이스 테스트에서는 사용 불가**.

```java
// BAD - extractData()는 200 OK만 처리 가능
MvcTestResult result = mockMvcTester.get().uri(...).exchange();
extractData(result, ErrorResponse.class); // hasStatusOk() 실패!

// GOOD - 에러 케이스는 직접 검증
MvcTestResult result = mockMvcTester.get().uri(...).exchange();
result.assertThat()
    .hasStatus(HttpStatus.BAD_REQUEST);
// 또는 response body 직접 파싱
```

**해당 테스트 파일**:
- `ReviewIntegrationTest.java` - `test_3()`, `test_5()` (validation 에러 검증)
- `UserQueryIntegrationTest.java` - `test_4()` (MYPAGE_NOT_ACCESSIBLE), `test_3()`, `test_4()` (마이보틀 에러)

### 9.2 jsonPath 검증 마이그레이션

MockMvc의 `andExpect(jsonPath(...))` 체인은 MockMvcTester에서 다르게 처리해야 함.

```java
// Before (MockMvc)
mockMvc.perform(get("/api"))
    .andExpect(jsonPath("$.code").value(200))
    .andExpect(jsonPath("$.data.userId").value(1L));

// After (MockMvcTester) - 방법 1: AssertJ 체인
MvcTestResult result = mockMvcTester.get().uri("/api").exchange();
result.assertThat()
    .hasStatusOk()
    .bodyJson()
    .extractingPath("$.code").isEqualTo(200);

// After (MockMvcTester) - 방법 2: extractData 후 객체 검증 (권장)
MyResponse data = extractData(result, MyResponse.class);
assertEquals(1L, data.getUserId());
```

**권장**: 복잡한 jsonPath 검증보다 `extractData()` 후 객체 단위 검증이 더 명확함.

### 9.3 andDo(print()) 제거

MockMvcTester는 `print()` 메서드가 없음. 디버깅이 필요하면 로그를 직접 출력.

```java
// Before
mockMvc.perform(get("/api")).andDo(print());

// After - print() 없음, 필요시 로그 출력
MvcTestResult result = mockMvcTester.get().uri("/api").exchange();
log.info("Response: {}", result.getResponse().getContentAsString());
```

### 9.4 content() 메서드 - byte[] vs String

MockMvc에서 `mapper.writeValueAsBytes()`를 사용하는 경우가 있음. MockMvcTester에서도 동일하게 동작.

```java
// 둘 다 동작함
.content(mapper.writeValueAsString(request))  // String
.content(mapper.writeValueAsBytes(request))   // byte[]
```

### 9.5 연속 API 호출 테스트

한 테스트에서 여러 API를 순차 호출하는 경우, 각각 별도의 `MvcTestResult`로 받아야 함.

```java
// ReviewIntegrationTest의 test_1() (리뷰 삭제 테스트) 참조
// 1. 리뷰 생성
MvcTestResult createResult = mockMvcTester.post().uri("/api/v1/reviews")...exchange();
ReviewCreateResponse created = extractData(createResult, ReviewCreateResponse.class);

// 2. 생성된 리뷰 삭제
MvcTestResult deleteResult = mockMvcTester.delete()
    .uri("/api/v1/reviews/{reviewId}", created.getId())...exchange();

// 3. 목록에서 삭제 확인
MvcTestResult listResult = mockMvcTester.get()
    .uri("/api/v1/reviews/{alcoholId}", alcoholId)...exchange();
```

### 9.6 @Nested 클래스의 @BeforeEach 주의

`ReviewIntegrationTest`의 `update` 클래스처럼 `@BeforeEach`에서 데이터를 설정하는 경우, `getTokenUserId()` 호출 시점에 주의.

```java
@Nested
class update {
    @BeforeEach
    void setUp() {
        // getTokenUserId()는 IntegrationTestSupport에서 제공
        final Long tokenUserId = getTokenUserId();
        Review review = ReviewObjectFixture.getReviewFixture(1L, tokenUserId, "content1");
        reviewRepository.save(review);
    }
}
```

이 패턴은 MockMvcTester 마이그레이션과 무관하게 유지됨.

### 9.7 Hamcrest matchers 의존성

일부 테스트에서 Hamcrest `hasSize()` 등을 사용 중.

```java
import static org.hamcrest.Matchers.hasSize;

// Before
.andExpect(jsonPath("$.errors", hasSize(2)))

// After - AssertJ로 변환 권장
result.assertThat()
    .bodyJson()
    .extractingPath("$.errors")
    .asArray()
    .hasSize(2);
```

### 9.8 파일별 특이사항

| 파일 | 특이사항 |
|------|----------|
| `ReviewIntegrationTest` | 에러 검증 케이스 다수, 연속 API 호출, `@BeforeEach` 사용 |
| `UserQueryIntegrationTest` | 에러 검증 케이스 다수, 다중 쿼리 파라미터 |
| `RatingIntegrationTest` | 테스트 내 데이터 직접 생성 후 조회 |
| `PicksIntegrationTest` | 가장 단순, 마이그레이션 연습용으로 적합 |

---

## 10. 마이그레이션 순서 권장

복잡도 낮은 것부터 시작하여 패턴 익히기:

1. **PicksIntegrationTest** (2개 메서드, 단순 CRUD) - 연습용
2. **LikesIntegrationTest** (유사 패턴)
3. **RatingIntegrationTest** (데이터 설정 패턴 학습)
4. **BannerIntegrationTest**, **TastingTagIntegrationTest** (단순)
5. **나머지 파일들** (복잡도 증가순)
6. **ReviewIntegrationTest**, **UserQueryIntegrationTest** (마지막 - 에러 케이스 다수)
