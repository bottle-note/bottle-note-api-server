package app.bottlenote.review.repository;

import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ReviewQuerySupporter {
	/**
	 * Alcohol 조회 API에 사용되는 ReviewInfo 클래스의 생성자 Projection 메서드입니다.
	 *
	 * @param userId 유저 ID
	 * @return ReviewInfo
	 */
	public ConstructorExpression<ReviewsDetailInfo.ReviewInfo> reviewInfoConstructor(Long userId) {
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
			isLikeByMeSubquery(userId),
			reviewReply.id.count().as("replyCount"),
			hasReplyByMeSubquery(userId),
			review.status.as("status"),
			review.imageUrl.as("reviewImageUrl"),
			review.createAt.as("createAt")
		);
	}

	/**
	 * 리뷰 목록 조회 API에 사용되는 생성자 Projection 메서드입니다.
	 *
	 * @param userId 유저 ID
	 * @return ReviewListResponse.ReviewInfo
	 */
	public ConstructorExpression<ReviewListResponse.ReviewInfo> reviewResponseConstructor(Long userId, Long bestReviewId) {
		return Projections.constructor(
			ReviewListResponse.ReviewInfo.class,
			review.id.as("reviewId"),
			review.content.as("reviewContent"),
			review.price.as("price"),
			review.sizeType.as("sizeType"),
			likesCountSubquery(),
			reviewReplyCountSubquery(),
			review.imageUrl.as("reviewImageUrl"),
			review.createAt.as("createAt"),
			user.id.as("userId"),
			user.nickName.as("nickName"),
			user.imageUrl.as("userProfileImage"),
			ratingSubquery(),
			review.status.as("status"),
			isMyReviewSubquery(userId),
			isLikeByMeSubquery(userId),
			hasReplyByMeSubquery(userId),
			isBestReviewSubquery(bestReviewId, review.id.longValue())
		);
	}

	/**
	 * 리뷰 상세 조회 API에 사용되는 생상자 Projection 메서드입니다.
	 *
	 * @param reviewId          리뷰 ID
	 * @param bestReviewId      베스트 리뷰 ID
	 * @param userId            유저 ID
	 * @param reviewTastingTags 리뷰 테이스팅 태그
	 * @return ReviewDetailResponse.ReviewInfo
	 */
	public ConstructorExpression<ReviewDetailResponse.ReviewInfo> reviewDetailResponseConstructor(Long reviewId, Long bestReviewId, Long userId, List<String> reviewTastingTags) {
		return Projections.constructor(
			ReviewDetailResponse.ReviewInfo.class,
			review.id.as("reviewId"),
			review.content.as("reviewContent"),
			review.price.as("price"),
			review.sizeType.as("sizeType"),
			likesCountSubquery(),
			reviewReplyCountSubquery(),
			review.imageUrl.as("reviewImageUrl"),
			review.createAt.as("createAt"),
			user.id.as("userId"),
			user.nickName.as("nickName"),
			user.imageUrl.as("userProfileImage"),
			ratingSubquery(),
			review.zipCode.as("zipCode"),
			review.address.as("address"),
			review.detailAddress.as("detailAddress"),
			review.status.as("status"),
			isMyReviewSubquery(userId),
			isLikeByMeSubquery(userId),
			hasReplyByMeSubquery(userId),
			isBestReviewSubquery(bestReviewId, reviewId),
			Expressions.constant(reviewTastingTags)
		);
	}

	/***
	 * 현재 리뷰가 베스트 리뷰인지 판별하는 서브쿼리
	 *
	 * @param bestReviewId 베스트 리뷰 ID
	 * @param reviewId 현재 리뷰 ID
	 * @return Boolean
	 */
	public BooleanExpression isBestReviewSubquery(Long bestReviewId, Long reviewId) {
		return Objects.equals(bestReviewId, reviewId) ? Expressions.asBoolean(true) : Expressions.asBoolean(false);
	}

	public BooleanExpression isBestReviewSubquery(Long bestReviewId, NumberExpression<Long> reviewId) {
		if (bestReviewId == null) {
			return reviewId.isNull();
		}
		return reviewId.eq(bestReviewId);
	}

	/**
	내가 댓글을 단 리뷰인지 판별
	 */
	public BooleanExpression hasReplyByMeSubquery(Long userId) {

		BooleanExpression eqUserId = 1 > userId ?
			reviewReply.userId.isNull() : reviewReply.userId.eq(userId);

		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(reviewReply)
				.where(reviewReply.review.id.eq(review.id).and(eqUserId))
				.exists()
		).as("hasReplyByMe");
	}

	/***
	내가 좋아요를 누른 리뷰인지 판별
	 */
	public BooleanExpression isLikeByMeSubquery(Long userId) {

		BooleanExpression eqUserId = 1 > userId ?
			likes.userInfo.userId.isNull() : likes.userInfo.userId.eq(userId);

		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(likes)
				.where(likes.review.id.eq(review.id).and(eqUserId))
				.exists()
		).as("isLikedByMe");
	}

	/***
	 * 내가 작성한 리뷰인지 판별
	 */
	private BooleanExpression isMyReviewSubquery(Long userId) {
		if (1 > userId) {
			return Expressions.asBoolean(false);
		}
		return review.userId.eq(userId)
			.as("isMyReview");
	}

	/***
	좋아요 개수 서브쿼리
	 */
	private Expression<Long> likesCountSubquery() {
		return ExpressionUtils.as(
			JPAExpressions.select(likes.id.count())
				.from(likes)
				.where(likes.review.id.eq(review.id))
			, "likeCount"
		);
	}

	/***
	별점 서브쿼리
	 */
	private Expression<Double> ratingSubquery() {
		return ExpressionUtils.as(
			JPAExpressions.select(rating.ratingPoint.rating)
				.from(rating)
				.where(
					rating.user.id.eq(review.userId)
						.and(rating.alcohol.id.eq(review.alcoholId)))
			, "rating"
		);
	}

	/***
	리뷰 댓글 개수 카운트 서브쿼리
	 */
	private Expression<Long> reviewReplyCountSubquery() {
		return ExpressionUtils.as(
			JPAExpressions.select(reviewReply.id.count())
				.from(reviewReply)
				.where(reviewReply.review.id.eq(review.id)),
			"replyCount"
		);
	}
}
