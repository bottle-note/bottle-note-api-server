package app.bottlenote.user.repository;

import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static com.querydsl.jpa.JPAExpressions.select;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.dsl.NumberPath;
import org.springframework.stereotype.Component;

@Component
public class FollowQuerySupporter {

  public Expression<Long> followReviewCountSubQuery(NumberPath<Long> userId) {
    return ExpressionUtils.as(
        select(review.count()).from(review).where(review.userId.eq(userId)), "reviewCount");
  }

  public Expression<Long> followRatingCountSubQuery(NumberPath<Long> userId) {
    return ExpressionUtils.as(
        select(rating.count()).from(rating).where(rating.id.userId.eq(userId)), "ratingCount");
  }
}
