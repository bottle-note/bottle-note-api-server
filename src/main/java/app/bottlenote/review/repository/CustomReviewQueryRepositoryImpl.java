package app.bottlenote.review.repository;

import app.bottlenote.like.domain.QLikes;
import app.bottlenote.rating.domain.QRating;
import app.bottlenote.review.domain.QReview;
import app.bottlenote.review.domain.QReviewReply;
import app.bottlenote.user.domain.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static app.bottlenote.alcohols.dto.response.AlcoholDetail.ReviewOfAlcoholDetail;

@RequiredArgsConstructor
public class CustomReviewQueryRepositoryImpl implements CustomReviewQueryRepository {

	private final JPAQueryFactory queryFactory;


	@Override
	public List<ReviewOfAlcoholDetail> findBestReviewsForAlcoholDetail(Long alcoholId, Long userId) {

		return List.of();
	}

	@Override
	public List<ReviewOfAlcoholDetail> findReviewsForAlcoholDetail(Long alcoholId, Long userId) {
		QReview review = QReview.review;
		QUser user = QUser.user;
		QReviewReply reviewReply = QReviewReply.reviewReply;
		QRating rating = QRating.rating;
		QLikes likes = QLikes.likes;

		return queryFactory
			.select(Projections.constructor(
				ReviewOfAlcoholDetail.class,
				user.id.as("userId"),
				user.imageUrl.as("imageUrl"),
				user.nickName.as("nickName"),
				review.id.as("reviewId"),
				review.content.as("reviewContent"),
				rating.ratingPoint.rating.coalesce(0.0).as("rating"),
				review.sizeType.as("sizeType"),
				review.price.as("price"),
				review.viewCount.coalesce(0L).as("viewCount"),
				likes.id.count().as("likeCount"),
				new CaseBuilder()
					.when(likes.user.id.eq(userId))
					.then("TRUE").otherwise("FALSE")
					.as("isMyLike"),
				reviewReply.id.count().as("replyCount"),
				new CaseBuilder()
					.when(reviewReply.user.id.eq(userId))
					.then("TRUE").otherwise("FALSE")
					.as("isMyReply"),
				review.status.as("status"),
				review.imageUrl.as("reviewImageUrl"),
				review.createAt.as("createAt")
			))
			.from(review)
			.join(review.user, user)
			.leftJoin(review.reviewReplies, reviewReply)
			.leftJoin(rating).on(rating.alcohol.id.eq(review.alcohol.id).and(rating.user.id.eq(user.id)))
			.leftJoin(likes).on(likes.review.id.eq(review.id))
			.where(review.alcohol.id.eq(alcoholId))
			.groupBy(user.id, user.imageUrl, user.nickName, review.id, review.content, rating.ratingPoint, review.createAt)
			.orderBy(review.createAt.desc())
			.limit(4)
			.fetch();
	}
}
