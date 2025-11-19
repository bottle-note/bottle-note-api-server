# 테스트 엔티티 팩토리 위반 사항 및 개선 계획

> 분석 일자: 2025-11-19
> 대상: bottlenote-mono/src/test/java/app/bottlenote/fixture/

## 📊 전체 준수 현황

| Factory | 단일책임 | 격리 | 순수성 | 명시성 | 응집성 | 종합 점수 |
|---------|:--------:|:----:|:------:|:------:|:------:|:---------:|
| UserTestFactory | ✅ | ⚠️ 45% | ✅ | ❌ 0% | ✅ | **3/5** |
| AlcoholTestFactory | ⚠️ | ❌ 32% | ✅ | ❌ 0% | ✅ | **2/5** |
| RatingTestFactory | ✅ | ❌ 40% | ❌ | ❌ 0% | ✅ | **2/5** |
| BusinessSupportTestFactory | ✅ | ✅ 100% | ✅ | ❌ 0% | ✅ | **4/5** ⭐ |

**모범 사례**: BusinessSupportTestFactory

**전체 통계**:
- persist 메서드 총 35개
- flush 호출: 14개 (40%)
- @NotNull/@Nullable: 0개 (0%)

---

## 🚨 Priority 1: 격리 원칙 위반 (flush 미호출 - 21개)

### UserTestFactory (6개 위반)

```java
// ❌ Line 30-42: persistUser()
public User persistUser() {
    User user = User.builder()...build();
    em.persist(user);
    // em.flush(); ← 누락!
    return user;
}

// ❌ Line 62-69: persistUser(User.UserBuilder builder)
// ❌ Line 100-109: persistFollow(Long followerId, Long followingId)
// ❌ Line 113-119: persistFollow(Follow.FollowBuilder builder)
```

**개선 필요 라인**: 41, 68, 108, 118

---

### AlcoholTestFactory (13개 위반)

```java
// ❌ Line 33-42: persistRegion()
// ❌ Line 46-54: persistRegion(String, String)
// ❌ Line 57-62: persistRegion(builder)
// ❌ Line 66-75: persistDistillery()
// ❌ Line 79-87: persistDistillery(String, String)
// ❌ Line 91-95: persistDistillery(builder)
// ❌ Line 99-120: persistAlcohol()
// ❌ Line 123-127: persistAlcohols(int)
// ❌ Line 131-152: persistAlcohol(AlcoholType)
// ❌ Line 156-177: persistAlcohol(String, String, AlcoholType)
// ❌ Line 181-202: persistAlcoholWithName()
// ❌ Line 206-223: persistAlcohol(AlcoholType, Region, Distillery)
// ❌ Line 227-233: persistAlcohol(builder)
```

**개선 필요 라인**: 41, 53, 61, 74, 86, 94, 119, 126, 151, 176, 201, 222, 232

---

### RatingTestFactory (3개 위반)

```java
// ❌ Line 30-38: persistRating(User, Alcohol, int)
// ❌ Line 42-50: persistRating(Long, Long, int)
// ❌ Line 54-60: persistRating(builder)
```

**개선 필요 라인**: 37, 49, 59

---

## 🚨 Priority 2: 순수성 원칙 위반 (Repository 의존성)

### RatingTestFactory

```java
// ❌ Line 24: Repository 주입
@Deprecated @Autowired private JpaRatingRepository ratingRepository;

// ❌ Line 76-83: Repository 사용 메서드
@Deprecated
public void createRating(User user, Alcohol alcohol, int point) {
    Rating rating = Rating.builder()...build();
    ratingRepository.saveAndFlush(rating); // ← Repository 사용
}
```

**개선 방안**:
- Line 24 삭제 (Repository 필드)
- Line 76-83 삭제 (createRating 메서드, 이미 @Deprecated)

---

## 🚨 Priority 3: 명시성 원칙 위반 (@NotNull/@Nullable 전무)

### 전체 Factory (35개 메서드 모두)

