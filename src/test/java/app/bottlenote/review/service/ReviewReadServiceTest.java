package app.bottlenote.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("리뷰 조회 서비스 레이어 테스트")
@ExtendWith(MockitoExtension.class)
class ReviewReadServiceTest {

	@Mock
	private ReviewRepository reviewRepository;

	@InjectMocks
	private ReviewService reviewService;

	private final PageableRequest request = ReviewObjectFixture.getEmptyPageableRequest();
	private final PageResponse<ReviewResponse> response = ReviewObjectFixture.getReviewListResponse();

	@Test
	@DisplayName("리뷰를 조회할 수 있다.")
	void testReviewRead() {
		//given

		//when
		when(reviewRepository.getReviews(
			anyLong(), any(PageableRequest.class), anyLong()))
			.thenReturn(response);
		PageResponse<ReviewResponse> actualResponse = reviewService.getReviews(1L, request, 1L);

		//then
		assertThat(response.content()).isEqualTo(actualResponse.content());
		assertThat(response.cursorPageable()).isEqualTo(actualResponse.cursorPageable());
		verify(reviewRepository).getReviews(anyLong(), any(PageableRequest.class), anyLong());
	}
}
