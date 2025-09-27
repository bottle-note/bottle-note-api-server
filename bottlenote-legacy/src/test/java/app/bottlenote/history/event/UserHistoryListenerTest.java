package app.bottlenote.history.event;

import app.bottlenote.alcohols.fixture.FakeAlcoholFacade;
import app.bottlenote.history.event.listener.HistoryListener;
import app.bottlenote.history.event.payload.HistoryEvent;
import app.bottlenote.history.fixture.InMemoryUserHistoryRepository;
import app.bottlenote.shared.history.constant.EventCategory;
import app.bottlenote.shared.history.constant.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserHistoryListenerTest {

  private HistoryListener historyListener;
  private FakeAlcoholFacade alcoholFacade;
  private InMemoryUserHistoryRepository userHistoryRepository;

  @BeforeEach
  void setUp() {
    alcoholFacade = new FakeAlcoholFacade();
    userHistoryRepository = new InMemoryUserHistoryRepository();
    historyListener = new HistoryListener(alcoholFacade, userHistoryRepository);
  }

  @DisplayName("유저 히스토리 이벤트를 저장할 수 있다.")
  @Test
  void test() {
    // given
    Long userId = 1L;
    HistoryEvent historyEvent =
        HistoryEvent.builder()
            .userId(userId)
            .eventCategory(EventCategory.PICK)
            .eventType(EventType.IS_PICK)
            .redirectUrl("redirectUrl")
            .alcoholId(1L)
            .build();
    // when
    historyListener.handleUserHistoryRegistry(historyEvent);

    userHistoryRepository.findAll().stream()
        .findFirst()
        .ifPresent(
            userHistory -> {
              // then
              assert userHistory.getUserId().equals(userId);
              assert userHistory.getEventCategory().equals(EventCategory.PICK);
              assert userHistory.getEventType().equals(EventType.IS_PICK);
            });
  }
}
