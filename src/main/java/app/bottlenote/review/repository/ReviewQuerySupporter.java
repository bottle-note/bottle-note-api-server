package app.bottlenote.review.repository;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.constant.ReviewSortType;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.facade.payload.ReviewInfo;
import app.bottlenote.review.facade.payload.UserInfo;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static app.bottlenote.global.service.cursor.SortOrder.DESC;
import static app.bottlenote.like.constant.LikeStatus.LIKE;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.constant.ReviewReplyStatus.NORMAL;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.review.domain.QReviewTastingTag.reviewTastingTag;
import static app.bottlenote.user.domain.QUser.user;

public class ReviewQuerySupporter {

	public static ConstructorExpression<UserInfo> getUserInfo() {
		return Projections.constructor(UserInfo.class,
			user.id.as("userId"),
			user.nickName.as("nickName"),
			user.imageUrl.as("userProfileImage")
		);
	}

	public static Expression<String> getTastingTag() {
		return ExpressionUtils.as(
			JPAExpressions.select(
					Expressions.stringTemplate("group_concat({0})", reviewTastingTag.tastingTag)
				)
				.from(reviewTastingTag)
				.where(reviewTastingTag.review.id.eq(review.id)),
			"tastingTag"
		);
	}

	/**
	 * 내가 댓글을 단 리뷰인지 판별
	 */
	public static BooleanExpression hasReplyByMeSubquery(Long userId) {

		BooleanExpression eqUserId = 1 > userId ?
			reviewReply.userId.isNull() : reviewReply.userId.eq(userId);

		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(reviewReply)
				.where(reviewReply.reviewId.eq(review.id)
					.and(eqUserId
						.and(reviewReply.status.eq(NORMAL))))
				.exists()
		).as("hasReplyByMe");
	}

	/***
	 내가 좋아요를 누른 리뷰인지 판별
	 */
	public static BooleanExpression isLikeByMeSubquery(Long userId) {
		if (userId < 1) {
			return Expressions.asBoolean(false);
		}
		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(likes)
				.where(
					likes.reviewId.eq(review.id)
						.and(likes.userInfo.userId.eq(userId))
						.and(likes.status.eq(LIKE))
				).exists()
		).as("isLikedByMe");
	}

	/***
	 * 내가 작성한 리뷰인지 판별
	 */
	public static BooleanExpression isMyReview(Long userId) {
		if (Objects.isNull(userId) || 1 > userId) {
			return Expressions.asBoolean(false);
		}
		return review.userId.eq(userId).as("isMyReview");
	}

	public static CursorPageable getCursorPageable(
		ReviewPageableRequest reviewPageableRequest,
		List<ReviewInfo> fetch
	) {

		boolean hasNext = isHasNext(reviewPageableRequest, fetch);
		return CursorPageable.builder()
			.cursor(reviewPageableRequest.cursor() + reviewPageableRequest.pageSize())
			.pageSize(reviewPageableRequest.pageSize())
			.hasNext(hasNext)
			.currentCursor(reviewPageableRequest.cursor())
			.build();
	}

	/**
	 * 다음 페이지가 있는지 확인하는 메소드
	 */
	public static boolean isHasNext(
		ReviewPageableRequest reviewPageableRequest,
		List<ReviewInfo> fetch
	) {
		boolean hasNext = fetch.size() > reviewPageableRequest.pageSize();
		if (hasNext) {
			fetch.remove(fetch.size() - 1);  // Remove the extra record
		}
		return hasNext;
	}

	public static List<OrderSpecifier<?>> sortBy(ReviewSortType reviewSortType, SortOrder sortOrder) {
		NumberExpression<Long> likesCount = likes.id.count();
		return switch (reviewSortType) {
			//인기순 -> 임시로 좋아요 순으로 구현
			case POPULAR -> Arrays.asList(
				new OrderSpecifier<>(sortOrder == DESC ? Order.DESC : Order.ASC, review.isBest).nullsLast(),
				new OrderSpecifier<>(sortOrder == DESC ? Order.DESC : Order.ASC, likesCount).nullsLast()
			);
			//좋아요 순
			case LIKES -> Collections.singletonList(sortOrder == DESC ? likesCount.desc() : likesCount.asc());

			//별점 순
			case RATING -> Collections.singletonList(
				sortOrder == DESC ? rating.ratingPoint.rating.desc()
					: rating.ratingPoint.rating.asc());

			//병 기준 가격 순
			case BOTTLE_PRICE -> {
				OrderSpecifier<?> sizeOrderSpecifier = new OrderSpecifier<>(
					Order.ASC, review.sizeType
				).nullsLast();

				OrderSpecifier<?> priceOrderSpecifier = new OrderSpecifier<>(
					sortOrder == DESC ? Order.DESC : Order.ASC,
					review.price
				);
				yield Arrays.asList(sizeOrderSpecifier, priceOrderSpecifier);
			}

			//잔 기준 가격 순
			case GLASS_PRICE -> {
				OrderSpecifier<?> sizeOrderSpecifier = new OrderSpecifier<>(
					Order.DESC, review.sizeType
				).nullsLast();

				OrderSpecifier<?> priceOrderSpecifier = new OrderSpecifier<>(
					sortOrder == DESC ? Order.DESC : Order.ASC,
					review.price
				);
				yield Arrays.asList(sizeOrderSpecifier, priceOrderSpecifier);
			}
		};
	}
}
