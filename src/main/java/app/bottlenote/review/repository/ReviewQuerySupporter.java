package app.bottlenote.review.repository;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.domain.constant.ReviewSortType;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.vo.LocationInfo;
import app.bottlenote.review.dto.vo.ReviewInfo;
import app.bottlenote.review.dto.vo.UserInfo;
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
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static app.bottlenote.global.service.cursor.SortOrder.DESC;
import static app.bottlenote.like.domain.LikeStatus.LIKE;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.review.domain.constant.ReviewReplyStatus.NORMAL;
import static app.bottlenote.user.domain.QUser.user;

@Component
public class ReviewQuerySupporter {
	/**
	 * 리뷰 상세 조회 API에 사용되는 생상자 Projection 메서드입니다.
	 *
	 * @param reviewId          리뷰 ID
	 * @param userId            유저 ID
	 * @param reviewTastingTags 리뷰 테이스팅 태그
	 * @return ReviewDetailResponse.ReviewInfo
	 */
	public ConstructorExpression<ReviewInfo> commonReviewInfoConstructor(Long reviewId, Long userId, List<String> reviewTastingTags) {
		return Projections.constructor(
			ReviewInfo.class,
			review.id.as("reviewId"),
			review.content.as("reviewContent"),
			review.price.as("price"),
			review.sizeType.as("sizeType"),
			likesCountSubquery(),
			reviewReplyCountSubquery(),
			review.imageUrl.as("reviewImageUrl"),
			Projections.constructor(UserInfo.class,
				user.id.as("userId"),
				user.nickName.as("nickName"),
				user.imageUrl.as("userProfileImage")
			),
			ratingSubquery(),
			review.viewCount.as("viewCount"),
			Projections.constructor(LocationInfo.class,
				review.reviewLocation.name.as("locationName"),
				review.reviewLocation.zipCode.as("zipCode"),
				review.reviewLocation.address.as("address"),
				review.reviewLocation.detailAddress.as("detailAddress"),
				review.reviewLocation.category.as("category"),
				review.reviewLocation.mapUrl.as("mapUrl"),
				review.reviewLocation.latitude.as("latitude"),
				review.reviewLocation.longitude.as("longitude")
			),
			review.status.as("status"),
			isMyReview(userId),
			isLikeByMeSubquery(userId),
			hasReplyByMeSubquery(userId),
			isBestReviewSubquery(reviewId),
			Expressions.constant(reviewTastingTags),
			review.createAt.as("createAt")
		);
	}

	/***
	 * 현재 리뷰가 베스트 리뷰인지 판별하는 서브쿼리
	 */
	public BooleanExpression isBestReviewSubquery(Long reviewId) {
		return reviewId != null ? review.id.eq(reviewId) : review.id.isNull().and(review.isBest.eq(true));
	}

	/**
	 * 내가 댓글을 단 리뷰인지 판별
	 */
	public BooleanExpression hasReplyByMeSubquery(Long userId) {

		BooleanExpression eqUserId = 1 > userId ?
			reviewReply.userId.isNull() : reviewReply.userId.eq(userId);

		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(reviewReply)
				.where(reviewReply.review.id.eq(review.id)
					.and(eqUserId
						.and(reviewReply.status.eq(NORMAL))))
				.exists()
		).as("hasReplyByMe");
	}

	/***
	 내가 좋아요를 누른 리뷰인지 판별
	 */
	public BooleanExpression isLikeByMeSubquery(Long userId) {
		if (userId < 1) {
			return Expressions.asBoolean(false);
		}
		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(likes)
				.where(
					likes.review.id.eq(review.id)
						.and(likes.userInfo.userId.eq(userId))
						.and(likes.status.eq(LIKE))
				).exists()
		).as("isLikedByMe");
	}

	/***
	 * 내가 작성한 리뷰인지 판별
	 */
	public BooleanExpression isMyReview(Long userId) {
		if (Objects.isNull(userId) || 1 > userId) {
			return Expressions.asBoolean(false);
		}
		return review.userId.eq(userId).as("isMyReview");
	}

	/***
	 좋아요 개수 서브쿼리
	 */
	public Expression<Long> likesCountSubquery() {
		return ExpressionUtils.as(
			JPAExpressions.select(likes.id.count())
				.from(likes)
				.where(likes.review.id.eq(review.id).and(likes.status.eq(LIKE)))
			, "likeCount"
		);
	}

	/***
	 별점 서브쿼리
	 */
	public Expression<Double> ratingSubquery() {
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
	public Expression<Long> reviewReplyCountSubquery() {
		return ExpressionUtils.as(
			JPAExpressions.select(reviewReply.id.count())
				.from(reviewReply)
				.where(reviewReply.review.id.eq(review.id)
					.and(reviewReply.status.eq(NORMAL))),
			"replyCount"
		);
	}


	public CursorPageable getCursorPageable(
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
	public boolean isHasNext(
		ReviewPageableRequest reviewPageableRequest,
		List<ReviewInfo> fetch
	) {
		boolean hasNext = fetch.size() > reviewPageableRequest.pageSize();
		if (hasNext) {
			fetch.remove(fetch.size() - 1);  // Remove the extra record
		}
		return hasNext;
	}

	public List<OrderSpecifier<?>> sortBy(ReviewSortType reviewSortType, SortOrder sortOrder) {
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
