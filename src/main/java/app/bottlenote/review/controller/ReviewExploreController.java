package app.bottlenote.review.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.service.ReviewExploreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reviews/explore")
public class ReviewExploreController {

	private final ReviewExploreService reviewExploreService;

	@GetMapping("/standard")
	public ResponseEntity<?> getStandardExplore(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false, defaultValue = "20") Integer size,
			@RequestParam(required = false, defaultValue = "0") Long cursor
	) {

		Long userId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
		//Pair<Long, PageResponse<List<AlcoholDetailItem>>> pair = reviewExploreService.getStandardExplore(userId, keyword, cursor, size);

		return GlobalResponse.ok(null);
	}
}
