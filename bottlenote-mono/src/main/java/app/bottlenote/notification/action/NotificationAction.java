package app.bottlenote.notification.action;

import app.bottlenote.notification.constant.NotificationActionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.nio.charset.StandardCharsets;

public record NotificationAction(
    NotificationActionType type,
    Long targetId,
    JsonNode payload,
    Integer version) {

  public static final int CURRENT_VERSION = 1;
  public static final int MAX_PAYLOAD_BYTES = 1024;

  public NotificationAction {
    if (type != NotificationActionType.OPEN_REVIEW) {
      throw new IllegalArgumentException("지원하지 않는 알림 Action입니다.");
    }
    if (targetId == null || targetId <= 0) {
      throw new IllegalArgumentException("Action 대상 식별자는 양수여야 합니다.");
    }
    if (version == null || version != CURRENT_VERSION) {
      throw new IllegalArgumentException("지원하지 않는 알림 Action 버전입니다.");
    }
    if (payload == null) {
      throw new IllegalArgumentException("알림 Action payload는 필수입니다.");
    }
    payload = payload.deepCopy();
    validateOpenReviewPayload(payload);
  }

  public static NotificationAction openReview(Long reviewId, Long replyId) {
    OpenReviewActionPayload payload = new OpenReviewActionPayload(replyId);
    return new NotificationAction(
        NotificationActionType.OPEN_REVIEW,
        reviewId,
        JsonNodeFactory.instance.objectNode().put("replyId", payload.replyId()),
        CURRENT_VERSION);
  }

  private static void validateOpenReviewPayload(JsonNode payload) {
    JsonNode replyId = payload.get("replyId");
    if (!payload.isObject()
        || payload.size() != 1
        || replyId == null
        || !replyId.isIntegralNumber()
        || !replyId.canConvertToLong()
        || replyId.longValue() <= 0) {
      throw new IllegalArgumentException("OPEN_REVIEW payload 형식이 올바르지 않습니다.");
    }
    if (payload.toString().getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
      throw new IllegalArgumentException("알림 Action payload가 크기 상한을 초과했습니다.");
    }
  }
}
