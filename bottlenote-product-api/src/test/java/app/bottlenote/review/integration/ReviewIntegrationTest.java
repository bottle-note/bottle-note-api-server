package app.bottlenote.review.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.exception.custom.code.ValidExceptionCode;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.constant.ReviewResultMessage;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.facade.payload.ReviewInfo;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.fixture.UserTestFactory;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] ReviewReplyController")
class ReviewIntegrationTest extends IntegrationTestSupport {

  @Autowired private ReviewRepository reviewRepository;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;

  @Nested
  @DisplayName("리뷰 조회 테스트")
  class select {

    @DisplayName("리뷰 목록 조회에 성공한다.")
    @Test
    void test_1() throws Exception {
      // given
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      reviewTestFactory.persistReview(user, alcohol);
      reviewTestFactory.persistReview(user, alcohol);

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/reviews/{alcoholId}", alcohol.getId())
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // then
      ReviewListResponse reviewListResponse = extractData(result, ReviewListResponse.class);
      List<ReviewInfo> reviewInfos = reviewListResponse.reviewList();

      assertNotNull(reviewListResponse);
      assertFalse(reviewInfos.isEmpty());
    }

    @DisplayName("리뷰 상세 조회에 성공한다.")
    @Test
    void test_2() throws Exception {
      // given
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      TokenItem token = getToken(user);

      ReviewCreateRequest reviewCreateRequest =
          ReviewObjectFixture.getReviewCreateRequestWithAlcoholId(alcohol.getId());

      MvcTestResult createResult =
          mockMvcTester
              .post()
              .uri("/api/v1/reviews")
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsBytes(reviewCreateRequest))
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      ReviewCreateResponse reviewCreateResponse =
          extractData(createResult, ReviewCreateResponse.class);

      final Long reviewId = reviewCreateResponse.getId();

      // when
      MvcTestResult detailResult =
          mockMvcTester
              .get()
              .uri("/api/v1/reviews/detail/{reviewId}", reviewId)
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // then
      ReviewDetailResponse reviewDetailResponse =
          extractData(detailResult, ReviewDetailResponse.class);

      assertNotNull(reviewDetailResponse.reviewInfo());
      reviewDetailResponse.reviewImageList().forEach(Assertions::assertNotNull);
      assertNotNull(reviewDetailResponse.alcoholInfo());
      assertEquals(
          reviewCreateRequest.content(), reviewDetailResponse.reviewInfo().reviewContent());
    }

    @DisplayName("내가 작성한 리뷰 조회에 성공한다.")
    @Test
    void test_3() throws Exception {
      // given
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      TokenItem token = getToken(user);

      Review review = reviewTestFactory.persistReview(user, alcohol);
      List<Review> reviewList = reviewRepository.findByUserId(user.getId());

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/reviews/me/{alcoholId}", alcohol.getId())
              .contentType(APPLICATION_JSON)
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      // then
      ReviewListResponse reviewListResponse = extractData(result, ReviewListResponse.class);

      assertNotNull(reviewListResponse.reviewList());
      assertEquals(reviewList.size(), reviewListResponse.reviewList().size());

      reviewList.forEach(
          r -> {
            ReviewInfo reviewInfo =
                reviewListResponse.reviewList().stream()
                    .filter(info -> info.reviewId().equals(r.getId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(r.getContent(), reviewInfo.reviewContent());
          });
    }
  }

  @Nested
  @DisplayName("리뷰 생성 테스트")
  class create {

    @DisplayName("모든 필드가 포함된 리뷰 생성에 성공한다.")
    @Test
    void test_1() throws Exception {
      // given
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      TokenItem token = getToken(user);

      ReviewCreateRequest reviewCreateRequest =
          ReviewObjectFixture.getReviewCreateRequestWithAlcoholId(alcohol.getId());

      // when
      MvcTestResult result =
          mockMvcTester
              .post()
              .uri("/api/v1/reviews")
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsBytes(reviewCreateRequest))
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      // then
      ReviewCreateResponse reviewCreateResponse = extractData(result, ReviewCreateResponse.class);

      assertEquals(reviewCreateRequest.content(), reviewCreateResponse.getContent());
    }
  }

  @Nested
  @DisplayName("리뷰 삭제 테스트")
  class deleteTest {

    @DisplayName("리뷰 삭제에 성공한다. (목록 조회 시 미노출)")
    @Test
    void test_1() throws Exception {
      // given
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      TokenItem token = getToken(user);

      ReviewCreateRequest reviewCreateRequest =
          ReviewObjectFixture.getReviewCreateRequestWithAlcoholId(alcohol.getId());

      // 리뷰 생성
      MvcTestResult createResult =
          mockMvcTester
              .post()
              .uri("/api/v1/reviews")
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsBytes(reviewCreateRequest))
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      ReviewCreateResponse reviewCreateResponse =
          extractData(createResult, ReviewCreateResponse.class);

      final Long alcoholId = reviewCreateRequest.alcoholId();
      final Long reviewId = reviewCreateResponse.getId();

      // 리뷰 삭제
      MvcTestResult deleteResult =
          mockMvcTester
              .delete()
              .uri("/api/v1/reviews/{reviewId}", reviewId)
              .contentType(APPLICATION_JSON)
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      ReviewResultResponse reviewResultResponse =
          extractData(deleteResult, ReviewResultResponse.class);

