package app.bottlenote.notification.dto.request;

import app.bottlenote.notification.constant.NotificationKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record NotificationPreferenceRequest(
    @Schema(description = "변경할 인앱 알림 유형과 수신 여부. 생략한 유형은 유지한다",
        example = "{\"REVIEW_LIKE\":false,\"FOLLOW\":true}")
    @NotEmpty @Size(max = 6) Map<@NotNull NotificationKind, @NotNull Boolean> settings) {}
