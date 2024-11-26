package app.bottlenote.user.repository;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowDetail;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.dsl.NumberPath;
import org.springframework.stereotype.Component;

import java.util.List;

import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static com.querydsl.jpa.JPAExpressions.select;


@Component
public class FollowerQuerySupporter {

	public Expression<Long> followerReviewCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(review.count())
				.from(review)
				.where(review.userId.eq(userId)),
			"reviewCount"
		);
	}

	public Expression<Long> followerRatingCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(rating.count())
				.from(rating)
				.where(rating.user.id.eq(userId)),
			"ratingCount"
		);
	}

	public CursorPageable followCursorPageable(FollowPageableCriteria criteria, List<FollowDetail> followDetails) {
		boolean hasNext = isHasNext(criteria, followDetails);
		return CursorPageable.builder()
			.cursor(criteria.cursor() + criteria.pageSize())
			.pageSize(criteria.pageSize())
			.hasNext(hasNext)
			.currentCursor(criteria.cursor())
			.build();
	}

	private boolean isHasNext(FollowPageableCriteria criteria, List<FollowDetail> fetch) {
		boolean hasNext = fetch.size() > criteria.pageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);
		}
		return hasNext;
	}
}
