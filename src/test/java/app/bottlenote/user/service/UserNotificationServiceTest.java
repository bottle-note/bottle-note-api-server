package app.bottlenote.user.service;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.NotificationMessage;
import app.bottlenote.user.fixture.InMemoryNotificationRepository;
import app.bottlenote.user.fixture.InMemoryUserQueryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static app.bottlenote.user.domain.constant.NotificationCategory.REVIEW;
import static app.bottlenote.user.domain.constant.NotificationType.USER;

class UserNotificationServiceTest {

	private InMemoryUserQueryRepository userQueryRepository;
	private InMemoryNotificationRepository notificationRepository;
	private NotificationService notificationService;

	@BeforeEach
	void setUp() {
		userQueryRepository = new InMemoryUserQueryRepository();
		notificationRepository = new InMemoryNotificationRepository();
		notificationService = new UserNotificationService(userQueryRepository, notificationRepository);
		userQueryRepository.save(User.builder().id(1L).email("email@test.com").nickName("nickName").build());
	}

	@Test
	@DisplayName("알림 이벤트를 등록 할 수 있다.")
	void test_() throws Exception {
		// given
		// a 5번 반복
		String title = "content title";
		String content = "테스트용 메시지 이벤트 발행";
		var notificationMessage = NotificationMessage.create(1L, USER, REVIEW, title, content);

		// when
		notificationService.sendNotification(notificationMessage);

		// then
		notificationRepository.findAll().stream().findFirst().ifPresent(notification -> {
			Assertions.assertEquals(1L, notification.getUserId());
			Assertions.assertNotNull(notification.getId());
			Assertions.assertNotNull(notification.getTitle());
			Assertions.assertNotNull(notification.getContent());
			Assertions.assertEquals(content, notification.getContent());
		});
	}

	@Test
	@DisplayName("존재하지 않는 사용자 식별자가 주어진 경우 예외를 발생시킨다.")
	void test_1() {
		// given
		Long userId = 2L;
		String title = "content title";
		String content = "테스트용 메시지 이벤트 발행";
		var notificationMessage = NotificationMessage.create(userId, USER, REVIEW, title, content);

		// when
		Assertions.assertThrows(Exception.class, () -> notificationService.sendNotification(notificationMessage));
	}
}
