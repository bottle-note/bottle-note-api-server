package app.bottlenote.review.service;

import static app.bottlenote.shared.review.exception.ReviewExceptionCode.NOT_FOUND_REVIEW_REPLY;
import static app.bottlenote.shared.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.history.fixture.FakeHistoryEventPublisher;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewReplyRepository;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.fixture.FakeProfanityClient;
import app.bottlenote.review.fixture.InMemoryReviewReplyRepository;
import app.bottlenote.review.fixture.InMemoryReviewRepository;
import app.bottlenote.review.fixture.ReviewReplyObjectFixture;
import app.bottlenote.shared.review.constant.ReviewReplyResultMessage;
import app.bottlenote.shared.review.exception.ReviewException;
import app.bottlenote.shared.review.exception.ReviewExceptionCode;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.facade.UserFacade;
import app.bottlenote.user.facade.payload.UserProfileItem;
import app.bottlenote.user.fixture.FakeUserFacade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] ReviewReplyService")
class ReviewReplyServiceTest {

  private static final Logger log = LogManager.getLogger(ReviewReplyServiceTest.class);
  private ReviewReplyRepository reviewReplyRepository;
  private ReviewRepository reviewRepository;
  private ProfanityClient profanityClient;
  private UserFacade userFacade;
  private ReviewReplyService reviewReplyService;
  private HistoryEventPublisher reviewReplyEventPublisher;

  @BeforeEach
  void setUp() {
    UserProfileItem user1 = UserProfileItem.create(1L, "user1", "");
    UserProfileItem user2 = UserProfileItem.create(2L, "user2", "");
    UserProfileItem user3 = UserProfileItem.create(3L, "user3", "");
    Review review1 = ReviewReplyObjectFixture.getReviewFixture(1L, 1L, 1L);
    Review review2 = ReviewReplyObjectFixture.getReviewFixture(2L, 1L, 2L);

    userFacade = new FakeUserFacade(user1, user2, user3);
    profanityClient = new FakeProfanityClient();
    reviewReplyRepository = new InMemoryReviewReplyRepository();
    reviewRepository = new InMemoryReviewRepository();
    reviewReplyEventPublisher = new FakeHistoryEventPublisher();

    reviewRepository.save(review1);
    reviewRepository.save(review2);

    reviewReplyService =
        new ReviewReplyService(
            reviewReplyRepository,
            reviewRepository,
            profanityClient,
            userFacade,
            reviewReplyEventPublisher);
  }

  @Nested
  @DisplayName("새로운 댓글을 등록할 수 있다.")
  class registry {

    @Test
    @DisplayName("리뷰에 댓글을 등록 할 수 있다.")
    void test_1() {
      // given
      log.info("init success profanityClient : {}", profanityClient);
      log.info("init success userDomainSupport : {}", userFacade);

      final Long reviewId = 1L;
      final Long userId = 1L;
      final String content = "댓글 내용입니다.";
      var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(content);

      // when
      var response = reviewReplyService.registerReviewReply(reviewId, userId, request);

      // then
      log.info("response = {}", response);
      assertNotNull(response);
      assertEquals(ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, response.codeMessage());
    }

    @Test
    @DisplayName("비속어가 포함되어 있는 경우 마스킹 처리 된 후 저장된다.")
    void test_2() {
      // given
      final Long reviewId = 1L;
      final Long userId = 1L;
      final String content = "댓글 내용입니다. 비속어";
      final String maskingContent = "댓글 내용입니다. ***";
      var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(content);

      // when
      var response = reviewReplyService.registerReviewReply(reviewId, userId, request);

      // then

      assertNotNull(response);
      assertEquals(ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, response.codeMessage());
      assertEquals(maskingContent, reviewReplyRepository.findReplyById(1L).get().getContent());
    }

