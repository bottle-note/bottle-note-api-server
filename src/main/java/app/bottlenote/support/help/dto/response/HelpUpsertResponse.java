package app.bottlenote.support.help.dto.response;

import app.bottlenote.support.help.dto.response.constant.HelpResultMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record HelpUpsertResponse(
	HelpResultMessage codeMessage,
	String message,
	Long helpId,
	String responseAt
) {

	public static HelpUpsertResponse response(
		HelpResultMessage message,
		Long helpId
	) {
		return new HelpUpsertResponse(
			message,
			message.getDescription(),
			helpId,
			LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
		);
	}

}
