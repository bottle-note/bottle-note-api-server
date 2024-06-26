package app.bottlenote.review.service;

import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.review.dto.response.ReviewReplyResultResponse;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.user.service.domain.UserDomainSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

import static app.bottlenote.review.dto.response.ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY;

@Service
@Slf4j
public class ReviewReplyService {

	private final ReviewRepository reviewRepository;
	private final ProfanityClient profanityClient;
	private final UserDomainSupport userDomainSupport;

	public ReviewReplyService(
		ReviewRepository reviewRepository,
		ProfanityClient profanityClient,
		UserDomainSupport userDomainSupport
	) {
		this.reviewRepository = reviewRepository;
		this.profanityClient = profanityClient;
		this.userDomainSupport = userDomainSupport;
	}

	/**
	 * 댓글을 등록합니다.
	 *
	 * @param reviewId the review id
	 * @param userId   the user id
	 * @param request  the request
	 * @return the review reply result response
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Transactional
	public ReviewReplyResultResponse registerReviewReply(
		final Long reviewId,
		final Long userId,
		final ReviewReplyRegisterRequest request
	) {
		Objects.requireNonNull(request.content(), "댓글 내용은 필수 입력값입니다.");
		Objects.requireNonNull(userId, "유저 식별자는 필수 입력값입니다.");
		Objects.requireNonNull(reviewId, "리뷰 식별자는 필수 입력값입니다.");

		long start = System.nanoTime();
		userDomainSupport.isValidUserId(userId);
		log.info("유저 정보 확인 시간 : {}", (System.nanoTime() - start) / 1_000_000 + "ms");

		long start2 = System.nanoTime();
		final String content = profanityClient.getFilteredText(request.content());
		log.info("욕설 필터링 시간 : {}", (System.nanoTime() - start2) / 1_000_000 + "ms");

		final Review review = reviewRepository.findById(reviewId)
			.orElseThrow(() -> new ReviewException(ReviewExceptionCode.REVIEW_NOT_FOUND));

		Optional<ReviewReply> parentReply = reviewRepository.isEligibleParentReply(reviewId, request.parentReplyId());
		log.info("상위(최상위) 댓글 확인");

		ReviewReply reply = ReviewReply.builder()
			.review(review)
			.userId(userId)
			.parentReviewReply(parentReply.orElse(null))
			.rootReviewReply(parentReply.map(ReviewReply::getRootReviewReply).orElse(null))
			.content(content)
			.build();

		review.addReply(reply);

		log.info("최종 처리 시간 : {}", (System.nanoTime() - start) / 1_000_000 + "ms");

		reviewRepository.save(review);

		return ReviewReplyResultResponse.response(
			SUCCESS_REGISTER_REPLY,
			review.getId()
		);
	}
}
