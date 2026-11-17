package app.bottlenote.history.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.history.constant.EventCategory;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.constant.RedirectUrlType;
import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.domain.UserHistoryRepository;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.service.PicksCommandService;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.service.RatingCommandService;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.service.ReviewService;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("integration")
class RegistryHistoryIntegrationTest extends IntegrationTestSupport {

  @Autowired private UserTestFactory users;
  @Autowired private AlcoholTestFactory alcohols;
  @Autowired private ReviewService reviews;
  @Autowired private PicksCommandService picks;
  @Autowired private RatingCommandService ratings;
  @Autowired private UserHistoryRepository histories;
  @Autowired private PlatformTransactionManager transactionManager;

  private User user;
  private Alcohol alcohol;
  private TransactionTemplate transaction;

  @BeforeEach
  void setUp() {
    user = users.persistUser();
    alcohol = alcohols.persistAlcohol();
    transaction = new TransactionTemplate(transactionManager);
  }

  @Test
  @DisplayName("같은 본문으로 리뷰를 두 번 작성할 때 각각의 경로와 본문을 커밋 후 기록한다")
  void repeatedReviews_preserveFieldsAndDeliveryCount() {
    var request = reviewRequest();
    var first =
        transaction.execute(
            status -> {
              var response = reviews.createReview(request, user.getId());
              assertThat(saved()).isEmpty();
              return response;
            });
    var second = reviews.createReview(request, user.getId());

    awaitCount(2);
    var saved = saved();
    assertCommon(saved.get(0), EventCategory.REVIEW, EventType.REVIEW_CREATE);
    assertCommon(saved.get(1), EventCategory.REVIEW, EventType.REVIEW_CREATE);
    assertThat(saved)
        .extracting(UserHistory::getRedirectUrl)
        .containsExactlyInAnyOrder(
            RedirectUrlType.REVIEW.getUrl() + "/" + first.getId(),
            RedirectUrlType.REVIEW.getUrl() + "/" + second.getId());
    assertThat(saved)
        .allSatisfy(
            history -> {
              assertThat(history.getContent()).isEqualTo(request.content());
              assertThat(history.getDynamicMessage()).isNull();
            });
  }

  @Test
  @DisplayName("찜과 해제를 반복할 때 상태 전이만 기존 필드로 기록한다")
  void picks_preserveTransitionsAndSuppressSameState() {
    pick(PicksStatus.PICK);
    pick(PicksStatus.PICK);
    pick(PicksStatus.UNPICK);
    pick(PicksStatus.UNPICK);
    pick(PicksStatus.PICK);

    awaitCount(3);
    assertThat(saved())
        .extracting(UserHistory::getEventType)
        .containsExactlyInAnyOrder(EventType.IS_PICK, EventType.UNPICK, EventType.IS_PICK);
    assertThat(saved())
        .allSatisfy(
            history -> {
              assertCommon(history, EventCategory.PICK, history.getEventType());
              assertThat(history.getRedirectUrl()).isEqualTo(alcoholUrl());
              assertThat(history.getContent()).isNull();
              assertThat(history.getDynamicMessage()).isNull();
            });
  }

  @Test
  @DisplayName("처음부터 찜 해제 상태로 요청할 때도 해제 이력을 기록한다")
  void initialUnpick_isRecorded() {
    pick(PicksStatus.UNPICK);
    awaitCount(1);
    assertCommon(saved().getFirst(), EventCategory.PICK, EventType.UNPICK);
    assertThat(saved().getFirst().getRedirectUrl()).isEqualTo(alcoholUrl());
  }

