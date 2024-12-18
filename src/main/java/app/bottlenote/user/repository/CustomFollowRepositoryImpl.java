package app.bottlenote.user.repository;

import static app.bottlenote.user.domain.QFollow.follow;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.domain.constant.FollowStatus;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowSearchResponse;
import app.bottlenote.user.dto.response.FollowerDetail;
import app.bottlenote.user.dto.response.FollowingDetail;
import com.querydsl.core.types.Projections;
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
	public PageResponse<FollowSearchResponse> getRelationList(Long userId, FollowPageableCriteria criteria) {

		Long cursor = criteria.cursor();
		Long pageSize = criteria.pageSize();

		List<FollowingDetail> followingDetails = getFollowingDetails(userId, cursor, pageSize);
		List<FollowerDetail> followerDetails = getFollowerDetails(userId, cursor, pageSize);

		Long totalCount = queryFactory
			.select(follow.id.count())
			.from(follow)
			.where(follow.userId.eq(userId)
				.and(follow.status.eq(FollowStatus.FOLLOWING)))
			.fetchOne();

		log.debug("FollowDetails: {}", followingDetails);

		CursorPageable cursorPageable = supporter.followCursorPageable(criteria, followingDetails);

		return PageResponse.of(FollowSearchResponse.of(totalCount, followingDetails, followerDetails), cursorPageable);
	}
	
	private List<FollowingDetail> getFollowingDetails(Long userId, Long cursor, Long pageSize) {
		return queryFactory
			.select(Projections.constructor(
				FollowingDetail.class,
				follow.userId.as("userId"),
				follow.followUser.id.as("followUserId"),
				user.nickName.as("nickName"),
				user.imageUrl.as("userProfileImage"),
				follow.status.as("status"),
				supporter.followReviewCountSubQuery(follow.followUser.id),
				supporter.followRatingCountSubQuery(follow.followUser.id)
			))
			.from(follow)
			.leftJoin(user).on(user.id.eq(follow.followUser.id))
			.where(follow.userId.eq(userId)
				.and(follow.status.eq(FollowStatus.FOLLOWING)))
			.orderBy(follow.lastModifyAt.desc())
			.offset(cursor)
			.limit(pageSize + 1)
			.fetch();
	}

	private List<FollowerDetail> getFollowerDetails(Long userId, Long cursor, Long pageSize) {
		return queryFactory
			.select(Projections.constructor(
				FollowerDetail.class,
				follow.userId.as("userId"),
				follow.followUser.id.as("followUserId"),
				user.nickName.as("nickName"),
				user.imageUrl.as("userProfileImage"),
				follow.status.as("status"),
				supporter.followReviewCountSubQuery(follow.userId),
				supporter.followRatingCountSubQuery(follow.userId)
			))
			.from(follow)
			.leftJoin(user).on(user.id.eq(follow.userId))
			.where(follow.followUser.id.eq(userId)
				.and(follow.status.eq(FollowStatus.FOLLOWING)))
			.orderBy(follow.lastModifyAt.desc())
			.offset(cursor)
			.limit(pageSize + 1)
			.fetch();
	}
}
