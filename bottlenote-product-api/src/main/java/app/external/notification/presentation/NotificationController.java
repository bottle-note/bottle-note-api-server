package app.external.notification.presentation;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.REQUIRED_AUTH;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.exception.UserException;
import app.external.notification.application.NotificationService;
import app.external.notification.data.response.NotificationListResponse;
import app.external.notification.data.response.NotificationMarkAllReadResponse;
import app.external.notification.data.response.NotificationMarkReadResponse;
import app.external.notification.data.response.NotificationUnreadCountResponse;
import app.external.notification.domain.Notification;
import app.external.notification.presentation.docs.NotificationApiDocs;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 인증 사용자의 알림함 조회·읽음 API. */
@RestController
@RequestMapping("/api/v1/external/notification")
@RequiredArgsConstructor
@SecurityPolicy(auth = REQUIRED_AUTH)
@NotificationApiDocs.ApiTag
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  @NotificationApiDocs.GetNotifications
  public ResponseEntity<GlobalResponse> getNotifications() {
    Long userId = currentUserId();
    List<Notification> notifications = notificationService.getNotifications(userId);
    return GlobalResponse.ok(NotificationListResponse.from(notifications));
  }

  @GetMapping("/unread-count")
  @NotificationApiDocs.GetUnreadCount
  public ResponseEntity<GlobalResponse> getUnreadCount() {
    Long userId = currentUserId();
    long unreadCount = notificationService.countUnread(userId);
    return GlobalResponse.ok(NotificationUnreadCountResponse.of(unreadCount));
  }

  @PatchMapping("/{notificationId}/read")
  @NotificationApiDocs.MarkAsRead
  public ResponseEntity<GlobalResponse> markAsRead(@PathVariable Long notificationId) {
    Long userId = currentUserId();
    notificationService.markAsRead(userId, notificationId);
    return GlobalResponse.ok(NotificationMarkReadResponse.of(notificationId));
  }

  @PatchMapping("/read-all")
  @NotificationApiDocs.MarkAllAsRead
  public ResponseEntity<GlobalResponse> markAllAsRead() {
    Long userId = currentUserId();
    int updatedCount = notificationService.markAllAsRead(userId);
    return GlobalResponse.ok(NotificationMarkAllReadResponse.of(updatedCount));
  }

  private Long currentUserId() {
    return SecurityContextUtil.getUserIdByContext()
        .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
  }
}
