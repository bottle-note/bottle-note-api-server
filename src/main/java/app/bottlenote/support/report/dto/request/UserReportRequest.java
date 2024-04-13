package app.bottlenote.support.report.dto.request;

import app.bottlenote.support.report.domain.constant.UserReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserReportRequest(
	@NotNull(message = "신고자 아이디는 필수입니다.")
	Long userId,

	@NotNull(message = "신고 대상자 아이디는 필수입니다.")
	Long reportUserId,

	@NotNull(message = "신고 타입이 적절하지 않습니다. ( SPAM , INAPPROPRIATE_CONTENT ,FRAUD ,COPYRIGHT_INFRINGEMENT ,OTHER )")
	UserReportType type,

	@NotBlank(message = "신고 내용은 필수입니다.")
	@Size(max = 300, message = "신고 내용은 300자 이내로 작성해주세요.")
	String content) {
}
