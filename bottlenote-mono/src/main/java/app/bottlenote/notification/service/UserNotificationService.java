package app.bottlenote.notification.service;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.notification.domain.Notification;
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
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService implements NotificationService {
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final UserFacade userFacade;
  private final NotificationRepository notificationRepository;

  @Transactional
  @Override
  public void sendNotification(NotificationMessage message) {
    log.info(
        "[Service] NotificationMessage: {} , thread name : : {}",
        message,
        Thread.currentThread().getName());

    // 알림 대상 없음은 user 일반 USER_NOT_FOUND가 아니라 notification 경계 코드로 유지
    if (!Boolean.TRUE.equals(userFacade.existsByUserId(message.userId()))) {
      throw new UserException(UserExceptionCode.NOTIFICATION_USER_NOT_FOUND);
    }

    Notification notification =
        Notification.builder()
            .userId(message.userId())
            .title(message.title())
            .content(message.content())
            .type(message.type())
            .category(message.category())
            .build();

    notificationRepository.save(notification);
  }

  @Transactional(readOnly = true)
  @Override
  public PageResponse<NotificationListResponse> getNotifications(
      Long userId, NotificationPageableRequest request) {
    NotificationListCriteria criteria =
        NotificationListCriteria.of(userId, request.cursor(), request.pageSize());
    long totalCount = notificationRepository.countByUserId(userId);
    List<Notification> fetched = notificationRepository.findPageByUserId(criteria);

    boolean hasNext = fetched.size() > criteria.pageSize();
    List<Notification> content =
        hasNext ? List.copyOf(fetched.subList(0, criteria.pageSize().intValue())) : fetched;

    List<NotificationListResponse.Item> items = content.stream().map(this::toItem).toList();

    // nextCursor = 마지막 반환 item id (keyset)
    Long nextCursor = items.isEmpty() ? criteria.cursor() : items.getLast().id();
    CursorPageable pageable =
        CursorPageable.builder()
            .currentCursor(criteria.cursor())
            .cursor(nextCursor)
            .pageSize(criteria.pageSize())
            .hasNext(hasNext)
            .build();

    return PageResponse.of(NotificationListResponse.of(totalCount, items), pageable);
  }

  @Transactional(readOnly = true)
  @Override
  public long countUnread(Long userId) {
    return notificationRepository.countByUserIdAndIsReadFalse(userId);
  }

  @Transactional
  @Override
  public void markAsRead(Long userId, Long notificationId) {
    int updated =
        notificationRepository.markAsReadByIdAndUserId(
            notificationId, userId, LocalDateTime.now(KST));
    if (updated == 0) {
      notificationRepository
          .findByIdAndUserId(notificationId, userId)
          .orElseThrow(
              () -> new NotificationException(NotificationExceptionCode.NOTIFICATION_NOT_FOUND));
    }
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
        notification.getCreateAt());
  }
}
