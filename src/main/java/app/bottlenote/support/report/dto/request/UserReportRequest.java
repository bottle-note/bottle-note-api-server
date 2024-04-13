package app.bottlenote.support.report.dto.request;

import app.bottlenote.support.report.domain.constant.UserReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserReportRequest(
	@NotBlank(message = "신고자 아이디는 필수입니다.")
	String userId,

	@NotBlank(message = "신고 대상자 아이디는 필수입니다.")
	String reportUserId,

	@NotNull(message = "신고 타입이 적절하지 않습니다. ( SPAM , INAPPROPRIATE_CONTENT ,FRAUD ,COPYRIGHT_INFRINGEMENT ,OTHER )")
	UserReportType type,

	@NotBlank(message = "신고 내용은 필수입니다.")
	String content) {
}