    @Test
    @DisplayName("유저 식별자가 적절하지 않은 경우 UserException을 발생시킨다.")
    void test_3() {
      // given
      final Long reviewId = 1L;
      final Long userId = -1L;
      final String content = "댓글 내용입니다.";
      var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(content);

      // when
      // then
      UserException aThrows =
          assertThrows(
              UserException.class,
              () -> reviewReplyService.registerReviewReply(reviewId, userId, request));
      assertEquals(UserExceptionCode.USER_NOT_FOUND.getMessage(), aThrows.getMessage());
      log.debug("aThrows : {}", aThrows.toString());
    }

    @Test
    @DisplayName("리뷰 식별자가 적절하지 않은 경우 ReviewException을 발생시킨다.")
    void test_4() {
      // given
      final Long reviewId = -1L;
      final Long userId = 1L;
      final String content = "댓글 내용입니다.";
      var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(content);

      // when&&then
      ReviewException aThrows =
          assertThrows(
              ReviewException.class,
              () -> reviewReplyService.registerReviewReply(reviewId, userId, request));
      assertEquals(REVIEW_NOT_FOUND.getMessage(), aThrows.getMessage());
      log.debug("aThrows : {}", aThrows.toString());
    }

    @Test
    @DisplayName("상위 댓글이 존재하는 경우 최상위 댓글도 등록된다")
    void test_5() {
      // given
      final Long reviewId = 1L;
      final Long userId = 1L;
      final String content = "자식 댓글 내용입니다.";

      Review review =
          reviewRepository
              .findById(reviewId)
              .orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));
      ReviewReply parentReply = ReviewReplyObjectFixture.getReviewReplyFixture(1L, review.getId());
      var request =
          ReviewReplyObjectFixture.getReviewReplyRegisterRequest(content, parentReply.getId());

      // when
      var response = reviewReplyService.registerReviewReply(reviewId, userId, request);

      // then
      assertNotNull(response);
      assertEquals(ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, response.codeMessage());
      assertEquals(reviewId, response.reviewId());
    }
  }

  @Nested
  @DisplayName("댓글 목록을 조회할 수 있다.")
  class select {}

  @Nested
  @DisplayName("댓글을 삭제할 수 있다.")
  class delete {

    @Test
    @DisplayName("정상적으로 댓글을 삭제할 수 있다.")
    void test_1() {
      // given
      final ReviewReply save =
          reviewReplyRepository.save(ReviewReplyObjectFixture.getReviewReplyFixture(99L, 1L));

      // when
      var response =
          reviewReplyService.deleteReviewReply(save.getReviewId(), save.getId(), save.getUserId());

      // then
      assertEquals(ReviewReplyResultMessage.SUCCESS_DELETE_REPLY, response.codeMessage());
      assertEquals(save.getReviewId(), response.reviewId());
      log.info("댓글 삭제 결과 : {}", response);
    }

    @Test
    @DisplayName("삭제할 댓글이 존재하지 않는 경우 ReviewException을 발생시킨다.")
    void test_2() {

      ReviewException aThrows =
          assertThrows(
              ReviewException.class, () -> reviewReplyService.deleteReviewReply(1L, -1L, 1L));

      assertEquals(NOT_FOUND_REVIEW_REPLY, aThrows.getExceptionCode());
      assertEquals(NOT_FOUND_REVIEW_REPLY.getMessage(), aThrows.getMessage());
    }

    @Test
    @DisplayName("자신의 댓글이 아닌 경우 ReviewException을 발생시킨다.")
    void test_3() {

      // given
      final ReviewReply save =
          reviewReplyRepository.save(ReviewReplyObjectFixture.getReviewReplyFixture(99L, 1L));

      // when && then
      ReviewException aThrows =
          assertThrows(
              ReviewException.class,
              () -> reviewReplyService.deleteReviewReply(save.getReviewId(), save.getId(), -1L));

      assertEquals(ReviewExceptionCode.REPLY_NOT_OWNER.getMessage(), aThrows.getMessage());
      assertEquals(ReviewExceptionCode.REPLY_NOT_OWNER, aThrows.getExceptionCode());
    }
  }
}
