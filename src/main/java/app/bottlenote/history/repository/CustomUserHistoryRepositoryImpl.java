package app.bottlenote.history.repository;

import app.bottlenote.history.domain.constant.EventType;
import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistoryDetail;
import app.bottlenote.picks.domain.PicksStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.history.domain.QUserHistory.userHistory;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;

@Slf4j
public class CustomUserHistoryRepositoryImpl implements CustomUserHistoryRepository {

	private final JPAQueryFactory queryFactory;

	public CustomUserHistoryRepositoryImpl(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	@Override
	public List<UserHistoryDetail> findUserHistoryListByUserId(Long userId, UserHistorySearchRequest request) {
		return queryFactory
			.select(
				Projections.constructor(
					UserHistoryDetail.class,
					userHistory.id,
					userHistory.createAt,
					userHistory.eventCategory,
					userHistory.eventType,
					userHistory.alcoholId,
					alcohol.korName,
					userHistory.imageUrl,
					userHistory.redirectUrl,
					userHistory.description,
					userHistory.message,
					userHistory.dynamicMessage
				)
			)
			.from(userHistory)
			.leftJoin(alcohol).on(userHistory.alcoholId.eq(alcohol.id))
			.leftJoin(rating).on(userHistory.alcoholId.eq(rating.id.alcoholId)
				.and(rating.id.userId.eq(userId)))
			.leftJoin(picks).on(userHistory.alcoholId.eq(picks.alcoholId))
			.where(
				userHistory.userId.eq(userId),
				request.ratingPoint() == null ? null : rating.ratingPoint.in(request.ratingPoint()),
				request.picksStatus() == null ? null :
					userHistory.eventType.in(
						request.picksStatus().stream()
							.map(status -> status == PicksStatus.PICK ? EventType.IS_PICK : EventType.UNPICK)
							.toList()
					),
				request.startDate() == null ? null : userHistory.createAt.goe(request.startDate()),
				request.endDate() == null ? null : userHistory.createAt.loe(request.endDate()),
				request.historyReviewFilterType() == null ? null : userHistory.eventType.in(request.toEventTypeList())
			)
			.orderBy(request.sortOrder().resolve(userHistory.createAt))
			.offset(request.cursor())
			.limit(request.pageSize())
			.fetch();
	}
}
