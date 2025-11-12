# TestFactory 구조 개선 계획

> 테스트 데이터 생성을 위한 TestFactory 구조 재설계

## 1. 현재 상황

### 현재 구조
```
bottlenote-product-api/src/test/java/app/bottlenote/
├── user/fixture/UserTestFactory.java
├── alcohols/fixture/AlcoholTestFactory.java
├── rating/fixture/RatingTestFactory.java
└── support/business/fixture/BusinessSupportTestFactory.java
```

### 문제점

1. **도메인별 폴더 분산**
   - 각 도메인마다 `fixture/` 폴더가 생성됨
   - TestFactory 전체 파악 어려움
   - 일관된 규칙 적용 어려움

2. **위치 불일치**
   - 엔티티는 `bottlenote-mono`에 위치
   - TestFactory는 `bottlenote-product-api`에 위치
   - 논리적 응집도 낮음

3. **명확하지 않은 작성 규칙**
   - Factory 간 의존성 처리 방법 불명확
   - 애그리거트 경계 불명확
   - Nullability 표현 부족 (JavaDoc에만 의존)

## 2. 개선 목표

### 2.1 중앙 집중 관리
- 모든 TestFactory를 한 곳에서 관리
- 일관된 작성 규칙 적용

### 2.2 논리적 응집도 향상
- 엔티티와 TestFactory를 같은 모듈(`bottlenote-mono`)에 배치
- 도메인 모델과 테스트 픽스처의 일관성 유지

### 2.3 명확한 계약
- `@NotNull`/`@Nullable` 어노테이션으로 사용법 명시
- JavaDoc 없이도 직관적 사용 가능

### 2.4 순환 의존성 차단
- Factory 간 주입 금지
- 파라미터 전달 방식으로 조합

## 3. 개선된 구조

### 3.1 디렉토리 구조

```
bottlenote-mono/src/test/java/
└── app/bottlenote/fixture/
    ├── Rule.md                       # 간단한 작성 규칙
    ├── UserTestFactory.java          # User 애그리거트
    ├── AlcoholTestFactory.java       # Alcohol 애그리거트
    ├── ReviewTestFactory.java        # Review 애그리거트
    ├── RatingTestFactory.java        # Rating 애그리거트
    ├── PickTestFactory.java          # Pick 애그리거트
    ├── LikeTestFactory.java          # Like 애그리거트
    └── scenario/                     # 복잡한 조합 (선택적)
        ├── ReviewTestScenario.java
        └── UserTestScenario.java
```

**장점:**
- ✅ 모든 TestFactory가 한눈에 보임
- ✅ 엔티티(`bottlenote-mono/domain`)와 같은 모듈
- ✅ `bottlenote-product-api`에서 `@Autowired` 주입 가능 (의존성 방향 일치)

### 3.2 애그리거트 경계

**하나의 Factory는 하나의 애그리거트를 관리:**

| Factory | 루트 엔티티 | 자식 엔티티 (같은 애그리거트) |
|---------|------------|---------------------------|
| `UserTestFactory` | User | Follow |
| `AlcoholTestFactory` | Alcohol, Region, Distillery | AlcoholsTastingTags, CurationKeyword |
| `ReviewTestFactory` | Review | ReviewReply, ReviewImage |
| `RatingTestFactory` | Rating | - |
| `PickTestFactory` | Pick | - |
| `LikeTestFactory` | Like | - |

**애그리거트 판단 기준:**
- 같은 트랜잭션 경계에서 변경되는가?
- 생명주기를 함께 관리하는가?
- 루트 없이 독립 존재 가능한가?

## 4. 핵심 설계 원칙

### 4.1 Factory 간 의존성 금지

```java
@Component
public class ReviewTestFactory {

    @Autowired private EntityManager em;

    // ❌ 절대 금지!
    // @Autowired private UserTestFactory userTestFactory;
    // @Autowired private AlcoholTestFactory alcoholTestFactory;

    // ✅ 파라미터로만 전달받음
    @Transactional
    @NotNull
    public Review persistReview(
        @NotNull User user,        // 다른 애그리거트
        @NotNull Alcohol alcohol   // 다른 애그리거트
    ) {
        Review review = Review.builder()
            .user(user)
            .alcohol(alcohol)
            .build();
        em.persist(review);
        return review;
    }
}
```

**이유:**
- 순환 의존성 방지
- 명시적 제어 가능
- 테스트 독립성 보장

### 4.2 Nullability 명시

