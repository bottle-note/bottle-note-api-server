package app.bottlenote.review.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.response.ReviewDetail;
import app.bottlenote.review.dto.response.ReviewResponse;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
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
	public PageResponse<ReviewResponse> getReviews(Long alcoholId, CursorPageable pageable,
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
			.where(alcohol.id.eq(alcoholId))
			.orderBy(review.createAt.asc())
			.offset(pageable.getCursor())
			.limit(pageable.getPageSize() + 1)
			.fetch();

		Long totalCount = queryFactory
			.select(review.id.count())
			.from(review)
			.where(review.alcohol.id.eq(alcoholId))
			.fetchOne();

		CursorPageable cursorPageable = getCursorPageable(pageable, fetch);

		log.info("CURSOR Pageable info :{}", cursorPageable.toString());
		return PageResponse.of(ReviewResponse.of(totalCount, fetch), cursorPageable);
	}


	private CursorPageable getCursorPageable(CursorPageable pageable, List<ReviewDetail> fetch) {

		boolean hasNext = isHasNext(pageable, fetch);
		return CursorPageable.builder()
			.cursor(pageable.getCursor() + pageable.getPageSize())
			.pageSize(pageable.getPageSize())
			.hasNext(hasNext)
			.currentCursor(pageable.getCursor())
			.build();
	}

	/**
	 * 다음 페이지가 있는지 확인하는 메소드
	 */
	private boolean isHasNext(CursorPageable pageable, List<ReviewDetail> fetch) {
		boolean hasNext = fetch.size() > pageable.getPageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);  // Remove the extra record
		}
		return hasNext;
	}

	//TODO :: ORDER SPECIFIER 구현예정
}
