package app.bottlenote.review.repository;

import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo;
import app.bottlenote.review.dto.response.ReviewReplyInfo;
import app.bottlenote.review.dto.response.ReviewResponse;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ReviewQuerySupporter {

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

	public ConstructorExpression<ReviewResponse> reviewResponseConstructor(Long userId) {
		return Projections.constructor(
			ReviewResponse.class,
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
			hasReplyByMeSubquery(userId)
		);
	}


	public ConstructorExpression<ReviewReplyInfo> reviewReplyInfoConstructor() {
		return Projections.constructor(
			ReviewReplyInfo.class,
			user.id.as("userId"),
			user.imageUrl.as("imageUrl"),
			user.nickName.as("nickName"),
			reviewReply.id.as("reviewReplyId"),
			reviewReply.content.as("reviewReplyContent"),
			reviewReply.createAt.as("createAt")
		);
	}

	/*
	내가 댓글을 단 리뷰인지 판별
	 */
	public BooleanExpression hasReplyByMeSubquery(Long userId) {

		BooleanExpression eqUserId = userId == null ?
			reviewReply.userId.isNull() : reviewReply.userId.eq(userId);

		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(reviewReply)
				.where(reviewReply.review.id.eq(review.id).and(eqUserId))
				.exists()
		).as("hasReplyByMe");
	}

	/*
	내가 좋아요를 누른 리뷰인지 판별
	 */
	public BooleanExpression isLikeByMeSubquery(Long userId) {

		BooleanExpression eqUserId = userId == null ?
			likes.user.id.isNull() : likes.user.id.eq(userId);

		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(likes)
				.where(likes.review.id.eq(review.id).and(eqUserId))
				.exists()
		).as("isLikedByMe");
	}

	/*
	 * 내가 작성한 리뷰인지 판별
	 */
	private BooleanExpression isMyReviewSubquery(Long userId) {
		if (userId == null) {
			return Expressions.asBoolean(false);
		}
		return review.userId.eq(userId)
			.as("isMyReview");
	}

	/*
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

	/*
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

	/*
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
