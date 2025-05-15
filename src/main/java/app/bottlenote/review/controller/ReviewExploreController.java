package app.bottlenote.review.controller;

import app.bottlenote.core.structure.Pair;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.review.dto.response.ReviewExploreItem;
import app.bottlenote.review.service.ReviewExploreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reviews/explore")
public class ReviewExploreController {

	private final ReviewExploreService reviewExploreService;

	@GetMapping("/standard")
	public ResponseEntity<?> getStandardExplore(
			@RequestParam(required = false) List<String> keywords,
			@RequestParam(required = false, defaultValue = "20") Integer size,
			@RequestParam(required = false, defaultValue = "0") Long cursor
	) {
		List<String> safeKeywords = keywords != null ? keywords : Collections.emptyList();
		Long userId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
		Pair<Long, CursorResponse<ReviewExploreItem>> pair = reviewExploreService.getStandardExplore(userId, safeKeywords, cursor, size);
		return GlobalResponse.ok(pair, Map.of("keywords", safeKeywords));
	}
}
