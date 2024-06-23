package app.bottlenote.review.controller;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.review.service.ReviewReplyService;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/review/reply")
public class ReviewReplyController {

	private final ReviewReplyService reviewReplyService;

	public ReviewReplyController(ReviewReplyService reviewReplyService) {
		this.reviewReplyService = reviewReplyService;
	}

	@PostMapping("/register/{reviewId}")
	public ResponseEntity<?> registerReviewReply(
		@PathVariable Long reviewId,
		@RequestBody @Valid ReviewReplyRegisterRequest request
	) {
		Long userId = SecurityContextUtil.getUserIdByContext()
			.orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));

		Object o = reviewReplyService.registerReviewReply(reviewId, userId, request);
		return ResponseEntity.ok(o);
	}

	@GetMapping
	public String getReviewReply() {
		reviewReplyService.saveReviewReply();
		return "getReviewReply";
	}
}
