package app.bottlenote.review.service;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.repository.JpaReviewRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static app.bottlenote.review.dto.response.ReviewResultMessage.DELETE_SUCCESS;
import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.when;

@DisplayName("리뷰 삭제 서비스 레이어 테스트")
@ExtendWith(MockitoExtension.class)
class ReviewDeleteServiceTest {

	@Mock
	private JpaReviewRepository jpaReviewRepository;

	@InjectMocks
	private ReviewService reviewService;
	private final Long reviewId = 1L;
	private final Long userId = 1L;
	private final Review review = ReviewObjectFixture.getReviewFixture();


	@Test
	@DisplayName("리뷰를 삭제할 수 있다.")
	void delete_review_success() {

		//given
		when(jpaReviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(review));

		//when
		ReviewResultResponse reviewResultResponse = reviewService.deleteReview(reviewId, userId);

		//then
		Assertions.assertEquals(DELETE_SUCCESS, reviewResultResponse.codeMessage());
	}

	@Test
	@DisplayName("존재하지 않는 리뷰는 삭제할 수 없다.")
	void delete_fail_when_review_is_not_exist() {

		//when
		when(jpaReviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenThrow(new ReviewException(REVIEW_NOT_FOUND));

		//then
		assertThrows(ReviewException.class, () -> reviewService.deleteReview(reviewId, userId));
	}
}
