package app.bottlenote.review.repository.custom;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.vo.ReviewInfo;
import app.bottlenote.review.dto.vo.UserInfo;
import app.bottlenote.review.repository.ReviewQuerySupporter;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
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
		List<ReviewInfo> fetch = queryFactory
			.select(Projections.constructor(
					ReviewInfo.class,
					review.id,
					review.content,
					review.price,
					review.sizeType,
					likes.count(),
					reviewReply.count(),
					Projections.constructor(UserInfo.class,
						user.id.as("userId"),
						user.nickName.as("nickName"),
						user.imageUrl.as("userProfileImage")
					),
					review.imageUrl,
					rating.ratingPoint.rating,
					review.viewCount,
					review.reviewLocation,
					review.status,
					supporter.isMyReview(userId),
					supporter.isLikeByMeSubquery(userId),
					supporter.hasReplyByMeSubquery(userId),
					review.isBest,
					ExpressionUtils.as(
						JPAExpressions.select(
								Expressions.stringTemplate("group_concat({0})", reviewTastingTag.tastingTag)
							)
							.from(reviewTastingTag)
							.where(reviewTastingTag.review.id.eq(review.id)),
						"tastingTag"
					),
					review.createAt
				)
			)
			.from(review)
			.join(user).on(review.userId.eq(user.id))
			.leftJoin(likes).on(review.id.eq(likes.review.id))
			.leftJoin(alcohol).on(alcohol.id.eq(review.alcoholId))
			.leftJoin(rating).on(review.userId.eq(rating.user.id))
			.leftJoin(reviewReply).on(review.id.eq(reviewReply.review.id))
			.where(review.alcoholId.eq(alcoholId)
				.and(review.activeStatus.eq(ACTIVE))
				.and(review.status.eq(PUBLIC)))
			.groupBy(review.id, review.isBest, review.sizeType, review.userId)
			.orderBy(supporter.sortBy(reviewPageableRequest.sortType(), reviewPageableRequest.sortOrder()).toArray(new OrderSpecifier[0]))
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

		CursorPageable cursorPageable = supporter.getCursorPageable(reviewPageableRequest, fetch);
		return PageResponse.of(ReviewListResponse.of(totalCount, fetch), cursorPageable);
	}

	@Override
	public PageResponse<ReviewListResponse> getReviewsByMe(
		Long alcoholId,
		ReviewPageableRequest reviewPageableRequest,
		Long userId
	) {
		List<ReviewInfo> fetch = queryFactory
			.select(Projections.constructor(
					ReviewInfo.class,
					review.id,
					review.content,
					review.price,
					review.sizeType,
					likes.count(),
					reviewReply.count(),
					Projections.constructor(UserInfo.class,
						user.id.as("userId"),
						user.nickName.as("nickName"),
						user.imageUrl.as("userProfileImage")
					),
					review.imageUrl,
					rating.ratingPoint.rating,
					review.viewCount,
					review.reviewLocation,
					review.status,
					supporter.isMyReview(userId),
					supporter.isLikeByMeSubquery(userId),
					supporter.hasReplyByMeSubquery(userId),
					review.isBest,
					ExpressionUtils.as(
						JPAExpressions.select(
								Expressions.stringTemplate("group_concat({0})", reviewTastingTag.tastingTag)
							)
							.from(reviewTastingTag)
							.where(reviewTastingTag.review.id.eq(review.id)),
						"tastingTag"
					),
					review.createAt
				)
			)
			.from(review)
			.join(user).on(review.userId.eq(user.id))
			.leftJoin(likes).on(review.id.eq(likes.review.id))
			.leftJoin(alcohol).on(alcohol.id.eq(review.alcoholId))
			.leftJoin(rating).on(review.userId.eq(rating.user.id))
			.leftJoin(reviewReply).on(review.id.eq(reviewReply.review.id))
			.where(review.userId.eq(userId)
				.and(review.activeStatus.eq(ACTIVE))
				.and(review.status.eq(PUBLIC)))
			.groupBy(review.id, review.isBest, review.sizeType, review.userId)
			.orderBy(supporter.sortBy(reviewPageableRequest.sortType(), reviewPageableRequest.sortOrder()).toArray(new OrderSpecifier[0]))
			.offset(reviewPageableRequest.cursor())
			.limit(reviewPageableRequest.pageSize() + 1)
			.fetch();

		Long totalCount = queryFactory
			.select(review.id.count())
			.from(review)
			.where(review.userId.eq(userId)
				.and(review.activeStatus.eq(ACTIVE))
				.and(review.status.eq(PUBLIC)))
			.fetchOne();

		CursorPageable cursorPageable = supporter.getCursorPageable(reviewPageableRequest, fetch);
		return PageResponse.of(ReviewListResponse.of(totalCount, fetch), cursorPageable);
	}

}
