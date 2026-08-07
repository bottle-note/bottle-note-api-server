package app.external.notification.data.response;

import app.external.notification.domain.Notification;
import app.external.notification.domain.constant.NotificationCategory;
import app.external.notification.domain.constant.NotificationStatus;
import app.external.notification.domain.constant.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 알림함 목록/단건 읽음 응답 항목. */
@Schema(name = "NotificationItemResponse", title = "알림 항목", description = "사용자 알림함의 단일 알림")
public record NotificationItemResponse(
    @Schema(description = "알림 식별자", example = "1") Long id,
    @Schema(description = "알림 제목", example = "새 댓글") String title,
    @Schema(description = "알림 내용", example = "리뷰에 댓글이 달렸습니다.") String content,
    @Schema(description = "알림 타입") NotificationType type,
    @Schema(description = "알림 카테고리") NotificationCategory category,
    @Schema(description = "알림 상태") NotificationStatus status,
    @Schema(description = "읽음 여부", example = "false") Boolean isRead,
    @Schema(description = "생성 시각") LocalDateTime createAt) {

  public static NotificationItemResponse from(Notification notification) {
    return new NotificationItemResponse(
        notification.getId(),
        notification.getTitle(),
        notification.getContent(),
        notification.getType(),
        notification.getCategory(),
        notification.getStatus(),
        notification.getIsRead(),
        notification.getCreateAt());
  }
}
