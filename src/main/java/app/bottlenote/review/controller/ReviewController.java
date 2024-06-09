package app.bottlenote.review.controller;

import static app.bottlenote.global.data.response.GlobalResponse.success;
import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.service.ReviewService;
import app.bottlenote.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	@PostMapping
	public ResponseEntity<GlobalResponse> createReviews(@RequestBody ReviewCreateRequest reviewCreateRequest) {
		Long currentUserId = SecurityContextUtil.getUserIdByContext().
			orElseThrow(() -> new UserException(USER_NOT_FOUND));

		ReviewCreateResponse createdReview = reviewService.createReviews(reviewCreateRequest, currentUserId);
		return ResponseEntity.ok(success(createdReview));
	}

	@GetMapping("/{alcoholId}")
	public ResponseEntity<GlobalResponse> getReviews(@PathVariable Long alcoholId, @ModelAttribute PageableRequest pageableRequest) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(null);

		log.info("currentUserId is : {} \nPageable INFO  : {}", currentUserId, pageableRequest.toString());

		PageResponse<ReviewResponse> pageResponse = reviewService.getReviews(
			alcoholId,
			pageableRequest,
			currentUserId
		);

		return ResponseEntity.ok(
			success(pageResponse.content(), MetaService.createMetaInfo().add("pageable", pageResponse.cursorPageable())
			));
	}

	@GetMapping("/me/{alcoholId}")
	public ResponseEntity<GlobalResponse> getMyReview(@ModelAttribute PageableRequest pageableRequest, @PathVariable Long alcoholId) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().orElseThrow(
			() -> new UserException(USER_NOT_FOUND)
		);

		PageResponse<ReviewResponse> myReview = reviewService.getMyReview(alcoholId, pageableRequest, currentUserId);

		return ResponseEntity.ok(
			success(myReview.content(), MetaService.createMetaInfo().add("pageable", myReview.cursorPageable()))
		);
	}
}
