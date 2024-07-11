package app.bottlenote.review.service;

import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.response.constant.ReviewReplyResultMessage;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.fixture.FakeProfanityClient;
import app.bottlenote.review.fixture.FakeUserDomainSupport;
import app.bottlenote.review.fixture.InMemoryReviewRepository;
import app.bottlenote.review.fixture.ReviewReplyObjectFixture;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.domain.UserDomainSupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("리뷰 댓글 서비스 테스트")
class ReviewReplyServiceTest {

	private static final Logger log = LogManager.getLogger(ReviewReplyServiceTest.class);
	private ReviewRepository reviewRepository;
	private ProfanityClient profanityClient;
	private UserDomainSupport userDomainSupport;
	private ReviewReplyService reviewReplyService;

	@BeforeEach
	void setUp() {
		User user1 = ReviewReplyObjectFixture.getUserFixture(1L);
		User user2 = ReviewReplyObjectFixture.getUserFixture(2L);
		Review review1 = ReviewReplyObjectFixture.getReviewFixture(1L, 1L, user1.getId());
		Review review2 = ReviewReplyObjectFixture.getReviewFixture(2L, 1L, user2.getId());

		reviewRepository = new InMemoryReviewRepository();
		profanityClient = new FakeProfanityClient();
		userDomainSupport = new FakeUserDomainSupport(user1, user2);

		reviewRepository.save(review1);
		reviewRepository.save(review2);

		reviewReplyService = new ReviewReplyService(
			reviewRepository,
			profanityClient,
			userDomainSupport
		);
	}

	@Nested
	@DisplayName("새로운 댓글을 등록할 수 있다.")
	class registry {

		@Test
		@DisplayName("리뷰에 댓글을 등록 할 수 있다.")
		void test_1() {
			//given
			final Long reviewId = 1L;
			final Long userId = 1L;
			final String content = "댓글 내용입니다.";
			var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(content);

			//when
			var response = reviewReplyService.registerReviewReply(reviewId, userId, request);

			//then
			log.info("response = {}", response);
			reviewRepository.findById(reviewId).get().getReviewReplies().stream().findFirst().ifPresent(reply -> {
				assertEquals(reply.getContent(), content);
				assertEquals(reply.getUserId(), userId);
			});
			assertNotNull(response);
			assertEquals(ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, response.codeMessage());
			assertEquals(reviewId, response.reviewId());
		}

		@Test
		@DisplayName("비속어가 포함되어 있는 경우 마스킹 처리 된 후 저장된다.")
		void test_2() {
			//given
			final Long reviewId = 1L;
			final Long userId = 1L;
			final String content = "댓글 내용입니다. 비속어";
			final String maskingContent = "댓글 내용입니다. ***";
			var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(content);

			//when
			var response = reviewReplyService.registerReviewReply(reviewId, userId, request);

			//then
			reviewRepository.findById(reviewId).get().getReviewReplies().stream().findFirst().ifPresent(reply -> {
				log.info("reply = {}", reply);
				assertEquals(reply.getContent(), maskingContent);
				assertEquals(reply.getUserId(), userId);
			});
			assertNotNull(response);
			assertEquals(ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, response.codeMessage());
			assertEquals(reviewId, response.reviewId());
		}

		@Test
		@DisplayName("유저 식별자가 적절하지 않은 경우 UserException을 발생시킨다.")
		void test_3() {
			//given
			final Long reviewId = 1L;
			final Long userId = -1L;
			final String content = "댓글 내용입니다.";
			var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(content);

			//when
			//then
			UserException aThrows = assertThrows(UserException.class, () -> reviewReplyService.registerReviewReply(reviewId, userId, request));
			assertEquals(UserExceptionCode.USER_NOT_FOUND.getMessage(), aThrows.getMessage());
			log.debug("aThrows : {}", aThrows.toString());
		}

		@Test
		@DisplayName("리뷰 식별자가 적절하지 않은 경우 ReviewException을 발생시킨다.")
		void test_4() {
			//given
			final Long reviewId = -1L;
			final Long userId = 1L;
			final String content = "댓글 내용입니다.";
			var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(content);

			//when
			//then
			ReviewException aThrows = assertThrows(ReviewException.class, () -> reviewReplyService.registerReviewReply(reviewId, userId, request));
			assertEquals(ReviewExceptionCode.REVIEW_NOT_FOUND.getMessage(), aThrows.getMessage());
			log.debug("aThrows : {}", aThrows.toString());
		}

		@Test
		@DisplayName("상위 댓글이 존재하는 경우 최상위 댓글도 등록된다")
		void test_5() {
			//given
			final Long reviewId = 1L;
			final Long userId = 1L;
			final String content = "자식 댓글 내용입니다.";

			Review review = reviewRepository.findById(reviewId).get();
			ReviewReply parentReply = ReviewReplyObjectFixture.getReviewReplyFixture(1L, review);
			review.getReviewReplies().add(parentReply);
			var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(content, parentReply.getId());

			//when
			var response = reviewReplyService.registerReviewReply(reviewId, userId, request);

			//then
			review.getReviewReplies().stream()
				.filter(r -> r.getContent().equals(content))
				.findFirst()
				.ifPresent(reply -> {
					assertEquals(reply.getContent(), content);
					assertEquals(reply.getUserId(), userId);
					assertEquals(reply.getParentReviewReply().getId(), parentReply.getId());
					log.info("reply = {} / {}", reply, reply.getParentReviewReply());
				});

			assertNotNull(response);
			assertEquals(ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, response.codeMessage());
			assertEquals(reviewId, response.reviewId());
		}
	}

}
