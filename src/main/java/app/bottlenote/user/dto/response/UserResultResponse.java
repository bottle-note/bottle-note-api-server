package app.bottlenote.user.dto.response;

import app.bottlenote.user.dto.response.constant.UserResultMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record UserResultResponse(
	UserResultMessage codeMessage,
	String message,
	Long userId,
	String responseAt
) {

	public static UserResultResponse response(
		UserResultMessage message,
		Long userId
	) {
		return new UserResultResponse(
			message,
			message.getMessage(),
			userId,
			LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
		);
	}
}
