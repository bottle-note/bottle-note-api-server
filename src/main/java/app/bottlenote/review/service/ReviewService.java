package app.bottlenote.review.service;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.repository.ReviewRepository;
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

	public PageResponse<ReviewResponse> getReviews(Long alcoholId,
		PageableRequest pageableRequest,
		Long userId) {

		PageResponse<ReviewResponse> reviews = reviewRepository.getReviews(alcoholId,
			pageableRequest, userId);

		log.info("review size is : {}", reviews.content());

		return reviews;
	}
}
