package app.bottlenote.review.repository.custom;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.global.service.cursor.SortOrder.DESC;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewTastingTag.reviewTastingTag;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.domain.constant.ReviewSortType;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.repository.ReviewQuerySupporter;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CustomReviewRepositoryImpl implements CustomReviewRepository {

	private final JPAQueryFactory queryFactory;

	private final ReviewQuerySupporter supporter;

	@Override
	public ReviewResponse getReview(Long reviewId, Long userId) {
		return null;
	}

	@Override
	public PageResponse<ReviewListResponse> getReviews(
		Long alcoholId,
		PageableRequest pageableRequest,
		Long userId
	) {
		List<ReviewResponse> fetch = queryFactory
			.select(supporter.reviewResponseConstructor(userId))
			.from(review)
			.join(user).on(review.userId.eq(user.id))
			.leftJoin(likes).on(review.id.eq(likes.review.id))
			.leftJoin(alcohol).on(alcohol.id.eq(review.alcoholId))
			.leftJoin(rating).on(review.userId.eq(rating.user.id))
			.leftJoin(reviewTastingTag).on(review.id.eq(reviewTastingTag.review.id))
			.where(alcohol.id.eq(alcoholId))
			.groupBy(review.id, review.sizeType, review.userId)
			.orderBy(sortBy(pageableRequest.sortType(), pageableRequest.sortOrder()).toArray(new OrderSpecifier[0]))
			.offset(pageableRequest.cursor())
			.limit(pageableRequest.pageSize() + 1)
			.fetch();

		fetch.forEach(reviewDetail -> {
			List<String> tastingTags = queryFactory
				.select(reviewTastingTag.tastingTag)
				.from(reviewTastingTag)
				.where(reviewTastingTag.review.id.eq(reviewDetail.getReviewId()))
				.fetch();
			reviewDetail.updateTastingTagList(tastingTags);
		});

		Long totalCount = queryFactory
			.select(review.id.count())
			.from(review)
			.where(review.alcoholId.eq(alcoholId))
			.fetchOne();

		CursorPageable cursorPageable = getCursorPageable(pageableRequest, fetch);

		return PageResponse.of(ReviewListResponse.of(totalCount, fetch), cursorPageable);
	}


	@Override
	public PageResponse<ReviewListResponse> getReviewsByMe(
		Long alcoholId,
		PageableRequest pageableRequest,
		Long userId
	) {
		List<ReviewResponse> fetch = queryFactory
			.select(supporter.reviewResponseConstructor(userId))
			.from(review)
			.join(user).on(review.userId.eq(user.id))
			.leftJoin(likes).on(review.id.eq(likes.review.id))
			.leftJoin(alcohol).on(alcohol.id.eq(review.alcoholId))
			.leftJoin(rating).on(review.userId.eq(rating.user.id))
			.where(review.userId.eq(userId).and(review.alcoholId.eq(alcoholId)))
			.groupBy(review.id, review.sizeType, review.userId)
			.orderBy(sortBy(pageableRequest.sortType(), pageableRequest.sortOrder()).toArray(new OrderSpecifier[0]))
			.offset(pageableRequest.cursor())
			.limit(pageableRequest.pageSize() + 1)
			.fetch();

		fetch.forEach(reviewDetail -> {
			List<String> tastingTags = queryFactory
				.select(reviewTastingTag.tastingTag)
				.from(reviewTastingTag)
				.where(reviewTastingTag.review.id.eq(reviewDetail.getReviewId()))
				.fetch();
			reviewDetail.updateTastingTagList(tastingTags);
		});

		Long totalCount = queryFactory
			.select(review.id.count())
			.from(review)
			.where(review.userId.eq(userId))
			.fetchOne();

		CursorPageable cursorPageable = getCursorPageable(pageableRequest, fetch);

		log.info("CURSOR Pageable info :{}", cursorPageable.toString());

		return PageResponse.of(ReviewListResponse.of(totalCount, fetch), cursorPageable);
	}


	private CursorPageable getCursorPageable(
		PageableRequest pageableRequest,
		List<ReviewResponse> fetch
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
		List<ReviewResponse> fetch
	) {
		boolean hasNext = fetch.size() > pageableRequest.pageSize();

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
