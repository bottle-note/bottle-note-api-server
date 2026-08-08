package app.bottlenote.notification.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.REQUIRED_AUTH;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.notification.controller.docs.NotificationApiDocs;
import app.bottlenote.notification.dto.request.NotificationPageableRequest;
import app.bottlenote.notification.dto.response.NotificationListResponse;
import app.bottlenote.notification.dto.response.NotificationMarkAllReadResponse;
import app.bottlenote.notification.dto.response.NotificationMarkReadResponse;
import app.bottlenote.notification.dto.response.NotificationUnreadCountResponse;
import app.bottlenote.notification.service.NotificationService;
import app.bottlenote.user.exception.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 인증 사용자의 알림함 조회·읽음 API. */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityPolicy(auth = REQUIRED_AUTH)
@NotificationApiDocs.ApiTag
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  @NotificationApiDocs.GetNotifications
  public ResponseEntity<GlobalResponse> getNotifications(
      @ModelAttribute @Valid NotificationPageableRequest request) {
    Long userId = currentUserId();
    PageResponse<NotificationListResponse> page =
        notificationService.getNotifications(userId, request);
    return GlobalResponse.ok(
        page.content(), MetaService.createMetaInfo().add("pageable", page.cursorPageable()));
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
  public ResponseEntity<GlobalResponse> updateRead(@PathVariable Long notificationId) {
    Long userId = currentUserId();
    notificationService.markAsRead(userId, notificationId);
    return GlobalResponse.ok(NotificationMarkReadResponse.of(notificationId));
  }

  @PatchMapping("/read-all")
  @NotificationApiDocs.MarkAllAsRead
  public ResponseEntity<GlobalResponse> updateAllRead() {
    Long userId = currentUserId();
    int updatedCount = notificationService.markAllAsRead(userId);
    return GlobalResponse.ok(NotificationMarkAllReadResponse.of(updatedCount));
  }

  private Long currentUserId() {
    return SecurityContextUtil.getUserIdByContext()
        .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
  }
}
