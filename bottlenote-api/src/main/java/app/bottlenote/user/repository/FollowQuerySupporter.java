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

  // 별점 0은 별점 없음이므로 마이페이지와 같이 평가 수에서 뺀다.
  public Expression<Long> followRatingCountSubQuery(NumberPath<Long> userId) {
    return ExpressionUtils.as(
        select(rating.count())
            .from(rating)
            .where(rating.id.userId.eq(userId), rating.ratingPoint.rating.gt(0.0)),
        "ratingCount");
  }
}
