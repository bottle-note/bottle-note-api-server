package app.bottlenote.review.fixture;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.review.dto.response.ReviewReplyInfo;
import app.bottlenote.review.dto.response.ReviewReplyResponse;
import app.bottlenote.review.dto.response.SubReviewReplyInfo;
import app.bottlenote.review.dto.response.constant.ReviewReplyResultMessage;
import app.bottlenote.user.domain.User;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReviewReplyObjectFixture {

	/**
	 * 리뷰 댓글 작성 요청 객체를 반환합니다.
	 */
	public static ReviewReplyRegisterRequest getReviewReplyRegisterRequest(String content) {
		return new ReviewReplyRegisterRequest(content, null);
	}

	/**
	 * 리뷰 댓글 작성 요청 객체를 반환합니다.
	 */
	public static ReviewReplyRegisterRequest getReviewReplyRegisterRequest(String content, Long parentReplyId) {
		return new ReviewReplyRegisterRequest(content, parentReplyId);
	}

	/**
	 * 리뷰 댓글 작성 요청 객체를 반환합니다.
	 */
	public static ReviewReplyRegisterRequest getReviewReplyRegisterRequest() {
		return new ReviewReplyRegisterRequest(RandomStringUtils.randomAlphabetic(100), null);
	}

	/**
	 * 리뷰 댓글 응답 객체를 반환합니다.
	 */
	public static ReviewReplyResponse getReviewReplyResponse() {
		return ReviewReplyResponse.of(ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, 1L);
	}

	/**
	 * 리뷰 댓글 엔티티 fixture를 반환합니다.
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
	 * 리뷰 엔티티 fixture를 반환합니다.
	 */
	public static Review getReviewFixture(Long reviewId, Long alcoholId, Long userId) {
		String content = RandomStringUtils.randomAlphabetic(150);
		return Review.builder()
			.id(reviewId)
			.alcoholId(alcoholId)
			.userId(userId)
			.content(content)
			.build();
	}

	/**
	 * 유저 엔티티 fixture를 반환합니다.
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

	public static ReviewReplyInfo getReviewReplyInfo(Long userId, Long index) {
		return ReviewReplyInfo.builder()
			.userId(userId)
			.imageUrl("https://picsum.photos/500")
			.nickName(RandomStringUtils.randomAlphabetic(10))
			.reviewReplyId(index)
			.reviewReplyContent(RandomStringUtils.randomAlphabetic(150))
			.subReplyCount(RandomUtils.nextLong(0, 5))
			.createAt(LocalDateTime.of(2024, 7, 11, 0, 0, 0))
			.build();
	}

	public static SubReviewReplyInfo getSubReviewReplyInfo(
		Long userId,
		Long index,
		Long rootReviewId,
		Long parentReviewReplyId,
		String parentReviewReplyAuthor
	) {
		return SubReviewReplyInfo.builder()
			.userId(userId)
			.imageUrl("https://picsum.photos/500")
			.nickName(RandomStringUtils.randomAlphabetic(10))

			.rootReviewId(rootReviewId)
			.parentReviewReplyId(parentReviewReplyId)
			.parentReviewReplyAuthor(parentReviewReplyAuthor)

			.reviewReplyId(index)
			.reviewReplyContent(RandomStringUtils.randomAlphabetic(150))
			.createAt(LocalDateTime.of(2024, 7, 11, 0, 0, 0))
			.build();
	}

	public static List<ReviewReplyInfo> getReviewReplyInfoList(long index) {
		List<ReviewReplyInfo> list = new ArrayList<>();
		for (long i = 0; i < index; i++) {
			list.add(getReviewReplyInfo(i, i));
		}
		return list;
	}

}
