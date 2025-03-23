package app.bottlenote.user.dto.response;

import app.bottlenote.user.dto.constant.WithdrawUserResultMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record WithdrawUserResultResponse(
	WithdrawUserResultMessage codeMessage,
	String message,
	Long userId,
	String responseAt
) {

	public static WithdrawUserResultResponse response(
		WithdrawUserResultMessage message,
		Long userId
	) {
		return new WithdrawUserResultResponse(
			message,
			message.getMessage(),
			userId,
			LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
		);
	}
}
