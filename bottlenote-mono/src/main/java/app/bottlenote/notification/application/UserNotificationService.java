package app.bottlenote.notification.application;

import app.bottlenote.notification.data.payload.NotificationMessage;
import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.domain.NotificationRepository;
import app.bottlenote.notification.exception.NotificationException;
import app.bottlenote.notification.exception.NotificationExceptionCode;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService implements NotificationService {
  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;

  @Transactional
  @Override
  public void sendNotification(NotificationMessage message) {
    log.info(
        "[Service] NotificationMessage: {} , thread name : : {}",
        message,
        Thread.currentThread().getName());

    User notiyTargetUser =
        userRepository
            .findById(message.userId())
            .orElseThrow(() -> new UserException(UserExceptionCode.NOTIFICATION_USER_NOT_FOUND));

    Notification notification =
        Notification.builder()
            .userId(notiyTargetUser.getId())
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
