# Mono Module Patterns

## Repository 3-Tier Pattern

### 1. Domain Repository (Required)
Pure business interface - no Spring/JPA dependency.

```java
// Location: {domain}/domain/{Domain}Repository.java
@DomainRepository
public interface RatingRepository {
    Rating save(Rating rating);
    Optional<Rating> findByAlcoholIdAndUserId(Long alcoholId, Long userId);
}
```

### 2. JPA Repository (Required)
Implements domain repo + extends JpaRepository.

```java
// Location: {domain}/repository/Jpa{Domain}Repository.java
@JpaRepositoryImpl
public interface JpaRatingRepository
    extends JpaRepository<Rating, Long>, RatingRepository, CustomRatingRepository {
}
```

### 3. QueryDSL Custom Repository (Optional - complex queries only)

```java
// Interface: {domain}/repository/Custom{Domain}Repository.java
public interface CustomRatingRepository {
    PageResponse<RatingListFetchResponse> fetchRatingList(RatingListFetchCriteria criteria);
}

// Implementation: {domain}/repository/Custom{Domain}RepositoryImpl.java
public class CustomRatingRepositoryImpl implements CustomRatingRepository {
    // JPAQueryFactory injection, BooleanBuilder for dynamic conditions
}

// Query supporter: {domain}/repository/{Domain}QuerySupporter.java
@Component
public class RatingQuerySupporter {
    // Reusable query fragments
}
```

**Use QueryDSL only for:** dynamic multi-condition filters, multi-table joins, complex projections.
**Do NOT use for:** simple CRUD, single-condition lookups (use method query or @Query JPQL).

## Service Pattern

서비스는 `{Domain}Service` 하나로 작성하는 것이 기본이다.
기존 코드에 Command/Query 분리(`CommandService`/`QueryService`)가 있지만 필수 패턴이 아니며, 신규 구현 시 하나로 작성해도 됨. 기존 분리된 서비스를 굳이 합칠 필요는 없음.

```java
@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final AlcoholFacade alcoholFacade; // 타 도메인 접근은 반드시 Facade를 통해

    @Transactional
    public RatingRegisterResponse register(Long alcoholId, Long userId, RatingPoint ratingPoint) {
        Objects.requireNonNull(alcoholId, "alcoholId must not be null");

        // 타 도메인 검증 - AlcoholFacade를 통해 요청
        if (FALSE.equals(alcoholFacade.existsByAlcoholId(alcoholId))) {
            throw new RatingException(RatingExceptionCode.ALCOHOL_NOT_FOUND);
        }

        // 자기 도메인 로직
        Rating rating = ratingRepository.findByAlcoholIdAndUserId(alcoholId, userId)
            .orElse(Rating.builder().alcoholId(alcoholId).userId(userId).build());
        rating.registerRatingPoint(ratingPoint);
        ratingRepository.save(rating);

        // 이벤트 발행 (부수 효과)
        eventPublisher.publishEvent(new RatingRegistryEvent(alcoholId, userId));

        return new RatingRegisterResponse(rating.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<RatingListFetchResponse> fetchList(RatingListFetchCriteria criteria) {
        // 읽기 전용 메서드는 readOnly = true
    }
}
```

## Aggregate Root & Facade Pattern

### Aggregate 개념

도메인은 Aggregate 단위로 묶인다. 절대적인 규칙은 아니지만 개념적 경계로 활용한다.

```
ranking (Aggregate Root)
├── RankingService          ← 외부에서 접근 가능 (Facade를 통해)
├── RankingPointService     ← 내부 구현, 외부 접근 불가
├── RankingHistoryService   ← 내부 구현, 외부 접근 불가
└── RankingFacade           ← 외부에 노출하는 유일한 창구
```

외부 도메인은 Aggregate Root(= Facade)를 통해서만 접근한다. Aggregate 내부의 하위 서비스에 직접 접근하면 안 된다.

```
[OK]  UserService → RankingFacade (Aggregate Root 접근)
[OK]  UserProfileService → RankingFacade (Aggregate Root 접근)
[NO]  UserService → RankingPointService (하위 도메인 직접 접근)
[NO]  UserProfileService → RankingHistoryService (하위 도메인 직접 접근)
```

### Facade의 역할

