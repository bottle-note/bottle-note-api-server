package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewReplyRegisterRequest(
	@NotBlank(message = "REVIEW_REPLY_CONTENT_REQUIRED")
	@Size(min = 1, max = 500, message = "REVIEW_CONTENT_SIZE_MIN_MAX")
	String content,
	
	Long parentReplyId
) {
}
