package app.bottlenote.review.service;

import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.review.dto.response.ReviewReplyInfo;
import app.bottlenote.review.dto.response.ReviewReplyResponse;
import app.bottlenote.review.dto.response.SubReviewReplyInfo;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.user.service.domain.UserDomainSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static app.bottlenote.review.dto.response.constant.ReviewReplyResultMessage.SUCCESS_DELETE_REPLY;
import static app.bottlenote.review.dto.response.constant.ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY;

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
	 * @return the review reply result of
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Transactional
	public ReviewReplyResponse registerReviewReply(
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

		return ReviewReplyResponse.of(
			SUCCESS_REGISTER_REPLY,
			review.getId()
		);
	}

	/**
	 * 최상위 리뷰 목록을 조회합니다
	 * 이때 대댓글 목록은 제외됩니다.
	 */
	@Transactional(readOnly = true)
	public List<ReviewReplyInfo> getReviewRootReplays(
		Long reviewId,
		Long cursor,
		Long pageSize
	) {
		return reviewRepository.getReviewRootReplies(
			reviewId,
			cursor,
			pageSize
		);
	}

	/**
	 * 대댓글 목록을 조회합니다.
	 *
	 * @param reviewId    조회 대상 리뷰 식별자.
	 * @param rootReplyId 조회 대상 최상위 댓글 식별자.
	 * @param cursor      조회 시작 위치.
	 * @param pageSize    조회 개수.
	 * @return the sub review replies
	 */
	@Transactional(readOnly = true)
	public List<SubReviewReplyInfo> getSubReviewReplies(
		Long reviewId,
		Long rootReplyId,
		Long cursor,
		Long pageSize
	) {
		return reviewRepository.getSubReviewReplies(
			reviewId,
			rootReplyId,
			cursor,
			pageSize
		);
	}

	/**
	 * 리뷰 댓글을 삭제합니다.
	 *
	 * @param reviewId 삭제 대상 댓글의 리뷰 식별자.
	 * @param replyId  삭제 대상 댓글 식별자.
	 * @param userId   삭제 요청자 식별자.
	 * @return 처리 결과 메시지.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Transactional(readOnly = true)
	public ReviewReplyResponse deleteReviewReply(
		Long reviewId,
		Long replyId,
		Long userId
	) {

		reviewRepository.findReplyByReviewIdAndReplyId(reviewId, replyId)
			.ifPresentOrElse(
				reply -> {
					if (!reply.isOwner(userId))
						throw new ReviewException(ReviewExceptionCode.REPLY_NOT_OWNER);

					reply.delete();
				},
				() -> {
					throw new ReviewException(ReviewExceptionCode.NOT_FOUND_REVIEW_REPLY);
				}
			);

		return ReviewReplyResponse.of(SUCCESS_DELETE_REPLY, reviewId);
	}
}
