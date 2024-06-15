package app.bottlenote.follow.repository;

import app.bottlenote.follow.domain.QFollow;
import app.bottlenote.follow.domain.constant.FollowStatus;
import app.bottlenote.follow.dto.request.FollowPageableRequest;
import app.bottlenote.follow.dto.response.FollowDetail;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static app.bottlenote.follow.domain.QFollow.follow;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.user.domain.QUser.user;
import static com.querydsl.jpa.JPAExpressions.select;

@Slf4j
@RequiredArgsConstructor
public class CustomFollowRepositoryImpl implements CustomFollowRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public PageResponse<FollowSearchResponse> findFollowerList(Long userId, FollowPageableRequest pageableRequest) {

		Long cursor = pageableRequest.cursor();
		int pageSize = pageableRequest.pageSize().intValue();

		QFollow follow1 = new QFollow("follow1");

		List<FollowDetail> followDetails = queryFactory
			.select(Projections.constructor(
				FollowDetail.class,
				follow.user.id.as("userId"),
				follow.followUser.id.as("followUserId"),
				user.nickName.as("nickName"),
				user.imageUrl.as("userProfileImage"),
				follow1.status.as("status"),
				ExpressionUtils.as(select(review.count()).from(review).where(review.user.id.eq(follow.user.id)), "reviewCount"),
				ExpressionUtils.as(select(rating.count()).from(rating).where(rating.user.id.eq(follow.user.id)), "ratingCount")
			))
			.from(follow)
			.leftJoin(user).on(user.id.eq(follow.user.id))
			.leftJoin(follow1).on(follow1.user.id.eq(userId).and(follow1.followUser.id.eq(follow.user.id)))
			.where(follow.followUser.id.eq(userId)
				.and(follow.status.eq(FollowStatus.FOLLOWING)))
			.orderBy(follow.createAt.desc())
			.offset(cursor)
			.limit(pageSize + 1)
			.fetch();

		log.info("FollowDetails: {}", followDetails); // []


		Long totalCount = queryFactory
			.select(follow.id.count())
			.from(follow)
			.where(follow.followUser.id.eq(userId)
				.and(follow.status.eq(FollowStatus.FOLLOWING)))
			.fetchOne();

		log.info("TotalCount: {}", totalCount);

		CursorPageable cursorPageable = getCursorPageable(pageableRequest, followDetails);

		return PageResponse.of(FollowSearchResponse.of(totalCount, followDetails), cursorPageable);
	}


	@Override
	public PageResponse<FollowSearchResponse> findFollowList(Long userId, FollowPageableRequest pageableRequest) {
		Long cursor = pageableRequest.cursor();
		int pageSize = pageableRequest.pageSize().intValue();

		List<FollowDetail> followDetails = queryFactory
			.select(Projections.constructor(
				FollowDetail.class,
				follow.followUser.id.as("followUserId"),
				follow.user.id.as("userId"),
				user.nickName.as("nickName"),
				user.imageUrl.as("userProfileImage"),
				follow.status.as("status"),
				ExpressionUtils.as(select(review.count()).from(review).where(review.user.id.eq(follow.followUser.id)), "reviewCount"),
				ExpressionUtils.as(select(rating.count()).from(rating).where(rating.user.id.eq(follow.followUser.id)), "ratingCount")
			))
			.from(follow)
			.leftJoin(user).on(user.id.eq(follow.followUser.id))
			.where(follow.user.id.eq(userId)
				.and(follow.status.eq(FollowStatus.FOLLOWING)))
			.orderBy(follow.createAt.desc())
			.offset(cursor)
			.limit(pageSize + 1)
			.fetch();

		log.info("FollowDetails: {}", followDetails);

		Long totalCount = queryFactory
			.select(follow.id.count())
			.from(follow)
			.where(follow.user.id.eq(userId)
				.and(follow.status.eq(FollowStatus.FOLLOWING)))
			.fetchOne();

		log.info("TotalCount: {}", totalCount);

		CursorPageable cursorPageable = getCursorPageable(pageableRequest, followDetails);

		return PageResponse.of(FollowSearchResponse.of(totalCount, followDetails), cursorPageable);
	}


	private CursorPageable getCursorPageable(
		FollowPageableRequest pageableRequest,
		List<FollowDetail> followDetails
	) {
		boolean hasNext = isHasNext(pageableRequest, followDetails);
		return CursorPageable.builder()
			.cursor(pageableRequest.cursor() + pageableRequest.pageSize())
			.pageSize(pageableRequest.pageSize())
			.hasNext(hasNext)
			.currentCursor(pageableRequest.cursor())
			.build();
	}

	private boolean isHasNext(
		FollowPageableRequest pageableRequest,
		List<FollowDetail> fetch
	) {
		boolean hasNext = fetch.size() > pageableRequest.pageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);
		}
		return hasNext;
	}
}
