package app.bottlenote.history.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.history.constant.EventType.IS_PICK;
import static app.bottlenote.history.constant.EventType.RATING_DELETE;
import static app.bottlenote.history.constant.EventType.RATING_MODIFY;
import static app.bottlenote.history.constant.EventType.START_RATING;
import static app.bottlenote.history.constant.EventType.UNPICK;
import static app.bottlenote.history.domain.QUserHistory.userHistory;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.global.pagination.Pagination;
import app.bottlenote.global.pagination.TimeIdCursor;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import app.bottlenote.history.dto.response.UserHistoryItem;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.picks.constant.PicksStatus;
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
  private final HmacCursorCodec cursorCodec;

  public CustomUserHistoryRepositoryImpl(
      JPAQueryFactory queryFactory, HmacCursorCodec cursorCodec) {
    this.queryFactory = queryFactory;
    this.cursorCodec = cursorCodec;
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
                userHistory.createAt.goe(request.resolvedStartDate()),
                userHistory.createAt.loe(request.resolvedEndDate()),
                eventTypeFilters.isEmpty() ? null : userHistory.eventType.in(eventTypeFilters))
            .orderBy(
                request.sortOrder().resolve(userHistory.createAt),
                request.sortOrder().resolve(userHistory.id))
            .where(historySeek(userId, request))
            .limit(request.size() + 1L)
            .fetch();

    final LocalDateTime subscriptionDate =
        queryFactory.select(user.createAt).from(user).where(user.id.eq(userId)).fetchOne();
    String context = historyContext(userId, request);
    Pagination.PageSlice<UserHistoryItem> slice =
        Pagination.fromOverflow(
            fetch,
            request.size(),
            item ->
                cursorCodec.encode(
                    context, TimeIdCursor.keys(item.getCreatedAt(), item.getHistoryId())));
    return PageResponse.of(
        UserHistorySearchResponse.of(subscriptionDate, slice.items()), slice.pagination());
  }

  private boolean isValidKeyword(String keyword) {
    return keyword != null && !keyword.trim().isEmpty();
  }

  private BooleanExpression historySeek(Long userId, UserHistorySearchRequest request) {
    if (request.cursor() == null || request.cursor().isBlank()) {
      return null;
    }
    var claims = cursorCodec.verify(request.cursor(), historyContext(userId, request));
    LocalDateTime lastCreateAt = TimeIdCursor.time(claims);
    Long lastId = TimeIdCursor.id(claims);
    if (request.sortOrder() == SortOrder.ASC) {
      return userHistory
          .createAt
          .gt(lastCreateAt)
          .or(userHistory.createAt.eq(lastCreateAt).and(userHistory.id.gt(lastId)));
    }
    return userHistory
        .createAt
        .lt(lastCreateAt)
        .or(userHistory.createAt.eq(lastCreateAt).and(userHistory.id.lt(lastId)));
  }

  static String historyContext(Long userId, UserHistorySearchRequest request) {
    return "history.list:"
        + userId
        + ":"
        + request.keyword()
        + ":"
        + request.sortOrder()
        + ":"
        + request.ratingPoint()
        + ":"
        + request.historyReviewFilterType()
        + ":"
        + request.picksStatus()
        + ":"
        + request.startDate()
        + ":"
        + request.endDate();
  }
}
