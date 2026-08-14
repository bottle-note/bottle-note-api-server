package app.bottlenote.user.repository;

import static app.bottlenote.user.domain.QFollow.follow;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.global.pagination.Pagination;
import app.bottlenote.global.pagination.TimeIdCursor;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.domain.QFollow;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.dto.response.RelationUserItem;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CustomFollowRepositoryImpl implements CustomFollowRepository {

  private final JPAQueryFactory queryFactory;
  private final FollowQuerySupporter supporter;
  private final HmacCursorCodec cursorCodec;

  @Override
  public PageResponse<FollowingSearchResponse> getFollowingList(
      Long userId, FollowPageableCriteria criteria) {
    String context = "follow.following:" + userId;
    List<RelationUserItem> details =
        queryFactory
            .select(
                Projections.constructor(
                    RelationUserItem.class,
                    follow.userId.as("userId"),
                    follow.targetUserId.as("followUserId"),
                    user.nickName.as("nickName"),
                    user.imageUrl.as("userProfileImage"),
                    follow.status.as("status"),
                    supporter.followReviewCountSubQuery(follow.targetUserId),
                    supporter.followRatingCountSubQuery(follow.targetUserId),
                    follow.id.as("followId"),
                    follow.lastModifyAt.as("lastModifyAt")))
            .from(follow)
            .leftJoin(user)
            .on(user.id.eq(follow.targetUserId))
            .where(
                follow.userId.eq(userId).and(follow.status.eq(FollowStatus.FOLLOWING)),
                followSeek(criteria))
            .orderBy(follow.lastModifyAt.desc(), follow.id.desc())
            .limit(criteria.size() + 1L)
            .fetch();
    return toPage(details, criteria.size(), context, FollowingSearchResponse::of);
  }

  @Override
  public PageResponse<FollowerSearchResponse> getFollowerList(
      Long userId, FollowPageableCriteria criteria) {
    String context = "follow.follower:" + userId;
    QFollow f2 = new QFollow("f2");
    BooleanExpression isFollowing =
        JPAExpressions.selectOne()
            .from(f2)
            .where(
                f2.userId
                    .eq(userId)
                    .and(f2.targetUserId.eq(follow.userId))
                    .and(f2.status.eq(FollowStatus.FOLLOWING)))
            .exists();
    List<RelationUserItem> details =
        queryFactory
            .select(
                Projections.constructor(
                    RelationUserItem.class,
                    follow.userId.as("userId"),
                    follow.targetUserId.as("followUserId"),
                    user.nickName.as("followUserNickname"),
                    user.imageUrl.as("userProfileImage"),
                    Expressions.stringTemplate(
                            "CASE WHEN {0} THEN {1} ELSE {2} END",
                            isFollowing,
                            FollowStatus.FOLLOWING.name(),
                            FollowStatus.UNFOLLOW.name())
                        .as("status"),
                    supporter.followReviewCountSubQuery(follow.userId),
                    supporter.followRatingCountSubQuery(follow.userId),
                    follow.id.as("followId"),
                    follow.lastModifyAt.as("lastModifyAt")))
            .from(follow)
            .leftJoin(user)
            .on(user.id.eq(follow.userId))
            .where(
                follow.targetUserId.eq(userId).and(follow.status.eq(FollowStatus.FOLLOWING)),
                followSeek(criteria))
            .orderBy(follow.lastModifyAt.desc(), follow.id.desc())
            .limit(criteria.size() + 1L)
            .fetch();
    return toPage(details, criteria.size(), context, FollowerSearchResponse::of);
  }

  private <T> PageResponse<T> toPage(
      List<RelationUserItem> details,
      int size,
      String context,
      java.util.function.Function<List<RelationUserItem>, T> mapper) {
    Pagination.PageSlice<RelationUserItem> slice =
        Pagination.fromOverflow(
            details,
            size,
            item ->
                cursorCodec.encode(
                    context, TimeIdCursor.keys(item.lastModifyAt(), item.followId())));
    return PageResponse.of(mapper.apply(slice.items()), slice.pagination());
  }

  private BooleanExpression followSeek(FollowPageableCriteria criteria) {
    if (!criteria.hasCursor()) {
      return null;
    }
    return follow
        .lastModifyAt
        .lt(criteria.lastModifyAt())
        .or(follow.lastModifyAt.eq(criteria.lastModifyAt()).and(follow.id.lt(criteria.lastId())));
  }
}
