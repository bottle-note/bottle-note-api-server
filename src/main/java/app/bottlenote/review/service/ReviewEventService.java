package app.bottlenote.review.service;

import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.facade.ReviewEventFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class ReviewEventService implements ReviewEventFacade {

	private final ReviewRepository reviewRepository;

	@Override
	@Transactional(readOnly = true)
	public Long getAlcoholIdByReviewId(Long reviewId) {
		return reviewRepository.findById(reviewId)
				.orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND)).getAlcoholId();
	}
}
