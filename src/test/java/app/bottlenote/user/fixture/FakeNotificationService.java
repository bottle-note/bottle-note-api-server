package app.bottlenote.user.fixture;

import app.bottlenote.user.domain.Notification;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.NotificationMessage;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.NotificationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;

public class FakeNotificationService implements NotificationService {

	private static final Logger log = LogManager.getLogger(FakeNotificationService.class);
	private InMemoryUserQueryRepository userQueryRepository;
	private InMemoryNotificationRepository notificationRepository;

	public FakeNotificationService(
		InMemoryUserQueryRepository userQueryRepository,
		InMemoryNotificationRepository notificationRepository
	) {
		this.userQueryRepository = userQueryRepository;
		this.notificationRepository = notificationRepository;
	}

	@BeforeEach
	void setUp() {
		userQueryRepository = new InMemoryUserQueryRepository();
		notificationRepository = new InMemoryNotificationRepository();
	}

	@Override
	public void sendNotification(NotificationMessage message) {
		log.info("[FakeNotificationService] NotificationMessage: {} , thread name : : {}", message, Thread.currentThread().getName());

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