      // 리뷰 목록에서 조회안되는지 검증
      MvcTestResult listResult =
          mockMvcTester
              .get()
              .uri("/api/v1/reviews/{alcoholId}", alcoholId)
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      ReviewListResponse reviewListResponse = extractData(listResult, ReviewListResponse.class);
      List<ReviewInfo> reviewInfos = reviewListResponse.reviewList();

      assertEquals(
          reviewResultResponse.codeMessage().name(), ReviewResultMessage.DELETE_SUCCESS.name());
      reviewInfos.forEach(reviewInfo -> assertNotEquals(reviewInfo.reviewId(), reviewId));
    }
  }

  @Nested
  @DisplayName("리뷰 수정 테스트")
  class update {

    @DisplayName("리뷰 수정에 성공한다.")
    @Test
    void test_1() throws Exception {
      // given
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      TokenItem token = getToken(user);
      Review review = reviewTestFactory.persistReview(user, alcohol);

      final Long reviewId = review.getId();
      final ReviewModifyRequest request =
          ReviewObjectFixture.getReviewModifyRequest(ReviewDisplayStatus.PUBLIC);

      // when
      MvcTestResult result =
          mockMvcTester
              .patch()
              .uri("/api/v1/reviews/{reviewId}", reviewId)
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(request))
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      // then
      result.assertThat().hasStatusOk();
      Review savedReview = reviewRepository.findById(reviewId).orElseThrow();
      assertEquals(savedReview.getContent(), request.content());
    }

    @DisplayName("content와 status를 제외한 필드에 null이 할당되어도 수정에 성공한다.")
    @Test
    void test_2() throws Exception {
      // given
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      TokenItem token = getToken(user);
      Review review = reviewTestFactory.persistReview(user, alcohol);

      final Long reviewId = review.getId();
      final ReviewModifyRequest request =
          ReviewObjectFixture.getNullableReviewModifyRequest(ReviewDisplayStatus.PRIVATE);

      // when
      MvcTestResult result =
          mockMvcTester
              .patch()
              .uri("/api/v1/reviews/{reviewId}", reviewId)
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(request))
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      // then
      result.assertThat().hasStatusOk();
      Review savedReview = reviewRepository.findById(reviewId).orElseThrow();
      assertEquals(savedReview.getContent(), request.content());
    }

    @DisplayName("Not Null인 필드에 null이 할당되면 리뷰 수정에 실패한다.")
    @Test
    void test_3() throws Exception {
      // given
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      TokenItem token = getToken(user);
      Review review = reviewTestFactory.persistReview(user, alcohol);

      final Error notNullEmpty = Error.of(ValidExceptionCode.REVIEW_CONTENT_REQUIRED);
      final Error notStatusEmpty = Error.of(ValidExceptionCode.REVIEW_DISPLAY_STATUS_NOT_EMPTY);

      final Long reviewId = review.getId();
      final ReviewModifyRequest request = ReviewObjectFixture.getWrongReviewModifyRequest();

      // when
      MvcTestResult result =
          mockMvcTester
              .patch()
              .uri("/api/v1/reviews/{reviewId}", reviewId)
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(request))
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      // then
      result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
      result.assertThat().bodyJson().extractingPath("$.errors").asArray().hasSize(2);
    }

    @DisplayName("리뷰 상태 변경에 성공한다.")
    @Test
    void test_4() throws Exception {
      // given
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      TokenItem token = getToken(user);
      Review review = reviewTestFactory.persistReview(user, alcohol);

      final Long reviewId = review.getId();
      final ReviewModifyRequest request =
          ReviewObjectFixture.getReviewModifyRequest(ReviewDisplayStatus.PRIVATE);

      // when
      MvcTestResult result =
          mockMvcTester
              .patch()
              .uri("/api/v1/reviews/{reviewId}/display", reviewId)
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(request))
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      // then
      result.assertThat().hasStatusOk();
      Review savedReview = reviewRepository.findById(reviewId).orElseThrow();
      assertEquals(ReviewDisplayStatus.PRIVATE, savedReview.getStatus());
    }

    @DisplayName("Not null인 필드에 null이 할당되면 리뷰 상태 변경에 실패한다.")
    @Test
    void test_5() throws Exception {
      // given
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      TokenItem token = getToken(user);
      Review review = reviewTestFactory.persistReview(user, alcohol);

      final Error notStatusEmpty = Error.of(ValidExceptionCode.REVIEW_DISPLAY_STATUS_NOT_EMPTY);
      final ReviewModifyRequest request = ReviewObjectFixture.getNullableReviewModifyRequest(null);

      final Long reviewId = review.getId();

      // when
      MvcTestResult result =
          mockMvcTester
              .patch()
              .uri("/api/v1/reviews/{reviewId}/display", reviewId)
              .contentType(APPLICATION_JSON)
              .content(mapper.writeValueAsString(request))
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      // then
      result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
      result.assertThat().bodyJson().extractingPath("$.success").isEqualTo(false);
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.errors[0].status")
          .isEqualTo(notStatusEmpty.status().name());
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.errors[0].message")
          .isEqualTo(notStatusEmpty.message());
    }
  }

  @Nested
  @DisplayName("리뷰 상태 변경")
  class changeStatus {}
}
