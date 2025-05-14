package app.bottlenote.review.service;

import app.bottlenote.core.structure.Pair;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.response.ReviewExploreItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewExploreService {
	private final ReviewRepository reviewRepository;

	public Pair<Long, CursorResponse<ReviewExploreItem>> getStandardExplore(Long userId, List<String> keywords, Long cursor, Integer size) {
		return reviewRepository.getStandardExplore(userId, keywords, cursor, size);
	}
}