```java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Component
public class ReviewTestFactory {

    // ✅ 모든 파라미터와 반환값에 명시
    @Transactional
    @NotNull
    public Review persistReview(
        @NotNull User user,           // 필수: null 불가
        @NotNull Alcohol alcohol,     // 필수: null 불가
        @Nullable String content      // 선택: null 허용
    ) {
        String finalContent = content != null ? content : "기본 리뷰 내용";
        Review review = Review.builder()
            .user(user)
            .alcohol(alcohol)
            .content(finalContent)
            .build();
        em.persist(review);
        return review;  // 항상 null이 아님
    }
}
```

**효과:**
- IDE에서 자동 경고
- JavaDoc 불필요
- 계약 명확화

### 4.3 메서드 명명 규칙

```java
// 기본 생성
@NotNull Review persistReview(@NotNull User user, @NotNull Alcohol alcohol)

// 선택적 필드 포함
@NotNull Review persistReview(@NotNull User user, @NotNull Alcohol alcohol, @Nullable String content)

// 빌더 패턴
@NotNull Review persistReview(@NotNull Review.ReviewBuilder builder)

// 일괄 생성
@NotNull List<Review> persistReviews(@NotNull User user, int count)

// 애그리거트 자식
@NotNull ReviewReply persistReviewReply(@NotNull Review review, @NotNull User author)
```

**패턴:**
- `persist{Entity}`: 단일 엔티티 생성
- `persist{Entities}`: 복수 엔티티 생성
- `persist{Entity}(..., @Nullable Type field)`: 선택적 필드

## 5. Factory 조합 방법

### 5.1 테스트에서 직접 조합 (기본)

```java
@SpringBootTest
class ReviewIntegrationTest {

    @Autowired private UserTestFactory userTestFactory;
    @Autowired private AlcoholTestFactory alcoholTestFactory;
    @Autowired private ReviewTestFactory reviewTestFactory;

    @Test
    void 리뷰_수정_테스트() {
        // Given: 각 Factory로 엔티티 생성 후 조합
        User author = userTestFactory.persistUser();
        Alcohol whisky = alcoholTestFactory.persistAlcohol();
        Review review = reviewTestFactory.persistReview(author, whisky);

        // When & Then
        // ...
    }
}
```

**특징:**
- 명시적 제어
- 테스트별 맞춤 데이터
- 보일러플레이트 증가 (허용 가능)

### 5.2 복잡한 시나리오 클래스 (선택적)

**여러 테스트에서 반복되는 복잡한 조합만 별도 관리:**

```java
// bottlenote-mono/src/test/java/app/bottlenote/fixture/scenario/ReviewTestScenario.java
@Component
public class ReviewTestScenario {

    @Autowired private UserTestFactory userTestFactory;
    @Autowired private AlcoholTestFactory alcoholTestFactory;
    @Autowired private ReviewTestFactory reviewTestFactory;

    /**
     * 리뷰 + 댓글 N개를 포함한 전체 시나리오
     */
    @NotNull
    public ReviewScenarioData createReviewWithReplies(int replyCount) {
        User author = userTestFactory.persistUser();
        Alcohol alcohol = alcoholTestFactory.persistAlcohol();
        Review review = reviewTestFactory.persistReview(author, alcohol);

        List<ReviewReply> replies = IntStream.range(0, replyCount)
            .mapToObj(i -> {
                User replyAuthor = userTestFactory.persistUser();
                return reviewTestFactory.persistReviewReply(review, replyAuthor);
            })
            .toList();

        return new ReviewScenarioData(author, alcohol, review, replies);
    }

    public record ReviewScenarioData(
        @NotNull User author,
        @NotNull Alcohol alcohol,
        @NotNull Review review,
        @NotNull List<ReviewReply> replies
    ) {}
}
```

**사용:**
```java
@Autowired private ReviewTestScenario reviewScenario;

@Test
void 댓글_포함_리뷰_조회() {
    ReviewScenarioData data = reviewScenario.createReviewWithReplies(5);
    // ...
}
```

**언제 사용?**
- 3개 이상 Factory 조합
- 5줄 이상 조합 로직
- 3회 이상 반복 사용

## 6. 구현 예시

### 6.1 UserTestFactory

```java
package app.bottlenote.fixture;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.Follow;
import jakarta.persistence.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserTestFactory {

    @Autowired private EntityManager em;

    @Transactional
    @NotNull
    public User persistUser() {
        User user = User.builder()
            .email("user" + System.nanoTime() + "@test.com")
            .nickName("테스터-" + System.nanoTime())
            .age(25)
            .gender(GenderType.MALE)
            .build();
        em.persist(user);
        return user;
    }

    @Transactional
    @NotNull
    public User persistUser(
        @NotNull String email,
        @NotNull String nickName
    ) {
        User user = User.builder()
            .email(email)
            .nickName(nickName)
            .age(25)
            .gender(GenderType.MALE)
            .build();
        em.persist(user);
        return user;
    }

    @Transactional
    @NotNull
    public Follow persistFollow(
        @NotNull User follower,
        @NotNull User following
    ) {
        Follow follow = Follow.builder()
            .userId(follower.getId())
            .targetUserId(following.getId())
            .status(FollowStatus.FOLLOWING)
            .build();
        em.persist(follow);
        return follow;
    }
}
```

