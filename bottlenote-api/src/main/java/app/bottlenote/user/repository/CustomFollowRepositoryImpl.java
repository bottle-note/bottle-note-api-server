package app.bottlenote.user.repository;

import static app.bottlenote.user.domain.QFollow.follow;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.global.pagination.TimeIdCursor;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.domain.QFollow;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.dto.response.RelationUserItem;
import app.bottlenote.user.facade.payload.FriendItem;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.jpa.support.PageableUtils;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class CustomFollowRepositoryImpl implements CustomFollowRepository {

  private final JPAQueryFactory queryFactory;
  private final EntityManager entityManager;
  private final FollowQuerySupporter supporter;
  private final HmacCursorCodec cursorCodec;

  @Override
  public KeysetPageResponse<FollowingSearchResponse> getFollowingList(
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
  public KeysetPageResponse<FollowerSearchResponse> getFollowerList(
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

  private <T> KeysetPageResponse<T> toPage(
      List<RelationUserItem> details,
      int size,
      String context,
      java.util.function.Function<List<RelationUserItem>, T> mapper) {
    KeysetPagination.PageSlice<RelationUserItem> slice =
        KeysetPagination.fromOverflow(
            details,
            size,
            item ->
                cursorCodec.encode(
                    context, TimeIdCursor.keys(item.lastModifyAt(), item.followId())));
    return KeysetPageResponse.of(mapper.apply(slice.items()), slice.pagination());
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

  @Override
  public List<FriendItem> getTastingFriendsInfoList(
      Long alcoholId, Long userId, PageRequest pageRequest) {
    String jpql =
        """
		    SELECT new app.bottlenote.user.facade.payload.FriendItem(
		        u.imageUrl,
		        u.id,
		        u.nickName,
		        r.ratingPoint.rating
		    )
		    FROM users u
		    JOIN rating r ON r.id.userId = u.id
		    WHERE u.id IN (
		        SELECT f.targetUserId
		        FROM follow f
		        WHERE f.userId = :userId
		        AND f.status = app.bottlenote.user.constant.FollowStatus.FOLLOWING
		    )
		    AND r.id.alcoholId = :alcoholId
		""";
    String sortedJpql = QueryUtils.applySorting(jpql, pageRequest.getSort(), "u");
    return entityManager
        .createQuery(sortedJpql, FriendItem.class)
        .setParameter("alcoholId", alcoholId)
        .setParameter("userId", userId)
        .setFirstResult(PageableUtils.getOffsetAsInteger(pageRequest))
        .setMaxResults(pageRequest.getPageSize())
        .getResultList();
  }
}
