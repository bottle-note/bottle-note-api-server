package app.bottlenote.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewDetail;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

	@InjectMocks
	private ReviewService reviewService;

	private ReviewModifyRequest reviewModifyRequest;
	private Review review;
	private User user;
	private Alcohol alcohol;
	private ReviewDetail reviewDetail;

	@BeforeEach
	void setUp() {
		alcohol = Alcohol.builder()
			.id(1L)
			.build();
		user = User.builder()
			.id(1L)
			.build();
		review = Review.builder()
			.id(1L)
			.alcohol(alcohol)
			.user(user)
			.content("아주 맛있어요")
			.build();

		reviewModifyRequest = new ReviewModifyRequest(
			"그저 그래요",
			null,
			null,
			null,
			null,
			null);

		reviewDetail = ReviewDetail.builder()
			.reviewId(review.getId())
			.reviewContent(reviewModifyRequest.content())
			.build();
	}


	@Test
	@DisplayName("리뷰를 수정할 수 있다.")
	void modify_review_success() {

		when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(review));

		when(reviewRepository.getReview(anyLong(), anyLong())).thenReturn(reviewDetail);

		reviewService.modifyReviews(reviewModifyRequest, 1L, 1L);

		assertEquals(reviewDetail.getReviewContent(), reviewModifyRequest.content());
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
