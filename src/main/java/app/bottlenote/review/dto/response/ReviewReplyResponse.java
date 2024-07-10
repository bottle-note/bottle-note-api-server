package app.bottlenote.review.dto.response;

import app.bottlenote.review.dto.response.constant.ReviewReplyResultMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ReviewReplyResponse(
	ReviewReplyResultMessage codeMessage,
	String message,
	Long reviewId,
	String responseAt
) {
	public static ReviewReplyResponse of(
		ReviewReplyResultMessage message,
		Long reviewId
	) {
		return new ReviewReplyResponse(
			message,
			message.getMessage(),
			reviewId,
			LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
		);
	}
}
