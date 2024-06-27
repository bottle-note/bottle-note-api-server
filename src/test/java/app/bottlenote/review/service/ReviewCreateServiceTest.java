package app.bottlenote.review.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.UserCommandRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("리뷰 등록 서비스 레이어 테스트")
@ExtendWith(MockitoExtension.class)
class ReviewCreateServiceTest {

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
