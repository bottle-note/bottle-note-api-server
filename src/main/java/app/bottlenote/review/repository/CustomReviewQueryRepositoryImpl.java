package app.bottlenote.review.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import static app.bottlenote.alcohols.dto.response.AlcoholDetail.ReviewOfAlcoholDetail;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.user.domain.QUser.user;

@RequiredArgsConstructor
public class CustomReviewQueryRepositoryImpl implements CustomReviewQueryRepository {
	private final JPAQueryFactory queryFactory;


	/**
	 * 베스트 리뷰 단건을 조회합니다.
	 */
	@Override
	public List<ReviewOfAlcoholDetail> findBestReviewsForAlcoholDetail(Long alcoholId, Long userId) {
		return queryFactory
			.select(Projections.constructor(
				ReviewOfAlcoholDetail.class,
				user.id.as("userId"),
				user.imageUrl.as("imageUrl"),
				user.nickName.as("nickName"),
				review.id.as("reviewId"),
				review.content.as("reviewContent"),
				rating.ratingPoint.rating.coalesce(0.0).as("rating"),
				review.sizeType.as("sizeType"),
				review.price.coalesce(BigDecimal.ZERO).as("price"),
				review.viewCount.coalesce(0L).as("viewCount"),
				likes.id.count().as("likeCount"),
				containsUserLike(userId).as("isMyLike"),
				reviewReply.id.count().as("replyCount"),
				containsUserReply(userId).as("isMyReply"),
				review.status.as("status"),
				review.imageUrl.as("reviewImageUrl"),
				review.createAt.as("createAt")
			))
			.from(review)
			.join(review.user, user)
			.leftJoin(review.reviewReplies, reviewReply)
			.leftJoin(rating).on(rating.alcohol.id.eq(review.alcohol.id).and(rating.user.id.eq(user.id)))
			.leftJoin(likes).on(likes.review.id.eq(review.id))
			.where(review.alcohol.id.eq(alcoholId))
			.groupBy(user.id, user.imageUrl, user.nickName, review.id, review.content, rating.ratingPoint, review.createAt)
			.orderBy(
				review.reviewReplies.size().multiply(0.3) // 댓글수
					.add(likes.id.count().multiply(0.5))  // 좋아요수
					.add(rating.ratingPoint.rating.coalesce(0.0).multiply(0.2)) // 평점
					.desc()
			)
			.limit(1)
			.fetch();
	}

	/**
	 * 최신순 리뷰 특정 건수 조회
	 */
	@Override
	public List<ReviewOfAlcoholDetail> findReviewsForAlcoholDetail(
		Long alcoholId,
		Long userId,
		List<Long> ids
	) {
		return queryFactory
			.select(Projections.constructor(
				ReviewOfAlcoholDetail.class,
				user.id.as("userId"),
				user.imageUrl.as("imageUrl"),
				user.nickName.as("nickName"),
				review.id.as("reviewId"),
				review.content.as("reviewContent"),
				rating.ratingPoint.rating.coalesce(0.0).as("rating"),
				review.sizeType.as("sizeType"),
				review.price.coalesce(BigDecimal.ZERO).as("price"),
				review.viewCount.coalesce(0L).as("viewCount"),
				likes.id.count().as("likeCount"),
				containsUserLike(userId).as("isMyLike"),
				reviewReply.id.count().as("replyCount"),
				containsUserReply(userId).as("isMyReply"),
				review.status.as("status"),
				review.imageUrl.as("reviewImageUrl"),
				review.createAt.as("createAt")
			))
			.from(review)
			.join(review.user, user)
			.leftJoin(review.reviewReplies, reviewReply)
			.leftJoin(rating).on(rating.alcohol.id.eq(review.alcohol.id).and(rating.user.id.eq(user.id)))
			.leftJoin(likes).on(likes.review.id.eq(review.id))
			.where(
				review.alcohol.id.eq(alcoholId)
				, exisingReview(ids)
			)
			.groupBy(user.id, user.imageUrl, user.nickName, review.id, review.content, rating.ratingPoint, review.createAt)
			.orderBy(review.createAt.desc())
			.limit(4)
			.fetch();
	}

	/**
	 * 베스트 리뷰들을 제거
	 */
	private Predicate exisingReview(List<Long> ids) {
		// 아이디들을 제외
		return review.id.notIn(ids);
	}

	private BooleanExpression containsUserLike(Long userId) {
		if (userId == null) {
			return Expressions.FALSE;
		}

		return new CaseBuilder()
			.when(likes.user.id.eq(userId))
			.then(true)
			.otherwise(false);
	}

	private BooleanExpression containsUserReply(Long userId) {
		if (userId == null) {
			return Expressions.FALSE;
		}

		return new CaseBuilder()
			.when(reviewReply.user.id.eq(userId))
			.then(true)
			.otherwise(false);
	}

}
