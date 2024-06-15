package app.bottlenote.review.service;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ReviewReplyService {

	private final ReviewRepository reviewRepository;

	public ReviewReplyService(ReviewRepository reviewRepository) {
		this.reviewRepository = reviewRepository;
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
