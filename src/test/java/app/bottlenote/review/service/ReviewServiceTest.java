package app.bottlenote.review.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.UserCommandRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static app.bottlenote.review.dto.response.ReviewResultMessage.DELETE_SUCCESS;
import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

	@Mock
	private ReviewRepository reviewRepository;
	@Mock
	private AlcoholQueryRepository alcoholQueryRepository;
	@Mock
	private UserCommandRepository userCommandRepository;
	@Mock
	private ReviewImageSupport reviewImageSupport;
	@Mock
	private ReviewTastingTagSupport reviewTastingTagSupport;

	@InjectMocks
	private ReviewService reviewService;
	private final ReviewCreateRequest reviewCreateRequest = ReviewObjectFixture.getReviewCreateRequest();
	private final Alcohol alcohol = ReviewObjectFixture.getAlcoholFixture();
	private final User user = ReviewObjectFixture.getUserFixture();
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
			when(alcoholQueryRepository.findById(anyLong()))
				.thenReturn(Optional.of(alcohol));

			when(userCommandRepository.findById(anyLong()))
				.thenReturn(Optional.of(user));

			when(reviewRepository.save(any(Review.class)))
				.thenReturn(review);

			ReviewCreateResponse response = reviewService.createReview(reviewCreateRequest, 1L);

			verify(reviewImageSupport, times(1)).saveImages(anyList(), any());
			verify(reviewTastingTagSupport, times(1)).saveReviewTastingTag(anyList(), any());

			assertEquals(response.getId(), review.getId());
		}

		@Test
		@DisplayName("Alcohol이 존재하지 않을 때 AlcoholException이 발생해야 한다.")
		void review_create_fail_when_alcohol_is_null() {
			// given
			when(alcoholQueryRepository.findById(anyLong())).thenReturn(Optional.empty());

			// when, then
			assertThrows(AlcoholException.class, () -> reviewService.createReview(reviewCreateRequest, 1L));
		}

		@Test
		@DisplayName("유저가 존재하지 않을 때 UserNotFoundException 발생해야 한다.")
		void review_create_fail_when_user_is_null() {
			// given
			when(alcoholQueryRepository.findById(anyLong())).thenReturn(Optional.of(alcohol));
			when(userCommandRepository.findById(anyLong())).thenReturn(Optional.empty());

			// when, then
			assertThrows(UserException.class, () -> reviewService.createReview(reviewCreateRequest, 1L));
		}
	}

	@Nested
	@DisplayName("리뷰 조회 서비스 레이어 테스트")
	class ReviewReadServiceTest {

		@Test
		@DisplayName("리뷰를 조회할 수 있다.")
		void testReviewRead() {
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
		void testMyReviewRead() {
			//given
			Long userId = 1L;

			//when
			when(reviewRepository.getReviewsByMe(anyLong(), any(PageableRequest.class), anyLong()))
				.thenReturn(response);

			PageResponse<ReviewListResponse> actualResponse = reviewService.getMyReviews(1L, request, userId);

			//then
			assertThat(response.content()).isEqualTo(actualResponse.content());
			assertThat(response.cursorPageable()).isEqualTo(actualResponse.cursorPageable());
			verify(reviewRepository).getReviewsByMe(anyLong(), any(PageableRequest.class), anyLong());
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
			Assertions.assertEquals(DELETE_SUCCESS, reviewResultResponse.codeMessage());
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
