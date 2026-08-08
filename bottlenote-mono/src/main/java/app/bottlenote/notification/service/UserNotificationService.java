package app.bottlenote.notification.service;

import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.domain.NotificationRepository;
import app.bottlenote.notification.exception.NotificationException;
import app.bottlenote.notification.exception.NotificationExceptionCode;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.facade.UserFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService implements NotificationService {
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
  public List<Notification> getNotifications(Long userId) {
    return notificationRepository.findAllByUserIdOrderByIdDesc(userId);
  }

  @Transactional(readOnly = true)
  @Override
  public long countUnread(Long userId) {
    return notificationRepository.countByUserIdAndIsReadFalse(userId);
  }

  @Transactional
  @Override
  public void markAsRead(Long userId, Long notificationId) {
    Notification notification =
        notificationRepository
            .findByIdAndUserId(notificationId, userId)
            .orElseThrow(
                () -> new NotificationException(NotificationExceptionCode.NOTIFICATION_NOT_FOUND));

    notification.markAsRead();
    notificationRepository.save(notification);
  }

  @Transactional
  @Override
  public int markAllAsRead(Long userId) {
    return notificationRepository.markAllAsReadByUserId(userId);
  }
}
