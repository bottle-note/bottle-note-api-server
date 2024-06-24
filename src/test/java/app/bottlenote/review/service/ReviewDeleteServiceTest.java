package app.bottlenote.review.service;

import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.exception.ReviewException;
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

@DisplayName("리뷰 삭제 서비스 레이어 테스트")
@ExtendWith(MockitoExtension.class)
class ReviewDeleteServiceTest {

	@Mock
	private ReviewRepository reviewRepository;

	@InjectMocks
	private ReviewService reviewService;

	private Long reviewId = 1L;
	private Long userId = 1L;
	private Alcohol alcohol;
	private Review review;
	private User user;

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
	}

	@Test
	@DisplayName("리뷰를 삭제할 수 있다.")
	void delete_review_success() {

		//given
		when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(review));

		//when
		reviewService.deleteReview(reviewId, userId);

		//then
		verify(reviewRepository, description("deleteById 메서드가 정상적으로 실행")).deleteById(anyLong());
	}

	@Test
	@DisplayName("존재하지 않는 리뷰는 삭제할 수 없다.")
	void delete_fail_when_review_is_not_exist() {

		//when
		when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenThrow(new ReviewException(REVIEW_NOT_FOUND));

		//then
		assertThrows(ReviewException.class, () -> reviewService.deleteReview(reviewId, userId));
		
		//deleteById 메서드가 호출되지 않음
		verify(reviewRepository, never()).deleteById(anyLong());
	}
}
