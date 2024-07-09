package app.bottlenote.review.service.event;

import app.bottlenote.review.domain.event.ReviewReplyRegistryEvent;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.fixture.FakeNotificationService;
import app.bottlenote.user.fixture.InMemoryNotificationRepository;
import app.bottlenote.user.fixture.InMemoryUserQueryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewReplyEventHandlerTest {
	private ReviewReplyEventHandler reviewReplyEventHandler;
	private InMemoryUserQueryRepository userQueryRepository;
	private InMemoryNotificationRepository notificationRepository;

	@BeforeEach
	void setUp() {
		userQueryRepository = new InMemoryUserQueryRepository();
		notificationRepository = new InMemoryNotificationRepository();
		reviewReplyEventHandler = new ReviewReplyEventHandler(new FakeNotificationService(userQueryRepository, notificationRepository));

		userQueryRepository.save(User.builder().id(1L).email("email@test.com").nickName("nickName").build());
	}

	@Test
	@DisplayName("알림 이벤트를 발행할 수 있다.")
	void test_() {

		// given
		Long reviewId = 1L;
		Long userId = 1L;
		String content = "테스트용 메시지 이벤트 발행";
		var event = ReviewReplyRegistryEvent.replyRegistryPublish(reviewId, userId, content);

		// when
		reviewReplyEventHandler.registryReviewReply(event);

		// then
		notificationRepository.findAll().stream().findFirst().ifPresent(notification -> {
			Assertions.assertEquals(userId, notification.getUserId());
			Assertions.assertNotNull(notification.getId());
			Assertions.assertNotNull(notification.getTitle());
			Assertions.assertNotNull(notification.getContent());
			Assertions.assertEquals(String.format("@reviewId=%d @content=%s", reviewId, content), notification.getContent());
		});
	}

	@Test
	@DisplayName("존재하지 않는 사용자 식별자가 주어진 경우 예외를 발생시킨다.")
	void test_1() {
		// given
		Long reviewId = 1L;
		Long userId = 2L;
		String content = "테스트용 메시지 이벤트 발행";
		var event = ReviewReplyRegistryEvent.replyRegistryPublish(reviewId, userId, content);

		// when
		Assertions.assertThrows(UserException.class, () -> reviewReplyEventHandler.registryReviewReply(event));

		// then
		Assertions.assertTrue(notificationRepository.findAll().isEmpty());
	}
}
