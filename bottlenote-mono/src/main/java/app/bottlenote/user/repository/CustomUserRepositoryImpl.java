package app.bottlenote.user.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.user.domain.QUser.user;
import static com.querydsl.jpa.JPAExpressions.select;

import app.bottlenote.alcohols.repository.AlcoholQuerySupporter;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.repository.PicksQuerySupporter;
import app.bottlenote.rating.repository.RatingQuerySupporter;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.domain.QReviewTastingTag;
import app.bottlenote.review.repository.ReviewQuerySupporter;
import app.bottlenote.user.constant.AdminUserSortType;
import app.bottlenote.user.constant.MyBottleType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.request.AdminUserSearchRequest;
import app.bottlenote.user.dto.response.AdminUserListResponse;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.dto.response.PicksMyBottleItem;
import app.bottlenote.user.dto.response.RatingMyBottleItem;
import app.bottlenote.user.dto.response.ReviewMyBottleItem;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@Slf4j
@RequiredArgsConstructor
public class CustomUserRepositoryImpl implements CustomUserRepository {

  private final JPAQueryFactory queryFactory;
  private final ReviewQuerySupporter reviewQuerySupporter;
  private final AlcoholQuerySupporter alcoholQuerySupporter;
  private final UserQuerySupporter userQuerySupporter;
  private final RatingQuerySupporter ratingQuerySupporter;
  private final PicksQuerySupporter pickQuerySupporter;

  /**
   * 마이페이지 조회
   *
   * @param userId 마이페이지 조회 대상 사용자
   * @param currentUserId 로그인 사용자
   * @return MyPageResponse
   */
  @Override
  public MyPageResponse getMyPage(Long userId, Long currentUserId) {

    return queryFactory
        .select(
            Projections.constructor(
                MyPageResponse.class,
                user.id.as("userId"),
                user.nickName.as("nickName"),
                user.imageUrl.as("userProfileImage"),
                reviewQuerySupporter.reviewCountSubQuery(user.id), // 마이 페이지 사용자의 리뷰 개수
                ratingQuerySupporter.ratingCountSubQuery(userId), // 마이 페이지 사용자의 평점 개수
                pickQuerySupporter.picksCountSubQuery(user.id), // 마이 페이지 사용자의 찜하기 개수
                userQuerySupporter.followingCountSubQuery(user.id), // 마이 페이지 사용자가 팔로우 하는 유저 수
                userQuerySupporter.followerCountSubQuery(user.id), //  마이 페이지 사용자를 팔로우 하는 유저 수
                userQuerySupporter.isFollowSubQuery(
                    user.id, currentUserId), // 로그인 사용자가 마이 페이지 사용자를 팔로우 하고 있는지 여부
                userQuerySupporter.isMyPageSubQuery(
                    userId, currentUserId) // 로그인 사용자가 마이 페이지 사용자인지 여부(나의 마이페이지인지 여부)
                ))
        .from(user)
        .where(user.id.eq(userId))
        .fetchOne();
  }

