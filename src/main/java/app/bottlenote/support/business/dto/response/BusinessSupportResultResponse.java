package app.bottlenote.support.business.dto.response;

import app.bottlenote.support.business.constant.BusinessResultMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record BusinessSupportResultResponse(
		BusinessResultMessage codeMessage,
		String message,
		Long id,
		String responseAt
) {
	public static BusinessSupportResultResponse response(BusinessResultMessage msg, Long id) {
		return new BusinessSupportResultResponse(
				msg,
				msg.getDescription(),
				id,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
		);
	}
}
