package app.bottlenote.review.repository;

import app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.user.domain.QUser.user;

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

	public BooleanExpression hasReplyByMeSubquery(Long userId) {
		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(reviewReply)
				.where(reviewReply.review.id.eq(review.id).and(reviewReply.user.id.eq(userId)))
				.exists()
		).as("hasReplyByMe");
	}

	public BooleanExpression isLikeByMeSubquery(Long userId) {
		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(likes)
				.where(likes.review.id.eq(review.id).and(likes.user.id.eq(userId)))
				.exists()
		).as("isLikedByMe");
	}
}
