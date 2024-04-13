package app.bottlenote.support.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class UserReportResponse {
	private final String message; //: "신고가 접수되었습니다.",
	private final Long reportUserId; //: 3,
	private final String reportUserName; //: "신고 대상 이름"

	protected UserReportResponse(String message, Long reportUserId, String reportUserName) {
		this.message = message;
		this.reportUserId = reportUserId;
		this.reportUserName = reportUserName;
	}

	public static UserReportResponse of(
		UserReportResponseEnum message
		, Long reportUserId
		, String reportUserName) {
		return new UserReportResponse(message.getMessage(), reportUserId, reportUserName);
	}

	@AllArgsConstructor
	@Getter
	public enum UserReportResponseEnum {
		SUCCESS("신고가 성공적으로 접수되었습니다."),
		DUPLICATE("이미 신고한 사용자입니다."),
		SAME_USER("자신을 신고할 수 없습니다.");
		private final String message;
	}
}
