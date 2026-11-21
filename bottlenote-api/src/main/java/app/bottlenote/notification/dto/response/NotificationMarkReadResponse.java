package app.bottlenote.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.jspecify.annotations.Nullable;

/**
 * 단건 알림의 멱등 읽음 처리 API 응답이다.
 *
 * <p>최초 읽음 시각과 상태 변경 여부, 처리 후 미읽음 개수를 제공한다.
 */
@Schema(name = "NotificationMarkReadResponse", title = "단건 읽음 결과", description = "멱등 읽음 처리 결과")
public record NotificationMarkReadResponse(
    @Schema(description = "읽음 처리된 알림 식별자", example = "1") Long notificationId,
    @Schema(description = "읽음 여부", example = "true") boolean isRead,
    @Schema(description = "최초 읽음 시각", example = "2026-08-10T09:30:00+09:00", nullable = true)
        @Nullable OffsetDateTime readAt,
    @Schema(description = "이번 요청에서 읽음 상태가 변경되었는지 여부", example = "true") boolean changed,
    @Schema(description = "처리 후 미읽음 알림 개수", example = "2") long unreadCount) {
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  /**
   * 서비스 결과의 KST 로컬 시각을 {@code +09:00} offset 응답으로 변환한다.
   *
   * <p>과거 시각이 없으면 {@code readAt}은 {@code null}로 유지한다.
   */
  public static NotificationMarkReadResponse of(
      Long notificationId,
      boolean isRead,
      @Nullable LocalDateTime firstReadAt,
      boolean changed,
      long unreadCount) {
    OffsetDateTime readAt = firstReadAt == null ? null : firstReadAt.atZone(KST).toOffsetDateTime();
    return new NotificationMarkReadResponse(notificationId, isRead, readAt, changed, unreadCount);
  }
}
