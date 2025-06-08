package app.bottlenote.picks.repository;

import app.bottlenote.picks.constant.PicksStatus;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import org.springframework.stereotype.Component;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static com.querydsl.jpa.JPAExpressions.select;

@Component
public class PicksQuerySupporter {

	/**
	 * 마이 페이지 사용자의 찜하기 개수를 조회한다.
	 *
	 * @param userId 마이 페이지 사용자
	 * @return 찜하기 개수
	 */
	public Expression<Long> picksCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
				select(picks.count())
						.from(picks)
						.where(picks.userId.eq(userId)
								.and(picks.status.eq(PicksStatus.PICK))),
				"picksCount"
		);
	}

	/**
	 * 사용자가 술을 찜 했는지 여부를 확인하는 서브 쿼리
	 *
	 * @param userId 사용자의 ID
	 * @return BooleanExpression
	 */
	public BooleanExpression isPickedSubQuery(Long userId) {
		if (userId == null)
			return Expressions.FALSE;

		return Expressions.asBoolean(
				JPAExpressions
						.selectOne()
						.from(picks)
						.where(picks.alcoholId.eq(alcohol.id),
								picks.userId.eq(userId))
						.exists()
		);
	}

	public BooleanExpression isPickedBothSubQuery(Long userId, Long targetUserId) {
		if (userId == null || targetUserId == null)
			return Expressions.FALSE;

		return JPAExpressions
				.selectOne()
				.from(picks)
				.where(picks.alcoholId.eq(alcohol.id)
						.and(picks.userId.eq(userId))
						.and(picks.status.eq(PicksStatus.PICK)))
				.exists();
	}

	/**
	 * 술의 전체 찜 개수를 조회하는 서브 쿼리
	 *
	 * @param alcoholId 술의 ID
	 * @return Expression<Long>
	 */
	public Expression<Long> totalPicksCountSubQuery(NumberPath<Long> alcoholId) {
		return ExpressionUtils.as(JPAExpressions
				.select(picks.count())
				.from(picks)
				.where(picks.alcoholId.eq(alcoholId)), "totalPicksCount");
	}
}
