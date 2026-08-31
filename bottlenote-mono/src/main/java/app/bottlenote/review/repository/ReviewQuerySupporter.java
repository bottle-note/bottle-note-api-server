package app.bottlenote.review.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.global.search.SearchKeywordLikePattern.ESCAPE;
import static app.bottlenote.global.search.SearchKeywordLikePattern.contains;
import static app.bottlenote.global.service.cursor.SortOrder.DESC;
import static app.bottlenote.like.constant.LikeStatus.LIKE;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.review.constant.ReviewReplyStatus.NORMAL;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.review.domain.QReviewTastingTag.reviewTastingTag;
import static app.bottlenote.user.domain.QUser.user;
import static com.querydsl.jpa.JPAExpressions.select;

import app.bottlenote.global.pagination.CursorClaims;
import app.bottlenote.global.pagination.CursorKeys;
import app.bottlenote.global.pagination.PaginationException;
import app.bottlenote.global.pagination.PaginationExceptionCode;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.constant.AdminReviewSortType;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.constant.ReviewSortType;
import app.bottlenote.review.constant.SizeType;
import app.bottlenote.review.dto.request.AdminReviewSearchRequest;
import app.bottlenote.review.facade.payload.ReviewInfo;
import app.bottlenote.review.facade.payload.UserInfo;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.util.StringUtils;
import com.querydsl.jpa.JPAExpressions;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ReviewQuerySupporter {

  public static ConstructorExpression<UserInfo> getUserInfo() {
    return Projections.constructor(
        UserInfo.class,
        user.id.as("userId"),
        user.nickName.as("nickName"),
        user.imageUrl.as("userProfileImage"));
  }

  public static Expression<String> getTastingTag() {
    return ExpressionUtils.as(
        JPAExpressions.select(
                Expressions.stringTemplate("group_concat({0})", reviewTastingTag.tastingTag))
            .from(reviewTastingTag)
            .where(reviewTastingTag.review.id.eq(review.id)),
        "tastingTag");
  }

  /** 내가 댓글을 단 리뷰인지 판별 */
  public static BooleanExpression hasReplyByMeSubquery(Long userId) {

    BooleanExpression eqUserId =
        1 > userId ? reviewReply.userId.isNull() : reviewReply.userId.eq(userId);

    return Expressions.asBoolean(
            JPAExpressions.selectOne()
                .from(reviewReply)
                .where(
                    reviewReply
                        .reviewId
                        .eq(review.id)
                        .and(eqUserId.and(reviewReply.status.eq(NORMAL))))
                .exists())
        .as("hasReplyByMe");
  }

  /***
   * 내가 좋아요를 누른 리뷰인지 판별
   */
  public static BooleanExpression isLikeByMeSubquery(Long userId) {
    if (userId < 1) {
      return Expressions.asBoolean(false);
    }
    return Expressions.asBoolean(
            JPAExpressions.selectOne()
                .from(likes)
                .where(
                    likes
                        .reviewId
                        .eq(review.id)
                        .and(likes.userInfo.userId.eq(userId))
                        .and(likes.status.eq(LIKE)))
                .exists())
        .as("isLikedByMe");
  }

  /***
   * 내가 작성한 리뷰인지 판별
   */
  public static BooleanExpression isMyReview(Long userId) {
    if (Objects.isNull(userId) || 1 > userId) {
      return Expressions.asBoolean(false);
    }
    return review.userId.eq(userId).as("isMyReview");
  }

  public static List<OrderSpecifier<?>> sortBy(ReviewSortType reviewSortType, SortOrder sortOrder) {
    NumberExpression<Long> likesCount = distinctLikesCount();
    // 모든 tie-breaker는 요청 방향을 따라야 keyset 연속성과 목록 순서가 일치한다.
    OrderSpecifier<?> createAt = sortOrder == DESC ? review.createAt.desc() : review.createAt.asc();
    OrderSpecifier<?> reviewId = sortOrder == DESC ? review.id.desc() : review.id.asc();
    return switch (reviewSortType) {
      // 최신순
      case LATEST -> Arrays.asList(createAt, reviewId);
      // 인기순 -> 임시로 좋아요 순으로 구현
      case POPULAR ->
          Arrays.asList(
              new OrderSpecifier<>(sortOrder == DESC ? Order.DESC : Order.ASC, review.isBest)
                  .nullsLast(),
              new OrderSpecifier<>(sortOrder == DESC ? Order.DESC : Order.ASC, likesCount)
                  .nullsLast(),
              createAt,
              reviewId);
      // 좋아요 순
      case LIKES ->
          Arrays.asList(
              sortOrder == DESC ? likesCount.desc() : likesCount.asc(), createAt, reviewId);

      // 별점 순 — 목록에 내려주는 review.reviewRating과 같은 컬럼으로 정렬한다
      case RATING ->
          Arrays.asList(
              sortOrder == DESC ? review.reviewRating.desc() : review.reviewRating.asc(),
              createAt,
              reviewId);

      // 병 기준 가격 순
      case BOTTLE_PRICE -> {
        OrderSpecifier<?> sizeOrderSpecifier =
            new OrderSpecifier<>(Order.ASC, review.sizeType).nullsLast();

        OrderSpecifier<?> priceOrderSpecifier =
            new OrderSpecifier<>(sortOrder == DESC ? Order.DESC : Order.ASC, review.price);
        yield Arrays.asList(sizeOrderSpecifier, priceOrderSpecifier, createAt, reviewId);
      }

      // 잔 기준 가격 순
      case GLASS_PRICE -> {
        OrderSpecifier<?> sizeOrderSpecifier =
            new OrderSpecifier<>(Order.DESC, review.sizeType).nullsLast();

        OrderSpecifier<?> priceOrderSpecifier =
            new OrderSpecifier<>(sortOrder == DESC ? Order.DESC : Order.ASC, review.price);
        yield Arrays.asList(sizeOrderSpecifier, priceOrderSpecifier, createAt, reviewId);
      }
    };
  }

  /**
   * {@link #sortBy}가 만드는 ORDER BY와 정확히 대응하는 keyset seek 조건을 만든다. ORDER BY의 각 컬럼을 앞에서부터 재귀적으로 비교하는
   * 튜플 비교이며, createAt과 review.id tie-breaker는 요청 정렬 방향을 따른다.
   */
  public static BooleanExpression keysetSeek(
      ReviewSortType sortType, SortOrder sortOrder, CursorClaims claims) {
    if (claims == null) {
      return null;
    }
    boolean desc = sortOrder != SortOrder.ASC;
    return switch (sortType) {
      case LATEST -> timeIdSeek(desc, claims);
      case POPULAR -> popularSeek(desc, claims);
      case LIKES -> likesSeek(desc, claims);
      case RATING -> ratingSeek(desc, claims);
      case BOTTLE_PRICE -> sizePriceSeek(desc, SizeType.BOTTLE, claims);
      case GLASS_PRICE -> sizePriceSeek(desc, SizeType.GLASS, claims);
    };
  }

  /** 해당 정렬이 실제로 사용하는 모든 sort 값을 커서에 담는다. 값이 NULL이면 키 자체를 넣지 않는다. */
  public static Map<String, String> cursorKeys(ReviewSortType sortType, ReviewInfo item) {
    Map<String, String> keys = new LinkedHashMap<>();
    switch (sortType) {
      case LATEST -> {}
      case POPULAR -> {
        putIfNotNull(keys, "best", item.isBestReview());
        // COUNT는 SQL에서 NULL이 아니라 0이므로 키를 생략하지 않는다
        keys.put("likes", String.valueOf(likeCountOrZero(item)));
      }
      case LIKES -> keys.put("likes", String.valueOf(likeCountOrZero(item)));
      case RATING -> putIfNotNull(keys, "rating", item.rating());
      case BOTTLE_PRICE, GLASS_PRICE -> {
        putIfNotNull(keys, "sizeType", item.sizeType() == null ? null : item.sizeType().name());
        putIfNotNull(keys, "price", item.price() == null ? null : item.price().toPlainString());
      }
    }
    keys.put("t", item.createAt().toString());
    keys.put("id", String.valueOf(item.reviewId()));
    return keys;
  }

  private static long likeCountOrZero(ReviewInfo item) {
    return item.likeCount() == null ? 0L : item.likeCount();
  }

  private static void putIfNotNull(Map<String, String> keys, String key, Object value) {
    if (value != null) {
      keys.put(key, String.valueOf(value));
    }
  }

  // POPULAR: isBest(nullsLast) -> likes.id.countDistinct()(nullsLast) -> createAt(dir) -> id(dir)
  private static BooleanExpression popularSeek(boolean desc, CursorClaims claims) {
    BooleanExpression tail = timeIdSeek(desc, claims);
    NumberExpression<Long> likesCount = distinctLikesCount();
    BooleanExpression likesStep =
        nullsLastNumberStep(likesCount, desc, CursorKeys.optionalLong(claims, "likes"), tail);
    return nullsLastBooleanStep(review.isBest, desc, optionalBoolean(claims, "best"), likesStep);
  }

  // LIKES: likes.id.countDistinct() -> createAt(dir) -> id(dir)
  private static BooleanExpression likesSeek(boolean desc, CursorClaims claims) {
    BooleanExpression tail = timeIdSeek(desc, claims);
    NumberExpression<Long> likesCount = distinctLikesCount();
    return plainNumberStep(likesCount, desc, CursorKeys.requireLong(claims, "likes"), tail);
  }

  // 조인 카테시안에서 좋아요가 부풀지 않게 PK 기준 DISTINCT로 센다
  public static NumberExpression<Long> distinctLikesCount() {
    return likes.id.countDistinct();
  }

  // RATING: review.reviewRating -> createAt(dir) -> id(dir). 컬럼이 nullable이라 MySQL 기본 NULL 순서를 따른다.
  private static BooleanExpression ratingSeek(boolean desc, CursorClaims claims) {
    BooleanExpression tail = timeIdSeek(desc, claims);
    return nativeNullableNumberStep(
        review.reviewRating, desc, CursorKeys.optionalDouble(claims, "rating"), tail);
  }

  // BOTTLE_PRICE/GLASS_PRICE: sizeType(nullsLast, 고정 방향) -> price(dir) -> createAt(dir) -> id(dir)
  private static BooleanExpression sizePriceSeek(
      boolean desc, SizeType rankFirst, CursorClaims claims) {
    BooleanExpression tail = timeIdSeek(desc, claims);
    BooleanExpression priceStep =
        nativeNullableNumberStep(review.price, desc, optionalPrice(claims), tail);
    return nullsLastSizeTypeStep(
        review.sizeType, rankFirst, optionalSizeType(claims, "sizeType"), priceStep);
  }

  // 모든 정렬의 공통 타이브레이커는 정렬 방향을 그대로 따른다.
  private static BooleanExpression timeIdSeek(boolean desc, CursorClaims claims) {
    LocalDateTime createAt = CursorKeys.requireTime(claims, "t");
    Long id = CursorKeys.requireLong(claims, "id");
    return desc
        ? review.createAt.lt(createAt).or(review.createAt.eq(createAt).and(review.id.lt(id)))
        : review.createAt.gt(createAt).or(review.createAt.eq(createAt).and(review.id.gt(id)));
  }

  /** nullsLast()가 붙은 숫자 표현식에 대한 keyset seek 한 단계. */
  private static <T extends Number & Comparable<?>> BooleanExpression nullsLastNumberStep(
      NumberExpression<T> expr, boolean desc, T cursorValue, BooleanExpression next) {
    if (cursorValue == null) {
      return expr.isNull().and(next);
    }
    BooleanExpression advances =
        (desc ? expr.lt(cursorValue) : expr.gt(cursorValue)).or(expr.isNull());
    return advances.or(expr.eq(cursorValue).and(next));
  }

  /** NULL이 나오지 않는 숫자 표현식에 대한 keyset seek 한 단계. */
  private static <T extends Number & Comparable<?>> BooleanExpression plainNumberStep(
      NumberExpression<T> expr, boolean desc, T cursorValue, BooleanExpression next) {
    BooleanExpression advances = desc ? expr.lt(cursorValue) : expr.gt(cursorValue);
    return advances.or(expr.eq(cursorValue).and(next));
  }

  /** nullsLast()가 붙은 불리언 표현식(isBest)에 대한 keyset seek 한 단계. */
  private static BooleanExpression nullsLastBooleanStep(
      BooleanExpression expr, boolean desc, Boolean cursorValue, BooleanExpression next) {
    if (cursorValue == null) {
      return expr.isNull().and(next);
    }
    boolean strictBeyondExists = desc == cursorValue;
    BooleanExpression advances =
        strictBeyondExists ? expr.eq(!cursorValue).or(expr.isNull()) : expr.isNull();
    return advances.or(expr.eq(cursorValue).and(next));
  }

  /** nullsLast()가 붙은 sizeType 표현식에 대한 keyset seek 한 단계. rankFirst가 정렬상 먼저 오는 값이다. */
  private static BooleanExpression nullsLastSizeTypeStep(
      EnumPath<SizeType> expr, SizeType rankFirst, SizeType cursorValue, BooleanExpression next) {
    if (cursorValue == null) {
      return expr.isNull().and(next);
    }
    SizeType rankSecond = rankFirst == SizeType.BOTTLE ? SizeType.GLASS : SizeType.BOTTLE;
    BooleanExpression advances =
        cursorValue == rankFirst ? expr.eq(rankSecond).or(expr.isNull()) : expr.isNull();
    return advances.or(expr.eq(cursorValue).and(next));
  }

  /**
   * nullsLast()가 없고 leftJoin으로 실제 NULL이 나오는 숫자 표현식(rating)에 대한 keyset seek. MySQL 기본 정렬은 DESC일 때
   * NULL이 맨 뒤(nullsLast와 동일), ASC일 때 NULL이 맨 앞(nullsFirst)에 온다.
   */
  private static <T extends Number & Comparable<?>> BooleanExpression nativeNullableNumberStep(
      NumberExpression<T> expr, boolean desc, T cursorValue, BooleanExpression next) {
    if (desc) {
      return nullsLastNumberStep(expr, true, cursorValue, next);
    }
    if (cursorValue == null) {
      return expr.isNotNull().or(expr.isNull().and(next));
    }
    return expr.gt(cursorValue).or(expr.eq(cursorValue).and(next));
  }

  private static Boolean optionalBoolean(CursorClaims claims, String key) {
    String raw = CursorKeys.optional(claims, key);
    return raw == null ? null : Boolean.valueOf(raw);
  }

  private static SizeType optionalSizeType(CursorClaims claims, String key) {
    String raw = CursorKeys.optional(claims, key);
    if (raw == null) {
      return null;
    }
    try {
      return SizeType.valueOf(raw);
    } catch (IllegalArgumentException exception) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
  }

  private static BigDecimal optionalPrice(CursorClaims claims) {
    String raw = CursorKeys.optional(claims, "price");
    if (raw == null) {
      return null;
    }
    try {
      return new BigDecimal(raw);
    } catch (NumberFormatException exception) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
  }

  public static BooleanExpression[] adminReviewFilters(AdminReviewSearchRequest request) {
    return new BooleanExpression[] {
      alcoholIdEq(request.alcoholId()),
      userIdEq(request.userId()),
      activeStatusEq(request.activeStatus()),
      displayStatusEq(request.displayStatus()),
      adminKeywordContains(request.keyword()),
      createdFromGoe(request.createdFrom()),
      createdToLoe(request.createdTo())
    };
  }

  public static List<OrderSpecifier<?>> adminReviewSortBy(
      AdminReviewSortType sortType, SortOrder sortOrder) {
    Order order = sortOrder == SortOrder.ASC ? Order.ASC : Order.DESC;
    NumberExpression<Long> replyCount = reviewReply.id.countDistinct();
    OrderSpecifier<?> latestReview = review.createAt.desc();
    OrderSpecifier<?> latestReviewId = review.id.desc();

    OrderSpecifier<?> primary =
        switch (sortType) {
          case CREATED_AT -> new OrderSpecifier<>(order, review.createAt);
          case REPLY_COUNT -> new OrderSpecifier<>(order, replyCount);
          case UPDATED_AT -> new OrderSpecifier<>(order, review.lastModifyAt);
        };

    return Arrays.asList(primary, latestReview, latestReviewId);
  }

  private static BooleanExpression alcoholIdEq(Long alcoholId) {
    return alcoholId != null ? review.alcoholId.eq(alcoholId) : null;
  }

  private static BooleanExpression userIdEq(Long userId) {
    return userId != null ? review.userId.eq(userId) : null;
  }

  private static BooleanExpression activeStatusEq(ReviewActiveStatus activeStatus) {
    return activeStatus != null ? review.activeStatus.eq(activeStatus) : null;
  }

  private static BooleanExpression displayStatusEq(ReviewDisplayStatus displayStatus) {
    return displayStatus != null ? review.status.eq(displayStatus) : null;
  }

  private static BooleanExpression adminKeywordContains(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    String value = "%" + keyword.trim() + "%";
    return review
        .content
        .likeIgnoreCase(value)
        .or(user.nickName.likeIgnoreCase(value))
        .or(user.email.likeIgnoreCase(value))
        .or(alcohol.korName.likeIgnoreCase(value))
        .or(alcohol.engName.likeIgnoreCase(value));
  }

  private static BooleanExpression createdFromGoe(java.time.LocalDateTime createdFrom) {
    return createdFrom != null ? review.createAt.goe(createdFrom) : null;
  }

  private static BooleanExpression createdToLoe(java.time.LocalDateTime createdTo) {
    return createdTo != null ? review.createAt.loe(createdTo) : null;
  }

  /** 검색 토큰을 이용해 작성자, 주류 정보, 리뷰 콘텐츠, 테이스팅 태그를 모두 검색하는 조건 생성 */
  public static BooleanExpression containsAllSearchTokens(List<String> searchTokens) {
    if (searchTokens == null || searchTokens.isEmpty()) {
      return null;
    }
    BooleanExpression finalCondition = null;

    // 각 토큰에 대해 개별 조건을 생성하고 AND 연산으로 결합
    for (String searchToken : searchTokens) {
      if (StringUtils.isNullOrEmpty(searchToken)) {
        continue;
      }
      BooleanExpression keywordCondition =
          user.nickName
              .likeIgnoreCase(contains(searchToken), ESCAPE)
              .or(alcohol.korName.likeIgnoreCase(contains(searchToken), ESCAPE))
              .or(alcohol.engName.likeIgnoreCase(contains(searchToken), ESCAPE))
              .or(review.content.likeIgnoreCase(contains(searchToken), ESCAPE));

      BooleanExpression reviewTastingTagCondition =
          JPAExpressions.selectOne()
              .from(reviewTastingTag)
              .where(
                  reviewTastingTag.review.id.eq(review.id),
                  reviewTastingTag.tastingTag.likeIgnoreCase(contains(searchToken), ESCAPE))
              .exists();

      keywordCondition = keywordCondition.or(reviewTastingTagCondition);
      if (finalCondition == null) {
        finalCondition = keywordCondition;
      } else {
        finalCondition = finalCondition.and(keywordCondition);
      }
    }

    return finalCondition;
  }

  /**
   * 마이 페이지 사용자의 리뷰 개수를 조회한다.
   *
   * @param userId 마이 페이지 사용자
   * @return 리뷰 개수
   */
  public Expression<Long> reviewCountSubQuery(NumberPath<Long> userId) {
    return ExpressionUtils.as(
        select(review.count())
            .from(review)
            .where(review.userId.eq(userId).and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE))),
        "reviewCount");
  }
}