  @Override
  public PageResponse<MyBottleResponse> getReviewMyBottle(MyBottlePageableCriteria request) {
    Long userId = request.userId();
    boolean isMyPage = userId.equals(request.currentUserId());

    List<ReviewMyBottleItem> reviewMyBottleList =
        queryFactory
            .select(
                Projections.constructor(
                    ReviewMyBottleItem.class,
                    Projections.constructor(
                        MyBottleResponse.BaseMyBottleInfo.class,
                        alcohol.id.as("alcoholId"),
                        alcohol.korName.as("alcoholKorName"),
                        alcohol.engName.as("alcoholEngName"),
                        alcohol.korCategory.as("korCategoryName"),
                        alcohol.imageUrl.as("imageUrl"),
                        alcoholQuerySupporter.isHot5(alcohol.id).as("isHot5")),
                    review.id.as("reviewId"),
                    review.id.isNotNull().as("isMyReview"),
                    review.lastModifyAt.as("reviewModifyAt"),
                    review.content.as("reviewContent"),
                    Expressions.constant(Collections.emptySet()),
                    review.isBest.as("isBestReview")))
            .from(alcohol)
            .join(review)
            .on(
                review
                    .alcoholId
                    .eq(alcohol.id)
                    .and(review.userId.eq(userId))
                    .and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE)))
            .where(
                userQuerySupporter.eqName(request.keyword()),
                userQuerySupporter.eqRegion(request.regionId()))
            .groupBy(
                alcohol.id,
                alcohol.korName,
                alcohol.engName,
                alcohol.korCategory,
                alcohol.imageUrl,
                review.id,
                review.lastModifyAt,
                review.content,
                review.isBest)
            .orderBy(
                userQuerySupporter.sortBy(
                    MyBottleType.REVIEW, request.sortType(), request.sortOrder()))
            .offset(request.cursor())
            .limit(request.pageSize() + 1)
            .fetch();

    List<Long> reviewIds = reviewMyBottleList.stream().map(ReviewMyBottleItem::reviewId).toList();

    log.debug("reviewIds : {}", reviewIds);

    // 3. 태그 조회
    QReviewTastingTag rtt = QReviewTastingTag.reviewTastingTag;

    Map<Long, Set<String>> reviewIdToTagsMap =
        queryFactory
            .select(rtt.review.id, rtt.tastingTag)
            .from(rtt)
            .where(rtt.review.id.in(reviewIds))
            .fetch()
            .stream()
            .collect(
                Collectors.groupingBy(
                    tuple -> tuple.get(0, Long.class),
                    Collectors.mapping(tuple -> tuple.get(1, String.class), Collectors.toSet())));

    log.debug("reviewIdToTagsMap : {}", reviewIdToTagsMap);

    // 4. 태그 조립
    List<ReviewMyBottleItem> mergedReviewMyBottleList =
        reviewMyBottleList.stream()
            .map(
                r ->
                    new ReviewMyBottleItem(
                        r.baseMyBottleInfo(),
                        r.reviewId(),
                        r.isMyReview(),
                        r.reviewModifyAt(),
                        r.reviewContent(),
                        reviewIdToTagsMap.getOrDefault(r.reviewId(), Collections.emptySet()),
                        r.isBestReview()))
            .toList();

    log.debug("mergedReviewMyBottleList : {}", mergedReviewMyBottleList);

    CursorPageable cursorPageable =
        userQuerySupporter.myBottleCursorPageable(request, mergedReviewMyBottleList);

    Long totalCount =
        queryFactory
            .select(alcohol.id.count())
            .from(alcohol)
            .join(review)
            .on(
                review
                    .alcoholId
                    .eq(alcohol.id)
                    .and(review.userId.eq(userId))
                    .and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE)))
            .where(
                userQuerySupporter.eqName(request.keyword()),
                userQuerySupporter.eqRegion(request.regionId()))
            .fetchOne();

    MyBottleResponse myBottleResponse =
        MyBottleResponse.create(userId, isMyPage, totalCount, mergedReviewMyBottleList);
    return PageResponse.of(myBottleResponse, cursorPageable);
  }

  @Override
  public PageResponse<MyBottleResponse> getRatingMyBottle(MyBottlePageableCriteria request) {

    Long userId = request.userId();
    boolean isMyPage = userId.equals(request.currentUserId());

    List<RatingMyBottleItem> ratingMyBottleList =
        queryFactory
            .select(
                Projections.constructor(
                    RatingMyBottleItem.class,
                    Projections.constructor(
                        MyBottleResponse.BaseMyBottleInfo.class,
                        alcohol.id.as("alcoholId"),
                        alcohol.korName.as("alcoholKorName"),
                        alcohol.engName.as("alcoholEngName"),
                        alcohol.korCategory.as("korCategoryName"),
                        alcohol.imageUrl.as("imageUrl"),
                        alcoholQuerySupporter.isHot5(alcohol.id).as("isHot5")),
                    rating.ratingPoint.rating.as("myRatingPoint"),
                    ratingQuerySupporter.averageRatingSubQuery(alcohol.id),
                    ratingQuerySupporter.averageRatingCountSubQuery(alcohol.id),
                    rating.lastModifyAt.as("ratingModifyAt")))
            .from(alcohol)
            .join(rating)
            .on(
                rating
                    .id
                    .alcoholId
                    .eq(alcohol.id)
                    .and(rating.id.userId.eq(userId))
                    .and(rating.ratingPoint.rating.gt(0.0)))
            .where(
                userQuerySupporter.eqName(request.keyword()),
                userQuerySupporter.eqRegion(request.regionId()))
            .groupBy(
                alcohol.id, alcohol.korName, alcohol.engName, alcohol.korCategory, alcohol.imageUrl)
            .orderBy(
                userQuerySupporter.sortBy(
                    MyBottleType.RATING, request.sortType(), request.sortOrder()))
            .offset(request.cursor())
            .limit(request.pageSize() + 1)
            .fetch();
    CursorPageable cursorPageable =
        userQuerySupporter.myBottleCursorPageable(request, ratingMyBottleList);

    Long totalCount =
        queryFactory
            .select(alcohol.id.count())
            .from(alcohol)
            .join(rating)
            .on(
                rating
                    .id
                    .alcoholId
                    .eq(alcohol.id)
                    .and(rating.id.userId.eq(userId))
                    .and(rating.ratingPoint.rating.gt(0.0)))
            .where(
                userQuerySupporter.eqName(request.keyword()),
                userQuerySupporter.eqRegion(request.regionId()))
            .fetchOne();

    MyBottleResponse myBottleResponse =
        MyBottleResponse.create(userId, isMyPage, totalCount, ratingMyBottleList);
    return PageResponse.of(myBottleResponse, cursorPageable);
  }

  @Override
  public PageResponse<MyBottleResponse> getPicksMyBottle(MyBottlePageableCriteria request) {
    final Long currentUserId = request.currentUserId();
    final Long targetUserId = request.userId();
    final boolean isMyPage = targetUserId.equals(request.currentUserId());

    List<PicksMyBottleItem> picksMyBottleList =
        queryFactory
            .select(
                Projections.constructor(
                    PicksMyBottleItem.class,
                    Projections.constructor(
                        MyBottleResponse.BaseMyBottleInfo.class,
                        alcohol.id.as("alcoholId"),
                        alcohol.korName.as("alcoholKorName"),
                        alcohol.engName.as("alcoholEngName"),
                        alcohol.korCategory.as("korCategoryName"),
                        alcohol.imageUrl.as("imageUrl"),
                        alcoholQuerySupporter.isHot5(alcohol.id).as("isHot5")),
                    pickQuerySupporter.isPickedBothSubQuery(currentUserId, targetUserId),
                    pickQuerySupporter.totalPicksCountSubQuery(alcohol.id)))
            .from(alcohol)
            .join(picks)
            .on(
                picks
                    .alcoholId
                    .eq(alcohol.id)
                    .and(picks.userId.eq(targetUserId))
                    .and(picks.status.eq(PicksStatus.PICK)))
            .where(
                userQuerySupporter.eqName(request.keyword()),
                userQuerySupporter.eqRegion(request.regionId()))
            .groupBy(
                alcohol.id,
                alcohol.korName,
                alcohol.engName,
                alcohol.korCategory,
                alcohol.imageUrl,
                picks.lastModifyAt)
            .orderBy(
                userQuerySupporter.sortBy(
                    MyBottleType.PICK, request.sortType(), request.sortOrder()))
            .offset(request.cursor())
            .limit(request.pageSize() + 1)
            .fetch();

    CursorPageable cursorPageable =
        userQuerySupporter.myBottleCursorPageable(request, picksMyBottleList);

    Long totalCount =
        queryFactory
            .select(alcohol.id.count())
            .from(alcohol)
            .join(picks)
            .on(
                picks
                    .alcoholId
                    .eq(alcohol.id)
                    .and(picks.userId.eq(targetUserId))
                    .and(picks.status.eq(PicksStatus.PICK)))
            .where(
                userQuerySupporter.eqName(request.keyword()),
                userQuerySupporter.eqRegion(request.regionId()))
            .fetchOne();

    MyBottleResponse myBottleResponse =
        MyBottleResponse.create(targetUserId, isMyPage, totalCount, picksMyBottleList);
    return PageResponse.of(myBottleResponse, cursorPageable);
  }

  @Override
  public Page<AdminUserListResponse> searchAdminUsers(AdminUserSearchRequest request) {
    Expression<Long> reviewCountExpr = reviewQuerySupporter.reviewCountSubQuery(user.id);
    Expression<Long> ratingCountExpr = ratingQuerySupporter.ratingCountSubQuery(user.id);
    Expression<Long> picksCountExpr = pickQuerySupporter.picksCountSubQuery(user.id);

    List<AdminUserRow> rows =
        queryFactory
            .select(
                Projections.constructor(
                    AdminUserRow.class,
                    user.id,
                    user.email,
                    user.nickName,
                    user.imageUrl,
                    user.role,
                    user.status,
                    reviewCountExpr,
                    ratingCountExpr,
                    picksCountExpr,
                    user.createAt,
                    user.lastLoginAt))
            .from(user)
            .where(adminUserKeyword(request.keyword()), adminUserStatus(request.status()))
            .orderBy(adminUserSortOrder(request.sortType(), request.sortOrder()))
            .offset((long) request.page() * request.size())
            .limit(request.size())
            .fetch();

    // socialType 배치 로딩
    List<Long> userIds = rows.stream().map(AdminUserRow::userId).toList();
    Map<Long, List<SocialType>> socialTypeMap =
        userIds.isEmpty()
            ? Map.of()
            : queryFactory.selectFrom(user).where(user.id.in(userIds)).fetch().stream()
                .collect(Collectors.toMap(User::getId, User::getSocialType));

    List<AdminUserListResponse> content =
        rows.stream()
            .map(
                row ->
                    new AdminUserListResponse(
                        row.userId(),
                        row.email(),
                        row.nickName(),
                        row.imageUrl(),
                        row.role(),
                        row.status(),
                        socialTypeMap.getOrDefault(row.userId(), List.of()),
                        row.reviewCount(),
                        row.ratingCount(),
                        row.picksCount(),
                        row.createAt(),
                        row.lastLoginAt()))
            .toList();

    Long total =
        queryFactory
            .select(user.id.count())
            .from(user)
            .where(adminUserKeyword(request.keyword()), adminUserStatus(request.status()))
            .fetchOne();

    return new PageImpl<>(
        content, PageRequest.of(request.page(), request.size()), total != null ? total : 0L);
  }

  private BooleanExpression adminUserKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    return user.nickName.containsIgnoreCase(keyword).or(user.email.containsIgnoreCase(keyword));
  }

  private BooleanExpression adminUserStatus(UserStatus status) {
    if (status == null) {
      return null;
    }
    return user.status.eq(status);
  }

  private OrderSpecifier<?> adminUserSortOrder(AdminUserSortType sortType, SortOrder sortOrder) {
    Order order = sortOrder == SortOrder.ASC ? Order.ASC : Order.DESC;
    return switch (sortType) {
      case CREATED_AT -> new OrderSpecifier<>(order, user.createAt);
      case NICK_NAME -> new OrderSpecifier<>(order, user.nickName);
      case EMAIL -> new OrderSpecifier<>(order, user.email);
      case REVIEW_COUNT ->
          new OrderSpecifier<>(
              order,
              select(review.count())
                  .from(review)
                  .where(
                      review
                          .userId
                          .eq(user.id)
                          .and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE))));
      case RATING_COUNT ->
          new OrderSpecifier<>(
              order,
              select(rating.count())
                  .from(rating)
                  .where(rating.id.userId.eq(user.id).and(rating.ratingPoint.rating.gt(0.0))));
    };
  }
}
