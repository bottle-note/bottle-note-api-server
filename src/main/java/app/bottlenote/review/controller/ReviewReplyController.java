package app.bottlenote.review.controller;

import app.bottlenote.review.service.ReviewReplyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/review/reply")
public class ReviewReplyController {

	private final ReviewReplyService reviewReplyService;

	public ReviewReplyController(ReviewReplyService reviewReplyService) {
		this.reviewReplyService = reviewReplyService;
	}

	@GetMapping
	public String getReviewReply() {
		reviewReplyService.saveReviewReply();
		return "getReviewReply";
	}
}