Facade는 Aggregate Root로서 도메인 간 경계를 보호한다.

**왜 필요한가:**
- UserService가 RankingPointService를 직접 호출하면, Ranking 내부 구조 변경 시 UserService도 깨짐
- RankingFacade를 통해 요청하면, Ranking이 내부 구현(서비스 분리, 테이블 구조, 캐시 전략)을 자유롭게 변경 가능
- Facade 인터페이스는 해당 도메인이 외부에 노출하는 계약(contract)

**원칙:**
- 같은 Aggregate 내에서는 Repository/Service를 직접 사용
- 다른 Aggregate의 데이터가 필요하면 반드시 해당 Aggregate의 Facade를 통해 접근
- Facade는 외부에 필요한 최소한의 인터페이스만 노출

```
[OK]  UserService → UserRepository (같은 Aggregate)
[OK]  UserService → AlcoholFacade (타 Aggregate의 Facade)
[NO]  UserService → AlcoholRepository (타 Aggregate 직접 접근)
[NO]  UserService → RankingPointService (타 Aggregate 하위 서비스 직접 호출)
```

```java
// Interface: {domain}/facade/{Domain}Facade.java
public interface AlcoholFacade {
    Boolean existsByAlcoholId(Long alcoholId);
    AlcoholInfo getAlcoholInfo(Long alcoholId);
}

// Implementation: {domain}/service/Default{Domain}Facade.java
@FacadeService
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultAlcoholFacade implements AlcoholFacade {
    private final AlcoholRepository alcoholRepository;
    // 자기 도메인의 Repository만 사용
}
```

## DTO Patterns

```java
// Request with validation
public record RatingRegisterRequest(
    @NotNull Long alcoholId,
    @NotNull Double rating
) {}

// Pageable request with defaults
public record ReviewPageableRequest(
    ReviewSortType sortType, SortOrder sortOrder, Long cursor, Long pageSize
) {
    @Builder
    public ReviewPageableRequest {
        sortType = sortType != null ? sortType : ReviewSortType.POPULAR;
        cursor = cursor != null ? cursor : 0L;
        pageSize = pageSize != null ? pageSize : 10L;
    }
}

// Response with factory method
public record RatingListFetchResponse(Long totalCount, List<Info> ratings) {
    public record Info(Long ratingId, Long alcoholId, Double rating) {}
    public static RatingListFetchResponse create(Long total, List<Info> infos) {
        return new RatingListFetchResponse(total, infos);
    }
}
```

## Exception Pattern

```java
// {domain}/exception/{Domain}Exception.java
public class RatingException extends AbstractCustomException {
    public RatingException(RatingExceptionCode code) {
        super(code);
    }
}

// {domain}/exception/{Domain}ExceptionCode.java
@Getter
public enum RatingExceptionCode implements ExceptionCode {
    INVALID_RATING_POINT(HttpStatus.BAD_REQUEST, "invalid rating point"),
    ALCOHOL_NOT_FOUND(HttpStatus.NOT_FOUND, "alcohol not found");

    private final HttpStatus httpStatus;
    private final String message;

    RatingExceptionCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
```

## Event Pattern

```java
// Event record
public record RatingRegistryEvent(Long alcoholId, Long userId) {}

// Listener
@DomainEventListener
@RequiredArgsConstructor
public class RatingEventListener {
    @TransactionalEventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRatingRegistry(RatingRegistryEvent event) {
        // Side effects in separate transaction
    }
}
```

## Cursor Pagination

```java
// In service/repository
public PageResponse<T> fetchList(Criteria criteria) {
    List<T> items = queryFactory.selectFrom(...)
        .where(cursorCondition(criteria.cursor()))
        .limit(criteria.pageSize() + 1) // fetch one extra to detect hasNext
        .fetch();

    CursorPageable pageable = CursorPageable.of(items, criteria.cursor(), criteria.pageSize());
    return PageResponse.of(items.subList(0, Math.min(items.size(), criteria.pageSize())), pageable);
}

// In controller
MetaInfos metaInfos = MetaService.createMetaInfo();
metaInfos.add("pageable", response.cursorPageable());
metaInfos.add("searchParameters", request);
return GlobalResponse.ok(response, metaInfos);
```
