package app.bottlenote.review.controller;

import static app.bottlenote.global.data.response.GlobalResponse.success;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
	public ResponseEntity<GlobalResponse> getReviews(
		@PathVariable Long alcoholId,
		@ModelAttribute PageableRequest pageableRequest) {

		Long currentUserId = SecurityUtil.getCurrentUserId();

		log.info("curruntUserId is : {}", currentUserId);
		log.info("Pageable INFO  : {}", pageableRequest.toString());

		PageResponse<ReviewResponse> pageResponse = reviewService.getReviews(alcoholId,
			pageableRequest,
			currentUserId);

		return ResponseEntity.ok(success(

			pageResponse.content(),
			MetaService.createMetaInfo()
				.add("pageable", pageResponse.cursorPageable())
		));
	}


}
