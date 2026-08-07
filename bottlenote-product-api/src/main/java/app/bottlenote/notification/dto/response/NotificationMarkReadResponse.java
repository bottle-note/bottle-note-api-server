package app.bottlenote.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 단건 읽음 처리 결과 응답. */
@Schema(name = "NotificationMarkReadResponse", title = "단건 읽음 결과", description = "읽음 처리된 알림 식별자")
public record NotificationMarkReadResponse(
    @Schema(description = "읽음 처리된 알림 식별자", example = "1") Long notificationId,
    @Schema(description = "읽음 여부", example = "true") boolean isRead) {

  public static NotificationMarkReadResponse of(Long notificationId) {
    return new NotificationMarkReadResponse(notificationId, true);
  }
}
