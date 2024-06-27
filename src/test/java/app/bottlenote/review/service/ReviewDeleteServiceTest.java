package app.bottlenote.review.service;

import static app.bottlenote.review.domain.constant.ReviewResponseMessage.DELETE_SUCCESS;
import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.when;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.constant.ReviewResponseMessage;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.repository.ReviewRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("리뷰 삭제 서비스 레이어 테스트")
@ExtendWith(MockitoExtension.class)
class ReviewDeleteServiceTest {

	@Mock
	private ReviewRepository reviewRepository;

	@InjectMocks
	private ReviewService reviewService;
	private final Long reviewId = 1L;
	private final Long userId = 1L;
	private final Review review = ReviewObjectFixture.getReviewFixture();


	@Test
	@DisplayName("리뷰를 삭제할 수 있다.")
	void delete_review_success() {

		//given
		when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(review));

		//when
		ReviewResponseMessage reviewResponseMessage = reviewService.deleteReview(reviewId, userId);

		//then
		Assertions.assertEquals(DELETE_SUCCESS, reviewResponseMessage);
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