**현재**:
```java
// ❌ 명시성 없음
public User persistUser(String email, String nickName)
```

**개선 후**:
```java
// ✅ 명시성 명확
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NotNull
public User persistUser(
    @NotNull String email,
    @Nullable String nickName
)
```

**개선 필요**:
- 모든 public 메서드의 반환값에 `@NotNull` 추가
- 모든 파라미터에 `@NotNull` 또는 `@Nullable` 추가

---

## ⚠️ Priority 4: 단일 책임 원칙 위반

### AlcoholTestFactory

```java
// ❌ Line 371-385: 조회 로직 (Factory 역할 벗어남)
@Transactional
public Set<AlcoholsTastingTags> getAlcoholTastingTags(Long alcoholId) {
    List<AlcoholsTastingTags> result = em.createQuery(...)
        .getResultList();
    return new HashSet<>(result);
}

// ❌ Line 394-441: 복잡한 비즈니스 로직 (@Deprecated)
@Deprecated(since = "2025-01", forRemoval = true)
@Transactional
public void appendTagsFromKeywordMapping(Long alcoholId, KeywordTagMapping mapping) {
    // 삭제, 생성, 매핑을 포함한 복잡한 로직
}
```

**개선 방안**:
- `getAlcoholTastingTags()` 제거 또는 별도 Helper 클래스로 이동
- `appendTagsFromKeywordMapping()` 삭제 (이미 @Deprecated)

---

## ✅ 모범 사례: BusinessSupportTestFactory

```java
@Component
@RequiredArgsConstructor
public class BusinessSupportTestFactory {

  @Autowired private EntityManager em; // ✅ EntityManager만 주입

  @Transactional
  public BusinessSupport persist(Long userId) { // ⚠️ @NotNull 필요
    BusinessSupport bs = BusinessSupport.create(...);
    em.persist(bs);
    em.flush(); // ✅ 격리 원칙 준수
    return bs;
  }
}
```

**장점**:
- ✅ 간결함 (26줄)
- ✅ 명확한 메서드 (persist)
- ✅ 일관된 flush 호출
- ✅ Repository 의존성 없음

**단점**:
- ⚠️ @NotNull/@Nullable 어노테이션 누락

---

## 📋 개선 작업 계획

### Phase 1: 긴급 수정 (격리 + 순수성)

1. **모든 persist 메서드에 em.flush() 추가** (21개)
   - UserTestFactory: 6개
   - AlcoholTestFactory: 13개
   - RatingTestFactory: 3개

2. **RatingTestFactory Repository 의존성 제거**
   - JpaRatingRepository 필드 삭제
   - createRating() 메서드 삭제

### Phase 2: 명시성 개선

3. **@NotNull/@Nullable 전체 적용** (35개 메서드)
   - import 추가: `org.jetbrains.annotations.*`
   - 모든 반환값에 @NotNull
   - 모든 파라미터에 적절한 어노테이션

### Phase 3: 단일 책임 정리

4. **AlcoholTestFactory 비즈니스 로직 제거**
   - getAlcoholTastingTags() 처리
   - appendTagsFromKeywordMapping() 삭제

---

## 🎯 개선 후 기대 효과

- ✅ 격리 원칙 준수율: 40% → 100%
- ✅ 순수성 원칙 준수: 3/4 → 4/4
- ✅ 명시성 원칙 준수: 0% → 100%
- ✅ 전체 철학 준수율: 2.5/5 → 4.75/5

---

## 체크리스트

개선 작업 시 확인:

- [ ] 모든 persist 메서드에 `em.flush()` 호출 확인
- [ ] Repository 의존성 완전 제거
- [ ] @NotNull/@Nullable import 추가
- [ ] 모든 public 메서드 반환값에 @NotNull
- [ ] 모든 파라미터에 적절한 null 가능성 표시
- [ ] 비즈니스 로직 제거
- [ ] 테스트 실행하여 정상 동작 확인
