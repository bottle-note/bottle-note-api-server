package app.bottlenote.notification.data.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 미읽음 알림 개수 응답. */
@Schema(name = "NotificationUnreadCountResponse", title = "미읽음 개수", description = "미읽음 알림 개수")
public record NotificationUnreadCountResponse(
    @Schema(description = "미읽음 알림 개수", example = "2") long unreadCount) {

  public static NotificationUnreadCountResponse of(long unreadCount) {
    return new NotificationUnreadCountResponse(unreadCount);
  }
}
