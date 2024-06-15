package app.bottlenote.review.repository;

import app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import static app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo.ReviewInfo;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.user.domain.QUser.user;

@RequiredArgsConstructor
public class CustomReviewQueryRepositoryImpl implements CustomReviewQueryRepository {
	private final JPAQueryFactory queryFactory;

	/**
	 * 리뷰 조회 select 모듈
	 */
	private ConstructorExpression<ReviewsDetailInfo.ReviewInfo> reviewOfAlcoholDetailExpression(Long userId) {
		return Projections.constructor(
			ReviewsDetailInfo.ReviewInfo.class,
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
			containsUserLike(userId).as("isLikedByMe"),
			reviewReply.id.count().as("replyCount"),
			containsUserReply(userId).as("hasReplyByMe"),
			review.status.as("status"),
			review.imageUrl.as("reviewImageUrl"),
			review.createAt.as("createAt")
		);
	}

	/**
	 * 베스트 리뷰 단건을 조회합니다.
	 */
	@Override
	public List<ReviewsDetailInfo.ReviewInfo> findBestReviewsForAlcoholDetail(Long alcoholId, Long userId) {
		return queryFactory
			.select(reviewOfAlcoholDetailExpression(userId))
			.from(review)
			.join(review.user, user)
			.leftJoin(review.reviewReplies, reviewReply)
			.leftJoin(rating).on(rating.alcohol.id.eq(review.alcohol.id).and(rating.user.id.eq(user.id)))
			.leftJoin(likes).on(likes.review.id.eq(review.id))
			.where(review.alcohol.id.eq(alcoholId))
			.groupBy(user.id, user.imageUrl, user.nickName, review.id, review.content, rating.ratingPoint, review.createAt)
			.orderBy(
				reviewReply.count().coalesce(0L)
					.add(likes.count().coalesce(0L))
					.add(rating.ratingPoint.rating.coalesce(0.0).avg())     // Rating
					.desc()
			)
			.limit(1)
			.fetch();
	}

	/**
	 * 최신순 리뷰 목록을 조회합니다. ( 최대 4개  , 베스트 리뷰 제외)
	 */
	@Override
	public List<ReviewInfo> findReviewsForAlcoholDetail(
		Long alcoholId,
		Long userId,
		List<Long> ids
	) {
		return queryFactory
			.select(reviewOfAlcoholDetailExpression(userId))
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

	/**
	 * 사용자가 좋아요를 눌렀는지 확인합니다.
	 */
	private BooleanExpression containsUserLike(Long userId) {
		if (userId == null) {
			return Expressions.FALSE;
		}

		return new CaseBuilder()
			.when(likes.user.id.eq(userId))
			.then(true)
			.otherwise(false);
	}

	/**
	 * 사용자가 리뷰에 답글을 달았는지 확인합니다.
	 */
	private BooleanExpression containsUserReply(Long userId) {
		if (userId == null) {
			return Expressions.FALSE;
		}

		return new CaseBuilder()
			.when(reviewReply.userId.eq(userId))
			.then(true)
			.otherwise(false);
	}

}
