package app.bottlenote.notification.dto.response;

import app.bottlenote.notification.action.NotificationAction.ActionPayload;
import app.bottlenote.notification.constant.NotificationActionFallbackType;
import app.bottlenote.notification.constant.NotificationActionType;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationStatus;
import app.bottlenote.notification.constant.NotificationType;
import java.time.OffsetDateTime;
import java.util.List;

/** 서비스 계층의 필터된 알림 목록 조회 결과다. */
public record NotificationListResponse(List<Item> items) {

  public static NotificationListResponse of(List<Item> items) {
    return new NotificationListResponse(items);
  }

  /**
   * 읽음 상태와 전달 상태를 분리한 알림함 목록 항목이다.
   *
   * <p>이동 계약이 유효할 때만 의미 기반 Action을 포함한다.
   */
  public record Item(
      Long id,
      String title,
      String content,
      NotificationType type,
      NotificationCategory category,
      NotificationStatus status,
      Boolean isRead,
      OffsetDateTime createAt,
      OffsetDateTime readAt,
      Action action) {}

  /**
   * 클라이언트가 플랫폼 내부 이동으로 변환하는 의미 기반 Action이다.
   *
   * <p>지원하지 않는 타입이나 payload는 항목 전체가 아닌 Action만 {@code null}로 응답한다.
   */
  public record Action(
      NotificationActionType type,
      Long targetId,
      ActionPayload payload,
      Integer version,
      NotificationActionFallbackType fallbackType) {}
}
