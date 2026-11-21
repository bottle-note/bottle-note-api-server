package app.bottlenote.notification.dto.request;

import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.exception.NotificationException;
import app.bottlenote.notification.exception.NotificationExceptionCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 알림 수신 설정 변경 요청. 그룹·전체 토글은 해당 발생 액션을 모두 담아 보낸다. */
@Schema(name = "NotificationSettingUpdateRequest", description = "알림 수신 설정 변경 요청")
public record NotificationSettingUpdateRequest(
    @Schema(description = "변경할 발생 액션별 수신 여부. 같은 액션은 한 번만 지정한다.")
        @NotEmpty(message = "NOTIFICATION_SETTINGS_REQUIRED")
        List<@Valid @NotNull(message = "NOTIFICATION_SETTING_REQUIRED") Item> settings) {

  public Map<NotificationEventAction, Boolean> toChanges() {
    Map<NotificationEventAction, Boolean> changes = new LinkedHashMap<>();
    for (Item item : settings) {
      if (changes.put(item.eventAction(), item.enabled()) != null) {
        throw new NotificationException(NotificationExceptionCode.DUPLICATE_NOTIFICATION_SETTING);
      }
    }
    return changes;
  }

  /** 발생 액션 하나의 수신 여부. */
  @Schema(name = "NotificationSettingUpdateItem", description = "변경할 알림 수신 설정")
  public record Item(
      @Schema(description = "알림 발생 액션", example = "REVIEW_LIKE_ADD")
          @NotNull(message = "NOTIFICATION_EVENT_ACTION_REQUIRED")
          NotificationEventAction eventAction,
      @Schema(description = "수신 여부", example = "false")
          @NotNull(message = "NOTIFICATION_ENABLED_REQUIRED")
          Boolean enabled) {}
}
