package app.bottlenote.user.service;

import app.bottlenote.user.domain.Notification;
import app.bottlenote.user.domain.NotificationRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserQueryRepository;
import app.bottlenote.user.dto.request.NotificationMessage;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserNotificationService implements NotificationService {

	private static final Logger log = LogManager.getLogger(UserNotificationService.class);
	private final UserQueryRepository userQueryRepository;
	private final NotificationRepository notificationRepository;

	public UserNotificationService(
		UserQueryRepository userQueryRepository,
		NotificationRepository notificationRepository
	) {
		this.userQueryRepository = userQueryRepository;
		this.notificationRepository = notificationRepository;
	}

	@Transactional
	@Override
	public void sendNotification(NotificationMessage message) {
		log.info("[Service] NotificationMessage: {} , thread name : : {}", message, Thread.currentThread().getName());

		User notiyTargetUser = userQueryRepository.findById(message.userId())
			.orElseThrow(() -> new UserException(UserExceptionCode.NOTIFICATION_USER_NOT_FOUND));

		Notification notification = Notification.builder()
			.userId(notiyTargetUser.getId())
			.title(message.title())
			.content(message.content())
			.type(message.type())
			.category(message.category())
			.build();

		notificationRepository.save(notification);
	}
}
