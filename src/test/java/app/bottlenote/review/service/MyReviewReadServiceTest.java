package app.bottlenote.review.service;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.repository.JpaReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("내가 작성한 리뷰 조회 서비스 레이어 테스트")
@ExtendWith(MockitoExtension.class)
class MyReviewReadServiceTest {

	@Mock
	private JpaReviewRepository jpaReviewRepository;

	@InjectMocks
	private ReviewService reviewService;

	private final PageableRequest request = ReviewObjectFixture.getEmptyPageableRequest();
	private final PageResponse<ReviewListResponse> response = ReviewObjectFixture.getReviewListResponse();


	@Test
	@DisplayName("내가 작성한 리뷰를 조회할 수 있다.")
	void testReviewRead() {
		//given
		Long userId = 1L;

		//when
		when(jpaReviewRepository.getReviewsByMe(anyLong(), any(PageableRequest.class), anyLong()))
			.thenReturn(response);

		PageResponse<ReviewListResponse> actualResponse = reviewService.getMyReviews(1L, request, userId);

		//then
		assertThat(response.content()).isEqualTo(actualResponse.content());
		assertThat(response.cursorPageable()).isEqualTo(actualResponse.cursorPageable());
		verify(jpaReviewRepository).getReviewsByMe(anyLong(), any(PageableRequest.class), anyLong());
	}

}
