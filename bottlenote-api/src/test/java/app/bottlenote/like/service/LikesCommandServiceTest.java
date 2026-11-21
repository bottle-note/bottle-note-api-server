package app.bottlenote.like.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bottlenote.like.constant.LikeStatus;
import app.bottlenote.like.event.payload.ReviewLikeActivityEvent;
import app.bottlenote.like.fixture.InMemoryLikesRepository;
import app.bottlenote.review.fixture.FakeReviewFacade;
import app.bottlenote.user.facade.payload.UserProfileItem;
import app.bottlenote.user.fixture.FakeUserFacade;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("unit")
@DisplayName("[unit] [service] LikesCommand")
class LikesCommandServiceTest {

  private static final Logger log = LoggerFactory.getLogger(LikesCommandServiceTest.class);
  private FakeUserFacade userFacade;
  private FakeReviewFacade reviewFacade;
  private LikesCommandService likesCommandService;
  private InMemoryLikesRepository likesRepository;
  private List<Object> events;

  @BeforeEach
  void setUp() {
    userFacade =
        new FakeUserFacade(
            UserProfileItem.create(1L, "user1", ""),
            UserProfileItem.create(2L, "user2", ""),
            UserProfileItem.create(3L, "user3", ""));
    reviewFacade = new FakeReviewFacade();
    likesRepository = new InMemoryLikesRepository();
    events = new ArrayList<>();
    likesCommandService =
        new LikesCommandService(userFacade, reviewFacade, likesRepository, events::add);
  }

  @Test
  @DisplayName("사용자는 리뷰에 좋아요를 할 수 있다.")
  void test_1() {
    Long userId = userFacade.userDatabase.keySet().stream().findFirst().orElseThrow();
    Long reviewId = reviewFacade.reviewDatabase.keySet().stream().findFirst().orElseThrow();
    var response = likesCommandService.updateLikes(userId, reviewId, LikeStatus.LIKE);

    log.info("response = {}", response);
    // then
    likesRepository.findAll().stream()
        .findFirst()
        .ifPresent(
            likes -> {
              log.info("db에 저장된 likes = {}", likes);
              assertEquals(1L, likes.getReviewId());
              assertEquals(1L, likes.getUserInfo().getUserId());
              assertEquals(LikeStatus.LIKE, likes.getStatus());
            });

    assertNotNull(response);
    assertEquals(1L, response.likesId());
    assertEquals(1L, response.reviewId());
    assertEquals(1L, response.userId());
    assertEquals(LikeStatus.LIKE, response.status());
  }

  @Test
  @DisplayName("이미 중복된 좋아요 요청이 들어와도 동일한 값을 반환한다. ")
  void test_2() {
    // given
    Long userId = userFacade.userDatabase.keySet().stream().findFirst().orElseThrow();
    Long reviewId = reviewFacade.reviewDatabase.keySet().stream().findFirst().orElseThrow();
    likesCommandService.updateLikes(userId, reviewId, LikeStatus.LIKE);

    // when
    var response = likesCommandService.updateLikes(userId, reviewId, LikeStatus.LIKE);

    // then
    assertNotNull(response);
    assertEquals(1L, response.likesId());
    assertEquals(reviewId, response.reviewId());
    assertEquals(userId, response.userId());
    assertEquals(LikeStatus.LIKE, response.status());
  }

  @Test
  @DisplayName("좋아요 요청 후 좋아요 취소 요청을 할 수 있다.")
  void test_3() {
    // given
    Long userId = userFacade.userDatabase.keySet().stream().findFirst().orElseThrow();
    Long reviewId = reviewFacade.reviewDatabase.keySet().stream().findFirst().orElseThrow();
    likesCommandService.updateLikes(userId, reviewId, LikeStatus.LIKE);

    // when
    var response = likesCommandService.updateLikes(userId, reviewId, LikeStatus.DISLIKE);

    // then
    assertNotNull(response);
    assertEquals(1L, response.likesId());
    assertEquals(reviewId, response.reviewId());
    assertEquals(userId, response.userId());
    assertEquals(LikeStatus.DISLIKE, response.status());
  }

  @Test
  @DisplayName("좋아요 상태 요청마다 활동 한 건을 발행하고 활성 전이만 구분한다")
  void updateLikes_publishesOneActivityPerRequest() {
    for (LikeStatus status :
        List.of(LikeStatus.LIKE, LikeStatus.LIKE, LikeStatus.DISLIKE, LikeStatus.LIKE)) {
      likesCommandService.updateLikes(1L, 1L, status);
    }

    assertThat(events).hasSize(4).allMatch(ReviewLikeActivityEvent.class::isInstance);
    List<ReviewLikeActivityEvent> activities =
        events.stream().map(ReviewLikeActivityEvent.class::cast).toList();
    assertThat(activities)
        .extracting(ReviewLikeActivityEvent::activated)
        .containsExactly(true, false, false, true);
    assertThat(activities).extracting(ReviewLikeActivityEvent::likeId).containsOnly(1L);
    assertThat(activities)
        .extracting(ReviewLikeActivityEvent::content)
        .containsOnly(reviewFacade.getReview(1L).reviewContent());
  }

  @Test
  @DisplayName("최초 요청이 취소일 때 History용 활동만 발행한다")
  void updateLikes_initialDislike_isNotActivation() {
    likesCommandService.updateLikes(1L, 1L, LikeStatus.DISLIKE);

    assertThat(events).hasSize(1);
    assertThat(((ReviewLikeActivityEvent) events.getFirst()).activated()).isFalse();
  }
}
