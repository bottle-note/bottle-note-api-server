package app.bottlenote.notification.dto.response;

import app.bottlenote.notification.constant.NotificationKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public record NotificationPreferenceResponse(
    @Schema(description = "여섯 인앱 알림 유형의 수신 여부. 미설정 유형은 true")
    Map<NotificationKind, Boolean> settings) {
  public NotificationPreferenceResponse {
    settings = Map.copyOf(settings);
  }
}
