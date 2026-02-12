# TestFactory generateRandomSuffix 충돌 수정

```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-02-09

** Core Achievements **
- 9개 TestFactory의 generateRandomSuffix()를 AtomicInteger 카운터로 교체
- Birthday Problem에 의한 CI flaky test (약 1/30 확률) 완전 제거
- CI 4개 테스트 전체 통과 확인 (check_rule_test, unit_test, integration_test, admin_integration_test)

** Key Components **
- UserTestFactory, AdminUserTestFactory (bottlenote-mono/.../user/fixture/)
- AlcoholTestFactory, AlcoholMetadataTestFactory, TastingTagTestFactory (bottlenote-mono/.../alcohols/fixture/)
- RatingTestFactory (bottlenote-mono/.../rating/fixture/)
- HistoryTestFactory (bottlenote-mono/.../history/fixture/)
- ReviewTestFactory (bottlenote-mono/.../review/fixture/)
- LikesTestFactory (bottlenote-mono/.../like/fixture/)
================================================================================
```

> CI flaky test 원인인 `random.nextInt(10000)` 기반 suffix 생성을 `AtomicInteger` 카운터로 교체하여 유니크 제약조건 충돌을 제거한다.

---

## 1. 문제 분석

### 발생 현상

- **CI Run**: https://github.com/bottle-note/bottle-note-api-server/actions/runs/21810567316
- **실패 Job**: `integration-tests (product, integration_test)` - 177개 중 1개 실패
- **실패 테스트**: `UserHistoryIntegrationTest.test_6` ("리뷰 필터 조건으로 유저 히스토리를 조회할 수 있다.")
- **예외**: `SQLIntegrityConstraintViolationException` at line 228 (`setupHistoryTestData()`)

### 근본 원인

```java
// 9개 TestFactory가 동일 패턴 사용
private final Random random = new SecureRandom();

private String generateRandomSuffix() {
    return String.valueOf(random.nextInt(10000));  // 범위: 0~9999
}
```

- `User.email`과 `User.nickName`에 `unique = true` DB 제약조건 존재
- `random.nextInt(10000)` 범위(0~9999)에서 **생일 문제(Birthday Problem)**에 의해 충돌 확률이 직관보다 훨씬 높음
- CI에서 반복 실행될수록 발생 빈도 증가하는 전형적인 flaky test

### 충돌 확률 분석 (Birthday Problem)

`nextInt(10000)` 범위에서 N개의 값을 뽑을 때, 아무 2개가 충돌할 확률:
`P ≈ 1 - e^(-N(N-1) / (2 * 10000))`

**단일 테스트 (`setupHistoryTestData()` 1회 호출 = 유저 8명):**

| 대상 | 생성 수 | 충돌 확률 |
|------|---------|----------|
| email suffix (8개) | 8 | ~0.28% (1/357) |
| nickName suffix (8개) | 8 | ~0.28% (1/357) |
| 둘 중 하나라도 충돌 | - | **~0.56% (1/180)** |

**테스트 클래스 전체 (`UserHistoryIntegrationTest` = 6개 테스트):**

| 시나리오 | 계산 | 충돌 확률 |
|----------|------|----------|
| 6개 테스트 중 1개 이상 실패 | `1 - (1 - 0.0056)^6` | **~3.3% (약 1/30)** |

> CI가 약 30회 실행될 때마다 1번은 이 테스트가 실패하는 빈도다.
> `UserHistoryIntegrationTest` 외에도 `setupHistoryTestData()`나 `persistUser()`를 호출하는 다른 테스트 클래스까지 합산하면 체감 빈도는 더 높아진다.

### 재현 조건

1. `UserHistoryIntegrationTest` 6개 테스트가 순차 실행
2. `@AfterEach`에서 `DataInitializer.deleteAll()` (TRUNCATE) → 테스트 간 격리는 정상
3. **한 테스트 내** `setupHistoryTestData()`가 8명의 유저를 생성할 때 suffix 충돌 발생 가능
4. email 또는 nickName의 랜덤 suffix가 동일 → `SQLIntegrityConstraintViolationException`

---

## 2. 영향 범위

### 수정 대상 파일 (9개)

모든 파일이 `bottlenote-mono/src/test/java/app/bottlenote/` 하위에 위치한다.

| # | 파일 | 위치 | unique 필드 충돌 위험 |
|---|------|------|---------------------|
| 1 | `UserTestFactory.java` | `user/fixture/` | email, nickName (직접 원인) |
| 2 | `AdminUserTestFactory.java` | `user/fixture/` | email, nickName |
| 3 | `AlcoholTestFactory.java` | `alcohols/fixture/` | Region/Distillery/Alcohol korName, engName |
| 4 | `AlcoholMetadataTestFactory.java` | `alcohols/fixture/` | Region, Distillery 이름 |
| 5 | `TastingTagTestFactory.java` | `alcohols/fixture/` | TastingTag 이름 |
| 6 | `RatingTestFactory.java` | `rating/fixture/` | 직접 위험 낮음 (suffix 내부용) |
| 7 | `HistoryTestFactory.java` | `history/fixture/` | 직접 위험 낮음 |
| 8 | `ReviewTestFactory.java` | `review/fixture/` | Review 내부 필드 |
| 9 | `LikesTestFactory.java` | `like/fixture/` | 직접 위험 낮음 |

