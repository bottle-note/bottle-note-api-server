package app.bottlenote.support.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class UserReportResponse {
	private final String message; //: "신고가 접수되었습니다.",
	private final Long report_user_id; //: 3,
	private final String report_user_name; //: "신고 대상 이름"

	protected UserReportResponse(String message, Long report_user_id, String report_user_name) {
		this.message = message;
		this.report_user_id = report_user_id;
		this.report_user_name = report_user_name;
	}

	public static UserReportResponse of(Long report_user_id, String report_user_name) {
		return new UserReportResponse(UserReportResponseEnum.SUCCESS.getMessage(), report_user_id, report_user_name);
	}

	@AllArgsConstructor
	@Getter
	public enum UserReportResponseEnum {
		SUCCESS("신고가 성공적으로 접수되었습니다.");
		private final String message;
	}
}
