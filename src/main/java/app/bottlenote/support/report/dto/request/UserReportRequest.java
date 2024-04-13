package app.bottlenote.support.report.dto.request;


import jakarta.validation.constraints.NotBlank;

public record UserReportRequest(
	@NotBlank(message = "신고자 아이디는 필수입니다.")
	String user_id,
	@NotBlank(message = "신고 대상자 아이디는 필수입니다.")
	String report_user_id,
	@NotBlank(message = "신고 타입은 필수입니다.")
	String type,
	@NotBlank(message = "신고 내용은 필수입니다.")
	String content) {
}
