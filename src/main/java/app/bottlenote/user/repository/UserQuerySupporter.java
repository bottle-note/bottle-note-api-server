package app.bottlenote.user.repository;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberPath;
import org.springframework.stereotype.Component;

import static app.bottlenote.follow.domain.QFollow.follow;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static com.querydsl.jpa.JPAExpressions.select;

@Component
public class UserQuerySupporter {

	/**
	 * 마이 페이지 사용자의 리뷰 개수를 조회한다.
	 *
	 * @param userId
	 * @return 리뷰 개수
	 */
	public Expression<Long> reviewCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(review.count())
				.from(review)
				.where(review.userId.eq(userId)),
			"reviewCount"
		);
	}

	/**
	 * 마이 페이지 사용자의 평점 개수를 조회한다.
	 *
	 * @param userId
	 * @return 평점 개수
	 */
	public Expression<Long> ratingCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(rating.count())
				.from(rating)
				.where(rating.user.id.eq(userId)),
			"ratingCount"
		);
	}

	/**
	 * 마이 페이지 사용자의 찜하기 개수를 조회한다.
	 *
	 * @param userId
	 * @return 찜하기 개수
	 */
	public Expression<Long> picksCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(picks.count())
				.from(picks)
				.where(picks.user.id.eq(userId)),
			"picksCount"
		);
	}

	/**
	 * 마이 페이지 사용자가 팔로우 하는 유저 수 를 조회한다.
	 *
	 * @param userId
	 * @return 팔로우 하는 유저 수
	 */
	public Expression<Long> followCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(follow.count())
				.from(follow)
				.where(follow.user.id.eq(userId)),
			"followCount"
		);
	}

	/**
	 * 마이 페이지 사용자를 팔로우 하는 유저 수(팔로워 수)를 조회한다.
	 *
	 * @param userId
	 * @return 팔로워 수
	 */
	public Expression<Long> followerCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(follow.count())
				.from(follow)
				.where(follow.followUser.id.eq(userId)),
			"followerCount"
		);
	}

	/**
	 * 로그인 사용자가 마이 페이지 사용자를 팔로우 하고 있는지 상태 여부를 조회한다.
	 *
	 * @param userId
	 * @param currentUserId
	 * @return 팔로우 여부 (true : 팔로우 중, false : 팔로우 중이 아님)
	 */
	public BooleanExpression isFollowSubQuery(NumberPath<Long> userId, Long currentUserId) {
		return new CaseBuilder()
			.when(follow.user.id.eq(currentUserId).and(follow.followUser.id.eq(userId)))
			.then(true)
			.otherwise(false);
	}

	/**
	 * 로그인 사용자가 마이 페이지 사용자인지 여부(나의 마이페이지인지 여부)를 조회한다.
	 *
	 * @param userId
	 * @param currentUserId
	 * @return 마이페이지 여부 (true : 나의 마이페이지, false : 나의 마이페이지가 아님)
	 */
	public BooleanExpression isMyPageSubQuery(NumberPath<Long> userId, Long currentUserId) {
		return userId.eq(currentUserId);
	}

}
