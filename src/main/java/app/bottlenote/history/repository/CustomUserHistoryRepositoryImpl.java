package app.bottlenote.history.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.history.domain.QUserHistory.userHistory;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.history.domain.constant.EventType;
import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistoryDetail;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.picks.domain.PicksStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomUserHistoryRepositoryImpl implements CustomUserHistoryRepository {

	private final JPAQueryFactory queryFactory;

	public CustomUserHistoryRepositoryImpl(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	@Override
	public PageResponse<UserHistorySearchResponse> findUserHistoryListByUserId(Long userId, UserHistorySearchRequest request) {
		final List<UserHistoryDetail> fetch = queryFactory
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
					userHistory.content,
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

		final Long totalCount = queryFactory
			.select(userHistory.id.count())
			.from(userHistory)
			.where(userHistory.userId.eq(userId))
			.fetchOne();

		final LocalDateTime subscriptionDate = queryFactory
			.select(user.createAt)
			.from(user)
			.where(user.id.eq(userId))
			.fetchOne();

		final UserHistorySearchResponse userHistorySearchResponse = new UserHistorySearchResponse(totalCount, subscriptionDate, fetch);
		final CursorPageable pageable = getCursorPageable(fetch, request.cursor(), request.pageSize());

		return PageResponse.of(userHistorySearchResponse, pageable);
	}

	private CursorPageable getCursorPageable(List<UserHistoryDetail> fetch, Long cursor, Long pageSize) {
		boolean hasNext = isHasNext(pageSize, fetch);
		return CursorPageable.builder()
			.currentCursor(cursor)
			.cursor(cursor + pageSize)
			.pageSize(pageSize)
			.hasNext(hasNext)
			.build();
	}

	private boolean isHasNext(Long pageSize, List<UserHistoryDetail> fetch) {
		boolean hasNext = fetch.size() > pageSize;

		if (hasNext) {
			fetch.remove(fetch.size() - 1);
		}
		return hasNext;
	}
}
