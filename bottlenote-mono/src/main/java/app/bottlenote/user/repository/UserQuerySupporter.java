package app.bottlenote.user.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.user.domain.QFollow.follow;
import static com.querydsl.jpa.JPAExpressions.select;

import app.bottlenote.global.pagination.CursorClaims;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.TimeIdCursor;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.constant.MyBottleSortType;
import app.bottlenote.user.constant.MyBottleType;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserQuerySupporter {

  private final app.bottlenote.alcohols.domain.RegionRepository regionRepository;

  /**
   * 마이 페이지 사용자의 팔로워 수 를 조회한다.
   *
   * @param userId 마이 페이지 사용자
   * @return 팔로워 수
   */
  public Expression<Long> followerCountSubQuery(NumberPath<Long> userId) {
    return ExpressionUtils.as(
        select(follow.count())
            .from(follow)
            .where(follow.userId.eq(userId).and(follow.status.eq(FollowStatus.FOLLOWING))),
        "followCount");
  }

  /**
   * 마이 페이지 사용자가 팔로잉 하는 유저 수를 조회한다.
   *
   * @param userId 마이 페이지 사용자
   * @return 팔로잉 수
   */
  public Expression<Long> followingCountSubQuery(NumberPath<Long> userId) {
    return ExpressionUtils.as(
        select(follow.count())
            .from(follow)
            .where(follow.targetUserId.eq(userId).and(follow.status.eq(FollowStatus.FOLLOWING))),
        "followerCount");
  }

  /**
   * 로그인 사용자가 마이 페이지 사용자를 팔로우 하고 있는지 상태 여부를 조회한다.
   *
   * @param userId 마이 페이지 사용자
   * @param currentUserId 로그인 사용자
   * @return 팔로우 여부 (true : 팔로우 중, false : 팔로우 중이 아님)
   */
  public BooleanExpression isFollowSubQuery(NumberPath<Long> userId, Long currentUserId) {
    return select(follow.count())
        .from(follow)
        .where(
            follow
                .userId
                .eq(currentUserId)
                .and(follow.targetUserId.eq(userId))
                .and(follow.status.eq(FollowStatus.FOLLOWING)))
        .gt(0L);
  }

  /**
   * 로그인 사용자가 조회하는 페이지의 사용자인지 여부(나의 마이페이지인지 여부)를 조회한다. 해당 조회는 마이보틀 페이지에서도 사용된다.
   *
   * @param userId 조회하는 페이지의 사용자
   * @param currentUserId 로그인 사용자
   * @return 마이페이지 여부 (true : 나의 마이페이지, false : 나의 마이페이지가 아님)
   */
  public BooleanExpression isMyPageSubQuery(Long userId, Long currentUserId) {
    return Expressions.asBoolean(Objects.equals(userId, currentUserId));
  }

  public <T> List<T> myBottlePageItems(MyBottlePageableCriteria request, List<T> myBottleList) {
    if (myBottleList.size() <= request.size()) {
      return myBottleList;
    }
    return myBottleList.stream().limit(request.size()).toList();
  }

  public BooleanExpression myBottleSeek(
      MyBottleType tab, MyBottlePageableCriteria request, HmacCursorCodec cursorCodec) {
    if (request.cursor() == null) {
      return null;
    }
    CursorClaims claims = cursorCodec.verify(request.cursor(), request.context(tab));
    long lastId = TimeIdCursor.id(claims);
    boolean desc = request.sortOrder() != SortOrder.ASC;
    return switch (request.sortType()) {
      case LATEST -> timeSeek(latestTimeExpression(tab), TimeIdCursor.time(claims), lastId, desc);
      case REVIEW ->
          timeSeek(
              reviewSortExpression(tab, request.userId()), TimeIdCursor.time(claims), lastId, desc);
      case RATING ->
          scoreSeek(
              ratingSortExpression(tab, request.userId()),
              Double.parseDouble(claims.sortKeys().getOrDefault("score", "0")),
              lastId,
              desc);
    };
  }

  public String encodeMyBottleCursor(
      MyBottleType tab,
      MyBottlePageableCriteria request,
      Long alcoholId,
      LocalDateTime latestAt,
      LocalDateTime reviewAt,
      Double ratingScore,
      HmacCursorCodec cursorCodec) {
    Map<String, String> keys =
        switch (request.sortType()) {
          case LATEST ->
              TimeIdCursor.keys(latestAt == null ? LocalDateTime.MIN : latestAt, alcoholId);
          case REVIEW ->
              TimeIdCursor.keys(reviewAt == null ? LocalDateTime.MIN : reviewAt, alcoholId);
          case RATING ->
              Map.of(
                  "id",
                  String.valueOf(alcoholId),
                  "score",
                  String.valueOf(ratingScore == null ? 0.0 : ratingScore));
        };
    return cursorCodec.encode(request.context(tab), keys);
  }

  public Expression<LocalDateTime> latestTimeExpression(MyBottleType tab) {
    return switch (tab) {
      case PICK -> picks.lastModifyAt;
      case REVIEW -> review.lastModifyAt;
      case RATING -> rating.lastModifyAt;
    };
  }

  private BooleanExpression timeSeek(
      Expression<? extends Comparable<?>> expression,
      LocalDateTime lastTime,
      long lastId,
      boolean desc) {
    DateTimeExpression<LocalDateTime> time =
        Expressions.dateTimeTemplate(LocalDateTime.class, "{0}", expression);
    BooleanExpression moved = desc ? time.lt(lastTime) : time.gt(lastTime);
    BooleanExpression idMoved = desc ? alcohol.id.lt(lastId) : alcohol.id.gt(lastId);
    return moved.or(time.eq(lastTime).and(idMoved));
  }

  private BooleanExpression scoreSeek(
      Expression<? extends Comparable<?>> expression, double lastScore, long lastId, boolean desc) {
    NumberExpression<Double> score = Expressions.numberTemplate(Double.class, "{0}", expression);
    BooleanExpression moved = desc ? score.lt(lastScore) : score.gt(lastScore);
    BooleanExpression idMoved = desc ? alcohol.id.lt(lastId) : alcohol.id.gt(lastId);
    return moved.or(score.eq(lastScore).and(idMoved));
  }

  /** 지역(리전) 검색조건 (부모 지역이면 하위 지역 포함) */
  public BooleanExpression eqRegion(Long regionId) {
    if (regionId == null) return null;
    List<Long> childIds = regionRepository.findChildRegionIds(regionId);
    if (childIds.isEmpty()) return alcohol.region.id.eq(regionId);
    List<Long> regionIds = new java.util.ArrayList<>(childIds.size() + 1);
    regionIds.add(regionId);
    regionIds.addAll(childIds);
    return alcohol.region.id.in(regionIds);
  }

  /** 술 이름을 검색하는 조건 */
  public BooleanExpression eqName(String name) {
    if (StringUtils.isNullOrEmpty(name)) return null;
    return alcohol.korName.like("%" + name + "%").or(alcohol.engName.like("%" + name + "%"));
  }

  /** 마이 보틀 정렬 조건을 반환 */
  public OrderSpecifier<?> sortBy(
      MyBottleType tabType, MyBottleSortType myBottleSortType, SortOrder sortOrder, Long userId) {
    myBottleSortType = (myBottleSortType != null) ? myBottleSortType : MyBottleSortType.LATEST;
    sortOrder = (sortOrder != null) ? sortOrder : SortOrder.DESC;

    return switch (myBottleSortType) {
      case RATING -> orderSpecifier(sortOrder, ratingSortExpression(tabType, userId));
      case REVIEW -> orderSpecifier(sortOrder, reviewSortExpression(tabType, userId));
      case LATEST ->
          switch (tabType) {
            case PICK -> orderSpecifier(sortOrder, picks.lastModifyAt);
            case REVIEW -> orderSpecifier(sortOrder, review.lastModifyAt);
            case RATING -> orderSpecifier(sortOrder, rating.lastModifyAt);
          };
    };
  }

  public OrderSpecifier<?> myBottleTieBreakerSortBy(SortOrder sortOrder) {
    sortOrder = (sortOrder != null) ? sortOrder : SortOrder.DESC;
    return orderSpecifier(sortOrder, alcohol.id);
  }

  private OrderSpecifier<?> orderSpecifier(
      SortOrder sortOrder, Expression<? extends Comparable<?>> expression) {
    Order order = sortOrder == SortOrder.ASC ? Order.ASC : Order.DESC;
    return new OrderSpecifier<>(order, expression).nullsLast();
  }

  public Expression<? extends Comparable<?>> ratingSortExpression(
      MyBottleType tabType, Long userId) {
    if (tabType == MyBottleType.RATING) {
      return rating.ratingPoint.rating.max();
    }
    return select(rating.ratingPoint.rating.max())
        .from(rating)
        .where(
            rating
                .id
                .userId
                .eq(userId)
                .and(rating.id.alcoholId.eq(alcohol.id))
                .and(rating.ratingPoint.rating.gt(0.0)));
  }

  public Expression<? extends Comparable<?>> reviewSortExpression(
      MyBottleType tabType, Long userId) {
    if (tabType == MyBottleType.REVIEW) {
      return review.createAt.max();
    }
    return select(review.createAt.max())
        .from(review)
        .where(
            review
                .userId
                .eq(userId)
                .and(review.alcoholId.eq(alcohol.id))
                .and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE)));
  }
}
