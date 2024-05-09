package app.bottlenote.review.controller;

import static app.bottlenote.global.data.response.GlobalResponse.success;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	@GetMapping("/{alcoholId}")
	public ResponseEntity<GlobalResponse> getReviews(@PathVariable Long alcoholId) {
		return ResponseEntity.ok(success(
			reviewService.getReviews(alcoholId)
		));
	}


}
