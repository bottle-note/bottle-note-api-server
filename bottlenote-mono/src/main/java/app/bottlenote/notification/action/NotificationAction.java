package app.bottlenote.notification.action;

import app.bottlenote.notification.constant.NotificationActionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.nio.charset.StandardCharsets;

/**
 * 알림에서 수행할 의미 기반 이동 정보다.
 *
 * <p>플랫폼별 경로 대신 허용된 Action 타입과 버전별 payload 계약을 보관한다.
 */
public record NotificationAction(
    NotificationActionType type, Long targetId, JsonNode payload, Integer version) {

  public static final int CURRENT_VERSION = 1;
  public static final int MAX_PAYLOAD_BYTES = 1024;

  public NotificationAction {
    if (type == null) {
      throw new IllegalArgumentException("지원하지 않는 알림 Action입니다.");
    }
    if (targetId == null || targetId <= 0) {
      throw new IllegalArgumentException("Action 대상 식별자는 양수여야 합니다.");
    }
    if (version == null || (version != CURRENT_VERSION && !(type == NotificationActionType.OPEN_REVIEW && version == 2))) {
      throw new IllegalArgumentException("지원하지 않는 알림 Action 버전입니다.");
    }
    if (payload == null) {
      throw new IllegalArgumentException("알림 Action payload는 필수입니다.");
    }
    payload = payload.deepCopy();
    validatePayloadSize(payload);
    switch (type) {
      case OPEN_REVIEW -> {
        if (version == 1) {
          OpenReviewActionPayload.from(payload);
        } else {
          OpenReviewDetailActionPayload.from(payload);
        }
      }
      case OPEN_HELP -> OpenHelpActionPayload.from(payload);
      case OPEN_USER -> OpenUserActionPayload.from(payload);
    }
  }

  /**
   * 리뷰 댓글 알림에 사용할 {@code OPEN_REVIEW} Action을 생성한다.
   *
   * <p>리뷰와 댓글 식별자를 검증한 뒤 저장 가능한 JSON payload로 변환한다.
   */
  public static NotificationAction openReview(Long reviewId, Long replyId) {
    OpenReviewActionPayload payload = new OpenReviewActionPayload(replyId);
    return new NotificationAction(
        NotificationActionType.OPEN_REVIEW,
        reviewId,
        JsonNodeFactory.instance.objectNode().put("replyId", payload.replyId()),
        CURRENT_VERSION);
  }

  /**
   * 문의 답변 알림에 사용할 {@code OPEN_HELP} Action을 생성한다.
   *
   * <p>문의 식별자를 검증하고 v1의 빈 객체 payload를 함께 저장한다.
   */
  public static NotificationAction openHelp(Long helpId) {
    return new NotificationAction(
        NotificationActionType.OPEN_HELP,
        helpId,
        JsonNodeFactory.instance.objectNode(),
        CURRENT_VERSION);
  }

  /**
   * DB에 저장된 raw 필드에서 검증된 Action을 복원한다.
   *
   * <p>지원하지 않는 타입이나 버전, 불완전한 payload는 예외로 거부한다.
   */
  public static NotificationAction restore(
      String type, Long targetId, JsonNode payload, Integer version) {
    if (type == null) {
      throw new IllegalArgumentException("알림 Action 타입은 필수입니다.");
    }

    NotificationActionType actionType;
    try {
      actionType = NotificationActionType.valueOf(type);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("지원하지 않는 알림 Action입니다.", exception);
    }
    return new NotificationAction(actionType, targetId, payload, version);
  }

  /**
   * 검증된 JSON payload를 Action 타입별 응답 DTO로 변환한다.
   *
   * <p>목록 응답은 raw JSON 대신 이 계약을 사용해 앱·웹에 전달한다.
   */
  public ActionPayload actionPayload() {
    return switch (type) {
      case OPEN_REVIEW -> version == 1 ? OpenReviewActionPayload.from(payload) : OpenReviewDetailActionPayload.from(payload);
      case OPEN_HELP -> OpenHelpActionPayload.from(payload);
      case OPEN_USER -> OpenUserActionPayload.from(payload);
    };
  }

  private static void validatePayloadSize(JsonNode payload) {
    if (payload.toString().getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
      throw new IllegalArgumentException("알림 Action payload가 크기 상한을 초과했습니다.");
    }
  }

  /**
   * Action 타입별 응답 payload가 구현하는 공통 계약이다.
   *
   * <p>구현 타입은 이 Action 내부에 두어 버전별 검증 책임을 한곳에 모은다.
   */
  public sealed interface ActionPayload permits OpenReviewActionPayload, OpenHelpActionPayload, OpenReviewDetailActionPayload, OpenUserActionPayload {}

  /**
   * {@code OPEN_REVIEW} Action이 사용하는 댓글 위치 정보다.
   *
   * <p>댓글 식별자 하나만 허용하며 저장·응답 경계에서 동일한 검증 규칙을 사용한다.
   */
  public record OpenReviewActionPayload(Long replyId) implements ActionPayload {

    public OpenReviewActionPayload {
      if (replyId == null || replyId <= 0) {
        throw new IllegalArgumentException("댓글 식별자는 양수여야 합니다.");
      }
    }

    private static OpenReviewActionPayload from(JsonNode payload) {
      if (!payload.isObject() || payload.size() != 1) {
        throw new IllegalArgumentException("OPEN_REVIEW payload 형식이 올바르지 않습니다.");
      }

      JsonNode replyId = payload.get("replyId");
      if (replyId == null || !replyId.isIntegralNumber() || !replyId.canConvertToLong()) {
        throw new IllegalArgumentException("OPEN_REVIEW payload 형식이 올바르지 않습니다.");
      }
      return new OpenReviewActionPayload(replyId.longValue());
    }
  }

  /**
   * {@code OPEN_HELP} v1이 사용하는 빈 payload 계약이다.
   *
   * <p>문의 이동에는 targetId만 필요하므로 추가 key를 허용하지 않는다.
   */
  public record OpenHelpActionPayload() implements ActionPayload {

    private static OpenHelpActionPayload from(JsonNode payload) {
      if (!payload.isObject() || !payload.isEmpty()) {
        throw new IllegalArgumentException("OPEN_HELP payload 형식이 올바르지 않습니다.");
      }
      return new OpenHelpActionPayload();
    }
  }
  public static NotificationAction openReview(Long reviewId) {
    return new NotificationAction(NotificationActionType.OPEN_REVIEW, reviewId,
        JsonNodeFactory.instance.objectNode(), 2);
  }

  public static NotificationAction openUser(Long userId) {
    return new NotificationAction(NotificationActionType.OPEN_USER, userId,
        JsonNodeFactory.instance.objectNode(), CURRENT_VERSION);
  }

  public record OpenReviewDetailActionPayload() implements ActionPayload {
    private static OpenReviewDetailActionPayload from(JsonNode payload) {
      if (!payload.isObject() || !payload.isEmpty()) {
        throw new IllegalArgumentException("OPEN_REVIEW v2 payload 형식이 올바르지 않습니다.");
      }
      return new OpenReviewDetailActionPayload();
    }
  }

  public record OpenUserActionPayload() implements ActionPayload {
    private static OpenUserActionPayload from(JsonNode payload) {
      if (!payload.isObject() || !payload.isEmpty()) {
        throw new IllegalArgumentException("OPEN_USER payload 형식이 올바르지 않습니다.");
      }
      return new OpenUserActionPayload();
    }
  }
}
