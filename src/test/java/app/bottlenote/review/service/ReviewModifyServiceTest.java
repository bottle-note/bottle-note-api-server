package app.bottlenote.review.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.repository.ReviewRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("리뷰 수정 서비스 레이어 테스트")
@ExtendWith(MockitoExtension.class)
class ReviewModifyServiceTest {

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private ReviewImageSupport reviewImageSupport;

	@Mock
	private ReviewTastingTagSupport reviewTastingTagSupport;

	@InjectMocks
	private ReviewService reviewService;

	private final ReviewModifyRequest reviewModifyRequest = ReviewObjectFixture.getReviewModifyRequest();
	private final Review review = ReviewObjectFixture.getReviewFixture();

	@Test
	@DisplayName("리뷰를 수정할 수 있다.")
	void modify_review_success_when_without_tasting_tag() {

		//when
		when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(review));

		reviewService.modifyReviews(reviewModifyRequest, 1L, 1L);

		verify(reviewImageSupport, times(1)).updateImages(anyList(), any());
		verify(reviewTastingTagSupport, times(1)).updateReviewTastingTag(anyList(), any());
		verify(reviewTastingTagSupport, never()).saveReviewTastingTag(any(), any());
	}

	@Test
	@DisplayName("존재하지 않는 리뷰는 수정할 수 없다.")
	void modify_review_fail_when_review_id_is_not_invalid() {
		when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenThrow(
			new ReviewException(ReviewExceptionCode.REVIEW_NOT_FOUND)
		);

		assertThrows(ReviewException.class, () -> reviewService.modifyReviews(reviewModifyRequest, 100L, 1L));
	}

}
