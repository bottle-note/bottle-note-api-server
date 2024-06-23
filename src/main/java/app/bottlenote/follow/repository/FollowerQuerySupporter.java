package app.bottlenote.follow.repository;

import app.bottlenote.follow.dto.dsl.FollowPageableCriteria;
import app.bottlenote.follow.dto.response.FollowDetail;
import app.bottlenote.global.service.cursor.CursorPageable;
import com.querydsl.core.types.dsl.NumberPath;
import org.springframework.stereotype.Component;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;

import static app.bottlenote.rating.domain.QRating.rating;
import static com.querydsl.jpa.JPAExpressions.select;
import static app.bottlenote.review.domain.QReview.review;

import java.util.List;


@Component
public class FollowerQuerySupporter {



	public Expression<Long> followerReviewCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(review.count())
				.from(review)
				.where(review.user.id.eq(userId)),
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
