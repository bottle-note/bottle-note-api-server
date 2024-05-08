package app.bottlenote.review.service;

import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.repository.ReviewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

	private final ReviewRepository reviewRepository;

	public List<ReviewResponse> getReviews(Long alcoholId) {
		List<ReviewResponse> reviews = reviewRepository.getReviews(alcoholId);

		log.info("review size is : {}", reviews.size());

		return reviews;
	}


}
