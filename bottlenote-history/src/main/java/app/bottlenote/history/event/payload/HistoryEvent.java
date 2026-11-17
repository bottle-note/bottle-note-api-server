package app.bottlenote.history.event.payload;

import app.bottlenote.history.constant.EventCategory;
import app.bottlenote.history.constant.EventType;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;

@Builder
public record HistoryEvent(
    Long userId,
    EventCategory eventCategory,
    EventType eventType,
    String redirectUrl,
    Long alcoholId,
    String content,
    Map<String, String> dynamicMessage) {

  public HistoryEvent {
    validateUserId(userId);
    validateAlcoholId(alcoholId);
  }

  private void validateUserId(Long userId) {
    Objects.requireNonNull(userId, "userId must not be null");
  }

  private void validateAlcoholId(Long alcoholId) {
    Objects.requireNonNull(alcoholId, "alcoholId must not be null");
  }
}
