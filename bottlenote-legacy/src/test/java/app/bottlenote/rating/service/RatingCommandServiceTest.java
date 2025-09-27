package app.bottlenote.rating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bottlenote.alcohols.fixture.FakeAlcoholFacade;
import app.bottlenote.core.alcohols.application.AlcoholFacade;
import app.bottlenote.core.users.application.UserFacade;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.history.fixture.FakeHistoryEventPublisher;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.Rating.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.response.RatingRegisterResponse;
import app.bottlenote.rating.exception.RatingException;
import app.bottlenote.rating.fixture.InMemoryRatingRepository;
import app.bottlenote.shared.users.payload.UserProfileItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.fixture.FakeUserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] RatingCommandService")
class RatingCommandServiceTest {
  private final Long userId = 1L;
  private final Long alcoholId = 1L;
  private RatingRepository fakeRatingRepository;
  private RatingCommandService ratingCommandService;

  @BeforeEach
  void setup() {
    UserProfileItem user1 = UserProfileItem.create(1L, "user1", "");
    UserProfileItem user2 = UserProfileItem.create(2L, "user2", "");
    UserProfileItem user3 = UserProfileItem.create(3L, "user3", "");

    fakeRatingRepository = new InMemoryRatingRepository();
    UserFacade fakeUserFacade = new FakeUserFacade(user1, user2, user3);
    AlcoholFacade fakeAlcoholFacade = new FakeAlcoholFacade();
    HistoryEventPublisher ratingEventPublisher = new FakeHistoryEventPublisher();
    ratingCommandService =
        new RatingCommandService(
            fakeRatingRepository, fakeUserFacade, fakeAlcoholFacade, ratingEventPublisher);
  }

  @Nested
  @DisplayName("별점 등록")
  class RegisterRatingPoint {

    @Test
    @DisplayName("신규 별점을 등록할 수 있다.")
    void test_1() {
      // given
      RatingPoint ratingPoint = RatingPoint.of(5);
      // when
      RatingRegisterResponse register =
          ratingCommandService.register(alcoholId, userId, ratingPoint);
      // then
      assertNotNull(register);
      assertEquals(register.rating(), ratingPoint.toString());
    }

    @Test
    @DisplayName("기존 별점을 수정 할 수 있다.")
    void test_2() {
      // given
      fakeRatingRepository.save(
          Rating.builder()
              .id(RatingId.is(userId, alcoholId))
              .ratingPoint(RatingPoint.of(1))
              .build());

      RatingPoint ratingPoint = RatingPoint.of(5);

      // when
      String 변경전_포인트 =
          fakeRatingRepository
              .findByAlcoholIdAndUserId(alcoholId, userId)
              .get()
              .getRatingPoint()
              .getRating()
              .toString();
      RatingRegisterResponse register =
          ratingCommandService.register(alcoholId, userId, ratingPoint);
      String 변경후_포인트 = register.rating();
      // then.
      assertNotNull(register);
      assertNotEquals(변경전_포인트, 변경후_포인트);
      assertEquals(변경후_포인트, ratingPoint.getRating().toString());
    }

    @Test
    @DisplayName("필수 파라미터가  없을 경우 NullPointerException을 발생시킨다.")
    void test_3() {
      // when  && then
      RatingPoint point = RatingPoint.of(5);
      assertThrows(
          NullPointerException.class, () -> ratingCommandService.register(null, userId, point));
      assertThrows(
          NullPointerException.class, () -> ratingCommandService.register(alcoholId, null, point));
      assertThrows(
          NullPointerException.class, () -> ratingCommandService.register(alcoholId, userId, null));
    }

    @Test
    @DisplayName("유저가 없을 경우 UserException을 발생시킨다.")
    void test_4() {
      // when  && then
      assertThrows(
          UserException.class,
          () -> ratingCommandService.register(alcoholId, 22L, RatingPoint.of(5)));
    }

    @Test
    @DisplayName("알코올이 없을 경우 RatingException 발생시킨다.")
    void test_5() {
      // when  && then
      assertThrows(
          RatingException.class,
          () -> ratingCommandService.register(22L, userId, RatingPoint.of(5)));
    }
  }
}
