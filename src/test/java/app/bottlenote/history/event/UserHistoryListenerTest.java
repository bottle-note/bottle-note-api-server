package app.bottlenote.history.event;

import app.bottlenote.alcohols.fixture.FakeAlcoholDomainSupport;
import app.bottlenote.history.domain.constant.EventCategory;
import app.bottlenote.history.domain.constant.EventType;
import app.bottlenote.history.dto.payload.HistoryEvent;
import app.bottlenote.history.event.listener.HistoryListener;
import app.bottlenote.history.fixture.InMemoryUserHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserHistoryListenerTest {

	private HistoryListener historyListener;
	private FakeAlcoholDomainSupport alcoholDomainSupport;
	private InMemoryUserHistoryRepository userHistoryRepository;

	@BeforeEach
	void setUp() {
		alcoholDomainSupport = new FakeAlcoholDomainSupport();
		userHistoryRepository = new InMemoryUserHistoryRepository();
		historyListener = new HistoryListener(alcoholDomainSupport, userHistoryRepository);
	}

	@DisplayName("유저 히스토리 이벤트를 저장할 수 있다.")
	@Test
	void test() {
		// given
		Long userId = 1L;
		HistoryEvent historyEvent = HistoryEvent.makeHistoryEvent(
			userId,
			EventCategory.PICK,
			EventType.IS_PICK,
			"type",
			1L,
			"message",
			null,
			"description"
		);
		// when
		historyListener.registryUserHistory(historyEvent);

		userHistoryRepository.findAll().stream().findFirst().ifPresent(userHistory -> {
			// then
			assert userHistory.getUserId().equals(userId);
			assert userHistory.getEventCategory().equals(EventCategory.PICK);
			assert userHistory.getEventType().equals(EventType.IS_PICK);
		});

	}

}
