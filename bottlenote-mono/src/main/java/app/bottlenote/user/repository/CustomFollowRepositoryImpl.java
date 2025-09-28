package app.bottlenote.user.repository;

import static app.bottlenote.user.domain.QFollow.follow;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
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

  @Override
  public PageResponse<FollowingSearchResponse> getFollowingList(
      Long userId, FollowPageableCriteria criteria) {

    Long cursor = criteria.cursor();
    Long pageSize = criteria.pageSize();

    List<RelationUserItem> followingDetails = getFollowingDetails(userId, cursor, pageSize);

    Long totalCount =
        queryFactory
            .select(follow.id.count())
            .from(follow)
            .where(follow.userId.eq(userId).and(follow.status.eq(FollowStatus.FOLLOWING)))
            .fetchOne();

    log.debug("FollowDetails: {}", followingDetails);

    CursorPageable cursorPageable = supporter.followingCursorPageable(criteria, followingDetails);

    return PageResponse.of(
        FollowingSearchResponse.of(totalCount, followingDetails), cursorPageable);
  }

  @Override
  public PageResponse<FollowerSearchResponse> getFollowerList(
      Long userId, FollowPageableCriteria criteria) {

    Long cursor = criteria.cursor();
    Long pageSize = criteria.pageSize();

    List<RelationUserItem> followerDetails = getFollowerDetails(userId, cursor, pageSize);

    Long totalCount =
        queryFactory
            .select(follow.id.count())
            .from(follow)
            .where(follow.targetUserId.eq(userId).and(follow.status.eq(FollowStatus.FOLLOWING)))
            .fetchOne();

    log.debug("FollowDetails: {}", followerDetails);

    CursorPageable cursorPageable = supporter.followerCursorPageable(criteria, followerDetails);

    return PageResponse.of(FollowerSearchResponse.of(totalCount, followerDetails), cursorPageable);
  }

  private List<RelationUserItem> getFollowingDetails(Long userId, Long cursor, Long pageSize) {
    return queryFactory
        .select(
            Projections.constructor(
                RelationUserItem.class,
                follow.userId.as("userId"),
                follow.targetUserId.as("followUserId"),
                user.nickName.as("nickName"),
                user.imageUrl.as("userProfileImage"),
                follow.status.as("status"),
                supporter.followReviewCountSubQuery(follow.targetUserId),
                supporter.followRatingCountSubQuery(follow.targetUserId)))
        .from(follow)
        .leftJoin(user)
        .on(user.id.eq(follow.targetUserId))
        .where(follow.userId.eq(userId).and(follow.status.eq(FollowStatus.FOLLOWING)))
        .orderBy(follow.lastModifyAt.desc())
        .offset(cursor)
        .limit(pageSize + 1)
        .fetch();
  }

  private List<RelationUserItem> getFollowerDetails(Long userId, Long cursor, Long pageSize) {
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

    return queryFactory
        .select(
            Projections.constructor(
                RelationUserItem.class,
                follow.userId.as("userId"),
                follow.targetUserId.as("followUserId"),
                user.nickName.as("followUserNickname"),
                user.imageUrl.as("userProfileImage"),
                Expressions.stringTemplate(
                        "CASE WHEN {0} THEN {1} ELSE {2} END",
                        isFollowing, FollowStatus.FOLLOWING.name(), FollowStatus.UNFOLLOW.name())
                    .as("status"),
                supporter.followReviewCountSubQuery(follow.userId),
                supporter.followRatingCountSubQuery(follow.userId)))
        .from(follow)
        .leftJoin(user)
        .on(user.id.eq(follow.userId))
        .where(follow.targetUserId.eq(userId).and(follow.status.eq(FollowStatus.FOLLOWING)))
        .orderBy(follow.lastModifyAt.desc())
        .offset(cursor)
        .limit(pageSize + 1)
        .fetch();
  }
}
