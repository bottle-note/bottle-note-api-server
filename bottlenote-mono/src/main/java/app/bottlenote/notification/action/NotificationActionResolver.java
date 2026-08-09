package app.bottlenote.notification.action;

import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.dto.response.NotificationListResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationActionResolver {

  private final ObjectMapper objectMapper;

  public NotificationListResponse.Action resolve(Notification notification) {
    if (!NotificationActionType.OPEN_REVIEW.name().equals(notification.getActionType())
        || notification.getActionVersion() == null
        || notification.getActionVersion().intValue() != NotificationAction.CURRENT_VERSION
        || notification.getActionTargetId() == null
        || notification.getActionTargetId() <= 0
        || notification.getActionPayload() == null) {
      return null;
    }

    try {
      byte[] serialized = objectMapper.writeValueAsBytes(notification.getActionPayload());
      if (serialized.length > NotificationAction.MAX_PAYLOAD_BYTES) {
        return null;
      }

      JsonNode payload = notification.getActionPayload();
      JsonNode replyId = payload.get("replyId");
      if (!payload.isObject()
          || payload.size() != 1
          || replyId == null
          || !replyId.isIntegralNumber()
          || !replyId.canConvertToLong()
          || replyId.longValue() <= 0) {
        return null;
      }

      return new NotificationListResponse.Action(
          NotificationActionType.OPEN_REVIEW,
          notification.getActionTargetId(),
          new OpenReviewActionPayload(replyId.longValue()),
          NotificationAction.CURRENT_VERSION,
          NotificationActionFallbackType.OPEN_NOTIFICATION_CENTER);
    } catch (JsonProcessingException | IllegalArgumentException exception) {
      return null;
    }
  }
}
