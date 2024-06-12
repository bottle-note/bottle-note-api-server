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
import app.bottlenote.user.domain.User;
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

@DisplayName("내가 작성한 리뷰 조회 서비스 레이어 테스트")
@ExtendWith(MockitoExtension.class)
class MyReviewReadServiceTest {

	@Mock
	private ReviewRepository reviewRepository;

	@InjectMocks
	private ReviewService reviewService;

	private PageableRequest request;
	private PageResponse<ReviewResponse> response;
	private User user;

	@BeforeEach
	void setUp() {
		request = PageableRequest.builder().build();
		response = getResponse();
		user = User.builder()
			.id(1L)
			.build();
	}

	@Test
	@DisplayName("내가 작성한 리뷰를 조회할 수 있다.")
	void testReviewRead() {
		//given

		//when
		when(reviewRepository.getReviewsByMe(anyLong(), any(PageableRequest.class), anyLong()))
			.thenReturn(response);

		PageResponse<ReviewResponse> actualResponse = reviewService.getMyReview(1L, request, user.getId());

		//then
		assertThat(response.content()).isEqualTo(actualResponse.content());
		assertThat(response.cursorPageable()).isEqualTo(actualResponse.cursorPageable());
		verify(reviewRepository).getReviewsByMe(anyLong(), any(PageableRequest.class), anyLong());
	}

	private PageResponse<ReviewResponse> getResponse() {

		ReviewDetail reviewDetail_1 = ReviewDetail.builder()
			.reviewId(1L)
			.reviewContent("맛있습니다")
			.price(BigDecimal.valueOf(100000L))
			.sizeType(SizeType.BOTTLE)
			.likeCount(5L)
			.replyCount(3L)
			.reviewImageUrl("https://picsum.photos/600/600")
			.createAt(LocalDateTime.now())
			.userId(1L)
			.nickName("test_user_1")
			.userProfileImage("user_profile_image_1")
			.rating(4.0)
			.status(ReviewStatus.PUBLIC)
			.isMyReview(true)
			.isLikedByMe(true)
			.hasReplyByMe(false)
			.build();

		ReviewDetail reviewDetail_2 = ReviewDetail.builder()
			.reviewId(2L)
			.reviewContent("나름 먹을만 하네요")
			.price(BigDecimal.valueOf(110000L))
			.sizeType(SizeType.BOTTLE)
			.likeCount(3L)
			.replyCount(6L)
			.reviewImageUrl("https://picsum.photos/600/600")
			.createAt(LocalDateTime.now().minusDays(1))
			.userId(2L)
			.nickName("test_user_2")
			.userProfileImage("user_profile_image_2")
			.rating(4.0)
			.status(ReviewStatus.PUBLIC)
			.isMyReview(true)
			.isLikedByMe(true)
			.hasReplyByMe(false)
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
