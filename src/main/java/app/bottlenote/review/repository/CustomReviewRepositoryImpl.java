package app.bottlenote.review.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.review.domain.QReview.review;

import app.bottlenote.review.dto.response.ReviewResponse;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomReviewRepositoryImpl implements CustomReviewRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<ReviewResponse> getReviews(Long alcoholId) {

		return queryFactory
			.select(Projections.fields(
				ReviewResponse.class,
				review.id.as("reviewId"),
				review.price.as("price"),
				review.sizeType.as("sizeType"),
				review.user.id.as("userId"),
				review.user.nickName.as("userNickname"),
				review.user.imageUrl.as("userProfileImage"),
				ExpressionUtils.as(
					JPAExpressions.select(likes.id.count())
						.from(likes)
						.where(likes.review.id.eq(review.id
						)), "likeCount"
				)
			))
			.from(alcohol)
			.leftJoin(review).on(alcohol.id.eq(review.alcohol.id))
			.where(alcohol.id.eq(alcoholId))
			.fetch();
	}
}
