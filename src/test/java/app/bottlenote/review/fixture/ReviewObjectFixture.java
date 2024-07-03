package app.bottlenote.review.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.LocationInfo;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewReplyResponse;
import app.bottlenote.review.dto.response.ReviewReplyResultMessage;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.user.domain.User;
import org.apache.commons.lang3.RandomStringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

public class ReviewObjectFixture {

	private static final String content = "맛있어요";

	// dto

	/**
	 * 리뷰 생성 요청 객체를 반환합니다.
	 *
	 * @return the review create request
	 */
	public static ReviewCreateRequest getReviewCreateRequest() {
		return new ReviewCreateRequest(
			1L,
			ReviewDisplayStatus.PUBLIC,
			content,
			SizeType.GLASS,
			new BigDecimal("30000.0"),
			new LocationInfo("11111", "서울시 강남구 청담동", "xx빌딩"),
			List.of(
				new ReviewImageInfo(1L, "url1"),
				new ReviewImageInfo(2L, "url2"),
				new ReviewImageInfo(3L, "url3")
			),
			List.of("테이스팅태그 1", "테이스팅태그 2", "테이스팅태그 3")
		);
	}

	/**
	 * 리뷰 생성 응답 객체를 반환합니다.
	 *
	 * @return the review create of
	 */
	public static ReviewCreateResponse getReviewCreateResponse() {
		return ReviewCreateResponse.builder()
			.id(1L)
			.content(content)
			.callback(String.valueOf(1L))
			.build();
	}

	/**
	 * 페이징 요청 객체를 반환합니다.
	 *
	 * @return the empty pageable request
	 */
	public static PageableRequest getEmptyPageableRequest() {
		return PageableRequest.builder().build();
	}

	/**
	 * 리뷰 리스트 응답 객체를 반환합니다.
	 *
	 * @return the review list of
	 */
	public static PageResponse<ReviewListResponse> getReviewListResponse() {

		ReviewResponse reviewResponse_1 = ReviewResponse.builder()
			.reviewId(1L)
			.reviewContent(content)
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
			.status(ReviewDisplayStatus.PUBLIC)
			.isMyReview(true)
			.isLikedByMe(true)
			.hasReplyByMe(false)
			.build();

		ReviewResponse reviewResponse_2 = ReviewResponse.builder()
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
			.status(ReviewDisplayStatus.PUBLIC)
			.isMyReview(true)
			.isLikedByMe(true)
			.hasReplyByMe(false)
			.build();

		Long totalCount = 2L;
		List<ReviewResponse> reviewResponses = List.of(reviewResponse_1, reviewResponse_2);
		CursorPageable cursorPageable = CursorPageable.builder()
			.currentCursor(0L)
			.cursor(1L)
			.pageSize(2L)
			.hasNext(false)
			.build();

		ReviewListResponse response = ReviewListResponse.of(totalCount, reviewResponses);
		return PageResponse.of(response, cursorPageable);
	}

	/**
	 * 리뷰 수정 요청 객체를 반환합니다.
	 *
	 * @return the review modify request
	 */
	public static ReviewModifyRequest getReviewModifyRequest() {
		return new ReviewModifyRequest(
			"그저 그래요",
			ReviewDisplayStatus.PUBLIC,
			BigDecimal.valueOf(10000L),
			List.of(new ReviewImageInfo(1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")),
			SizeType.GLASS,
			List.of(),
			new LocationInfo("11111", "서울시 강남구 청담동", "xx빌딩"));
	}

	/**
	 * 리뷰 댓글 작성 요청 객체를 반환합니다.
	 *
	 * @return the review reply register request
	 */
	public static ReviewReplyRegisterRequest getReviewReplyRegisterRequest(String content) {
		return new ReviewReplyRegisterRequest(content, null);
	}

	/**
	 * 리뷰 댓글 작성 요청 객체를 반환합니다.
	 *
	 * @param content       the content
	 * @param parentReplyId the parent reply id
	 * @return the review reply register request
	 */
	public static ReviewReplyRegisterRequest getReviewReplyRegisterRequest(String content, Long parentReplyId) {
		return new ReviewReplyRegisterRequest(content, parentReplyId);
	}

	/**
	 * 리뷰 댓글 작성 요청 객체를 반환합니다.
	 *
	 * @param parentReplyId the parent reply id
	 * @return the review reply register request
	 */
	public static ReviewReplyRegisterRequest getReviewReplyRegisterRequest() {
		return new ReviewReplyRegisterRequest(RandomStringUtils.randomAlphabetic(100), null);
	}

	/**
	 * 리뷰 댓글 응답 객체를 반환합니다.
	 *
	 * @return the review reply response
	 */
	public static ReviewReplyResponse getReviewReplyResponse() {
		return ReviewReplyResponse.of(ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, 1L);
	}


	// domain

	/**
	 * 리뷰 엔티티 fixture를 반환합니다.
	 *
	 * @return the review fixture
	 */
	public static Review getReviewFixture() {
		Long alcoholId = 1L;
		Long userId = 1L;

		return Review.builder()
			.id(1L)
			.alcoholId(alcoholId)
			.userId(userId)
			.content(content)
			.build();
	}

	/**
	 * 리뷰 엔티티 fixture를 반환합니다.
	 *
	 * @param reviewId  the review id
	 * @param alcoholId the alcohol id
	 * @param userId    the user id
	 * @return the review fixture
	 */
	public static Review getReviewFixture(Long reviewId, Long alcoholId, Long userId) {
		return Review.builder()
			.id(reviewId)
			.alcoholId(alcoholId)
			.userId(userId)
			.content(content)
			.build();
	}

	/**
	 * 리뷰 댓글 엔티티 fixture를 반환합니다.
	 *
	 * @param id     the id
	 * @param review the review
	 * @return the review reply fixture
	 */
	public static ReviewReply getReviewReplyFixture(Long id, Review review) {
		Long userId = 1L;
		String content = RandomStringUtils.randomAlphabetic(50);
		return ReviewReply.builder()
			.id(id)
			.review(review)
			.userId(userId)
			.content(content)
			.build();
	}

	/**
	 * 주류 엔티티 fixture를 반환합니다.
	 *
	 * @return the alcohol fixture
	 */
	public static Alcohol getAlcoholFixture() {
		final String randomized = RandomStringUtils.randomAlphabetic(10);
		return Alcohol.builder()
			.id(1L)
			.korName(randomized)
			.engName(randomized)
			.build();
	}

	/**
	 * 유저 엔티티 fixture를 반환합니다.
	 *
	 * @return the user fixture
	 */
	public static User getUserFixture() {
		return User.builder()
			.id(1L)
			.build();
	}

	/**
	 * 유저 엔티티 fixture를 반환합니다.
	 *
	 * @param id the id
	 * @return the user fixture
	 */
	public static User getUserFixture(Long id) {
		final String randomized = RandomStringUtils.randomAlphabetic(10);
		final Integer age = new Random().nextInt(40);

		return User.builder()
			.id(id)
			.email(randomized + "@gmail.com")
			.nickName(randomized)
			.age(age)
			.build();
	}

}
