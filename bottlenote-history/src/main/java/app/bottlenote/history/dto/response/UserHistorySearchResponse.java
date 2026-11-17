package app.bottlenote.history.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserHistorySearchResponse(
    LocalDateTime subscriptionDate, List<UserHistoryItem> userHistories) {

  public static UserHistorySearchResponse of(
      LocalDateTime subscriptionDate, List<UserHistoryItem> userHistories) {
    return new UserHistorySearchResponse(subscriptionDate, userHistories);
  }
}
