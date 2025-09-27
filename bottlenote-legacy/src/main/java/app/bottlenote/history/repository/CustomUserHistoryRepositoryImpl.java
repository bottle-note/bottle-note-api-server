package app.bottlenote.history.repository;

import static app.bottlenote.core.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.history.domain.QUserHistory.userHistory;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.shared.history.constant.EventType.IS_PICK;
import static app.bottlenote.shared.history.constant.EventType.RATING_DELETE;
import static app.bottlenote.shared.history.constant.EventType.RATING_MODIFY;
import static app.bottlenote.shared.history.constant.EventType.START_RATING;
import static app.bottlenote.shared.history.constant.EventType.UNPICK;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.global.util.SortOrderUtils;
import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistoryItem;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.shared.history.constant.EventType;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomUserHistoryRepositoryImpl implements CustomUserHistoryRepository {

  private final JPAQueryFactory queryFactory;

  public CustomUserHistoryRepositoryImpl(JPAQueryFactory queryFactory) {
    this.queryFactory = queryFactory;
  }

  @Override
  public PageResponse<UserHistorySearchResponse> findUserHistoryListByUserId(
      Long userId, UserHistorySearchRequest request) {

    // 요청에 따른 eventType 필터 구성
    final List<EventType> eventTypeFilters = new ArrayList<>();
    if (request.ratingPoint() != null) {
      eventTypeFilters.addAll(Arrays.asList(START_RATING, RATING_MODIFY, RATING_DELETE));
    }
    if (request.picksStatus() != null) {
      eventTypeFilters.addAll(
          request.picksStatus().stream()
              .map(status -> status == PicksStatus.PICK ? IS_PICK : UNPICK)
              .toList());
    }
    if (request.historyReviewFilterType() != null) {
      eventTypeFilters.addAll(request.toEventTypeList());
    }

    // ratingPoint가 있을 경우 dynamicMessage의 currentValue 조건 생성
    BooleanExpression ratingDynamicCondition = null;
    if (request.ratingPoint() != null && !request.ratingPoint().isEmpty()) {
      ratingDynamicCondition =
          request.ratingPoint().stream()
              .map(
                  point ->
                      Expressions.stringTemplate(
                              "JSON_UNQUOTE(JSON_EXTRACT({0}, '$.currentValue'))",
                              userHistory.dynamicMessage)
                          .eq(point.toString()))
              .reduce(BooleanExpression::or)
              .orElse(null);
    }

    // rating 이벤트(평점 이벤트)는 반드시 dynamicMessage 조건을 만족해야 함
    BooleanExpression ratingEventCondition = null;
    if (ratingDynamicCondition != null) {
      ratingEventCondition =
          userHistory
              .eventType
              .in(START_RATING, RATING_MODIFY, RATING_DELETE)
              .and(ratingDynamicCondition);
    }

    // rating 이벤트가 아닌 경우(예: PICK 등)는 dynamic 조건 없이 조회
    BooleanExpression nonRatingEventCondition =
        userHistory.eventType.notIn(START_RATING, RATING_MODIFY, RATING_DELETE);

    // 두 조건을 OR로 결합 – rating 이벤트인 경우에는 dynamic 조건을 적용하고, 그 외는 그대로 통과
    BooleanExpression combinedEventCondition =
        ratingEventCondition != null ? ratingEventCondition.or(nonRatingEventCondition) : null;

    // 기본 userId 조건에 이벤트 조건을 추가
    BooleanExpression condition = userHistory.userId.eq(userId);
    if (combinedEventCondition != null) {
      condition = condition.and(combinedEventCondition);
    }

    final List<UserHistoryItem> fetch =
        queryFactory
            .select(
                Projections.constructor(
                    UserHistoryItem.class,
                    userHistory.id,
                    userHistory.createAt,
                    userHistory.eventCategory,
                    userHistory.eventType,
                    userHistory.alcoholId,
                    alcohol.korName,
                    userHistory.imageUrl,
                    userHistory.redirectUrl,
                    userHistory.content,
                    userHistory.dynamicMessage))
            .distinct()
            .from(userHistory)
            .leftJoin(alcohol)
            .on(userHistory.alcoholId.eq(alcohol.id))
            .leftJoin(rating)
            .on(userHistory.alcoholId.eq(rating.id.alcoholId).and(rating.id.userId.eq(userId)))
            .leftJoin(picks)
            .on(userHistory.alcoholId.eq(picks.alcoholId).and(picks.userId.eq(userId)))
            .where(
                condition,
                isValidKeyword(request.keyword())
                    ? alcohol.korName.like("%" + request.keyword() + "%")
                    : null,
                request.startDate() == null ? null : userHistory.createAt.goe(request.startDate()),
                request.endDate() == null ? null : userHistory.createAt.loe(request.endDate()),
                eventTypeFilters.isEmpty() ? null : userHistory.eventType.in(eventTypeFilters))
            .orderBy(SortOrderUtils.resolve(request.sortOrder(), userHistory.createAt))
            .offset(request.cursor())
            .limit(request.pageSize() + 1)
            .fetch();

    final Long totalCount =
        queryFactory
            .select(userHistory.id.count())
            .from(userHistory)
            .where(userHistory.userId.eq(userId))
            .fetchOne();

    final LocalDateTime subscriptionDate =
        queryFactory.select(user.createAt).from(user).where(user.id.eq(userId)).fetchOne();

    final UserHistorySearchResponse userHistorySearchResponse =
        new UserHistorySearchResponse(totalCount, subscriptionDate, fetch);
    final CursorPageable pageable = getCursorPageable(fetch, request.cursor(), request.pageSize());

    return PageResponse.of(userHistorySearchResponse, pageable);
  }

  private boolean isValidKeyword(String keyword) {
    return keyword != null && !keyword.trim().isEmpty();
  }

  private CursorPageable getCursorPageable(
      List<UserHistoryItem> fetch, Long cursor, Long pageSize) {
    boolean hasNext = isHasNext(pageSize, fetch);
    return CursorPageable.builder()
        .currentCursor(cursor)
        .cursor(cursor + pageSize)
        .pageSize(pageSize)
        .hasNext(hasNext)
        .build();
  }

  private boolean isHasNext(Long pageSize, List<UserHistoryItem> fetch) {
    boolean hasNext = fetch.size() > pageSize;

    if (hasNext) {
      fetch.remove(fetch.size() - 1);
    }
    return hasNext;
  }
}
