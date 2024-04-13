package app.bottlenote.support.report.domain.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserReportType {
	SPAM("스팸"),
	INAPPROPRIATE_CONTENT("부적절한 콘텐츠"),
	FRAUD("사기"),
	COPYRIGHT_INFRINGEMENT("저작권 침해"),
	OTHER("기타");

	private final String status;
}
