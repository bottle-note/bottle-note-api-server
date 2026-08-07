package app.bottlenote.notification.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 인증 사용자 본인 알림 목록 응답. */
@Schema(name = "NotificationListResponse", title = "알림 목록", description = "알림 개수와 항목 목록")
public record NotificationListResponse(
    @Schema(description = "알림 총 개수", example = "3") long totalCount,
    @ArraySchema(schema = @Schema(implementation = NotificationItemResponse.class))
        List<NotificationItemResponse> items) {

  public static NotificationListResponse of(List<NotificationItemResponse> items) {
    return new NotificationListResponse(items.size(), items);
  }
}
