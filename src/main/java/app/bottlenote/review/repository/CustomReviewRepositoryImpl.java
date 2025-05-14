package app.bottlenote.review.repository;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.like.constant.LikeStatus;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.facade.payload.ReviewInfo;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.constant.ReviewActiveStatus.ACTIVE;
import static app.bottlenote.review.constant.ReviewDisplayStatus.PUBLIC;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewImage.reviewImage;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.review.repository.ReviewQuerySupporter.getCursorPageable;
import static app.bottlenote.review.repository.ReviewQuerySupporter.getTastingTag;
import static app.bottlenote.review.repository.ReviewQuerySupporter.getUserInfo;
import static app.bottlenote.review.repository.ReviewQuerySupporter.hasReplyByMeSubquery;
import static app.bottlenote.review.repository.ReviewQuerySupporter.isLikeByMeSubquery;
import static app.bottlenote.review.repository.ReviewQuerySupporter.isMyReview;
import static app.bottlenote.review.repository.ReviewQuerySupporter.sortBy;
import static app.bottlenote.user.domain.QUser.user;

@Slf4j
@RequiredArgsConstructor
public class CustomReviewRepositoryImpl implements CustomReviewRepository {

	private final JPAQueryFactory queryFactory;

	private static ConstructorExpression<ReviewInfo> composeReviewInfoResult(Long userId) {
		return Projections.constructor(
				ReviewInfo.class,
				// 기본 리뷰 정보
				review.id,
				review.content,
				review.imageUrl,
				review.createAt,
				reviewImage.id.countDistinct(),

				// 사용자 정보
				getUserInfo(),
				isMyReview(userId),

				// 리뷰 상태 및 속성
				review.status,
				review.isBest,
				review.reviewLocation,
				review.sizeType,

				// 가격 및 평점 정보
				review.price,
				rating.ratingPoint.rating,

				// 좋아요 및 댓글 정보
				likes.countDistinct(),
				reviewReply.countDistinct(),
				isLikeByMeSubquery(userId),
				hasReplyByMeSubquery(userId),

				// 기타 정보
				review.viewCount,
				getTastingTag()
		);
	}

	@Override
	public ReviewInfo getReview(Long reviewId, Long userId) {
		return queryFactory.select(composeReviewInfoResult(userId))
				.from(review)
				.join(user).on(review.userId.eq(user.id))
				.leftJoin(likes).on(review.id.eq(likes.reviewId).and(likes.status.eq(LikeStatus.LIKE)))
				.leftJoin(alcohol).on(alcohol.id.eq(review.alcoholId))
				.leftJoin(rating).on(rating.id.alcoholId.eq(review.alcoholId).and(rating.id.userId.eq(review.userId)))
				.leftJoin(reviewImage).on(review.id.eq(reviewImage.review.id))
				.leftJoin(reviewReply).on(review.id.eq(reviewReply.reviewId))
				.where(review.id.eq(reviewId)
						.and(review.userId.eq(userId).or(review.status.eq(PUBLIC)))
						.and(review.activeStatus.eq(ACTIVE)))
				.groupBy(review.id, review.isBest, review.sizeType, review.userId)
				.fetchOne();
	}

	@Override
	public PageResponse<ReviewListResponse> getReviews(
			Long alcoholId,
			ReviewPageableRequest reviewPageableRequest,
			Long userId
	) {
		List<ReviewInfo> fetch = queryFactory.select(composeReviewInfoResult(userId))
				.from(review)
				.join(user).on(review.userId.eq(user.id))
				.leftJoin(likes).on(review.id.eq(likes.reviewId).and(likes.status.eq(LikeStatus.LIKE)))
				.leftJoin(alcohol).on(alcohol.id.eq(review.alcoholId))
				.leftJoin(rating).on(rating.id.alcoholId.eq(review.alcoholId).and(rating.id.userId.eq(review.userId)))
				.leftJoin(reviewReply).on(review.id.eq(reviewReply.reviewId))
				.leftJoin(reviewImage).on(review.id.eq(reviewImage.review.id))
				.where(review.alcoholId.eq(alcoholId)
						.and(review.userId.eq(userId).or(review.status.eq(PUBLIC))) // 내 리뷰는 모두 조회 아니면 공개된 리뷰만 조회
						.and(review.activeStatus.eq(ACTIVE)))
				.groupBy(review.id, review.isBest, review.sizeType, review.userId)
				.orderBy(sortBy(reviewPageableRequest.sortType(), reviewPageableRequest.sortOrder()).toArray(new OrderSpecifier[0]))
				.offset(reviewPageableRequest.cursor())
				.limit(reviewPageableRequest.pageSize() + 1)
				.fetch();

		Long totalCount = queryFactory
				.select(review.id.count())
				.from(review)
				.where(review.alcoholId.eq(alcoholId)
						.and(review.userId.eq(userId).or(review.status.eq(PUBLIC))) // 내 리뷰는 모두 조회 아니면 공개된 리뷰만 조회
						.and(review.activeStatus.eq(ACTIVE)))
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
		//특정한 위스키의 내 리뷰만 조회
		List<ReviewInfo> fetch = queryFactory.select(composeReviewInfoResult(userId))
				.from(review)
				.join(alcohol).on(alcohol.id.eq(review.alcoholId).and(alcohol.id.eq(alcoholId)))
				.join(user).on(review.userId.eq(user.id))
				.leftJoin(likes).on(review.id.eq(likes.reviewId).and(likes.status.eq(LikeStatus.LIKE)))
				.leftJoin(rating).on(rating.id.alcoholId.eq(review.alcoholId).and(rating.id.userId.eq(review.userId)))
				.leftJoin(reviewReply).on(review.id.eq(reviewReply.reviewId))
				.leftJoin(reviewImage).on(review.id.eq(reviewImage.review.id))
				.where(review.userId.eq(userId)
						.and(review.activeStatus.eq(ACTIVE)))
				//.and(review.status.eq(PUBLIC)))// 공개 여부와 상관 없이 모두 조회
				.groupBy(review.id, review.isBest, review.sizeType, review.userId)
				.orderBy(sortBy(reviewPageableRequest.sortType(), reviewPageableRequest.sortOrder()).toArray(new OrderSpecifier[0]))
				.offset(reviewPageableRequest.cursor())
				.limit(reviewPageableRequest.pageSize() + 1)
				.fetch();

		Long totalCount = queryFactory
				.select(review.id.count())
				.from(review)
				.where(review.userId.eq(userId)
						.and(review.alcoholId.eq(alcoholId))
						.and(review.activeStatus.eq(ACTIVE))) //
				//.and(review.status.eq(PUBLIC))) // 공개 여부와 상관 없이 모두 조회
				.fetchOne();

		CursorPageable cursorPageable = getCursorPageable(reviewPageableRequest, fetch);
		return PageResponse.of(ReviewListResponse.of(totalCount, fetch), cursorPageable);
	}

}
