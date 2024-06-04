package app.bottlenote.review.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.repository.AlcoholQueryRepository;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewImage;
import app.bottlenote.review.domain.ReviewTastingTag;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.LocationInfo;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.repository.ReviewImageRepository;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.review.repository.ReviewTastingTagRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.UserCommandRepository;
import java.math.BigDecimal;
import java.util.Collections;
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
	private ReviewImageRepository reviewImageRepository;
	@Mock
	private ReviewTastingTagRepository reviewTastingTagRepository;


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
			LocationInfo.builder()
				.zipCode("34222")
				.address("서울시 영등포구")
				.detailAddress("aaa 바")
				.build()
			,
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

		when(reviewImageRepository.saveAll(anyList()))
			.thenReturn(reviewImage);

		when(reviewTastingTagRepository.saveAll(anySet()))
			.thenReturn(reviewTastingTag);

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

	@Test
	@DisplayName("리뷰이미지가 5장 이상일 때 ReviewException이 발생해야 한다.")
	void review_create_fail_when_review_image_is_more_than_5() {

		// given

		ReviewCreateRequest request = new ReviewCreateRequest(
			1L,
			ReviewStatus.PUBLIC,
			"맛있어요",
			SizeType.GLASS,
			new BigDecimal("30000.0"),
			LocationInfo.builder()
				.zipCode("34222")
				.address("서울시 영등포구")
				.detailAddress("aaa 바")
				.build()
			,
			List.of(
				new ReviewImageInfo(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1"),
				new ReviewImageInfo(2L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/2"),
				new ReviewImageInfo(3L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/3"),
				new ReviewImageInfo(4L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/3"),
				new ReviewImageInfo(5L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/3"),
				new ReviewImageInfo(6L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/3")
			),
			List.of("테이스팅태그 1", "테이스팅태그 2", "테이스팅태그 3")
		);

		// when,
		when(alcoholQueryRepository.findById(anyLong())).thenReturn(Optional.of(alcohol));
		when(userCommandRepository.findById(anyLong())).thenReturn(Optional.of(user));

		// then
		assertThrows(ReviewException.class, () -> reviewService.createReviews(request, 1L));
	}

	@Test
	@DisplayName("테이스팅태그가 12자 이상일 때 ReviewException이 발생해야 한다.")
	void review_create_fail_when_tasting_tag_is_more_than_12() {

		// given

		ReviewCreateRequest request = new ReviewCreateRequest(
			1L,
			ReviewStatus.PUBLIC,
			"맛있어요",
			SizeType.GLASS,
			new BigDecimal("30000.0"),
			LocationInfo.builder()
				.zipCode("34222")
				.address("서울시 영등포구")
				.detailAddress("aaa 바")
				.build()
			,
			List.of(
				new ReviewImageInfo(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1"),
				new ReviewImageInfo(2L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/2"),
				new ReviewImageInfo(3L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/3"),
				new ReviewImageInfo(4L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/3"),
				new ReviewImageInfo(5L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/3")
			),
			List.of("테이스팅태그테이스팅태그태그", "테이스팅태그 2", "테이스팅태그 3")
		);

		// when,
		when(alcoholQueryRepository.findById(anyLong())).thenReturn(Optional.of(alcohol));
		when(userCommandRepository.findById(anyLong())).thenReturn(Optional.of(user));

		// then
		assertThrows(ReviewException.class, () -> reviewService.createReviews(request, 1L));
	}

	@Test
	@DisplayName("reviewImageInfo가 null이면 리뷰 이미지는 save되지 않는다.")
	void review_image_does_not_save_when_image_info_is_null() {

		// given

		ReviewCreateRequest request = new ReviewCreateRequest(
			1L,
			ReviewStatus.PUBLIC,
			"맛있어요",
			SizeType.GLASS,
			new BigDecimal("30000.0"),
			LocationInfo.builder()
				.zipCode("34222")
				.address("서울시 영등포구")
				.detailAddress("aaa 바")
				.build()
			,
			Collections.emptyList()
			,
			List.of("테이스팅태그1", "테이스팅태그 2", "테이스팅태그 3")
		);

		ReviewImage reviewImage1 = ReviewImage.builder().build();

		//when
		when(alcoholQueryRepository.findById(anyLong()))
			.thenReturn(Optional.of(alcohol));

		when(userCommandRepository.findById(anyLong()))
			.thenReturn(Optional.of(user));

		when(reviewRepository.save(any(Review.class)))
			.thenReturn(review);

		// then
		reviewService.createReviews(request, 1L);

		//imageInfo가 빈 리스트가 들어와서 saveAll 메서드가 호출되지 않음
		verify(reviewImageRepository, times(0)).saveAll(List.of(reviewImage1));
	}
}
