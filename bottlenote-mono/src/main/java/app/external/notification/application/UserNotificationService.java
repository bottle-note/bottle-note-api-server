package app.external.notification.application;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.external.notification.data.payload.NotificationMessage;
import app.external.notification.domain.Notification;
import app.external.notification.repository.NotificationRepository;
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
}
