package app.bottlenote.review.repository.custom;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.domain.constant.ReviewSortType;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.vo.ReviewInfo;
import app.bottlenote.review.repository.ReviewQuerySupporter;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.NumberExpression;
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
import static app.bottlenote.review.domain.QReviewImage.reviewImage;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.review.domain.QReviewTastingTag.reviewTastingTag;
import static app.bottlenote.review.domain.constant.ReviewActiveStatus.ACTIVE;
import static app.bottlenote.review.domain.constant.ReviewDisplayStatus.PUBLIC;
import static app.bottlenote.user.domain.QUser.user;

@Slf4j
@RequiredArgsConstructor
public class CustomReviewRepositoryImpl implements CustomReviewRepository {

	private final JPAQueryFactory queryFactory;

	private final ReviewQuerySupporter supporter;

	@Override
	public ReviewInfo getReview(Long reviewId, Long userId) {

		List<String> tastingTagList = queryFactory
			.select(reviewTastingTag.tastingTag)
			.from(reviewTastingTag)
			.where(reviewTastingTag.review.id.eq(reviewId))
			.fetch();

		return queryFactory
			.select(supporter.commonReviewInfoConstructor(reviewId, userId, tastingTagList))
			.from(review)
			.join(user).on(review.userId.eq(user.id))
			.leftJoin(likes).on(review.id.eq(likes.review.id))
			.leftJoin(alcohol).on(alcohol.id.eq(review.alcoholId))
			.leftJoin(rating).on(review.userId.eq(rating.user.id))
			.leftJoin(reviewTastingTag).on(review.id.eq(reviewTastingTag.review.id))
			.leftJoin(reviewImage).on(review.id.eq(reviewImage.review.id))
			.leftJoin(reviewReply).on(review.id.eq(reviewReply.review.id))
			.where(review.id.eq(reviewId)
				.and(review.activeStatus.eq(ACTIVE))
				.and(review.status.eq(PUBLIC)))
			.groupBy(review.id, review.sizeType, review.userId)
			.fetchOne();
	}

	@Override
	public PageResponse<ReviewListResponse> getReviews(
		Long alcoholId,
		ReviewPageableRequest reviewPageableRequest,
		Long userId
	) {
		Long currentReviewId = queryFactory
			.select(review.id)
			.from(review)
			.where(review.alcoholId.eq(alcoholId)
				.and(review.activeStatus.eq(ACTIVE))
				.and(review.status.eq(PUBLIC)))
			.limit(1)
			.fetchOne();


		List<ReviewInfo> fetch = queryFactory
			.select(supporter.reviewResponseConstructor(userId, currentReviewId))
			.from(review)
			.join(user).on(review.userId.eq(user.id))
			.leftJoin(likes).on(review.id.eq(likes.review.id))
			.leftJoin(alcohol).on(alcohol.id.eq(review.alcoholId))
			.leftJoin(rating).on(review.userId.eq(rating.user.id))
			.leftJoin(reviewTastingTag).on(review.id.eq(reviewTastingTag.review.id))
			.where(alcohol.id.eq(alcoholId)
				.and(review.activeStatus.eq(ACTIVE))
				.and(review.status.eq(PUBLIC)))
			.groupBy(review.id, review.sizeType, review.userId)
			.orderBy(sortBy(reviewPageableRequest.sortType(), reviewPageableRequest.sortOrder()).toArray(new OrderSpecifier[0]))
			.offset(reviewPageableRequest.cursor())
			.limit(reviewPageableRequest.pageSize() + 1)
			.fetch();

		Long totalCount = queryFactory
			.select(review.id.count())
			.from(review)
			.where(review.alcoholId.eq(alcoholId)
				.and(review.activeStatus.eq(ACTIVE))
				.and(review.status.eq(PUBLIC)))
			.fetchOne();

		CursorPageable cursorPageable = getCursorPageable(reviewPageableRequest, fetch);

		return PageResponse.of(ReviewListResponse.of(totalCount, fetch), cursorPageable);
	}


	@Override
	public PageResponse<ReviewListResponse> getReviewsByMe(
		Long alcoholId,
		ReviewPageableRequest reviewPageableRequest,
		Long userId
	) {

		Long bestReviewId = queryFactory
			.select(review.id)
			.from(review)
			.join(user).on(review.userId.eq(user.id))
			.leftJoin(review.reviewReplies, reviewReply)
			.leftJoin(rating).on(rating.alcohol.id.eq(review.alcoholId))
			.leftJoin(likes).on(likes.review.id.eq(review.id))
			.where(alcohol.id.eq(alcoholId)
				.and(review.activeStatus.eq(ACTIVE))
				.and(review.status.eq(PUBLIC)))
			.groupBy(user.id, user.imageUrl, user.nickName, review.id, review.content, rating.ratingPoint, review.createAt)
			.orderBy(reviewReply.count().coalesce(0L)
				.add(likes.count().coalesce(0L))
				.add(rating.ratingPoint.rating.coalesce(0.0).avg())
				.desc()
			)
			.limit(1)
			.fetchOne();

		List<ReviewInfo> fetch = queryFactory
			.select(supporter.reviewResponseConstructor(userId, bestReviewId))
			.from(review)
			.join(user).on(review.userId.eq(user.id))
			.leftJoin(likes).on(review.id.eq(likes.review.id))
			.leftJoin(alcohol).on(alcohol.id.eq(review.alcoholId))
			.leftJoin(rating).on(review.userId.eq(rating.user.id))
			.where(review.userId.eq(userId)
				.and(review.alcoholId.eq(alcoholId))
				.and(review.activeStatus.eq(ACTIVE)))
			.groupBy(review.id, review.sizeType, review.userId)
			.orderBy(sortBy(reviewPageableRequest.sortType(), reviewPageableRequest.sortOrder()).toArray(new OrderSpecifier[0]))
			.offset(reviewPageableRequest.cursor())
			.limit(reviewPageableRequest.pageSize() + 1)
			.fetch();

		Long totalCount = queryFactory
			.select(review.id.count())
			.from(review)
			.where(review.userId.eq(userId)
				.and(review.alcoholId.eq(alcoholId))
				.and(review.activeStatus.eq(ACTIVE)))
			.fetchOne();

		CursorPageable cursorPageable = getCursorPageable(reviewPageableRequest, fetch);

		log.info("CURSOR Pageable info :{}", cursorPageable.toString());

		return PageResponse.of(ReviewListResponse.of(totalCount, fetch), cursorPageable);
	}


	private CursorPageable getCursorPageable(
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
	private boolean isHasNext(
		ReviewPageableRequest reviewPageableRequest,
		List<ReviewInfo> fetch
	) {
		boolean hasNext = fetch.size() > reviewPageableRequest.pageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);  // Remove the extra record
		}
		return hasNext;
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
