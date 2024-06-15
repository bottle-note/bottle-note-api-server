package app.bottlenote.review.repository;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.domain.constant.ReviewSortType;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewDetail;
import app.bottlenote.review.dto.response.ReviewResponse;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.global.service.cursor.SortOrder.DESC;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;

@Slf4j
@RequiredArgsConstructor
public class CustomReviewRepositoryImpl implements CustomReviewRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public PageResponse<ReviewResponse> getReviews(
		Long alcoholId,
		PageableRequest pageableRequest,
		Long userId) {

		List<ReviewDetail> fetch = queryFactory
			.select(Projections.fields(
				ReviewDetail.class,
				review.id.as("reviewId"),
				review.content.as("reviewContent"),
				review.price.as("price"),
				review.sizeType.as("sizeType"),
				review.imageUrl.as("reviewImageUrl"),
				review.status.as("status"),
				review.createAt.as("createAt"),
				ratingSubQuery(),
				review.user.id.as("userId"),
				review.user.nickName.as("nickName"),
				review.user.imageUrl.as("userProfileImage"),
				isMyReview(userId).as("isMyReview"),
				likedByMe(userId, review.id).as("isLikedByMe"),
				hasCommentedByMe(userId, review.id).as("hasReplyByMe"),
				reviewReplyCountSubQuery(),
				likesCountSubQuery()
			))
			.from(review)
			.leftJoin(likes).on(review.id.eq(likes.review.id))
			.leftJoin(alcohol).on(alcohol.id.eq(review.alcohol.id))
			.leftJoin(rating).on(review.user.id.eq(rating.user.id))
			.where(alcohol.id.eq(alcoholId))
			.groupBy(review.id, review.sizeType, review.user)
			.orderBy(sortBy(pageableRequest.sortType(), pageableRequest.sortOrder()).toArray(new OrderSpecifier[0]))
			.offset(pageableRequest.cursor())
			.limit(pageableRequest.pageSize() + 1)
			.fetch();

		Long totalCount = queryFactory
			.select(review.id.count())
			.from(review)
			.where(review.alcohol.id.eq(alcoholId))
			.fetchOne();

		CursorPageable cursorPageable = getCursorPageable(pageableRequest, fetch);

		log.info("CURSOR Pageable info :{}", cursorPageable.toString());
		return PageResponse.of(ReviewResponse.of(totalCount, fetch), cursorPageable);
	}

	@Override
	public PageResponse<ReviewResponse> getReviewsByMe(
		Long alcoholId,
		PageableRequest pageableRequest,
		Long userId) {

		List<ReviewDetail> fetch = queryFactory
			.select(Projections.fields(
				ReviewDetail.class,
				review.id.as("reviewId"),
				review.content.as("reviewContent"),
				review.price.as("price"),
				review.sizeType.as("sizeType"),
				review.imageUrl.as("reviewImageUrl"),
				review.status.as("status"),
				review.createAt.as("createAt"),
				ratingSubQuery(),
				review.user.id.as("userId"),
				review.user.nickName.as("nickName"),
				review.user.imageUrl.as("userProfileImage"),
				isMyReview(userId).as("isMyReview"),
				likedByMe(userId, review.id).as("isLikedByMe"),
				hasCommentedByMe(userId, review.id).as("hasReplyByMe"),
				reviewReplyCountSubQuery(),
				likesCountSubQuery()
			))
			.from(review)
			.leftJoin(likes).on(review.id.eq(likes.review.id))
			.leftJoin(alcohol).on(alcohol.id.eq(review.alcohol.id))
			.leftJoin(rating).on(review.user.id.eq(rating.user.id))
			.where(review.user.id.eq(userId).and(review.alcohol.id.eq(alcoholId)))
			.groupBy(review.id, review.sizeType, review.user)
			.orderBy(sortBy(pageableRequest.sortType(), pageableRequest.sortOrder()).toArray(new OrderSpecifier[0]))
			.offset(pageableRequest.cursor())
			.limit(pageableRequest.pageSize() + 1)
			.fetch();

		Long totalCount = queryFactory
			.select(review.id.count())
			.from(review)
			.where(review.user.id.eq(userId))
			.fetchOne();

		CursorPageable cursorPageable = getCursorPageable(pageableRequest, fetch);

		log.info("CURSOR Pageable info :{}", cursorPageable.toString());

		return PageResponse.of(ReviewResponse.of(totalCount, fetch), cursorPageable);
	}


	private CursorPageable getCursorPageable(
		PageableRequest pageableRequest,
		List<ReviewDetail> fetch
	) {

		boolean hasNext = isHasNext(pageableRequest, fetch);
		return CursorPageable.builder()
			.cursor(pageableRequest.cursor() + pageableRequest.pageSize())
			.pageSize(pageableRequest.pageSize())
			.hasNext(hasNext)
			.currentCursor(pageableRequest.cursor())
			.build();
	}

	/**
	 * 다음 페이지가 있는지 확인하는 메소드
	 */
	private boolean isHasNext(
		PageableRequest pageableRequest,
		List<ReviewDetail> fetch
	) {
		boolean hasNext = fetch.size() > pageableRequest.pageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);  // Remove the extra record
		}
		return hasNext;
	}

	/*
	내가 좋아요를 누른 댓글인지 판별
	 */

	private BooleanExpression likedByMe(Long userId, NumberExpression<Long> reviewId) {
		if (userId == null) {
			return Expressions.asBoolean(false);
		}

		return JPAExpressions
			.selectOne()
			.from(likes)
			.where(likes.user.id.eq(userId)
				.and(likes.review.id.eq(reviewId)))
			.exists();
	}

	/*
	 * 내가 작성한 리뷰인지 판별
	 */
	private BooleanExpression isMyReview(Long userId) {
		if (userId == null) {
			return Expressions.asBoolean(false);
		}
		return review.user.id.eq(userId);
	}

	/*
	리뷰 댓글 개수 카운트 서브쿼리
	 */
	private Expression<Long> reviewReplyCountSubQuery() {
		return ExpressionUtils.as(
			JPAExpressions.select(reviewReply.id.count())
				.from(reviewReply)
				.where(reviewReply.review.id.eq(review.id)),
			"replyCount"
		);
	}

	/*
	별점 서브쿼리
	 */
	private Expression<Double> ratingSubQuery() {
		return ExpressionUtils.as(
			JPAExpressions.select(rating.ratingPoint.rating)
				.from(rating)
				.where(
					rating.user.id.eq(review.user.id)
						.and(rating.alcohol.id.eq(review.alcohol.id)))
			, "rating"
		);
	}

	/*
	좋아요 개수 서브쿼리
	 */
	private Expression<Long> likesCountSubQuery() {
		return ExpressionUtils.as(
			JPAExpressions.select(likes.id.count())
				.from(likes)
				.where(likes.review.id.eq(review.id))
			, "likeCount"
		);
	}

	/*
	내가 작성한 댓글이 있는 리뷰인지 판별
	 */
	private BooleanExpression hasCommentedByMe(Long userId, NumberExpression<Long> reviewId) {
		if (userId == null) {
			return Expressions.asBoolean(false);
		}

		return JPAExpressions
			.selectOne()
			.from(reviewReply)
			.where(reviewReply.userId.eq(userId)
				.and(reviewReply.review.id.eq(reviewId)))
			.exists();
	}

	private List<OrderSpecifier<?>> sortBy(ReviewSortType reviewSortType, SortOrder sortOrder) {

		NumberExpression<Long> likesCount = likes.id.count();
		return switch (reviewSortType) {
			//인기순 -> 임시로 좋아요 순으로 구현
			case POPULAR -> Collections.singletonList(sortOrder == DESC ? likesCount.desc() : likesCount.asc());
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
