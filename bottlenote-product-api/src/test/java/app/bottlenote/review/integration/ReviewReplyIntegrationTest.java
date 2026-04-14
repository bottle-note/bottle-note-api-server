package app.bottlenote.review.integration;

import static app.bottlenote.review.constant.ReviewReplyStatus.DELETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.review.constant.ReviewReplyResultMessage;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewReplyRepository;
import app.bottlenote.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.review.dto.response.ReviewReplyResponse;
import app.bottlenote.review.dto.response.RootReviewReplyResponse;
import app.bottlenote.review.dto.response.SubReviewReplyResponse;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.fixture.UserTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] ReviewReplyController")
class ReviewReplyIntegrationTest extends IntegrationTestSupport {

  @Autowired private ReviewReplyRepository reviewReplyRepository;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;

  @Nested
  @DisplayName("리뷰 댓글 생성 테스트")
  class create {

    @DisplayName("리뷰의 댓글을 생성할 수 있다.")
    @Test
    void test_1() throws Exception {
      // given
      User reviewAuthor = userTestFactory.persistUser();
      User replyAuthor = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(reviewAuthor, alcohol);
      TokenItem token = getToken(replyAuthor);

      ReviewReplyRegisterRequest replyRegisterRequest =
          new ReviewReplyRegisterRequest("댓글 내용", null);

      // when
      MvcTestResult result =
          mockMvcTester
              .post()
              .uri("/api/v1/review/reply/register/{reviewId}", review.getId())
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(replyRegisterRequest))
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      // then
      ReviewReplyResponse reviewReplyResponse = extractData(result, ReviewReplyResponse.class);
      assertEquals(
          ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, reviewReplyResponse.codeMessage());
    }

    @DisplayName("댓글의 대댓글을 생성할 수 있다.")
    @Test
    void test_2() throws Exception {
      // given
      User reviewAuthor = userTestFactory.persistUser();
      User replyAuthor = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(reviewAuthor, alcohol);
      ReviewReply parentReply = reviewTestFactory.persistReviewReply(review, replyAuthor);
      TokenItem token = getToken(replyAuthor);

      ReviewReplyRegisterRequest replyRegisterRequest =
          new ReviewReplyRegisterRequest("대댓글 내용", parentReply.getId());

      // when
      MvcTestResult result =
          mockMvcTester
              .post()
              .uri("/api/v1/review/reply/register/{reviewId}", review.getId())
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(replyRegisterRequest))
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      // then
      ReviewReplyResponse reviewReplyResponse = extractData(result, ReviewReplyResponse.class);
      assertEquals(
          ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, reviewReplyResponse.codeMessage());
    }
  }

  @Nested
  @DisplayName("리뷰 댓글 조회 테스트")
  class read {

    @DisplayName("리뷰의 최상위 댓글을 조회할 수 있다.")
    @Test
    void test_1() throws Exception {
      // given
      User reviewAuthor = userTestFactory.persistUser();
      User replyAuthor1 = userTestFactory.persistUser();
      User replyAuthor2 = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(reviewAuthor, alcohol);
      reviewTestFactory.persistReviewReply(review, replyAuthor1);
      reviewTestFactory.persistReviewReply(review, replyAuthor2);

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/review/reply/{reviewId}", review.getId())
              .contentType(APPLICATION_JSON)
              .param("cursor", "0")
              .param("pageSize", "50")
              .with(csrf())
              .exchange();

      // then
      result.assertThat().hasStatusOk();
      RootReviewReplyResponse rootResponse = extractData(result, RootReviewReplyResponse.class);
      assertEquals(2, rootResponse.totalCount());
    }

    @DisplayName("리뷰의 대댓글 목록을 조회할 수 있다.")
    @Test
    void test_2() throws Exception {
      // given
      User reviewAuthor = userTestFactory.persistUser();
      User replyAuthor = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(reviewAuthor, alcohol);
      ReviewReply parentReply = reviewTestFactory.persistReviewReply(review, replyAuthor);
      TokenItem token = getToken(replyAuthor);

      ReviewReplyRegisterRequest replyRegisterRequest =
          new ReviewReplyRegisterRequest("대댓글 내용", parentReply.getId());
      final int count = 2;

      for (int i = 0; i < count; i++) {
        MvcTestResult registerResult =
            mockMvcTester
                .post()
                .uri("/api/v1/review/reply/register/{reviewId}", review.getId())
                .contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(replyRegisterRequest))
                .header("Authorization", "Bearer " + token.accessToken())
                .with(csrf())
                .exchange();

        registerResult.assertThat().hasStatusOk();
      }

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri(
                  "/api/v1/review/reply/{reviewId}/sub/{rootReplyId}",
                  review.getId(),
                  parentReply.getId())
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(replyRegisterRequest))
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      // then
      SubReviewReplyResponse subReviewReplyResponse =
          extractData(result, SubReviewReplyResponse.class);
      assertEquals(count, subReviewReplyResponse.totalCount());
    }
  }

  @Nested
  @DisplayName("리뷰 댓글 삭제 테스트")
  class deleteTest {

    @DisplayName("리뷰 댓글을 삭제할 수 있다.")
    @Test
    void test_1() throws Exception {
      // given
      User reviewAuthor = userTestFactory.persistUser();
      User replyAuthor1 = userTestFactory.persistUser();
      User replyAuthor2 = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(reviewAuthor, alcohol);
      ReviewReply replyToDelete = reviewTestFactory.persistReviewReply(review, replyAuthor1);
      reviewTestFactory.persistReviewReply(review, replyAuthor2);
      TokenItem token = getToken(replyAuthor1);

      // when - 삭제
      MvcTestResult deleteResult =
          mockMvcTester
              .delete()
              .uri(
                  "/api/v1/review/reply/{reviewId}/{replyId}",
                  review.getId(),
                  replyToDelete.getId())
              .contentType(APPLICATION_JSON)
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      deleteResult.assertThat().hasStatusOk();

      // then - 조회하여 삭제 상태 확인
      MvcTestResult listResult =
          mockMvcTester
              .get()
              .uri("/api/v1/review/reply/{reviewId}", review.getId())
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      RootReviewReplyResponse rootReviewReplyResponse =
          extractData(listResult, RootReviewReplyResponse.class);

      assertEquals(
          rootReviewReplyResponse.reviewReplies().get(0).reviewReplyContent(),
          DELETED.getMessage());
    }
  }
}
