package app.bottlenote.review.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.domain.constant.ReviewSortType;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewDetail;
import app.bottlenote.review.dto.response.ReviewResponse;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CustomReviewRepositoryImpl implements CustomReviewRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public PageResponse<ReviewResponse> getReviews(Long alcoholId, PageableRequest pageableRequest,
		Long userId) {

		List<ReviewDetail> fetch = queryFactory
			.select(Projections.fields(
				ReviewDetail.class,
				review.id.as("reviewId"),
				review.content.as("reviewContent"),
				review.price.as("price"),
				review.sizeType.as("sizeType"),
				review.imageUrl.as("thumbnailImage"),
				review.status.as("status"),
				review.createAt.as("reviewCreatedAt"),

				review.user.id.as("userId"),
				review.user.nickName.as("userNickname"),
				review.user.imageUrl.as("userProfileImage"),
				likedByMe(userId, review.id).as("isLikedByMe"),
				//isMyReview(userId).as("isMyReview"),

				hasCommentedByMe(userId, review.id).as("hasCommentedByMe"),

				ExpressionUtils.as(
					JPAExpressions.select(likes.id.count())
						.from(likes)
						.where(likes.review.id.eq(review.id
						)), "likeCount"
				),
				ExpressionUtils.as(
					JPAExpressions.select(reviewReply.id.count())
						.from(reviewReply)
						.where(reviewReply.review.id.eq(review.id)),
					"replyCount"
				)
			))
			.from(review)
			.leftJoin(alcohol).on(alcohol.id.eq(review.alcohol.id))
			.leftJoin(likes).on(likes.id.eq(review.user.id))
			.where(alcohol.id.eq(alcoholId))
			.groupBy(review.id)
			.orderBy(sortBy(pageableRequest.sortType(), pageableRequest.sortOrder()))
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


	private CursorPageable getCursorPageable(PageableRequest pageableRequest,
		List<ReviewDetail> fetch) {

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
	private boolean isHasNext(PageableRequest pageableRequest, List<ReviewDetail> fetch) {
		boolean hasNext = fetch.size() > pageableRequest.pageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);  // Remove the extra record
		}
		return hasNext;
	}

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

	private BooleanExpression hasCommentedByMe(Long userId, NumberExpression<Long> reviewId) {
		if (userId == null) {
			return Expressions.asBoolean(false);
		}

		return JPAExpressions
			.selectOne()
			.from(reviewReply)
			.where(reviewReply.user.id.eq(userId)
				.and(reviewReply.review.id.eq(reviewId)))
			.exists();
	}

	private OrderSpecifier<?> sortBy(ReviewSortType reviewSortType, SortOrder sortOrder) {
		return switch (reviewSortType) {
			case DATE ->
				sortOrder == SortOrder.DESC ? review.createAt.desc() : review.createAt.asc();
			case LIKES -> sortOrder == SortOrder.DESC ? likes.review.id.count().desc()
				: likes.review.id.count().asc();
		};
	}
}