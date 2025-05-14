package app.bottlenote.review.service;

import app.bottlenote.review.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewExploreService {
	private final ReviewRepository reviewRepository;

}
