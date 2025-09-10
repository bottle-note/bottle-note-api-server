package app.bottlenote.user.repository;

import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static com.querydsl.jpa.JPAExpressions.select;

import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.RelationUserItem;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.dsl.NumberPath;
import java.util.List;
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

  public CursorPageable followingCursorPageable(
      FollowPageableCriteria criteria, List<RelationUserItem> followingDetails) {
    boolean hasNext = isHasNext(criteria, followingDetails);
    return CursorPageable.builder()
        .cursor(criteria.cursor() + criteria.pageSize())
        .pageSize(criteria.pageSize())
        .hasNext(hasNext)
        .currentCursor(criteria.cursor())
        .build();
  }

  public CursorPageable followerCursorPageable(
      FollowPageableCriteria criteria, List<RelationUserItem> followerDetails) {
    boolean hasNext = isHasNext(criteria, followerDetails);
    return CursorPageable.builder()
        .cursor(criteria.cursor() + criteria.pageSize())
        .pageSize(criteria.pageSize())
        .hasNext(hasNext)
        .currentCursor(criteria.cursor())
        .build();
  }

  private boolean isHasNext(FollowPageableCriteria criteria, List<?> fetch) {
    boolean hasNext = fetch.size() > criteria.pageSize();

    if (hasNext) {
      fetch.remove(fetch.size() - 1);
    }
    return hasNext;
  }
}
