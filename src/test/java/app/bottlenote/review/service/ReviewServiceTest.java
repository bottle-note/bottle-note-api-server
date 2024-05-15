package app.bottlenote.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewDetail;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.repository.ReviewRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("리뷰 조회 서비스 레이어 테스트")
@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

	@Mock
	private ReviewRepository reviewRepository;

	@InjectMocks
	private ReviewService reviewService;

	private Long userId;
	private PageableRequest request;
	private PageResponse<ReviewResponse> response;

	@BeforeEach
	void setUp() {
		userId = 1L;
		request = PageableRequest.builder().build();
		response = getResponse();
	}

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

	private PageResponse<ReviewResponse> getResponse() {

		ReviewDetail reviewDetail_1 = ReviewDetail.builder()
			.reviewId(1L)
			.reviewContent("맛있습니다")
			.price(BigDecimal.valueOf(100000L))
			.sizeType(SizeType.BOTTLE)
			.likeCount(5L)
			.replyCount(3L)
			.thumbnailImage("thumbnail_image_1")
			.reviewCreatedAt(LocalDateTime.now())
			.userId(1L)
			.userNickname("test_user_1")
			.userProfileImage("user_profile_image_1")
			.ratingPoint(4.0)
			.status(ReviewStatus.PUBLIC)
			.isMyReview(true)
			.isLikedByMe(true)
			.hasCommentedByMe(false)
			.build();

		ReviewDetail reviewDetail_2 = ReviewDetail.builder()
			.reviewId(2L)
			.reviewContent("나름 먹을만 하네요")
			.price(BigDecimal.valueOf(110000L))
			.sizeType(SizeType.BOTTLE)
			.likeCount(3L)
			.replyCount(6L)
			.thumbnailImage("thumbnail_image_2")
			.reviewCreatedAt(LocalDateTime.now().minusDays(1))
			.userId(2L)
			.userNickname("test_user_2")
			.userProfileImage("user_profile_image_2")
			.ratingPoint(4.0)
			.status(ReviewStatus.PUBLIC)
			.isMyReview(true)
			.isLikedByMe(true)
			.hasCommentedByMe(false)
			.build();

		Long totalCount = 2L;
		List<ReviewDetail> reviewDetails = List.of(reviewDetail_1, reviewDetail_2);
		CursorPageable cursorPageable = CursorPageable.builder()
			.currentCursor(0L)
			.cursor(1L)
			.pageSize(2L)
			.hasNext(false)
			.build();

		ReviewResponse response = ReviewResponse.of(totalCount, reviewDetails);
		return PageResponse.of(response, cursorPageable);
	}

}