### 수정하지 않는 것

- `DataInitializer.java`: TRUNCATE 기반 cleanup은 정상 동작 중
- `TestDataSetupHelper.java`: Factory를 호출하는 쪽이므로 변경 불필요
- `IntegrationTestSupport.java`: `@AfterEach` cleanup 구조는 정상

---

## 3. 해결 방안

### 방안 비교

| 방안 | 유니크 보장 | 가독성 | 구현 복잡도 | 비고 |
|------|:----------:|:------:|:----------:|------|
| **A. `AtomicInteger` 카운터** | 보장 | 높음 | 낮음 | JVM 내 유일성 보장 |
| B. `UUID.randomUUID()` | 사실상 보장 | 낮음 (긴 문자열) | 낮음 | DB 필드 길이 초과 가능 |
| C. `System.nanoTime()` | 거의 보장 | 보통 | 낮음 | 동일 나노초 내 충돌 가능 |
| D. 범위 확대 (`nextInt(Integer.MAX_VALUE)`) | 거의 보장 | 높음 | 낮음 | 확률 감소만, 제거 아님 |

### 선택: A. `AtomicInteger` 카운터

**이유**: JVM 내에서 유일성이 **확정적으로** 보장되며, 생성된 suffix가 짧고 예측 가능하여 디버깅에도 유리하다.

### 변경 내용

```java
// Before (9개 파일 공통)
private final Random random = new SecureRandom();

private String generateRandomSuffix() {
    return String.valueOf(random.nextInt(10000));
}

// After
private static final AtomicInteger counter = new AtomicInteger(0);

private String generateRandomSuffix() {
    return String.valueOf(counter.incrementAndGet());
}
```

**핵심 포인트:**
- `static`: 동일 Factory 클래스의 모든 인스턴스에서 카운터 공유
- `AtomicInteger`: 멀티스레드 환경에서도 안전
- 각 Factory별 독립 카운터 → Factory 간 suffix 번호 독립
- `Random` / `SecureRandom` 필드가 더 이상 필요 없으면 삭제

---

## 4. 구현 순서

### Phase 1: 수정 (mono 모듈만)

| 순서 | 작업 | 파일 |
|------|------|------|
| 1 | `UserTestFactory` 수정 | `user/fixture/UserTestFactory.java` |
| 2 | `AdminUserTestFactory` 수정 | `user/fixture/AdminUserTestFactory.java` |
| 3 | `AlcoholTestFactory` 수정 | `alcohols/fixture/AlcoholTestFactory.java` |
| 4 | `AlcoholMetadataTestFactory` 수정 | `alcohols/fixture/AlcoholMetadataTestFactory.java` |
| 5 | `TastingTagTestFactory` 수정 | `alcohols/fixture/TastingTagTestFactory.java` |
| 6 | `RatingTestFactory` 수정 | `rating/fixture/RatingTestFactory.java` |
| 7 | `HistoryTestFactory` 수정 | `history/fixture/HistoryTestFactory.java` |
| 8 | `ReviewTestFactory` 수정 | `review/fixture/ReviewTestFactory.java` |
| 9 | `LikesTestFactory` 수정 | `like/fixture/LikesTestFactory.java` |

> 각 파일에서: (1) `Random`/`SecureRandom` 필드 → `AtomicInteger counter` 교체, (2) `generateRandomSuffix()` 메서드 본문 변경, (3) 불필요한 `Random` import 제거 + `AtomicInteger` import 추가

### Phase 2: 검증

| 순서 | 검증 항목 | 명령어 |
|------|----------|--------|
| 1 | 컴파일 | `./gradlew :bottlenote-mono:compileTestJava` |
| 2 | 코드 포맷팅 | `./gradlew :bottlenote-mono:spotlessCheck` |
| 3 | 아키텍처 규칙 | `./gradlew check_rule_test` |
| 4 | 단위 테스트 | `./gradlew unit_test` |
| 5 | Product 통합 테스트 | `./gradlew integration_test` |
| 6 | Admin 통합 테스트 | `./gradlew admin_integration_test` |

---

## 5. 참고

### 관련 문서

- `plan/complete/testfactory-improvement.md`: TestFactory 구조 개선 (2025-11-19 완료)
  - 해당 계획의 코드 예시에서 `System.nanoTime()` 방식을 제안했으나 실제 적용은 미완

### 기존 TestFactory 철학과의 관계

이번 수정은 TestFactory 5가지 철학 중 **격리(Isolation)** 원칙 보강에 해당한다:
- 격리 원칙: "팩토리 메서드 밖에서는 엔티티가 완전히 영속화된 상태"
- suffix 충돌로 persist 자체가 실패하면 격리 원칙 이전에 생성 자체가 불가능
- `AtomicInteger` 카운터로 생성 안정성을 확보하는 것이 선행 조건
