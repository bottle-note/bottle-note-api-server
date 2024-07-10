package app.bottlenote.review.service;

import static app.bottlenote.review.dto.response.constant.ReviewResultMessage.DELETE_SUCCESS;
import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.alcohols.service.domain.AlcoholDomainSupport;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.user.service.domain.UserDomainSupport;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

	@Mock
	private ReviewRepository reviewRepository;
	@Mock
	private AlcoholDomainSupport alcoholDomainSupport;
	@Mock
	private UserDomainSupport userDomainSupport;
	@Mock
	private ReviewImageSupport reviewImageSupport;
	@Mock
	private ReviewTastingTagSupport reviewTastingTagSupport;

	@InjectMocks
	private ReviewService reviewService;
	private final ReviewCreateRequest reviewCreateRequest = ReviewObjectFixture.getReviewCreateRequest();
	private final Review review = ReviewObjectFixture.getReviewFixture();

	private final PageableRequest request = ReviewObjectFixture.getEmptyPageableRequest();
	private final PageResponse<ReviewListResponse> response = ReviewObjectFixture.getReviewListResponse();
	private final ReviewModifyRequest reviewModifyRequest = ReviewObjectFixture.getReviewModifyRequest();

	private final Long reviewId = 1L;
	private final Long userId = 1L;

	@Nested
	@DisplayName("리뷰 등록 서비스 레이어 테스트")
	class ReviewCreateServiceTest {

		@Test
		@DisplayName("리뷰를 등록할 수 있다.")
		void review_create_success() {
			//given

			//when
			when(reviewRepository.save(any(Review.class)))
				.thenReturn(review);

			ReviewCreateResponse response = reviewService.createReview(reviewCreateRequest, 1L);

			verify(reviewImageSupport, times(1)).saveImages(anyList(), any());
			verify(reviewTastingTagSupport, times(1)).saveReviewTastingTag(anyList(), any());

			assertEquals(response.getId(), review.getId());
		}
	}

	@Nested
	@DisplayName("리뷰 조회 서비스 레이어 테스트")
	class ReviewReadServiceTest {

		@Test
		@DisplayName("리뷰를 조회할 수 있다.")
		void test_review_read() {
			//given

			//when
			when(reviewRepository.getReviews(
				anyLong(), any(PageableRequest.class), anyLong()))
				.thenReturn(response);
			PageResponse<ReviewListResponse> actualResponse = reviewService.getReviews(1L, request, 1L);

			//then
			assertThat(response.content()).isEqualTo(actualResponse.content());
			assertThat(response.cursorPageable()).isEqualTo(actualResponse.cursorPageable());
			verify(reviewRepository).getReviews(anyLong(), any(PageableRequest.class), anyLong());
		}

		@Test
		@DisplayName("내가 작성한 리뷰를 조회할 수 있다.")
		void test_my_review_read() {
			//given
			Long userId = 1L;

			//when
			when(reviewRepository.getReviewsByMe(anyLong(), any(PageableRequest.class), anyLong()))
				.thenReturn(response);

			PageResponse<ReviewListResponse> actualResponse = reviewService.getMyReviews(request, 1L, userId);

			//then
			assertThat(response.content()).isEqualTo(actualResponse.content());
			assertThat(response.cursorPageable()).isEqualTo(actualResponse.cursorPageable());
			verify(reviewRepository).getReviewsByMe(anyLong(), any(PageableRequest.class), anyLong());
		}

		@DisplayName("리뷰 상세조회를 할 수 있다.")
		@Test
		void test_review_detail_reveiw() {

			// when
			when(reviewRepository.findById(anyLong()))
				.thenReturn(Optional.of(ReviewObjectFixture.getReviewFixture()));

			when(alcoholDomainSupport.findAlcoholInfoById(anyLong(), anyLong()))
				.thenReturn(Optional.of(ReviewObjectFixture.getAlcoholInfo()));

			when(reviewRepository.getReview(anyLong(), anyLong()))
				.thenReturn(ReviewObjectFixture.getReviewResponse());

			ReviewDetailResponse detailReview = reviewService.getDetailReview(1L, 1L);

			// then
			assertEquals(detailReview.reviewResponse().reviewId(), review.getId());
		}

		@DisplayName("리뷰가 존재하지 않으면 조회할 수 없다.")
		@Test
		void test_review_read_fail_when_review_not_exist() {

			// when
			when(reviewRepository.findById(anyLong())).thenThrow(new ReviewException(REVIEW_NOT_FOUND));
			// then
			assertThrows(ReviewException.class, () -> reviewService.getDetailReview(1L, 1L));
		}

		@DisplayName("리뷰 이미지가 존재하지 않아도 리뷰 상세 조회가 가능하다")
		@Test
		void test_review_read_success_when_image_is_null() {
			// given

			when(reviewRepository.findById(anyLong())).thenReturn(Optional.of(review));

			// when
			ReviewDetailResponse reviewDetail = reviewService.getDetailReview(1L, 1L);

			// then
			assertNotNull(response);

			assertTrue(reviewDetail.reviewImageList().isEmpty());
		}
	}

	@Nested
	@DisplayName("리뷰 수정 서비스 레이어 테스트")
	class ReviewModifyServiceTest {

		@Test
		@DisplayName("리뷰를 수정할 수 있다.")
		void modify_review_success_when_without_tasting_tag() {

			//when
			when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(review));

			reviewService.modifyReview(reviewModifyRequest, 1L, 1L);

			verify(reviewImageSupport, times(1)).updateImages(anyList(), any());
			verify(reviewTastingTagSupport, times(1)).updateReviewTastingTags(anyList(), any());
			verify(reviewTastingTagSupport, never()).saveReviewTastingTag(any(), any());
		}

		@Test
		@DisplayName("존재하지 않는 리뷰는 수정할 수 없다.")
		void modify_review_fail_when_review_id_is_not_invalid() {
			when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenThrow(
				new ReviewException(ReviewExceptionCode.REVIEW_NOT_FOUND)
			);

			assertThrows(ReviewException.class, () -> reviewService.modifyReview(reviewModifyRequest, 100L, 1L));
		}
	}

	@Nested
	@DisplayName("리뷰 삭제 서비스 레이어 테스트")
	class ReviewDeleteServiceTest {

		@Test
		@DisplayName("리뷰를 삭제할 수 있다.")
		void delete_review_success() {

			//given
			when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(review));

			//when
			ReviewResultResponse reviewResultResponse = reviewService.deleteReview(reviewId, userId);

			//then
			assertEquals(DELETE_SUCCESS, reviewResultResponse.codeMessage());
		}

		@Test
		@DisplayName("존재하지 않는 리뷰는 삭제할 수 없다.")
		void delete_fail_when_review_is_not_exist() {

			//when
			when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenThrow(new ReviewException(REVIEW_NOT_FOUND));

			//then
			assertThrows(ReviewException.class, () -> reviewService.deleteReview(reviewId, userId));
		}
	}
}
