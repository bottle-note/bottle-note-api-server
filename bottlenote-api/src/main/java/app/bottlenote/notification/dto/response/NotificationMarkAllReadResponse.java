package app.bottlenote.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 전체 읽음 처리 결과 응답. */
@Schema(
    name = "NotificationMarkAllReadResponse",
    title = "전체 읽음 결과",
    description = "읽음 처리로 갱신된 알림 건수")
public record NotificationMarkAllReadResponse(
    @Schema(description = "읽음으로 갱신된 알림 건수", example = "5") int updatedCount) {

  public static NotificationMarkAllReadResponse of(int updatedCount) {
    return new NotificationMarkAllReadResponse(updatedCount);
  }
}