### 6.2 ReviewTestFactory

```java
package app.bottlenote.fixture;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.user.domain.User;
import app.bottlenote.alcohols.domain.Alcohol;
import jakarta.persistence.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReviewTestFactory {

    @Autowired private EntityManager em;

    @Transactional
    @NotNull
    public Review persistReview(
        @NotNull User user,
        @NotNull Alcohol alcohol
    ) {
        Review review = Review.builder()
            .user(user)
            .alcohol(alcohol)
            .content("기본 리뷰 내용")
            .status(ReviewStatus.PUBLIC)
            .rating(4.0)
            .build();
        em.persist(review);
        return review;
    }

    @Transactional
    @NotNull
    public Review persistReview(
        @NotNull User user,
        @NotNull Alcohol alcohol,
        @Nullable String content,
        @Nullable Double rating
    ) {
        Review review = Review.builder()
            .user(user)
            .alcohol(alcohol)
            .content(content != null ? content : "기본 리뷰 내용")
            .status(ReviewStatus.PUBLIC)
            .rating(rating != null ? rating : 4.0)
            .build();
        em.persist(review);
        return review;
    }

    @Transactional
    @NotNull
    public ReviewReply persistReviewReply(
        @NotNull Review review,
        @NotNull User author,
        @Nullable String content
    ) {
        ReviewReply reply = ReviewReply.builder()
            .review(review)
            .user(author)
            .content(content != null ? content : "기본 댓글 내용")
            .build();
        em.persist(reply);
        return reply;
    }
}
```

## 7. 마이그레이션 계획

### Phase 0: 준비
1. `bottlenote-mono/src/test/java/app/bottlenote/fixture/` 디렉토리 생성
2. `Rule.md` 작성 (간단한 작성 규칙만)
3. 기존 TestFactory 중 하나(UserTestFactory) 이동 및 리팩토링
4. 테스트 실행하여 정상 작동 확인

### Phase 1: 순차 이동
1. `AlcoholTestFactory` 이동 및 리팩토리
2. `ReviewTestFactory` 이동 및 리팩토링
3. `RatingTestFactory` 이동 및 리팩토링
4. 나머지 Factory 이동

### Phase 2: 보완
1. 누락된 Factory 추가 (`PickTestFactory`, `LikeTestFactory` 등)
2. Scenario 클래스 추가 (필요시)
3. `@NotNull`/`@Nullable` 전체 적용 검토

### Phase 3: 정리
1. 기존 `bottlenote-product-api/.../fixture/` 디렉토리 삭제
2. 테스트 전체 실행 확인

## 8. Rule.md 구조 (간단 버전)

```markdown
# TestFactory 작성 규칙

## 필수 규칙
- `@Component` 선언
- `EntityManager`만 주입
- 다른 Factory 주입 금지 ❌
- `@NotNull`/`@Nullable` 모든 곳에 명시
- `persist{Entity}` 명명 패턴
- `@Transactional` 선언

## 애그리거트 경계
- UserTestFactory: User, Follow
- AlcoholTestFactory: Alcohol, Region, Distillery
- ReviewTestFactory: Review, ReviewReply, ReviewImage

## 조합 방법
- 테스트에서 여러 Factory 직접 조합
- 복잡한 경우: scenario/ 패키지
```

## 9. 체크리스트

마이그레이션 시 확인:

- [ ] 위치: `bottlenote-mono/src/test/java/app/bottlenote/fixture/`
- [ ] 패키지: `package app.bottlenote.fixture;`
- [ ] `@Component`, `EntityManager`만 주입
- [ ] 다른 Factory 주입 제거
- [ ] `@NotNull`/`@Nullable` 추가
- [ ] `persist{Entity}` 명명 통일
- [ ] 애그리거트 경계 준수
- [ ] 테스트 정상 실행

## 10. 기대 효과

### 개발자 경험 개선
- 모든 TestFactory를 한눈에 파악
- 일관된 사용법 (IDE 자동 완성 지원)
- 명확한 계약 (`@NotNull`/`@Nullable`)

### 코드 품질 향상
- 순환 의존성 차단
- 애그리거트 경계 명확화
- 테스트 독립성 보장

### 유지보수성 향상
- 중앙 집중 관리
- 통일된 작성 규칙
- 명시적 조합 방식
