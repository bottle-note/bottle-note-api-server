package app.bottlenote.review.service;

import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.support.report.repository.UserReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
public class ReviewReplyService {

	private final ReviewRepository reviewRepository;
	private final ProfanityClient profanityClient;
	private final UserReportRepository userReportRepository;

	public ReviewReplyService(
		ReviewRepository reviewRepository,
		ProfanityClient profanityClient,
		UserReportRepository userReportRepository) {
		this.reviewRepository = reviewRepository;
		this.profanityClient = profanityClient;
		this.userReportRepository = userReportRepository;
	}

	public Object registerReviewReply(
		Long reviewId,
		Long userId,
		ReviewReplyRegisterRequest request
	) {
		Objects.requireNonNull(request.content(), "댓글 내용은 필수 입력값입니다.");

		String content = profanityClient.containsProfanity(request.content()).filteredText();

		Review review = reviewRepository.findById(reviewId)
			.orElseThrow(() -> new ReviewException(ReviewExceptionCode.REVIEW_NOT_FOUND));

		ReviewReply reply = ReviewReply.builder()
			.review(review)

			.build();
		return null;
	}

	@Transactional
	public void saveReviewReply() {

		Review review = reviewRepository.findById(1L).get();

		ReviewReply reply = ReviewReply.builder()
			.review(review)
			.userId(2L)
			.content("댓글 내용")
			.build();

		review.addReply(reply);
		reviewRepository.save(review);
	}

}
