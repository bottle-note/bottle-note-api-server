package app.bottlenote.history.dto.request;

import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.constant.HistoryReviewFilterType;
import app.bottlenote.history.exception.UserHistoryException;
import app.bottlenote.history.exception.UserHistoryExceptionCode;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.shared.cursor.SortOrder;
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
    Long cursor,
    Long pageSize) {

  public UserHistorySearchRequest {
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    cursor = cursor != null ? cursor : 0L;
    pageSize = pageSize != null ? pageSize : 10L;
    endDate = endDate != null ? endDate.plusDays(1) : LocalDateTime.now().plusDays(1);
    startDate = startDate != null ? startDate : LocalDateTime.now().minusYears(2);
    if (endDate.isBefore(startDate)) {
      throw new UserHistoryException(UserHistoryExceptionCode.INVALID_HISTORY_DATE);
    }
    if (startDate.plusYears(2).plusDays(1).isBefore(endDate)) {
      throw new UserHistoryException(UserHistoryExceptionCode.INVALID_HISTORY_DATE_RANGE);
    }
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