  @Test
  @DisplayName("별점 등록과 반복 수정과 삭제를 요청할 때 각 상태의 메시지를 보존한다")
  void rating_preservesMessagesAndRepeatedRequests() {
    rate(2.5);
    rate(4.0);
    rate(4.0);
    rate(0.0);
    rate(0.0);
    rate(3.0);

    awaitCount(6);
    assertThat(saved())
        .allSatisfy(
            history -> {
              assertCommon(history, EventCategory.RATING, history.getEventType());
              assertThat(history.getRedirectUrl()).isEqualTo(alcoholUrl());
              assertThat(history.getContent()).isNull();
            });
    assertThat(saved())
        .extracting(UserHistory::getEventType, UserHistory::getDynamicMessage)
        .containsExactlyInAnyOrder(
            tuple(EventType.START_RATING, Map.of("currentValue", "2.5")),
            tuple(EventType.RATING_MODIFY, message("4.0", "2.5", "1.5")),
            tuple(EventType.RATING_MODIFY, message("4.0", "4.0", "0.0")),
            tuple(EventType.RATING_DELETE, message("0.0", "4.0", "-4.0")),
            tuple(EventType.RATING_DELETE, message("0.0", "0.0", "0.0")),
            tuple(EventType.RATING_MODIFY, message("3.0", "0.0", "3.0")));
  }

  @Test
  @DisplayName("첫 별점으로 0점을 등록할 때 삭제가 아닌 시작 이력을 기록한다")
  void initialZeroRating_isStartRating() {
    rate(0.0);
    awaitCount(1);
    assertRating(saved().getFirst(), EventType.START_RATING, Map.of("currentValue", "0.0"));
  }

  @Test
  @DisplayName("리뷰와 찜과 별점의 원본 트랜잭션을 롤백할 때 이력을 남기지 않는다")
  void rollback_hasNoHistory() {
    transaction.executeWithoutResult(
        status -> {
          reviews.createReview(reviewRequest(), user.getId());
          pick(PicksStatus.PICK);
          rate(2.5);
          assertThat(saved()).isEmpty();
          status.setRollbackOnly();
        });

    awaitCount(0);
  }

  private ReviewCreateRequest reviewRequest() {
    var template = ReviewObjectFixture.getReviewCreateRequestWithAlcoholId(alcohol.getId());
    return new ReviewCreateRequest(
        template.alcoholId(),
        template.status(),
        template.content(),
        template.sizeType(),
        template.price(),
        template.locationInfo(),
        List.of(),
        template.tastingTagList(),
        template.rating(),
        template.tastingProfile());
  }

  private void pick(PicksStatus status) {
    picks.updatePicks(new PicksUpdateRequest(alcohol.getId(), status), user.getId());
  }

  private void rate(double point) {
    ratings.register(alcohol.getId(), user.getId(), RatingPoint.of(point));
  }

  private List<UserHistory> saved() {
    return histories.findAll().stream()
        .filter(history -> history.getUserId().equals(user.getId()))
        .sorted(Comparator.comparing(UserHistory::getId))
        .toList();
  }

  private void awaitCount(int count) {
    await()
        .atMost(Duration.ofSeconds(10))
        .during(Duration.ofSeconds(1))
        .untilAsserted(() -> assertThat(saved()).hasSize(count));
  }

  private void assertCommon(UserHistory history, EventCategory category, EventType type) {
    assertThat(history.getUserId()).isEqualTo(user.getId());
    assertThat(history.getAlcoholId()).isEqualTo(alcohol.getId());
    assertThat(history.getEventCategory()).isEqualTo(category);
    assertThat(history.getEventType()).isEqualTo(type);
    assertThat(history.getImageUrl()).isEqualTo(alcohol.getImageUrl());
    assertThat(history.getEventYear()).isEqualTo(String.valueOf(LocalDateTime.now().getYear()));
    assertThat(history.getEventMonth()).isEqualTo(String.valueOf(LocalDateTime.now().getMonth()));
  }

  private void assertRating(UserHistory history, EventType type, Map<String, String> message) {
    assertCommon(history, EventCategory.RATING, type);
    assertThat(history.getRedirectUrl()).isEqualTo(alcoholUrl());
    assertThat(history.getContent()).isNull();
    assertThat(history.getDynamicMessage()).isEqualTo(message);
  }

  private String alcoholUrl() {
    return RedirectUrlType.ALCOHOL.getUrl() + "/" + alcohol.getId();
  }

  private Map<String, String> message(String current, String previous, String difference) {
    return Map.of("currentValue", current, "prevValue", previous, "ratingDiff", difference);
  }
}
