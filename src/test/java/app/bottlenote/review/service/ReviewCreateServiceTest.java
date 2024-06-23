package app.bottlenote.review.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewImage;
import app.bottlenote.review.domain.ReviewTastingTag;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.LocationInfo;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.UserCommandRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
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
	private ReviewCreateRequest reviewCreateRequest;
	private Alcohol alcohol;
	private User user;
	private Review review;
	private List<ReviewImage> reviewImage;
	private Set<ReviewTastingTag> reviewTastingTag;

	@BeforeEach
	void setUp() {

		alcohol = Alcohol.builder()
			.id(1L)
			.build();
		user = User.builder()
			.id(1L)
			.build();

		reviewCreateRequest = new ReviewCreateRequest(
			1L,
			ReviewStatus.PUBLIC,
			"맛있어요",
			SizeType.GLASS,
			new BigDecimal("30000.0"),
			new LocationInfo("11111", "서울시 강남구 청담동", "xx빌딩"),
			List.of(
				new ReviewImageInfo(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1"),
				new ReviewImageInfo(2L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/2"),
				new ReviewImageInfo(3L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/3")
			),
			List.of("테이스팅태그 1", "테이스팅태그 2", "테이스팅태그 3")
		);

		review = Review.builder()
			.id(1L)
			.alcohol(alcohol)
			.user(user)
			.content(reviewCreateRequest.content())
			.sizeType(reviewCreateRequest.sizeType())
			.price(reviewCreateRequest.price())
			.zipCode(reviewCreateRequest.locationInfo().zipCode())
			.address(reviewCreateRequest.locationInfo().address())
			.detailAddress(reviewCreateRequest.locationInfo().detailAddress())
			.status(reviewCreateRequest.status())
			.build();

		reviewTastingTag = reviewCreateRequest.tastingTagList().stream()
			.map(tastingTag -> ReviewTastingTag.builder()
				.review(review)
				.tastingTag(tastingTag)
				.build())
			.collect(Collectors.toSet());

		reviewImage = reviewCreateRequest.imageUrlList().stream()
			.map(image -> ReviewImage.builder()
				.review(review)
				.order(image.order())
				.imageUrl(image.viewUrl())
				.build())
			.toList();
	}

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

		ReviewCreateResponse response = reviewService.createReviews(reviewCreateRequest, 1L);

		assertEquals(response.getId(), review.getId());
	}

	@Test
	@DisplayName("Alcohol이 존재하지 않을 때 AlcoholException이 발생해야 한다.")
	void review_create_fail_when_alcohol_is_null() {
		// given
		when(alcoholQueryRepository.findById(anyLong())).thenReturn(Optional.empty());

		// when, then
		assertThrows(AlcoholException.class, () -> reviewService.createReviews(reviewCreateRequest, 1L));
	}

	@Test
	@DisplayName("유저가 존재하지 않을 때 UserNotFoundException 발생해야 한다.")
	void review_create_fail_when_user_is_null() {
		// given
		when(alcoholQueryRepository.findById(anyLong())).thenReturn(Optional.of(alcohol));
		when(userCommandRepository.findById(anyLong())).thenReturn(Optional.empty());

		// when, then
		assertThrows(UserException.class, () -> reviewService.createReviews(reviewCreateRequest, 1L));
	}
}
