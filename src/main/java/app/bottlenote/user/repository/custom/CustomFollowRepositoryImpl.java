package app.bottlenote.user.repository.custom;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.domain.constant.FollowStatus;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowDetail;
import app.bottlenote.user.dto.response.FollowSearchResponse;
import app.bottlenote.user.repository.FollowQuerySupporter;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static app.bottlenote.user.domain.QFollow.follow;
import static app.bottlenote.user.domain.QUser.user;

@Slf4j
@RequiredArgsConstructor
public class CustomFollowRepositoryImpl implements CustomFollowRepository {

	private final JPAQueryFactory queryFactory;
	private final FollowQuerySupporter supporter;

	@Override
	public PageResponse<FollowSearchResponse> followList(FollowPageableCriteria criteria) {

		Long cursor = criteria.cursor();
		Long pageSize = criteria.pageSize();
		Long userId = criteria.userId();

		List<FollowDetail> followDetails = queryFactory
			.select(Projections.constructor(
				FollowDetail.class,
				follow.followUser.id.as("followUserId"),
				follow.userId.as("userId"),
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

		Long totalCount = queryFactory
			.select(follow.id.count())
			.from(follow)
			.where(follow.userId.eq(userId)
				.and(follow.status.eq(FollowStatus.FOLLOWING)))
			.fetchOne();

		log.debug("FollowDetails: {}", followDetails);

		CursorPageable cursorPageable = supporter.followCursorPageable(criteria, followDetails);

		return PageResponse.of(FollowSearchResponse.of(totalCount, followDetails), cursorPageable);
	}

}
