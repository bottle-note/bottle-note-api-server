package app.bottlenote.support.help.dto.response;

import app.bottlenote.support.help.dto.response.constant.HelpResultMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record HelpResultResponse(
	HelpResultMessage codeMessage,
	String message,
	Long helpId,
	String responseAt
) {

	public static HelpResultResponse response(
		HelpResultMessage message,
		Long helpId
	) {
		return new HelpResultResponse(
			message,
			message.getDescription(),
			helpId,
			LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
		);
	}

}
