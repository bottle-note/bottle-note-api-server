package app.bottlenote.follow.repository.follower;

import app.bottlenote.follow.domain.QFollow;
import app.bottlenote.follow.domain.constant.FollowStatus;
import app.bottlenote.follow.dto.dsl.FollowPageableCriteria;
import app.bottlenote.follow.dto.response.FollowDetail;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.follow.repository.FollowQuerySupporter;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static app.bottlenote.follow.domain.QFollow.follow;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.user.domain.QUser.user;
import static com.querydsl.jpa.JPAExpressions.select;

@Slf4j
@RequiredArgsConstructor
public class CustomFollowerRepositoryImpl implements CustomFollowerRepository{

	private final JPAQueryFactory queryFactory;
	private final FollowQuerySupporter followQuerySupporter;

	@Override
	public PageResponse<FollowSearchResponse> followerList(FollowPageableCriteria criteria, Long userId) {

		Long cursor = criteria.cursor();
		Long pageSize = criteria.pageSize();


		QFollow follow1 = new QFollow("follow1");

		List<FollowDetail> followDetails = queryFactory
			.select(Projections.constructor(
				FollowDetail.class,
				follow.user.id.as("userId"),
				follow.followUser.id.as("followUserId"),
				user.nickName.as("nickName"),
				user.imageUrl.as("userProfileImage"),
				follow1.status.as("status"),
				reviewCountSubQuery(),
				ratingCountSubQuery()
			))
			.from(follow)
			.leftJoin(user).on(user.id.eq(follow.user.id))
			.leftJoin(follow1).on(follow1.user.id.eq(userId).and(follow1.followUser.id.eq(follow.user.id)))
			.where(follow.followUser.id.eq(userId)
				.and(follow.status.eq(FollowStatus.FOLLOWING)))
			.orderBy(follow.lastModifyAt.desc())
			.offset(cursor)
			.limit(pageSize + 1)
			.fetch();

		Long totalCount = queryFactory
			.select(follow.id.count())
			.from(follow)
			.where(follow.followUser.id.eq(userId)
				.and(follow.status.eq(FollowStatus.FOLLOWING)))
			.fetchOne();

		log.debug("FollowDetails: {}", followDetails);

		CursorPageable cursorPageable = followQuerySupporter.getCursorPageable(criteria, followDetails);

		return PageResponse.of(FollowSearchResponse.of(totalCount, followDetails), cursorPageable);
	}


	private Expression<Long> reviewCountSubQuery() {
		return ExpressionUtils.as(
			select(review.count())
				.from(review)
				.where(review.user.id.eq(follow.followUser.id)),
			"reviewCount"
		);
	}

	private Expression<Long> ratingCountSubQuery() {
		return ExpressionUtils.as(
			select(rating.count())
				.from(rating)
				.where(rating.user.id.eq(follow.followUser.id)),
			"ratingCount"
		);
	}

	private CursorPageable getCursorPageable(
		FollowPageableCriteria criteria,
		List<FollowDetail> followDetails
	) {
		boolean hasNext = isHasNext(criteria, followDetails);
		return CursorPageable.builder()
			.cursor(criteria.cursor() + criteria.pageSize())
			.pageSize(criteria.pageSize())
			.hasNext(hasNext)
			.currentCursor(criteria.cursor())
			.build();
	}


	private boolean isHasNext(
		FollowPageableCriteria pageableRequest,
		List<FollowDetail> fetch
	) {
		boolean hasNext = fetch.size() > pageableRequest.pageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);
		}
		return hasNext;
	}
}
