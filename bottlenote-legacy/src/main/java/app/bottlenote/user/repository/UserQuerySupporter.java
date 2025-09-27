package app.bottlenote.user.repository;

import static app.bottlenote.core.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.user.domain.QFollow.follow;
import static com.querydsl.jpa.JPAExpressions.select;

import app.bottlenote.global.util.SortOrderUtils;
import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.SortOrder;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.constant.MyBottleSortType;
import app.bottlenote.user.constant.MyBottleType;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class UserQuerySupporter {

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

  /**
   * 마이 보틀 CursorPageable 생성
   *
   * @param request MyBottlePageableCriteria
   * @param myBottleList List<MyBottleResponse.MyBottleInfo>
   * @return CursorPageable
   */
  public CursorPageable myBottleCursorPageable(
      MyBottlePageableCriteria request, List<?> myBottleList) {

    List<?> items = new ArrayList<>(myBottleList);

    boolean hasNext = isHasNext(request, items);

    if (hasNext) {
      items.remove(items.size() - 1);
    }
    return CursorPageable.builder()
        .cursor(request.cursor() + request.pageSize())
        .pageSize(request.pageSize())
        .hasNext(hasNext)
        .currentCursor(request.cursor())
        .build();
  }

  private boolean isHasNext(MyBottlePageableCriteria request, List<?> myBottleList) {
    return myBottleList.size() > request.pageSize();
  }

  /** 지역(리전) 검색조건 */
  public BooleanExpression eqRegion(Long regionId) {
    if (regionId == null) return null;

    return alcohol.region.id.eq(regionId);
  }

  /** 술 이름을 검색하는 조건 */
  public BooleanExpression eqName(String name) {
    if (StringUtils.isNullOrEmpty(name)) return null;
    return alcohol.korName.like("%" + name + "%").or(alcohol.engName.like("%" + name + "%"));
  }

  /** 마이 보틀 정렬 조건을 반환 */
  public OrderSpecifier<?> sortBy(
      MyBottleType tabType, MyBottleSortType myBottleSortType, SortOrder sortOrder) {
    myBottleSortType = (myBottleSortType != null) ? myBottleSortType : MyBottleSortType.LATEST;
    sortOrder = (sortOrder != null) ? sortOrder : SortOrder.DESC; // 기본값은 내림차순

    return switch (myBottleSortType) {
      case RATING -> SortOrderUtils.resolve(sortOrder, rating.ratingPoint.rating.max());
      case REVIEW -> SortOrderUtils.resolve(sortOrder, review.createAt.max());
      case LATEST ->
          switch (tabType) {
            case PICK -> SortOrderUtils.resolve(sortOrder, picks.lastModifyAt);
            case REVIEW -> SortOrderUtils.resolve(sortOrder, review.lastModifyAt);
            case RATING -> SortOrderUtils.resolve(sortOrder, rating.lastModifyAt);
          };
    };
  }
}
