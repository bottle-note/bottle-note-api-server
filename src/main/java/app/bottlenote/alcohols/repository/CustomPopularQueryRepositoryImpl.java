package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.response.Populars;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static com.querydsl.core.types.dsl.Expressions.FALSE;
import static com.querydsl.core.types.dsl.Expressions.numberTemplate;


@RequiredArgsConstructor
public class CustomPopularQueryRepositoryImpl implements CustomPopularQueryRepository {
	private final JPAQueryFactory queryFactory;

	/**
	 * 주간 인기 위스키 리스트 조회
	 *
	 * @param size   주간 인기 위스키 조회 개수
	 * @param userId 로그인 시 사용자 id
	 */
	@Override
	public List<Populars> getPopularOfWeek(Integer size, Long userId) {
		return queryFactory
			.select(Projections.constructor(
				Populars.class,
				alcohol.id.as("alcoholId"),
				alcohol.korName.as("korName"),
				alcohol.engName.as("engName"),
				rating.ratingPoint.rating
					.avg().multiply(2)
					.castToNum(Double.class)
					.round().divide(2)
					.coalesce(0.0).as("rating"),
				alcohol.korCategory.as("korCategory"),
				alcohol.engCategory.as("engCategory"),
				alcohol.imageUrl.as("imageUrl"),
				isPicked(userId).as("isPicked")
			))
			.from(alcohol)
			.leftJoin(rating).on(alcohol.id.eq(rating.alcohol.id))
			.where(
				// 주간 조건은 데이터가 없어 일단 전체 데이터 대상으로 조회
				// rating.createAt.between(
				// 	LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
				// 	LocalDateTime.now()
				// )
			)
			.groupBy(alcohol.id)
			.orderBy(rating.ratingPoint.rating.avg().desc())
			.orderBy(numberTemplate(Double.class, "function('rand')").asc())
			.limit(size)
			.fetch();
	}

	/**
	 * 내가 픽 했는지 확인 하는 서브쿼리
	 */
	private BooleanExpression isPicked(Long userId) {
		if (userId == null)
			return FALSE;
		return JPAExpressions
			.selectFrom(picks)
			.where(picks.user.id.eq(userId)
				.and(picks.alcohol.id.eq(alcohol.id)))
			.exists();
	}
}
