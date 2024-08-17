package app.bottlenote.support.report.dto.request;

import app.bottlenote.support.report.domain.constant.UserReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserReportRequest(
	@NotNull(message = "REQUIRED_USER_ID")
	Long userId,

	@NotNull(message = "REPORT_TARGET_USER_ID_REQUIRED")
	Long reportUserId,

	@NotNull(message = "REPORT_TYPE_NOT_VALID")
	UserReportType type,

	@NotBlank(message = "NOT_BLANK")
	@Size(max = 300, message = "REPORT_CONTENT_MAX_SIZE")
	String content) {
}
