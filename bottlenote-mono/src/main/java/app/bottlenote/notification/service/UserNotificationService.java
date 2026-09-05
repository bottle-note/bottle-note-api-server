package app.bottlenote.notification.service;

import app.bottlenote.global.pagination.CursorClaims;
import app.bottlenote.global.pagination.CursorKeys;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.notification.action.NotificationAction;
import app.bottlenote.notification.constant.NotificationActionFallbackType;
import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.domain.NotificationPreferenceRepository;
import app.bottlenote.notification.constant.NotificationKind;
import app.bottlenote.notification.domain.NotificationRepository;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import app.bottlenote.notification.dto.request.NotificationPageableRequest;
import app.bottlenote.notification.dto.response.NotificationListResponse;
import app.bottlenote.notification.exception.NotificationException;
import app.bottlenote.notification.exception.NotificationExceptionCode;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.facade.UserFacade;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService implements NotificationService {
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final UserFacade userFacade;
  private final NotificationRepository notificationRepository;
  private final HmacCursorCodec cursorCodec;
  private final NotificationPreferenceRepository preferenceRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Override
  public void sendNotification(NotificationMessage message) {
    log.info(
        "알림 저장 요청 - userId: {}, sourceType: {}, sourceId: {}, threadName: {}",
        message.userId(),
        message.sourceType(),
        message.sourceId(),
        Thread.currentThread().getName());

    // 알림 대상 없음은 user 일반 USER_NOT_FOUND가 아니라 notification 경계 코드로 유지
    if (!Boolean.TRUE.equals(userFacade.existsByUserId(message.userId()))) {
      throw new UserException(UserExceptionCode.NOTIFICATION_USER_NOT_FOUND);
    }

    if (message.kind() != null && !isEnabled(message.userId(), message.kind())) {
      return;
    }

    Notification notification =
        Notification.builder()
            .userId(message.userId())
            .title(message.title())
            .content(message.content())
            .type(message.type())
            .category(message.category())
            .sourceType(message.sourceType() != null ? message.sourceType().name() : null)
            .sourceId(message.sourceId())
            .action(message.action())
            .build();

    notificationRepository.saveIfAbsent(notification);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isEnabled(Long userId, NotificationKind kind) {
    return preferenceRepository.findByUserId(userId).getOrDefault(kind, true);
  }

  @Transactional(readOnly = true)
  @Override
  public KeysetPageResponse<NotificationListResponse> getNotifications(
      Long userId, NotificationPageableRequest request) {
    String context = notificationContext(userId, request);
    Long lastId = null;
    if (request.cursor() != null) {
      CursorClaims claims = cursorCodec.verify(request.cursor(), context);
      lastId = CursorKeys.requireLong(claims, "id");
    }
    NotificationListCriteria criteria = request.toCriteria(userId, lastId);
    List<Notification> fetched = notificationRepository.findPageByUserId(criteria);
    KeysetPagination.PageSlice<Notification> slice =
        KeysetPagination.fromOverflow(
            fetched,
            request.size(),
            item ->
                cursorCodec.encode(context, java.util.Map.of("id", String.valueOf(item.getId()))));
    List<NotificationListResponse.Item> items = slice.items().stream().map(this::toItem).toList();
    return KeysetPageResponse.of(NotificationListResponse.of(items), slice.pagination());
  }

  private static String notificationContext(Long userId, NotificationPageableRequest request) {
    return "notification.list:"
        + userId
        + ":"
        + request.types()
        + ":"
        + request.categories()
        + ":"
        + request.readStatus()
        + ":"
        + request.createdFrom()
        + ":"
        + request.createdTo();
  }

  @Transactional(readOnly = true)
  @Override
  public long countUnread(Long userId) {
    return notificationRepository.countByUserIdAndIsReadFalse(userId);
  }

  @Transactional
  @Override
  public NotificationMarkReadResult markAsRead(Long userId, Long notificationId) {
    int updated =
        notificationRepository.markAsReadByIdAndUserId(
            notificationId, userId, LocalDateTime.now(KST));
    Notification notification =
        notificationRepository
            .findByIdAndUserId(notificationId, userId)
            .orElseThrow(
                () -> new NotificationException(NotificationExceptionCode.NOTIFICATION_NOT_FOUND));
    long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
    return new NotificationMarkReadResult(
        notification.getId(),
        Boolean.TRUE.equals(notification.getIsRead()),
        notification.getReadAt(),
        updated == 1,
        unreadCount);
  }

  @Transactional
  @Override
  public int markAllAsRead(Long userId) {
    return notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now(KST));
  }

  private NotificationListResponse.Item toItem(Notification notification) {
    return new NotificationListResponse.Item(
        notification.getId(),
        notification.getTitle(),
        notification.getContent(),
        notification.getType(),
        notification.getCategory(),
        notification.getStatus(),
        notification.getIsRead(),
        toKstOffset(notification.getCreateAt()),
        toKstOffset(notification.getReadAt()),
        resolveAction(notification));
  }

  private NotificationListResponse.Action resolveAction(Notification notification) {
    try {
      NotificationAction action =
          NotificationAction.restore(
              notification.getActionType(),
              notification.getActionTargetId(),
              notification.getActionPayload(),
              notification.getActionVersion() != null
                  ? notification.getActionVersion().intValue()
                  : null);
      return new NotificationListResponse.Action(
          action.type(),
          action.targetId(),
          action.actionPayload(),
          action.version(),
          NotificationActionFallbackType.OPEN_NOTIFICATION_CENTER);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private OffsetDateTime toKstOffset(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.atZone(KST).toOffsetDateTime() : null;
  }
}
