package app.bottlenote.review.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewDomainService;
import app.bottlenote.review.domain.ReviewModifyVO;
import app.bottlenote.review.domain.ReviewTastingTag;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.LocationInfo;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.review.repository.ReviewTastingTagRepository;
import app.bottlenote.user.domain.User;
import java.math.BigDecimal;
import java.util.HashSet;
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

@DisplayName("리뷰 수정 서비스 레이어 테스트")
@ExtendWith(MockitoExtension.class)
class ReviewModifyServiceTest {

	@Mock
	private ReviewRepository reviewRepository;
	@Mock
	private ReviewTastingTagRepository reviewTastingTagRepository;
	@Mock
	private ReviewDomainService reviewDomainService;

	@InjectMocks
	private ReviewService reviewService;

	private ReviewModifyRequest reviewModifyRequest;
	private Review review;
	private User user;
	private Alcohol alcohol;
	private ReviewModifyVO reviewModifyVO;

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
			.reviewTastingTags(new HashSet<>())
			.build();
	}


	@Test
	@DisplayName("리뷰를 수정할 수 있다. - 테이스팅 태그는 수정하지 않는 경우")
	void modify_review_success_when_without_tasting_tag() {
		//given
		reviewModifyRequest = new ReviewModifyRequest(
			"그저 그래요",
			ReviewStatus.PUBLIC,
			BigDecimal.valueOf(10000L),
			SizeType.GLASS,
			List.of(),
			new LocationInfo("11111", "서울시 강남구 청담동", "xx빌딩"));

		reviewModifyVO = ReviewModifyVO.builder()
			.content(reviewModifyRequest.content())
			.price(reviewModifyRequest.price())
			.reviewStatus(reviewModifyRequest.status())
			.zipCode(reviewModifyRequest.content())
			.address(reviewModifyRequest.locationInfo().address())
			.detailAddress(reviewModifyRequest.locationInfo().detailAddress())
			.sizeType(reviewModifyRequest.sizeType())
			.build();

		when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(review));

		when(reviewDomainService.createValidatedReview(reviewModifyRequest)).thenReturn(reviewModifyVO);

		reviewService.modifyReviews(reviewModifyRequest, 1L, 1L);

		verify(reviewDomainService, times(1)).createValidatedReview(any());

		verify(reviewDomainService, never()).createValidatedReviewTastingTag(any(), any());
	}

	@Test
	@DisplayName("리뷰를 수정할 수 있다. - 테이스팅 태그도 함께 수정하는 경우")
	void modify_review_success_with_tasting_tag() {
		//given
		reviewModifyRequest = new ReviewModifyRequest(
			"그저 그래요",
			ReviewStatus.PUBLIC,
			BigDecimal.valueOf(10000L),
			SizeType.GLASS,
			List.of("달콤"),
			new LocationInfo("11111", "서울시 강남구 청담동", "xx빌딩"));

		reviewModifyVO = ReviewModifyVO.builder()
			.content(reviewModifyRequest.content())
			.price(reviewModifyRequest.price())
			.reviewStatus(reviewModifyRequest.status())
			.zipCode(reviewModifyRequest.content())
			.address(reviewModifyRequest.locationInfo().address())
			.detailAddress(reviewModifyRequest.locationInfo().detailAddress())
			.sizeType(reviewModifyRequest.sizeType())
			.build();

		Set<ReviewTastingTag> tastingTags = reviewModifyRequest.tastingTagList().stream()
			.map(tag -> ReviewTastingTag.builder()
				.review(review)
				.tastingTag(tag)
				.build())
			.collect(Collectors.toSet());

		when(reviewRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(review));

		when(reviewDomainService.createValidatedReview(reviewModifyRequest)).thenReturn(reviewModifyVO);

		when(reviewDomainService.createValidatedReviewTastingTag(reviewModifyRequest.tastingTagList(), review))
			.thenReturn(tastingTags);

		reviewService.modifyReviews(reviewModifyRequest, 1L, 1L);

		verify(reviewDomainService, times(1)).createValidatedReviewTastingTag(any(), any());
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
