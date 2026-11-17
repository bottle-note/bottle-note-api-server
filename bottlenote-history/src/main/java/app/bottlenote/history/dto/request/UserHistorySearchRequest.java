package app.bottlenote.history.dto.request;

import app.bottlenote.global.pagination.KeysetPageRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.constant.HistoryReviewFilterType;
import app.bottlenote.history.exception.UserHistoryException;
import app.bottlenote.history.exception.UserHistoryExceptionCode;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.rating.domain.RatingPoint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record UserHistorySearchRequest(
    String keyword,
    List<RatingPoint> ratingPoint,
    List<HistoryReviewFilterType> historyReviewFilterType,
    List<PicksStatus> picksStatus,
    LocalDateTime startDate,
    LocalDateTime endDate,
    SortOrder sortOrder,
    String cursor,
    Integer size) {

  public static final int DEFAULT_SIZE = 10;
  public static final int MAX_SIZE = 100;

  public UserHistorySearchRequest {
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    KeysetPageRequest page = KeysetPageRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
    LocalDateTime resolvedEnd =
        endDate != null ? endDate.plusDays(1) : LocalDateTime.now().plusDays(1);
    LocalDateTime resolvedStart = startDate != null ? startDate : LocalDateTime.now().minusYears(2);
    if (resolvedEnd.isBefore(resolvedStart)) {
      throw new UserHistoryException(UserHistoryExceptionCode.INVALID_HISTORY_DATE);
    }
    if (resolvedStart.plusYears(2).plusDays(1).isBefore(resolvedEnd)) {
      throw new UserHistoryException(UserHistoryExceptionCode.INVALID_HISTORY_DATE_RANGE);
    }
  }

  public LocalDateTime resolvedStartDate() {
    return startDate != null ? startDate : LocalDateTime.now().minusYears(2);
  }

  public LocalDateTime resolvedEndDate() {
    return endDate != null ? endDate.plusDays(1) : LocalDateTime.now().plusDays(1);
  }

  public List<EventType> toEventTypeList() {
    if (historyReviewFilterType == null || historyReviewFilterType.isEmpty()) {
      return Collections.emptyList();
    }

    List<EventType> eventTypes = new ArrayList<>();
    for (HistoryReviewFilterType filterType : historyReviewFilterType) {
      switch (filterType) {
        case ALL ->
            eventTypes.addAll(
                List.of(
                    EventType.REVIEW_CREATE,
                    EventType.BEST_REVIEW_SELECTED,
                    EventType.REVIEW_CREATE,
                    EventType.REVIEW_LIKES,
                    EventType.REVIEW_REPLY_CREATE));
        case BEST_REVIEW -> eventTypes.add(EventType.BEST_REVIEW_SELECTED);
        case REVIEW_CREATE -> eventTypes.add(EventType.REVIEW_CREATE);
        case REVIEW_LIKE -> eventTypes.add(EventType.REVIEW_LIKES);
        case REVIEW_REPLY -> eventTypes.add(EventType.REVIEW_REPLY_CREATE);
      }
    }
    return eventTypes;
  }
}
