package app.bottlenote.review.repository.custom;

import app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo;
import app.bottlenote.review.repository.ReviewQuerySupporter;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo.ReviewInfo;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.user.domain.QUser.user;

public class CustomReviewQueryRepositoryImpl implements CustomReviewQueryRepository {

	private static final Logger log = LogManager.getLogger(CustomReviewQueryRepositoryImpl.class);
	private final JPAQueryFactory queryFactory;
	private final ReviewQuerySupporter supporter;

	public CustomReviewQueryRepositoryImpl(
		JPAQueryFactory queryFactory,
		ReviewQuerySupporter supporter
	) {
		this.queryFactory = queryFactory;
		this.supporter = supporter;
	}

	/**
	 * 베스트 리뷰 단건을 조회합니다.
	 */
	@Override
	public List<ReviewsDetailInfo.ReviewInfo> fetchTopReviewByAlcohol(Long alcoholId, Long userId) {
		userId = userId == null ? -1L : userId;
		return queryFactory
			.select(supporter.reviewInfoConstructor(userId))
			.from(review)
			.join(user).on(review.userId.eq(user.id))
			.leftJoin(review.reviewReplies, reviewReply)
			.leftJoin(rating).on(rating.alcohol.id.eq(review.alcoholId).and(rating.user.id.eq(user.id)))
			.leftJoin(likes).on(likes.review.id.eq(review.id))
			.where(review.alcoholId.eq(alcoholId))
			.groupBy(user.id, user.imageUrl, user.nickName, review.id, review.content, rating.ratingPoint, review.createAt)
			.orderBy(reviewReply.count().coalesce(0L)
				.add(likes.count().coalesce(0L))
				.add(rating.ratingPoint.rating.coalesce(0.0).avg())
				.desc()
			)
			.limit(1)
			.fetch();
	}


	/**
	 * 최신순 리뷰 목록을 조회합니다. ( 최대 4개  , 베스트 리뷰 제외)
	 */
	@Override
	public List<ReviewInfo> fetchLatestReviewsByAlcoholExcludingIds(
		Long alcoholId,
		Long userId,
		List<Long> ids
	) {
		userId = userId == null ? -1L : userId;
		return queryFactory
			.select(supporter.reviewInfoConstructor(userId))
			.from(review)
			.leftJoin(user).on(review.userId.eq(user.id))
			.leftJoin(review.reviewReplies, reviewReply)
			.leftJoin(rating).on(rating.alcohol.id.eq(review.alcoholId).and(rating.user.id.eq(user.id)))
			.leftJoin(likes).on(likes.review.id.eq(review.id))
			.where(review.alcoholId.eq(alcoholId), review.id.notIn(ids))
			.groupBy(user.id, user.imageUrl, user.nickName, review.id, review.content, rating.ratingPoint, review.createAt)
			.orderBy(review.createAt.desc())
			.limit(4)
			.fetch();
	}


	@Override
	public Long countByAlcoholId(Long alcoholId) {
		return queryFactory
			.select(review.id.count())
			.from(review)
			.where(review.alcoholId.eq(alcoholId))
			.fetchOne();
	}

	@Override
	public ReviewsDetailInfo fetchUserReviewsForAlcoholDetail(Long alcoholId, Long userId) {
		long start = System.nanoTime();
		List<ReviewsDetailInfo.ReviewInfo> bestReviewInfos = fetchTopReviewByAlcohol(alcoholId, userId);
		log.info("베스트 리뷰 조회 elapsed time : {}", System.nanoTime() - start);
		start = System.nanoTime();
		List<ReviewsDetailInfo.ReviewInfo> reviewInfos = fetchLatestReviewsByAlcoholExcludingIds(alcoholId, userId,
			bestReviewInfos.
				stream().
				map(ReviewsDetailInfo.ReviewInfo::reviewId).toList()
		);
		log.info("최신 리뷰 조회 elapsed time : {}", System.nanoTime() - start);
		Long reviewTotalCount = countByAlcoholId(alcoholId);

		return ReviewsDetailInfo.builder()
			.totalReviewCount(reviewTotalCount)
			.bestReviewInfos(bestReviewInfos)
			.recentReviewInfos(reviewInfos)
			.build();
	}
}
