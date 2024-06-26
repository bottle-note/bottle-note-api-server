package app.bottlenote.review.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ReviewReplyResultResponse(
	ReviewReplyResultMessage codeMessage,
	String message,
	Long reviewId,
	String responseAt
) {
	public static ReviewReplyResultResponse response(
		ReviewReplyResultMessage message,
		Long reviewId
	) {
		return new ReviewReplyResultResponse(
			message,
			message.getMessage(),
			reviewId,
			LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
		);
	}
}
