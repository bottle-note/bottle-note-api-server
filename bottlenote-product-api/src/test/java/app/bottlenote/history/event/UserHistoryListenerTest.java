package app.bottlenote.history.event;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.fixture.FakeAlcoholFacade;
import app.bottlenote.history.constant.EventCategory;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.constant.RedirectUrlType;
import app.bottlenote.history.event.listener.HistoryListener;
import app.bottlenote.history.event.payload.HistoryEvent;
import app.bottlenote.history.fixture.InMemoryUserHistoryRepository;
import app.bottlenote.like.event.payload.ReviewLikeActivityEvent;
import app.bottlenote.review.event.payload.ReviewReplyActivityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
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

  @Test
  @DisplayName("댓글 활동을 기록할 때 기존 사용자와 주류와 본문과 경로를 보존한다")
  void replyActivity_preservesHistory() {
    historyListener.handleReviewReplyActivity(
        new ReviewReplyActivityEvent(10L, 20L, 1L, 2L, 30L, 3L, "댓글"));

    assertThat(userHistoryRepository.findAll())
        .singleElement()
        .satisfies(
            history -> {
              assertThat(history.getUserId()).isEqualTo(2L);
              assertThat(history.getAlcoholId()).isEqualTo(20L);
              assertThat(history.getEventCategory()).isEqualTo(EventCategory.REVIEW);
              assertThat(history.getEventType()).isEqualTo(EventType.REVIEW_REPLY_CREATE);
              assertThat(history.getContent()).isEqualTo("댓글");
              assertThat(history.getRedirectUrl())
                  .isEqualTo(RedirectUrlType.REVIEW.getUrl() + "/10");
            });
  }

  @Test
  @DisplayName("좋아요 활동이 활성 전이가 아니어도 기존 History 조건을 보존한다")
  void likeActivity_preservesAllRequests() {
    historyListener.handleReviewLikeActivity(
        new ReviewLikeActivityEvent(30L, 10L, 20L, 1L, 2L, "리뷰", true));
    historyListener.handleReviewLikeActivity(
        new ReviewLikeActivityEvent(30L, 10L, 20L, 1L, 2L, "리뷰", false));

    assertThat(userHistoryRepository.findAll())
        .hasSize(2)
        .allSatisfy(
            history -> {
              assertThat(history.getUserId()).isEqualTo(2L);
              assertThat(history.getAlcoholId()).isEqualTo(20L);
              assertThat(history.getEventCategory()).isEqualTo(EventCategory.REVIEW);
              assertThat(history.getEventType()).isEqualTo(EventType.REVIEW_LIKES);
              assertThat(history.getContent()).isEqualTo("리뷰");
              assertThat(history.getRedirectUrl())
                  .isEqualTo(RedirectUrlType.REVIEW.getUrl() + "/10");
            });
  }
}
